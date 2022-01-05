package dupin.core

import cats.Applicative
import cats.Traverse
import cats.implicits._

trait ValidatorComapToP[F[_], E, A, G[_]] {
    def apply(v: Validator[F, E, A]): Validator[F, E, G[A]]
}

object ValidatorComapToP {
    implicit def validatorComapToPForTraverse[
        F[_] : Applicative, E, A, G[_] : Traverse
    ]: ValidatorComapToP[F, E, A, G] = new ValidatorComapToP[F, E, A, G] {
        override def apply(v: Validator[F, E, A]): Validator[F, E, G[A]] = new Validator[F, E, G[A]](a => a
            .mapWithIndex((b, i) => v.comapPE[G[A]](IndexPart(i.toString) :: Root, _ => b))
            .foldLeft[Validator[F, E, G[A]]](Validator[F, E].success)(_ combine _)
            .run(a)
        )
    }
}