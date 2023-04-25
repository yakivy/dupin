package dupin.core

import scala.language.experimental.macros

trait ParserBinCompat[F[_], E, A, B] { this: Parser[F, E, A, B] =>
    /**
     * Contravariant map with macros generated path prefix.
     *
     * @see [comap]
     */
    inline def comapP[AA](inline f: AA => A): Parser[F, E, AA, B] = ${
        ValidatorMacro.runWithFieldPath('{ path => this.comapPE(path, f) }, 'f)
    }
}
