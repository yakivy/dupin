package dupin.core

import cats.Applicative
import cats.Traverse
import cats.data.Validated
import cats.implicits._

trait ValidatorInstances {
    implicit def dupinValidatorForTraverse[L, R, F[_], RR[_]](
        implicit validator: Validator[L, R, F], AF: Applicative[F], TRR: Traverse[RR]
    ): Validator[L, RR[R], F] = new Validator[L, RR[R], F](
        (a: RR[R]) => a.mapWithIndex((b, i) => validator.composeP[RR[R]](IndexPart(i.toString) :: Root, _ => b))
            .foldLeft(new Validator[L, RR[R], F](_ => AF.pure(Validated.Valid(a))))(_ combine _)
            .f(a)
    )
}
