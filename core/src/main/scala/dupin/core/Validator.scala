package dupin.core

import cats.Functor
import cats.Semigroupal
import cats.data.NonEmptyList
import cats.syntax.contravariantSemigroupal._
import dupin.Message
import dupin.core.Validator._
import scala.language.experimental.macros

abstract class Validator[L, R, F[_]](implicit F: Functor[F]) {
    def validate(a: R): F[Result[L, R]]

    def recover(ff: NonEmptyList[Message[R, L]] => Result[Message[R, L], R]): Validator[L, R, F]

    def recoverAsF(m1: Message[R, L], ms: Message[R, L]*): Validator[L, R, F] =
        recover(_ => Fail(NonEmptyList(m1, ms.toList)))

    def composeP[RR](p: Path, f: RR => R): Validator[L, RR, F]

    def compose[RR](f: RR => R): Validator[L, RR, F] = composeP(Root, f)

    def combine(
        v: Validator[L, R, F])(implicit S: Semigroupal[F]
    ): Validator[L, R, F] = CombinedValidator(this, v, new ValidatorResultCombiner {
        override def apply[LL, RR](a: Result[LL, RR], b: Result[LL, RR]): Result[LL, RR] = a combine b
    })

    def orElse(
        v: Validator[L, R, F])(implicit S: Semigroupal[F]
    ): Validator[L, R, F] = CombinedValidator(this, v, new ValidatorResultCombiner {
        override def apply[LL, RR](a: Result[LL, RR], b: Result[LL, RR]): Result[LL, RR] = a orElse b
    })

    def &&(
        v: Validator[L, R, F])(implicit S: Semigroupal[F]
    ): Validator[L, R, F] = combine(v)

    def ||(
        v: Validator[L, R, F])(implicit S: Semigroupal[F]
    ): Validator[L, R, F] = orElse(v)

    def combineR(
        f: R => F[Boolean], m: Message[R, L])(
        implicit S: Semigroupal[F]
    ): Validator[L, R, F] = combine(Builder.apply.root(f, m))

    def combineP[RR](f: R => RR): PartiallyAppliedCombineP[L, R, F, RR] =
        macro ValidatorMacro.combinePImpl[L, R, F, RR]

    def combinePR[RR](f: R => RR): PartiallyAppliedCombinePR[L, R, F, RR] =
        macro ValidatorMacro.combinePRImpl[L, R, F, RR]
}

object Validator {
    case class SingleValidator[L, R, F[_]](
        path: Path, f: R => F[Result[Message[R, L], R]])(implicit F: Functor[F]
    ) extends Validator[L, R, F]{
        override def validate(a: R): F[Result[L, R]] =
            F.map(f(a))(_.leftMap(_.apply(Context(path, a))))

        override def recover(
            ff: NonEmptyList[Message[R, L]] => Result[Message[R, L], R]
        ): Validator[L, R, F] = SingleValidator(path, a => F.map(f(a))(_.recoverWith(ff)))

        override def composeP[RR](p: Path, ff: RR => R): Validator[L, RR, F] = SingleValidator(
            p ::: path, a => F.map(f(ff(a)))(_.bimap(_.compose(_.map(path, ff)), _ => a))
        )
    }

    case class CombinedValidator[L, R, F[_]](
        v1: Validator[L, R, F], v2: Validator[L, R, F], f: ValidatorResultCombiner)(
        implicit F: Functor[F], S: Semigroupal[F]
    ) extends Validator[L, R, F] {
        override def validate(a: R): F[Result[L, R]] =
            (v1.validate(a), v2.validate(a)).mapN((r1, r2) => f(r1, r2))

        override def recover(
            ff: NonEmptyList[Message[R, L]] => Result[Message[R, L], R]
        ): Validator[L, R, F] = CombinedValidator(v1.recover(ff), v2.recover(ff), f)

        override def composeP[RR](p: Path, ff: RR => R): Validator[L, RR, F] =
            CombinedValidator(v1.composeP(p, ff), v2.composeP(p, ff), f)
    }

    trait ValidatorResultCombiner {
        def apply[L, R](a: Result[L, R], b: Result[L, R]): Result[L, R]
    }

    case class PartiallyAppliedCombineP[L, R, F[_], RR](iv: Validator[L, R, F], p: PathPart, f: R => RR) {
        def apply(
            v: Validator[L, RR, F])(implicit F: Functor[F], S: Semigroupal[F]
        ): Validator[L, R, F] = iv.combine(Builder.apply.path(p :: Root, f)(v))
    }
    case class PartiallyAppliedCombinePR[L, R, F[_], RR](iv: Validator[L, R, F], p: PathPart, f: R => RR) {
        def apply(
            fv: RR => F[Boolean], m: Message[RR, L])(
            implicit F: Functor[F], S: Semigroupal[F]
        ): Validator[L, R, F] = iv.combine(
            Builder.apply.path(p :: Root, f)(Builder.apply.root(fv, m))
        )
    }
}

