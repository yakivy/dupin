package dupin.core

import cats.Functor
import dupin.core.Validator.PartiallyAppliedCombineP
import dupin.core.Validator.PartiallyAppliedCombinePL
import dupin.core.Validator.PartiallyAppliedCombinePR
import scala.language.experimental.macros

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
    def comapP[AA](f: AA => A)(implicit F: Functor[F]): Validator[F, E, AA] =
        macro ValidatorMacro.comapPImpl[F, E, A, AA]

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
    def combinePL[G[_], AA](f: A => G[AA]): PartiallyAppliedCombinePL[F, E, A, G, AA] =
        macro ValidatorMacro.combinePLImpl[F, E, A, G, AA]

    /**
     * Combines with implicit field validator using macros generated path
     */
    def combinePI[AA](f: A => AA)(implicit V: Validator[F, E, AA]): Validator[F, E, A] =
        macro ValidatorMacro.combinePIImpl[F, E, A, AA]
}
