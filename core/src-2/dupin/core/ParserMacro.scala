package dupin.core

import cats.Functor
import scala.reflect.macros.blackbox

private[dupin] class ParserMacro(val c: blackbox.Context) {
    import dupin.core.ValidatorMacro._

    def comapPImpl[F[_], E, A, B, AA](f: c.Expr[A => AA]): c.Expr[Parser[F, E, AA, B]] = {
        import c.universe._
        c.Expr(q"""${c.prefix}.comapPE(${getFieldPath(c)(f)}, $f)""")
    }
}
