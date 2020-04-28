package dupin.instances

import cats.Applicative
import cats.Traverse
import cats.implicits._
import dupin.core.IndexPart
import dupin.core.Root
import dupin.core.Success
import dupin.core.Validator
import dupin.instances.DupinInstances.PartialElement

trait DupinInstances {
    def element[RR[_]]: PartialElement[RR] = new PartialElement[RR]
}

object DupinInstances {
    class PartialElement[RR[_]] {
        def apply[L, R, F[_]](
            validator: Validator[L, R, F])(implicit AF: Applicative[F], TRR: Traverse[RR]
        ): Validator[L, RR[R], F] = Validator[L, RR[R], F](
            a => a.mapWithIndex((b, i) => validator.composeP[RR[R]](IndexPart(i.toString) :: Root, _ => b))
                .foldLeft(Validator[L, RR[R], F](_ => AF.pure(Success(a))))(_ combine _)
                .f(a)
        )
    }
}
