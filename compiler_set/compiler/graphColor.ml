open Asm

(* グラフ彩色によるレジスタ割り当て *)

(* ノードの状態を示すデータ型 *)
type info =
  | Colored of Id.t
  | Prefer of Id.t list

(* デバッグ用関数 *)
let print_env env =
  M.iter (fun x y -> Format.eprintf "%s:%s " x y) env;
  Format.eprintf "@."
let print_graph g =
  M.iter (fun x y -> Format.eprintf "%s : " x; List.iter (Format.eprintf "%s,") y; Format.eprintf "@.") g;
  Format.eprintf "@."
let print_colored_graph g =
  let q = function
    | Colored c -> c
    | Prefer p -> "p("^String.concat "," p^")" in
  M.iter (fun x (y,_) -> Format.eprintf "%s:%s " x (q y)) g;
  Format.eprintf "@."

(* 命令列(分岐,関数呼び出しなし)に対し整数,浮動小数のグラフを構成 *)
let addn x l m =
  try let k = M.find x m in M.add x (remove_and_uniq S.empty (l@k)) m
  with Not_found -> M.add x l m
let rec make' rr rf = function
  | Ans(exp) -> (rr, rf)
  | Let((x,t), _, e) ->
      let rr' =
	match t with
	| Type.Float | Type.Unit -> rr
	| _ ->
	    let fvs = remove_and_uniq (S.singleton x) (fv_int e) in
	    List.fold_left
	      (fun r y -> addn y [x] r)
	      (addn x fvs rr)
	      fvs in
      let rf' =	
	match t with
	| Type.Float ->
	    let fvs = remove_and_uniq (S.singleton x) (fv_float e) in
	    List.fold_left
	      (fun r y -> addn y [x] r)
	      (addn x fvs rf)
	      fvs
	| _ -> rf in
      make' rr' rf' e
let make e =
  (* まず,一番最初に生きている変数で完全グラフを作る *)
  let rr =
    List.fold_left
      (fun r y -> addn y (remove_and_uniq (S.singleton y) (fv_int e)) r)
      M.empty
      (fv_int e) in
  let rf =
    List.fold_left
      (fun r y -> addn y (remove_and_uniq (S.singleton y) (fv_float e)) r)
      M.empty
      (fv_float e) in
  make' rr rf e
	

(* グラフのノードを次数の大きい順に消して,全ての次数をn未満にする *)
let take_max g =
  M.fold
    (fun x l (y,k) -> if List.length l > List.length k then (x,l) else (y,k))
    g
    (M.choose g)
let rem x g = M.map (List.filter (fun y -> x <> y)) (M.remove x g)
let spill g n = 
  let rec spill' g s =
    try
      let (m,ml) = take_max g in
      if List.length ml < n then (g, s)
      else spill' (rem m g) (m::s) 
    with Not_found -> (g, s) in
  let (g', s) = spill' g [] in
  (g', List.fold_left (fun r y -> M.add y (M.find y g) r) M.empty s)



(* グラフの彩色 *)

let able c l g =
  List.for_all
    (fun x -> match M.find x g with
              | (Colored(d),_) when c = d -> false
	      | _ -> true)
    l 
let color cs regenv prefer g s = 
  (* まずregenv,preferにしたがって情報を付加 *)
  let g1 =
    M.mapi
      (fun x l ->
	let y =
	  if is_reg x then Colored(x) else
	  try Colored (M.find x regenv)
	  with Not_found ->
	    let k =
	      try List.filter (fun x -> M.mem x g || List.mem x cs) (M.find x prefer)
	      with Not_found -> [] in
	    Prefer k in
	(y, l))
      g in
  let s1 =
    M.mapi
      (fun x l ->
	let y =
	  if is_reg x then Colored(x) else
	  let k =
	    try M.find x regenv::List.filter (fun x -> M.mem x g || List.mem x cs) (M.find x prefer)
	    with Not_found -> try [M.find x regenv]
	    with Not_found -> try (List.filter (fun x -> List.mem x allfregs) (M.find x prefer))
	    with Not_found -> [] in
	  Prefer k in
	(y, l))
      s in

  (* ノードに対し彩色する補助関数 *)
  (* 1次版 *)
  let col1 x y g =
    match y with
    | (Colored _, _) -> M.add x y g
    | (Prefer p, l) ->
        let (p',q') = List.partition (fun x -> is_reg x) p in
	let y =
	  try Colored(List.find (fun c -> able c l g) p')
	  with Not_found -> Prefer q' in
        M.add x (y, l) g in
  (* 2次版 *)
  let col2 x y g =
    match y with
    | (Colored _, _) -> M.add x y g
    | (Prefer p, l) ->
        let q' = List.fold_left
	            (fun q' x -> match M.find x g with
	                         | (Colored(c),_) -> c::q'
		                 | _ -> q')
	            []
  	            p in
        M.add x (Colored(List.find (fun c -> able c l g) (q'@cs)), l) g in

  (* グラフをpreferにしたがって彩色 *)
  let g2 = M.fold col1 g1 g1 in
  let g3 = M.fold col2 g2 g2 in

  (* spillされた変数のうち,実は彩色できるものを彩色.残りは後で適当に塗る。 *)
  let el = function
    | (Prefer p, l) -> (Prefer (List.filter is_reg p), l)
    | c -> c in
  let g4 = M.fold col1 s1 g3 in
  let g5 =
    M.fold
      (fun x y g2' -> (try col2 x y g2' with Not_found -> M.add x (el y) g2'))
      g4
      g4 in
  M.map fst g5


