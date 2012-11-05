type closure = { entry : Id.l; actual_fv : Id.t list }
type t = (* �����������Ѵ���μ� *)
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
  | Add(x, y) | Sub(x, y) | Mul(x, y) | FAdd(x, y) | FSub(x, y) | FMul(x, y) | FDiv(x, y) | Get(x, y) -> S.of_list [x; y]
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

let rec g env known = function (* �����������Ѵ��롼�������� *)
  | KNormal.Unit -> Unit
  | KNormal.Int(i) -> Int(i)
  | KNormal.Float(d) -> Float(d)
  | KNormal.Neg(x) -> Neg(x)
  | KNormal.Add(x, y) -> Add(x, y)
  | KNormal.Sub(x, y) -> Sub(x, y)
  | KNormal.Mul(x, y) -> Mul(x, y)
  | KNormal.Sll(x, y) -> Sll(x, y)
  | KNormal.Sra(x, y) -> Sra(x, y)
  | KNormal.FNeg(x) -> FNeg(x)
  | KNormal.FAdd(x, y) -> FAdd(x, y)
  | KNormal.FSub(x, y) -> FSub(x, y)
  | KNormal.FMul(x, y) -> FMul(x, y)
  | KNormal.FDiv(x, y) -> FDiv(x, y)
  | KNormal.IfEq(x, y, e1, e2) -> IfEq(x, y, g env known e1, g env known e2)
  | KNormal.IfLE(x, y, e1, e2) -> IfLE(x, y, g env known e1, g env known e2)
  | KNormal.IfLT(x, y, e1, e2) -> IfLT(x, y, g env known e1, g env known e2)
  | KNormal.Let((x, t), e1, e2) ->
      Let((x, t), g env known e1, g (M.add x t env) known e2)
  | KNormal.Var(x) -> Var(x)
  | KNormal.LetRec({ KNormal.name = (x, t); KNormal.args = yts; KNormal.body = e1 }, e2) -> (* �ؿ�����ξ�� *)
      (* �ؿ����let rec x y1 ... yn = e1 in e2�ξ��ϡ�
	 x�˼�ͳ�ѿ����ʤ�(closure��𤵤�direct�˸ƤӽФ���)
	 �Ȳ��ꤷ��known���ɲä���e1�򥯥��������Ѵ����Ƥߤ� *)
      let toplevel_backup = !toplevel in
      let env' = M.add x t env in
      let known' = S.add x known in
      let e1' = g (M.add_list yts env') known' e1 in
      (* �����˼�ͳ�ѿ����ʤ��ä������Ѵ����e1'���ǧ���� *)
      (* ����: e1'��x���Ȥ��ѿ��Ȥ��ƽи��������closure��ɬ��! *)
      let zs = S.diff (fv e1') (S.of_list (List.map fst yts)) in
      let known', e1' =
	if S.is_empty zs then known', e1' else
	(* ���ܤ��ä������(toplevel����)���ᤷ�ơ������������Ѵ�����ľ�� *)
	(toplevel := toplevel_backup;
	 let e1' = g (M.add_list yts env') known e1 in
	 known, e1') in
      let zs = S.elements (S.diff (fv e1') (S.add x (S.of_list (List.map fst yts)))) in (* ��ͳ�ѿ��Υꥹ�� *)
      let zts = List.map (fun z -> (z, M.find z env')) zs in (* �����Ǽ�ͳ�ѿ�z�η����������˰���env��ɬ�� *)
      toplevel := { name = (Id.L(x), t); args = yts; formal_fv = zts; body = e1' } :: !toplevel; (* �ȥåץ�٥�ؿ����ɲ� *)
      let e2' = g env' known' e2 in
      if S.mem x (fv e2') then (* x���ѿ��Ȥ���e2'�˽и����뤫 *)
	MakeCls((x, t), { entry = Id.L(x); actual_fv = zs }, e2') (* �и����Ƥ����������ʤ� *)
      else e2' (* �и����ʤ����MakeCls���� *)
  | KNormal.App(x, ys) when S.mem x known -> (* �ؿ�Ŭ�Ѥξ�� *)
      AppDir(Id.L(x), ys)
  | KNormal.App(f, xs) -> AppCls(f, xs)
  | KNormal.Tuple(xs) -> Tuple(xs)
  | KNormal.LetTuple(xts, y, e) ->
      LetTuple(xts, y, g (M.add_list xts env) known e)
  | KNormal.Get(x, y) -> Get(x, y)
  | KNormal.Put(x, y, z) -> Put(x, y, z)
  | KNormal.ExtArray(x) -> ExtArray(Id.L(x))
  | KNormal.ExtFunApp(x, ys) -> AppDir(Id.L("min_caml_" ^ x), ys)

let f e =
  Format.eprintf "making closures...@.";
  toplevel := [];
  let e' = g M.empty S.empty e in
  Prog(List.rev !toplevel, e')



(**********************************************************************)
(* �ǥХå��Ѵؿ�. t�����. n�Ͽ���. *)
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
  | PutTuple (a, b, c) -> Printf.eprintf "Put %s %s %s\n%!" a b ("( " ^ (String.concat " , " c) ^ " )")
  | ExtArray (Id.L a) -> Printf.eprintf "ExtArray L %s\n%!" a

(* �ǥХå��Ѵؿ�. fundef�����. *)
let dbprint2 f =
  ind 1;
  match f.name with
    (Id.L a, b) -> Printf.eprintf "%s:\t%s (Args = (%s), FV = {%s}) =\n%!" a (Type.show b) (String.concat "," (List.map (fun (x,y) -> x^":"^(Type.show y)) f.args)) (String.concat "," (List.map (fun (x,y) -> x^":"^(Type.show y)) f.formal_fv));
           dbprint 2 f.body;
      Printf.eprintf "\n%!"