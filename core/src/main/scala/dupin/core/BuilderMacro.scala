package dupin.core

import dupin.core.Builder.PartiallyAppliedCombinePR
import dupin.core.Builder.PartiallyAppliedPath
import scala.reflect.macros.blackbox

object BuilderMacro {
    def pathImpl[L, R, F[_], RR](
        c: blackbox.Context)(f: c.Expr[R => RR]
    ): c.Expr[PartiallyAppliedPath[L, R, F, RR]] = {
        import c.universe._
        f.tree match {
            case q"($_) => $_.${field: Name}" =>
                c.Expr[PartiallyAppliedPath[L, R, F, RR]](q"""
                   _root_.dupin.core.Builder.PartiallyAppliedPath(
                        _root_.dupin.core.FieldPart(${field.decodedName.toString}), $f
                   )
                 """)
            case a => c.abort(c.enclosingPosition, s"Unable to create path from $a")
        }
    }

    def combinePRImpl[L, R, F[_], RR](
        c: blackbox.Context)(f: c.Expr[R => RR]
    ): c.Expr[PartiallyAppliedCombinePR[L, R, F, RR]] = {
        import c.universe._
        f.tree match {
            case q"($_) => $_.${field: Name}" =>
                c.Expr[PartiallyAppliedCombinePR[L, R, F, RR]](q"""
                   _root_.dupin.core.Builder.PartiallyAppliedCombinePR(
                        _root_.dupin.core.FieldPart(${field.decodedName.toString}), $f
                   )
                 """)
            case a => c.abort(c.enclosingPosition, s"Unable to create path from $a")
        }
    }
}
