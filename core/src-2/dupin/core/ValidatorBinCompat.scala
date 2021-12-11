package dupin.core

import cats.Functor
import dupin.core.Validator.PartiallyAppliedCombineP
import dupin.core.Validator.PartiallyAppliedCombinePK
import dupin.core.Validator.PartiallyAppliedCombinePR
import scala.language.experimental.macros

trait ValidatorBinCompat[F[_], E, A] { this: Validator[F, E, A] =>
    /**
     * Composes validator with macros generated field path
     */
    def composeP[AA](f: AA => A)(implicit F: Functor[F]): Validator[F, E, AA] =
        macro ValidatorMacro.composePImpl[F, E, A, AA]

    /**
     * Combines with field validator using macros generated path.
     */
    def combineP[AA](f: A => AA): PartiallyAppliedCombineP[F, E, A, AA] =
        macro ValidatorMacro.combinePImpl[F, E, A, AA]

    /**
     * Combines with field validator passed by separate arguments using macros generated path.
     */
    def combinePR[AA](f: A => AA): PartiallyAppliedCombinePR[F, E, A, AA] =
        macro ValidatorMacro.combinePRImpl[F, E, A, AA]

    /**
     * Combines with lifted field validator using macros generated path.
     */
    def combinePK[AF[_], AA](f: A => AF[AA]): PartiallyAppliedCombinePK[F, E, A, AF, AA] =
        macro ValidatorMacro.combinePKImpl[F, E, A, AF, AA]

    /**
     * Combines with implicit field validator using macros generated path
     */
    def combinePI[AA](f: A => AA)(implicit V: Validator[F, E, AA]): Validator[F, E, A] =
        macro ValidatorMacro.combinePIImpl[F, E, A, AA]
}
