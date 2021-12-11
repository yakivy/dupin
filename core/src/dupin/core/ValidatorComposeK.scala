package dupin.core

import cats.Applicative
import cats.Traverse
import cats.implicits._

trait ValidatorComposeK[F[_], AF[_]] {
    def apply[E, A](v: Validator[F, E, A]): Validator[F, E, AF[A]]
}

object ValidatorComposeK {
    implicit def validatorComposeKForTraverse[
        F[_] : Applicative, AF[_] : Traverse
    ]: ValidatorComposeK[F, AF] = new ValidatorComposeK[F, AF] {
        override def apply[E, A](v: Validator[F, E, A]): Validator[F, E, AF[A]] = new Validator[F, E, AF[A]](
            (a: AF[A]) => a.mapWithIndex((b, i) => v.composeEP[AF[A]](IndexPart(i.toString) :: Root, _ => b))
                .foldLeft[Validator[F, E, AF[A]]](Validator[F, E].success)(_ combine _)
                .run(a)
        )
    }
}