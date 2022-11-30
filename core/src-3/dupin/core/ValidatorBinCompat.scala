package dupin.core

import cats.Applicative
import cats.Functor
import dupin.core.Validator.PartiallyAppliedCombineP
import dupin.core.Validator.PartiallyAppliedCombinePC
import dupin.core.Validator.PartiallyAppliedCombinePL
import dupin.core.Validator.PartiallyAppliedCombinePR
import dupin.core.Validator.PartiallyAppliedCombinePRF

trait ValidatorBinCompat[F[_], E, A] { this: Validator[F, E, A] =>
    /**
     * Contravariant map with macros generated path prefix. Example:
     * {{{
     * scala> case class User(age: Int)
     * scala> val user = User(1)
     * scala> val validator = dupin.basic.BasicValidator.failure[Int](c => s"${c.path} is wrong")
     *
     * scala> validator.comap[User](_.age).validate(user)
     * res0: cats.Id[cats.data.ValidatedNec[String,User]] = Invalid(Chain(. is wrong))
     *
     * scala> validator.comapP[User](_.age).validate(user)
     * res1: cats.Id[cats.data.ValidatedNec[String,User]] = Invalid(Chain(.age is wrong))
     * }}}
     */
    inline def comapP[AA](inline f: AA => A)(implicit F: Functor[F]): Validator[F, E, AA] = ${
        ValidatorMacro.runWithFieldPath('{path => this.comapPE(path, f)}, 'f)
    }

    /**
     * Combines with field validator using macros generated path.
     */
    inline def combineP[AA](inline f: A => AA): PartiallyAppliedCombineP[F, E, A, AA] = ${
        ValidatorMacro.runWithFieldPath('{path => PartiallyAppliedCombineP(this, path, f)}, 'f)
    }

    /**
     * Combines with field validator from context using macros generated path.
     */
    inline def combinePC[AA](inline f: A => AA): PartiallyAppliedCombinePC[F, E, A, AA] = ${
        ValidatorMacro.runWithFieldPath('{path => PartiallyAppliedCombinePC(this, path, f)}, 'f)
    }

    /**
     * Combines with field validator passed by separate arguments using macros generated path.
     */
    inline def combinePR[AA](inline f: A => AA): PartiallyAppliedCombinePR[F, E, A, AA] = ${
        ValidatorMacro.runWithFieldPath('{path => PartiallyAppliedCombinePR(this, path, f)}, 'f)
    }

    inline def combinePRF[AA](inline f: A => AA): PartiallyAppliedCombinePRF[F, E, A, AA] = ${
        ValidatorMacro.runWithFieldPath('{path => PartiallyAppliedCombinePRF(this, path, f)}, 'f)
    }

    /**
     * Combines with lifted field validator using macros generated path.
     */
    inline def combinePL[AF[_], AA](inline f: A => AF[AA]): PartiallyAppliedCombinePL[F, E, A, AF, AA] = ${
        ValidatorMacro.runWithFieldPath('{path => PartiallyAppliedCombinePL(this, path, f)}, 'f)
    }

    /**
     * Combines with implicit field validator using macros generated path
     */
    inline def combinePI[AA](
        inline f: A => AA)(
        implicit V: Validator[F, E, AA], A: Applicative[F]
    ): Validator[F, E, A] = ${
        ValidatorMacro.runWithFieldPath('{path => this.combinePE(path, f)(V)}, 'f)
    }
}
