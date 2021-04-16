package dupin.core

import dupin.core.Validator.PartiallyAppliedCombineP
import dupin.core.Validator.PartiallyAppliedCombinePK
import dupin.core.Validator.PartiallyAppliedCombinePR
import scala.reflect.macros.blackbox

object ValidatorMacro {
    def combinePImpl[F[_], E, A, AA](
        c: blackbox.Context)(f: c.Expr[A => AA]
    ): c.Expr[PartiallyAppliedCombineP[F, E, A, AA]] = {
        import c.universe._
        c.Expr(q"""_root_.dupin.core.Validator.PartiallyAppliedCombineP(
            ${c.prefix}, _root_.dupin.core.FieldPart(${getFieldName(c)(f).decodedName.toString}), $f
        )""")
    }

    def combinePKImpl[F[_], E, A, AF[_], AA](
        c: blackbox.Context)(f: c.Expr[A => AF[AA]]
    ): c.Expr[PartiallyAppliedCombinePK[F, E, A, AF, AA]] = {
        import c.universe._
        c.Expr(q"""_root_.dupin.core.Validator.PartiallyAppliedCombinePK(
            ${c.prefix}, _root_.dupin.core.FieldPart(${getFieldName(c)(f).decodedName.toString}), $f
        )""")
    }

    def combinePRImpl[F[_], E, A, AA](
        c: blackbox.Context)(f: c.Expr[A => AA]
    ): c.Expr[PartiallyAppliedCombinePR[F, E, A, AA]] = {
        import c.universe._
        c.Expr(q"""_root_.dupin.core.Validator.PartiallyAppliedCombinePR(
            ${c.prefix}, _root_.dupin.core.FieldPart(${getFieldName(c)(f).decodedName.toString}), $f
        )""")
    }

    def combinePIImpl[F[_], E, A, AA](
        c: blackbox.Context)(f: c.Expr[A => AA])(v: c.Expr[Validator[F, E, AA]]
    ): c.Expr[Validator[F, E, A]] = {
        import c.universe._
        c.Expr(q"""${c.prefix}.combineP(
            _root_.dupin.core.Root.::(_root_.dupin.core.FieldPart(${getFieldName(c)(f).decodedName.toString})), $f)($v
        )""")
    }

    private def getFieldName(
        c: blackbox.Context)(
        f: c.Expr[_ => _]
    ): c.Name = {
        import c.universe._
        f.tree match {
            case q"($_) => $_.${field: c.Name}" => field
            case a => c.abort(c.enclosingPosition, s"Unable to create path from $a")
        }
    }

    def combineDImpl[F[_], E, A](
        c: blackbox.Context)(implicit FT: c.WeakTypeTag[F[_]], LT: c.WeakTypeTag[E], RT: c.WeakTypeTag[A]
    ): c.Expr[Validator[F, E, A]] = {
        import c.universe._
        c.Expr(RT.tpe.members.sorted.collect {
            case m: MethodSymbol if m.isParamAccessor => m
        }.foldLeft(c.prefix.tree) { case (t, m) => q"""
                $t.combineP(
                    _root_.dupin.core.Root.::(_root_.dupin.core.FieldPart(${m.name.toString})),
                    _.${m.name})(
                    implicitly[_root_.dupin.core.Validator[$FT, $LT, ${m.returnType}]]
                )
        """})
    }
}
