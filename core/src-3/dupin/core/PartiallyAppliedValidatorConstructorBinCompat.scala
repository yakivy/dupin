package dupin.core

import cats.Applicative
import scala.compiletime.*
import scala.deriving.*
import scala.quoted.*

trait PartiallyAppliedValidatorConstructorBinCompat[F[_], E] {
    /**
     * Creates a root validator from implicit validators for all fields that have accessors
     * using macros generated path.
     */
    inline def derive[A](implicit inline A: Applicative[F]): Validator[F, E, A] = ${
        ValidatorMacro.derive[F, E, A]('A)
    }
}
