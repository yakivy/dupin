package dupin.instances

import cats.Applicative
import cats.Functor
import cats.Semigroupal
import dupin.core.IndexPart
import dupin.core.Root
import dupin.core.Success
import dupin.core.Validator

trait DupinInstances {
    implicit def option[L, R, F[_]](
        implicit validator: Validator[L, R, F], F: Functor[F], A: Applicative[F]
    ): Validator[L, Option[R], F] = Validator[L, Option[R], F](
        a => a.map(b => validator.compose[Option[R]](_ => b).f(a)).getOrElse(A.pure(Success(a)))
    )

    def genericIterable[L, R, F[_], RR <: Iterable[R]](
        implicit validator: Validator[L, R, F], F: Functor[F], A: Applicative[F], S: Semigroupal[F]
    ): Validator[L, RR, F] = Validator[L, RR, F](
        a => a.zipWithIndex
            .map(b => validator.composeP[RR](IndexPart(b._2.toString) :: Root, _ => b._1))
            .reduceLeftOption(_ combine _)
            .map(_.f(a))
            .getOrElse(A.pure(Success(a)))
    )

    implicit def seq[L, R, F[_]](
        implicit validator: Validator[L, R, F], F: Functor[F], A: Applicative[F], S: Semigroupal[F]
    ): Validator[L, Seq[R], F] = genericIterable
    implicit def iterable[L, R, F[_]](
        implicit validator: Validator[L, R, F], F: Functor[F], A: Applicative[F], S: Semigroupal[F]
    ): Validator[L, Iterable[R], F] = genericIterable
    implicit def list[L, R, F[_]](
        implicit validator: Validator[L, R, F], F: Functor[F], A: Applicative[F], S: Semigroupal[F]
    ): Validator[L, List[R], F] = genericIterable
}
