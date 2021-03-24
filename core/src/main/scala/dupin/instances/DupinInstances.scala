package dupin.instances

import cats.Applicative
import cats.Traverse
import cats.data.Validated
import cats.implicits._
import dupin.core.IndexPart
import dupin.core.Root
import dupin.core.Validator
import dupin.instances.DupinInstances.PartialElement

trait DupinInstances {
    def element[RR[_]]: PartialElement[RR] = new PartialElement[RR]

    implicit def implicitElement[L, R, F[_], RR[_]](
        implicit validator: Validator[L, R, F], AF: Applicative[F], TRR: Traverse[RR]
    ): Validator[L, RR[R], F] = element[RR](validator)
}

object DupinInstances {
    class PartialElement[RR[_]] {
        def apply[L, R, F[_]](
            validator: Validator[L, R, F])(implicit AF: Applicative[F], TRR: Traverse[RR]
        ): Validator[L, RR[R], F] = new Validator[L, RR[R], F](
            (a: RR[R]) => a.mapWithIndex((b, i) => validator.composeP[RR[R]](IndexPart(i.toString) :: Root, _ => b))
                .foldLeft(new Validator[L, RR[R], F](_ => AF.pure(Validated.Valid(a))))(_ combine _)
                .f(a)
        )
    }
}
