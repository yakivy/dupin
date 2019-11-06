package dupin.core

import dupin.core.Validator.PartiallyAppliedCombineP
import dupin.core.Validator.PartiallyAppliedCombinePR
import scala.reflect.macros.blackbox

object ValidatorMacro {
    def combinePImpl[A, B, C[_], D](
        c: blackbox.Context)(f: c.Expr[A => D]
    ): c.Expr[PartiallyAppliedCombineP[A, B, C, D]] = {
        import c.universe._
        f.tree match {
            case q"($_) => $_.${field: Name}" =>
                c.Expr[PartiallyAppliedCombineP[A, B, C, D]](q"""
                   _root_.dupin.core.Validator.PartiallyAppliedCombineP(
                        ${c.prefix}, _root_.dupin.core.FieldPart(${field.decodedName.toString}), $f
                   )
                 """)
            case a => c.abort(c.enclosingPosition, s"Unable to create path from $a")
        }
    }

    def combinePRImpl[A, B, C[_], D](
        c: blackbox.Context)(f: c.Expr[A => D]
    ): c.Expr[PartiallyAppliedCombinePR[A, B, C, D]] = {
        import c.universe._
        f.tree match {
            case q"($_) => $_.${field: Name}" =>
                c.Expr[PartiallyAppliedCombinePR[A, B, C, D]](q"""
                   _root_.dupin.core.Validator.PartiallyAppliedCombinePR(
                        ${c.prefix}, _root_.dupin.core.FieldPart(${field.decodedName.toString}), $f
                   )
                 """)
            case a => c.abort(c.enclosingPosition, s"Unable to create path from $a")
        }
    }
}
