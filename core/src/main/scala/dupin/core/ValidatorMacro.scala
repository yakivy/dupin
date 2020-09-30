package dupin.core

import dupin.core.Validator.PartiallyAppliedCombineP
import dupin.core.Validator.PartiallyAppliedCombinePR
import scala.reflect.macros.blackbox

object ValidatorMacro {
    def combinePImpl[L, R, F[_], RR](
        c: blackbox.Context)(f: c.Expr[R => RR]
    ): c.Expr[PartiallyAppliedCombineP[L, R, F, RR]] = {
        import c.universe._
        handleField[R, RR, PartiallyAppliedCombineP[L, R, F, RR]](c)(f)(field => c.Expr(q"""
           _root_.dupin.core.Validator.PartiallyAppliedCombineP(
                ${c.prefix}, _root_.dupin.core.FieldPart(${field.decodedName.toString}), $f
           )
        """))
    }

    def combinePRImpl[L, R, F[_], RR](
        c: blackbox.Context)(f: c.Expr[R => RR]
    ): c.Expr[PartiallyAppliedCombinePR[L, R, F, RR]] = {
        import c.universe._
        handleField[R, RR, PartiallyAppliedCombinePR[L, R, F, RR]](c)(f)(field => c.Expr(q"""
           _root_.dupin.core.Validator.PartiallyAppliedCombinePR(
                ${c.prefix}, _root_.dupin.core.FieldPart(${field.decodedName.toString}), $f
           )
        """))
    }

    def combinePIImpl[L, R, F[_], RR](
        c: blackbox.Context)(f: c.Expr[R => RR])(v: c.Expr[Validator[L, RR, F]]
    ): c.Expr[Validator[L, R, F]] = {
        import c.universe._
        handleField[R, RR, Validator[L, R, F]](c)(f)(field => c.Expr(q"""
           ${c.prefix}.combineP(
               _root_.dupin.core.Root.::(_root_.dupin.core.FieldPart(${field.decodedName.toString})), $f)($v
           )
        """))
    }

    private def handleField[R, RR, RS](
        c: blackbox.Context)(
        f: c.Expr[R => RR])(
        handler: c.Name => c.Expr[RS]
    ): c.Expr[RS] = {
        import c.universe._
        f.tree match {
            case q"($_) => $_.${field: c.Name}" => handler(field)
            case a => c.abort(c.enclosingPosition, s"Unable to create path from $a")
        }
    }
}
