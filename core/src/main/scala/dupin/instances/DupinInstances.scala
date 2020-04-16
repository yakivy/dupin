package dupin.instances

import cats.Applicative
import cats.Foldable
import cats.Traverse
import cats.syntax.foldable._
import cats.syntax.traverse._
import dupin.core.IndexPart
import dupin.core.Root
import dupin.core.Success
import dupin.core.Validator

trait DupinInstances {
    def element[L, R, F[_], RR[_]](
        validator: Validator[L, R, F])(implicit AF: Applicative[F], TRR: Traverse[RR], RRR: Foldable[RR]
    ): Validator[L, RR[R], F] = Validator[L, RR[R], F](
        a => a.mapWithIndex((b, i) => validator.composeP[RR[R]](IndexPart(i.toString) :: Root, _ => b))
            .foldLeft(Validator[L, RR[R], F](_ => AF.pure(Success(a))))(_ combine _)
            .f(a)
    )
}
