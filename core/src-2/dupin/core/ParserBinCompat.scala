package dupin.core

import cats.Functor
import scala.language.experimental.macros

trait ParserBinCompat[F[_], E, A, B] { this: Parser[F, E, A, B] =>
    /**
     * Contravariant map with macros generated path prefix.
     *
     * @see [comap]
     */
    def comapP[AA](f: AA => A): Parser[F, E, AA, B] =
        macro ParserMacro.comapPImpl[F, E, A, B, AA]
}
