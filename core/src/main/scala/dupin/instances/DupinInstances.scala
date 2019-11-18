package dupin.instances

import cats.Applicative
import cats.Functor
import cats.Semigroupal
import dupin.core.IndexPart
import dupin.core.Root
import dupin.core.Success
import dupin.core.Validator

trait DupinInstances {
    implicit def optionConversion[L, R, F[_]](
        validator: Validator[L, R, F])(implicit F: Functor[F], A: Applicative[F]
    ): Validator[L, Option[R], F] = Validator[L, Option[R], F](
        a => a.map(b => validator.compose[Option[R]](_ => b).f(a)).getOrElse(A.pure(Success(a)))
    )
    implicit def optionImplicit[L, R, F[_]](
        implicit V: Validator[L, R, F], F: Functor[F], A: Applicative[F]
    ): Validator[L, Option[R], F] = optionConversion(V)

    implicit def genericIterableOnceConversion[L, R, F[_], RR <: Iterable[R]](
        validator: Validator[L, R, F])(implicit F: Functor[F], A: Applicative[F], S: Semigroupal[F]
    ): Validator[L, RR, F] = Validator[L, RR, F](
        a => a.iterator.zipWithIndex
            .map(b => validator.composeP[RR](IndexPart(b._2.toString) :: Root, _ => b._1))
            .reduceLeftOption(_ combine _)
            .map(_.f(a))
            .getOrElse(A.pure(Success(a)))
    )

    implicit def iterableImplicit[L, R, F[_]](
        implicit V: Validator[L, R, F], F: Functor[F], A: Applicative[F], S: Semigroupal[F]
    ): Validator[L, Iterable[R], F] = genericIterableOnceConversion(V)
    implicit def seqImplicit[L, R, F[_]](
        implicit V: Validator[L, R, F], F: Functor[F], A: Applicative[F], S: Semigroupal[F]
    ): Validator[L, Seq[R], F] = genericIterableOnceConversion(V)
    implicit def listImplicit[L, R, F[_]](
        implicit V: Validator[L, R, F], F: Functor[F], A: Applicative[F], S: Semigroupal[F]
    ): Validator[L, List[R], F] = genericIterableOnceConversion(V)
}
