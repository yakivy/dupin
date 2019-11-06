package dupin.core

import dupin.core.Builder.PartiallyAppliedCombinePR
import dupin.core.Builder.PartiallyAppliedPath
import scala.reflect.macros.blackbox

object BuilderMacro {
    def pathImpl[A, B, C[_], D](
        c: blackbox.Context)(f: c.Expr[A => D]
    ): c.Expr[PartiallyAppliedPath[A, B, C, D]] = {
        import c.universe._
        f.tree match {
            case q"($_) => $_.${field: Name}" =>
                c.Expr[PartiallyAppliedPath[A, B, C, D]](q"""
                   _root_.dupin.core.Builder.PartiallyAppliedPath(
                        _root_.dupin.core.FieldPart(${field.decodedName.toString}), $f
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
                   _root_.dupin.core.Builder.PartiallyAppliedCombinePR(
                        _root_.dupin.core.FieldPart(${field.decodedName.toString}), $f
                   )
                 """)
            case a => c.abort(c.enclosingPosition, s"Unable to create path from $a")
        }
    }
}
