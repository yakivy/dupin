package dupin.core

import dupin.core.Validator.PartiallyAppliedCombineP
import dupin.core.Validator.PartiallyAppliedCombinePR
import scala.reflect.macros.blackbox

object ValidatorMacro {
    def combinePImpl[F[_], E, A, AA](
        c: blackbox.Context)(f: c.Expr[A => AA]
    ): c.Expr[PartiallyAppliedCombineP[F, E, A, AA]] = {
        import c.universe._
        handleField[A, AA, PartiallyAppliedCombineP[F, E, A, AA]](c)(f)(field => c.Expr(q"""
           _root_.dupin.core.Validator.PartiallyAppliedCombineP(
                ${c.prefix}, _root_.dupin.core.FieldPart(${field.decodedName.toString}), $f
           )
        """))
    }

    def combinePRImpl[F[_], E, A, AA](
        c: blackbox.Context)(f: c.Expr[A => AA]
    ): c.Expr[PartiallyAppliedCombinePR[F, E, A, AA]] = {
        import c.universe._
        handleField[A, AA, PartiallyAppliedCombinePR[F, E, A, AA]](c)(f)(field => c.Expr(q"""
           _root_.dupin.core.Validator.PartiallyAppliedCombinePR(
                ${c.prefix}, _root_.dupin.core.FieldPart(${field.decodedName.toString}), $f
           )
        """))
    }

    def combinePIImpl[F[_], E, A, AA](
        c: blackbox.Context)(f: c.Expr[A => AA])(v: c.Expr[Validator[F, E, AA]]
    ): c.Expr[Validator[F, E, A]] = {
        import c.universe._
        handleField[A, AA, Validator[F, E, A]](c)(f)(field => c.Expr(q"""
           ${c.prefix}.combineP(
               _root_.dupin.core.Root.::(_root_.dupin.core.FieldPart(${field.decodedName.toString})), $f)($v
           )
        """))
    }

    private def handleField[A, AA, R](
        c: blackbox.Context)(
        f: c.Expr[A => AA])(
        handler: c.Name => c.Expr[R]
    ): c.Expr[R] = {
        import c.universe._
        f.tree match {
            case q"($_) => $_.${field: c.Name}" => handler(field)
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
