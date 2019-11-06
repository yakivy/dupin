package dupin.core

import cats.Functor
import cats.Semigroupal
import cats.instances.function._
import cats.syntax.contravariantSemigroupal._
import cats.syntax.functor._
import dupin.dsl.Message
import dupin.core.Validator.PartiallyAppliedCombineP
import dupin.core.Validator.PartiallyAppliedCombinePR
import scala.language.experimental.macros

case class Validator[L, R, F[_]](path: Path, f: R => F[Result[Message[R, L], R]]) {
    def validate(a: R)(implicit F: Functor[F]): F[Result[L, R]] =
        F.map(f(a))(_.leftMap(_.apply(Context(path, a))))

    def messages(
        m1: Message[R, L], ms: Message[R, L]*)(implicit F: Functor[F]
    ): Validator[L, R, F] = Validator(path, f.map(F.map(_)(_.messages(m1, ms: _*))))

    def compose[RR](ff: RR => R)(implicit F: Functor[F]): Validator[L, RR, F] = Validator(
        path, a => F.map(f(ff(a)))(_.bimap(_.compose(_.map(path, ff)), _ => a))
    )

    def combine(
        v: Validator[L, R, F])(implicit F: Functor[F], S: Semigroupal[F]
    ): Validator[L, R, F] = Validator(path, a => (this.f(a), v.f(a)).mapN(_ combine _))

    def orElse(
        v: Validator[L, R, F])(implicit F: Functor[F], S: Semigroupal[F]
    ): Validator[L, R, F] = Validator(path, a => (f(a), v.f(a)).mapN(_ orElse _))

    def &&(
        v: Validator[L, R, F])(implicit F: Functor[F], S: Semigroupal[F]
    ): Validator[L, R, F] = combine(v)

    def ||(
        v: Validator[L, R, F])(implicit F: Functor[F], S: Semigroupal[F]
    ): Validator[L, R, F] = orElse(v)

    def combineR(
        f: R => F[Boolean], m1: Message[R, L], ms: Message[R, L]*)(
        implicit F: Functor[F], S: Semigroupal[F]
    ): Validator[L, R, F] = combine(Builder.apply.root(f, m1, ms: _*))

    def combineP[D](f: R => D): PartiallyAppliedCombineP[L, R, F, D] =
        macro ValidatorMacro.combinePImpl[L, R, F, D]

    def combinePR[D](f: R => D): PartiallyAppliedCombinePR[L, R, F, D] =
        macro ValidatorMacro.combinePRImpl[L, R, F, D]
}

object Validator {
    case class PartiallyAppliedCombineP[L, R, F[_], RR](iv: Validator[L, R, F], p: PathPart, f: R => RR) {
        def apply(
            v: Validator[L, RR, F])(implicit F: Functor[F], S: Semigroupal[F]
        ): Validator[L, R, F] = iv.combine(Builder.apply.path(p, f)(v))
    }
    case class PartiallyAppliedCombinePR[L, R, F[_], RR](iv: Validator[L, R, F], p: PathPart, f: R => RR) {
        def apply(
            fv: RR => F[Boolean], m1: Message[RR, L], ms: Message[RR, L]*)(
            implicit F: Functor[F], S: Semigroupal[F]
        ): Validator[L, R, F] = iv.combine(
            Builder.apply.path(p, f)(Builder.apply.root(fv, m1, ms: _*))
        )
    }
}

