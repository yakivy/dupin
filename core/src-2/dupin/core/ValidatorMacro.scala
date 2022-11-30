package dupin.core

import cats.Functor
import dupin.core.Validator.PartiallyAppliedCombineP
import dupin.core.Validator.PartiallyAppliedCombinePC
import dupin.core.Validator.PartiallyAppliedCombinePL
import dupin.core.Validator.PartiallyAppliedCombinePR
import dupin.core.Validator.PartiallyAppliedCombinePRF
import scala.reflect.macros.blackbox

object ValidatorMacro {
    def comapPImpl[F[_], E, A, AA](
        c: blackbox.Context)(f: c.Expr[A => AA])(F: c.Expr[Functor[F]]
    ): c.Expr[Validator[F, E, AA]] = {
        import c.universe._
        c.Expr(q"""${c.prefix}.comapPE(${getFieldPath(c)(f)}, $f)""")
    }

    def combinePImpl[F[_], E, A, AA](
        c: blackbox.Context)(f: c.Expr[A => AA]
    ): c.Expr[PartiallyAppliedCombineP[F, E, A, AA]] = {
        import c.universe._
        c.Expr(q"""_root_.dupin.Validator.PartiallyAppliedCombineP(${c.prefix}, ${getFieldPath(c)(f)}, $f)""")
    }

    def combinePCImpl[F[_], E, A, AA](
        c: blackbox.Context)(f: c.Expr[A => AA]
    ): c.Expr[PartiallyAppliedCombinePC[F, E, A, AA]] = {
        import c.universe._
        c.Expr(q"""_root_.dupin.Validator.PartiallyAppliedCombinePC(${c.prefix}, ${getFieldPath(c)(f)}, $f)""")
    }

    def combinePLImpl[F[_], E, A, G[_], AA](
        c: blackbox.Context)(f: c.Expr[A => G[AA]]
    ): c.Expr[PartiallyAppliedCombinePL[F, E, A, G, AA]] = {
        import c.universe._
        c.Expr(q"""_root_.dupin.Validator.PartiallyAppliedCombinePL(${c.prefix}, ${getFieldPath(c)(f)}, $f)""")
    }

    def combinePRImpl[F[_], E, A, AA](
        c: blackbox.Context)(f: c.Expr[A => AA]
    ): c.Expr[PartiallyAppliedCombinePR[F, E, A, AA]] = {
        import c.universe._
        c.Expr(q"""_root_.dupin.Validator.PartiallyAppliedCombinePR(${c.prefix}, ${getFieldPath(c)(f)}, $f)""")
    }

    def combinePRFImpl[F[_], E, A, AA](
        c: blackbox.Context)(f: c.Expr[A => AA]
    ): c.Expr[PartiallyAppliedCombinePRF[F, E, A, AA]] = {
        import c.universe._
        c.Expr(q"""_root_.dupin.Validator.PartiallyAppliedCombinePRF(${c.prefix}, ${getFieldPath(c)(f)}, $f)""")
    }

    def combinePIImpl[F[_], E, A, AA](
        c: blackbox.Context)(f: c.Expr[A => AA])(V: c.Expr[Validator[F, E, AA]]
    ): c.Expr[Validator[F, E, A]] = {
        import c.universe._
        c.Expr(q"""${c.prefix}.combinePE(${getFieldPath(c)(f)}, $f)($V)""")
    }

    private def getFieldPath(
        c: blackbox.Context)(
        f: c.Expr[_ => _]
    ): c.Tree = {
        import c.universe._
        def abort = c.abort(c.enclosingPosition, s"Unable to retrieve field name from function ${f.tree}")
        def rec(argName: Name, selects: Tree, acc: Tree): Tree = selects match {
            case Ident(identName) if identName == argName => acc
            case Select(qualifier, name) =>
                q"${rec(argName, qualifier, acc)}.append(_root_.dupin.core.FieldPart(${name.decodedName.toString}))"
            case _ => abort
        }
        f.tree match {
            case Function(List(ValDef(_, argName, _, _)), selects) => rec(argName, selects, q"_root_.dupin.Path.empty")
            case _ => abort
        }
    }

    def deriveImpl[F[_], E, A](
        c: blackbox.Context)(implicit FT: c.WeakTypeTag[F[Any]], ET: c.WeakTypeTag[E], AT: c.WeakTypeTag[A]
    ): c.Expr[Validator[F, E, A]] = {
        import c.universe._
        c.Expr(AT.tpe.members.toList.sortBy(_.fullName).collect {
            case m: MethodSymbol if m.isParamAccessor => m
        }.foldLeft(q"""_root_.dupin.Validator[$FT, $ET].success[$AT]""") { case (t, m) => q"""
            $t.combinePE(
                _root_.dupin.Path(_root_.dupin.core.FieldPart(${m.name.toString})),
                _.${m.name})(
                implicitly[_root_.dupin.core.Validator[$FT, $ET, ${m.returnType}]]
            )
        """})
    }
}
