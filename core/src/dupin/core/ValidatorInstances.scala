package dupin.core

import cats._

trait ValidatorInstances {
    implicit final def validatorWithComapToP[F[_], E, A, G[_]](
        implicit v: Validator[F, E, A], f: ValidatorComapToP[F, E, A, G]
    ): Validator[F, E, G[A]] = f(v)

    implicit final def validatorMonoid[F[_] : Applicative, E, A]: Monoid[Validator[F, E, A]] =
        new Monoid[Validator[F, E, A]] {
            override val empty: Validator[F, E, A] = Validator[F, E].success[A]
            override def combine(x: Validator[F, E, A], y: Validator[F, E, A]): Validator[F, E, A] =
                x combine y
        }
}
