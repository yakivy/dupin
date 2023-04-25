package dupin.core

import scala.language.experimental.macros

trait PartiallyAppliedValidatorConstructorBinCompat[F[_], E] {
    /**
     * Creates a root validator from implicit validators for all fields that have accessors
     * using macros generated path.
     */
    def derive[A]: Validator[F, E, A] = macro ValidatorMacro.deriveImpl[F, E, A]
}
