package dupin.core

import cats.Applicative
import cats.Traverse
import cats.data.ValidatedNec
import cats.implicits._
import dupin.MessageBuilder

trait ValidatorComapToP[F[_], E, A, G[_]] {
    def apply(v: Validator[F, E, A]): Validator[F, E, G[A]]
}

object ValidatorComapToP {
    implicit def validatorComapToPForTraverse[
        F[_] : Applicative, E, A, G[_] : Traverse
    ]: ValidatorComapToP[F, E, A, G] = new ValidatorComapToP[F, E, A, G] {
        override def apply(v: Validator[F, E, A]): Validator[F, E, G[A]] = Validator[F, E].runF[G[A]](t => t
            .mapWithIndex((a, i) => v.comapPE[G[A]](Path(IndexPart(i.toString)), _ => a).runF(t))
            .sequence.map(_.sequence[({type L[AA] = ValidatedNec[MessageBuilder[G[A], E], AA]})#L, G[A]].map(_ => t))
        )
    }
}