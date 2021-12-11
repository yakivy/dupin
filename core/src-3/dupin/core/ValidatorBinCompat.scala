package dupin.core

import cats.Applicative
import cats.Functor
import dupin.core.Validator.PartiallyAppliedCombineP
import dupin.core.Validator.PartiallyAppliedCombinePK
import dupin.core.Validator.PartiallyAppliedCombinePR

trait ValidatorBinCompat[F[_], E, A] { this: Validator[F, E, A] =>
    /**
     * Composes validator with macros generated field path
     */
    inline def composeP[AA](inline f: AA => A)(implicit F: Functor[F]): Validator[F, E, AA] = ${
        ValidatorMacro.runWithFieldName('{name => this.composeEP(FieldPart(name) :: Root, f)}, 'f)
    }

    /**
     * Combines with field validator using macros generated path.
     */
    inline def combineP[AA](inline f: A => AA): PartiallyAppliedCombineP[F, E, A, AA] = ${
        ValidatorMacro.runWithFieldName('{name => PartiallyAppliedCombineP(this, FieldPart(name), f)}, 'f)
    }

    /**
     * Combines with field validator passed by separate arguments using macros generated path.
     */
    inline def combinePR[AA](inline f: A => AA): PartiallyAppliedCombinePR[F, E, A, AA] = ${
        ValidatorMacro.runWithFieldName('{name => PartiallyAppliedCombinePR(this, FieldPart(name), f)}, 'f)
    }

    /**
     * Combines with lifted field validator using macros generated path.
     */
    inline def combinePK[AF[_], AA](inline f: A => AF[AA]): PartiallyAppliedCombinePK[F, E, A, AF, AA] = ${
        ValidatorMacro.runWithFieldName('{name => PartiallyAppliedCombinePK(this, FieldPart(name), f)}, 'f)
    }

    /**
     * Combines with implicit field validator using macros generated path
     */
    inline def combinePI[AA](
        inline f: A => AA)(
        implicit V: Validator[F, E, AA], A: Applicative[F]
    ): Validator[F, E, A] = ${
        ValidatorMacro.runWithFieldName('{name => this.combineEP(FieldPart(name) :: Root, f)(V)}, 'f)
    }
}
