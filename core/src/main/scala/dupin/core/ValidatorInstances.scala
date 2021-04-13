package dupin.core

import cats.Applicative
import cats.Traverse
import cats.data.Validated
import cats.implicits._

trait ValidatorInstances {
    implicit def dupinValidatorForTraverse[F[_], E, A, AA[_]](
        implicit validator: Validator[F, E, A], AF: Applicative[F], TAA: Traverse[AA]
    ): Validator[F, E, AA[A]] = new Validator[F, E, AA[A]](
        (a: AA[A]) => a.mapWithIndex((b, i) => validator.composeP[AA[A]](IndexPart(i.toString) :: Root, _ => b))
            .foldLeft(new Validator[F, E, AA[A]](_ => AF.pure(Validated.Valid(a))))(_ combine _)
            .f(a)
    )
}
