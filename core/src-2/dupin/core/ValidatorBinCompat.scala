package dupin.core

import cats.Functor
import dupin.core.Validator.PartiallyAppliedCombineP
import dupin.core.Validator.PartiallyAppliedCombinePC
import dupin.core.Validator.PartiallyAppliedCombinePR
import dupin.core.Validator.PartiallyAppliedCombinePRF
import scala.language.experimental.macros

trait ValidatorBinCompat[F[_], E, A] { this: Validator[F, E, A] =>
    /**
     * Contravariant map with macros generated path prefix.
     *
     * @see [comap]
     */
    def comapP[AA](f: AA => A): Validator[F, E, AA] =
        macro ValidatorMacro.comapPImpl[F, E, A, AA]

    /**
     * Combines with field validator using macros generated path.
     */
    def combineP[AA](f: A => AA): PartiallyAppliedCombineP[F, E, A, AA] =
        macro ValidatorMacro.combinePImpl[F, E, A, AA]

    /**
     * Combines with field validator from context using macros generated path.
     */
    def combinePC[AA](f: A => AA): PartiallyAppliedCombinePC[F, E, A, AA] =
        macro ValidatorMacro.combinePCImpl[F, E, A, AA]

    /**
     * Combines with field validator passed by separate arguments using macros generated path.
     */
    def combinePR[AA](f: A => AA): PartiallyAppliedCombinePR[F, E, A, AA] =
        macro ValidatorMacro.combinePRImpl[F, E, A, AA]

    def combinePRF[AA](f: A => AA): PartiallyAppliedCombinePRF[F, E, A, AA] =
        macro ValidatorMacro.combinePRFImpl[F, E, A, AA]

    /**
     * Combines with implicit field validator using macros generated path
     */
    def combinePI[AA](f: A => AA)(implicit AA: Validator[F, E, AA]): Validator[F, E, A] =
        macro ValidatorMacro.combinePIImpl[F, E, A, AA]
}
