package dupin.core

import cats.Functor
import cats.Semigroupal
import cats.data.NonEmptyList
import cats.syntax.contravariantSemigroupal._
import dupin.Message
import dupin.core.Validator._
import scala.language.experimental.macros

case class Validator[L, R, F[_]](f: R => F[Result[Message[R, L], R]]) {
    def validate(a: R)(implicit F: Functor[F]): F[Result[L, R]] =
        F.map(f(a))(_.leftMap(_.apply(Context(Root, a))))

    def recover(ff: NonEmptyList[Message[R, L]] => Result[Message[R, L], R])(implicit F: Functor[F]): Validator[L, R, F] =
        Validator(a => F.map(f(a))(_.recoverWith(ff)))

    def recoverAsF(m1: Message[R, L], ms: Message[R, L]*)(implicit F: Functor[F]): Validator[L, R, F] =
        recover(_ => Fail(NonEmptyList(m1, ms.toList)))

    def composeP[RR](p: Path, ff: RR => R)(implicit F: Functor[F]): Validator[L, RR, F] = Validator(
        a => F.map(f(ff(a)))(_.bimap(_.compose(_.map(p, ff)), _ => a))
    )

    def compose[RR](f: RR => R)(implicit F: Functor[F]): Validator[L, RR, F] = composeP(Root, f)

    def combine(
        v: Validator[L, R, F])(implicit F: Functor[F], S: Semigroupal[F]
    ): Validator[L, R, F] = Validator(a => (this.f(a), v.f(a)).mapN(_ combine _))

    def orElse(
        v: Validator[L, R, F])(implicit F: Functor[F], S: Semigroupal[F]
    ): Validator[L, R, F] = Validator(a => (this.f(a), v.f(a)).mapN(_ orElse _))

    def &&(
        v: Validator[L, R, F])(implicit F: Functor[F], S: Semigroupal[F]
    ): Validator[L, R, F] = combine(v)

    def ||(
        v: Validator[L, R, F])(implicit F: Functor[F], S: Semigroupal[F]
    ): Validator[L, R, F] = orElse(v)

    def combineR(
        f: R => F[Boolean], m: Message[R, L])(
        implicit F: Functor[F], S: Semigroupal[F]
    ): Validator[L, R, F] = combine(Builder.apply.root(f, m))

    def combineP[RR](f: R => RR): PartiallyAppliedCombineP[L, R, F, RR] =
        macro ValidatorMacro.combinePImpl[L, R, F, RR]

    def combinePR[RR](f: R => RR): PartiallyAppliedCombinePR[L, R, F, RR] =
        macro ValidatorMacro.combinePRImpl[L, R, F, RR]
}

object Validator {
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

