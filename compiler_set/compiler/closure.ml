type closure = { entry : Id.l; actual_fv : Id.t list }
type t = (* クロージャ変換後の式 *)
  | Unit
  | Int of int
  | Float of float
  | Neg of Id.t
  | Add of Id.t * Id.t
  | Sub of Id.t * Id.t
  | Mul of Id.t * Id.t
  | Sll of Id.t * int
  | Sra of Id.t * int
  | FNeg of Id.t
  | FAdd of Id.t * Id.t
  | FSub of Id.t * Id.t
  | FMul of Id.t * Id.t
  | FDiv of Id.t * Id.t
  | IfEq of Id.t * Id.t * t * t
  | IfLE of Id.t * Id.t * t * t
  | IfLT of Id.t * Id.t * t * t
  | Let of (Id.t * Type.t) * t * t
  | Var of Id.t
  | MakeCls of (Id.t * Type.t) * closure * t
  | AppCls of Id.t * Id.t list
  | AppDir of Id.l * Id.t list
  | Tuple of Id.t list
  | LetTuple of (Id.t * Type.t) list * Id.t * t
  | Get of Id.t * Id.t
  | Put of Id.t * Id.t * Id.t
  | GetTuple of Id.t * Id.t 
  | PutTuple of Id.t * Id.t * Id.t list
  | ExtArray of Id.l
type fundef = { name : Id.l * Type.t;
		args : (Id.t * Type.t) list;
		formal_fv : (Id.t * Type.t) list;
		body : t }
type prog = Prog of fundef list * t

let rec fv = function
  | Unit | Int(_) | Float(_) | ExtArray(_) -> S.empty
  | Neg(x) | FNeg(x) | Sll(x, _) | Sra(x, _) -> S.singleton x
  | Add(x, y) | Sub(x, y) | Mul(x, y) | FAdd(x, y) | FSub(x, y) | FMul(x, y) | FDiv(x, y) | Get(x, y) | GetTuple(x, y) -> S.of_list [x; y]
  | IfEq(x, y, e1, e2)| IfLE(x, y, e1, e2) | IfLT (x, y, e1, e2) -> S.add x (S.add y (S.union (fv e1) (fv e2)))
  | Let((x, t), e1, e2) -> S.union (fv e1) (S.remove x (fv e2))
  | Var(x) -> S.singleton x
  | MakeCls((x, t), { entry = l; actual_fv = ys }, e) -> S.remove x (S.union (S.of_list ys) (fv e))
  | AppCls(x, ys) -> S.of_list (x :: ys)
  | AppDir(_, xs) | Tuple(xs) -> S.of_list xs
  | LetTuple(xts, y, e) -> S.add y (S.diff (fv e) (S.of_list (List.map fst xts)))
  | Put(x, y, z) -> S.of_list [x; y; z]
  | PutTuple(x,y,z) -> S.of_list (x::y::z)

let toplevel : fundef list ref = ref []

(* クロージャ変換ルーチン本体 *)
let rec g env known = function
  | ANormal.Let((x, t), exp, e) ->
      Let((x, t), g' env known exp, g (M.add x t env) known e)
  | ANormal.LetRec({ ANormal.name = (x, t); ANormal.args = yts; ANormal.body = e1 }, e2) -> (* 関数定義の場合 *)
      (* 関数定義let rec x y1 ... yn = e1 in e2の場合は、
	 xに自由変数がない(closureを介さずdirectに呼び出せる)
	 と仮定し、knownに追加してe1をクロージャ変換してみる *)
      let toplevel_backup = !toplevel in
      let env' = M.add x t env in
      let known' = S.add x known in
      let e1' = g (M.add_list yts env') known' e1 in
      (* 本当に自由変数がなかったか、変換結果e1'を確認する *)
      (* 注意: e1'にx自身が変数として出現する場合はclosureが必要! *)
      let zs = S.diff (fv e1') (S.of_list (List.map fst yts)) in
      let known', e1' =
	if S.is_empty zs then known', e1' else
	(* 駄目だったら状態(toplevelの値)を戻して、クロージャ変換をやり直す *)
	(toplevel := toplevel_backup;
	 let e1' = g (M.add_list yts env') known e1 in
	 known, e1') in
      let zs = S.elements (S.diff (fv e1') (S.add x (S.of_list (List.map fst yts)))) in (* 自由変数のリスト *)
      let zts = List.map (fun z -> (z, M.find z env')) zs in (* ここで自由変数zの型を引くために引数envが必要 *)
      toplevel := { name = (Id.L(x), t); args = yts; formal_fv = zts; body = e1' } :: !toplevel; (* トップレベル関数を追加 *)
      let e2' = g env' known' e2 in
      if S.mem x (fv e2') then (* xが変数としてe2'に出現するか *)
	MakeCls((x, t), { entry = Id.L(x); actual_fv = zs }, e2') (* 出現していたら削除しない *)
      else e2' (* 出現しなければMakeClsを削除 *)
  | ANormal.LetTuple(xts, y, e) ->
      LetTuple(xts, y, g (M.add_list xts env) known e)
  | ANormal.Ans(exp) -> g' env known exp
and g' env known =  function 
  | ANormal.Unit -> Unit
  | ANormal.Int(i) -> Int(i)
  | ANormal.Float(d) -> Float(d)
  | ANormal.Neg(x) -> Neg(x)
  | ANormal.Add(x, y) -> Add(x, y)
  | ANormal.Sub(x, y) -> Sub(x, y)
  | ANormal.Mul(x, y) -> Mul(x, y)
  | ANormal.Sll(x, y) -> Sll(x, y)
  | ANormal.Sra(x, y) -> Sra(x, y)
  | ANormal.FNeg(x) -> FNeg(x)
  | ANormal.FAdd(x, y) -> FAdd(x, y)
  | ANormal.FSub(x, y) -> FSub(x, y)
  | ANormal.FMul(x, y) -> FMul(x, y)
  | ANormal.FDiv(x, y) -> FDiv(x, y)
  | ANormal.IfEq(x, y, e1, e2) -> IfEq(x, y, g env known e1, g env known e2)
  | ANormal.IfLE(x, y, e1, e2) -> IfLE(x, y, g env known e1, g env known e2)
  | ANormal.IfLT(x, y, e1, e2) -> IfLT(x, y, g env known e1, g env known e2)
  | ANormal.Var(x) -> Var(x)
  | ANormal.App(x, ys) when S.mem x known -> (* 関数適用の場合 *)
      AppDir(Id.L(x), ys)
  | ANormal.App(f, xs) -> AppCls(f, xs)
  | ANormal.Tuple(xs) -> Tuple(xs)
  | ANormal.Get(x, y) -> Get(x, y)
  | ANormal.Put(x, y, z) -> Put(x, y, z)
  | ANormal.ExtArray(x) -> ExtArray(Id.L(x))
  | ANormal.ExtFunApp(x, ys) -> AppDir(Id.L("min_caml_" ^ x), ys)

let f e =
  Format.eprintf "making closures...@.";
  toplevel := [];
  let e' = g M.empty S.empty e in
  Prog(List.rev !toplevel, e')



(**********************************************************************)
(* デバッグ用関数. tを出力. nは深さ. *)
let rec ind m = if m <= 0 then ()
                else (Printf.eprintf "  "; ind (m-1))
let rec dbprint n t =
  ind n;
  match t with
  | Unit -> Printf.eprintf "Unit\n%!"
  | Int a -> Printf.eprintf "Int %d\n%!" a
  | Float a-> Printf.eprintf "Float %f\n%!" a
  | Neg a -> Printf.eprintf "Neg %s\n%!" a
  | Add (a, b) -> Printf.eprintf "Add %s %s\n%!" a b
  | Sub (a, b) -> Printf.eprintf "Sub %s %s\n%!" a b
  | Mul (a, b) -> Printf.eprintf "Mul %s %s\n%!" a b
  | Sll (a, b) -> Printf.eprintf "Sll %s %d\n%!" a b
  | Sra (a, b) -> Printf.eprintf "Sar %s %d\n%!" a b
  | FNeg a -> Printf.eprintf "FNeg %s\n%!" a
  | FAdd (a, b) -> Printf.eprintf "FAdd %s %s\n%!" a b
  | FSub (a, b) -> Printf.eprintf "FSub %s %s\n%!" a b
  | FMul (a, b) -> Printf.eprintf "FMul %s %s\n%!" a b
  | FDiv (a, b) -> Printf.eprintf "FDiv %s %s\n%!" a b
  | IfEq (a, b, p, q) -> Printf.eprintf "If %s = %s Then\n%!" a b;
                         dbprint (n+1) p; ind n; Printf.eprintf "Else\n%!"; dbprint (n+1) q
  | IfLE (a, b, p, q) -> Printf.eprintf "If %s <= %s Then\n%!" a b;
                         dbprint (n+1) p; ind n; Printf.eprintf "Else\n%!"; dbprint (n+1) q
  | IfLT (a, b, p, q) -> Printf.eprintf "If %s < %s Then\n%!" a b;
                         dbprint (n+1) p; ind n; Printf.eprintf "Else\n%!"; dbprint (n+1) q
  | Let ((a, t), b, c) -> Printf.eprintf "Let (%s:%s) =\n%!" a (Type.show t);
                          dbprint (n+1) b; ind n; Printf.eprintf "In\n%!"; dbprint n c
  | Var a -> Printf.eprintf "Var %s\n%!" a
  | MakeCls ((a, t), c, e) ->
      (match c.entry with Id.L l ->
	Printf.eprintf "MakeCls %s:%s (L %s, FV={%s}) =\n%!" a (Type.show t) l (String.concat "," c.actual_fv);
	dbprint n e)
  | AppCls (a, l) -> Printf.eprintf "AppCls %s to %s\n%!" a (String.concat " " l)
  | AppDir ((Id.L a), l) -> Printf.eprintf "AppDir L %s to %s\n%!" a (String.concat " " l)
  | Tuple l -> Printf.eprintf "Tuple (%s)\n%!" (String.concat " , " l)
  | LetTuple (l, a, b) -> Printf.eprintf "LetTuple (%s) = %s in\n%!" (String.concat "," (List.map (fun (x,y) -> "(" ^ x ^ ":" ^ Type.show y ^ ")") l)) a;
                          dbprint n b
  | Get (a, b) -> Printf.eprintf "Get %s %s \n%!" a b
  | Put (a, b, c) -> Printf.eprintf "Put %s %s %s\n%!" a b c
  | GetTuple (a, b) -> Printf.eprintf "GetTuple %s %s\n%!" a b
  | PutTuple (a, b, c) -> Printf.eprintf "PutTuple %s %s %s\n%!" a b ("( " ^ (String.concat " , " c) ^ " )")
  | ExtArray (Id.L a) -> Printf.eprintf "ExtArray L %s\n%!" a

(* デバッグ用関数. fundefを出力. *)
let dbprint2 f =
  ind 1;
  match f.name with
    (Id.L a, b) -> Printf.eprintf "%s:\t%s (Args = (%s), FV = {%s}) =\n%!" a (Type.show b) (String.concat "," (List.map (fun (x,y) -> x^":"^(Type.show y)) f.args)) (String.concat "," (List.map (fun (x,y) -> x^":"^(Type.show y)) f.formal_fv));
           dbprint 2 f.body;
      Printf.eprintf "\n%!"
