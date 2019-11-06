package dupin.core

import cats.Functor
import cats.Semigroupal
import cats.instances.function._
import cats.syntax.contravariantSemigroupal._
import cats.syntax.functor._
import dupin.Message
import dupin.core.Validator.PartiallyAppliedCombineP
import dupin.core.Validator.PartiallyAppliedCombinePR
import scala.language.experimental.macros


case class Validator[A, B, C[_]](path: Path, f: A => C[Result[Message[A, B], A]]) {
    def validate(a: A)(implicit F: Functor[C]): C[Result[B, A]] =
        F.map(f(a))(_.leftMap(_.apply(Context(path, a))))

    def messages(
        m1: Message[A, B], ms: Message[A, B]*)(implicit F: Functor[C]
    ): Validator[A, B, C] = Validator(path, f.map(F.map(_)(_.messages(m1, ms: _*))))

    def compose[D](ff: D => A)(implicit F: Functor[C]): Validator[D, B, C] = Validator(
        path, a => F.map(f(ff(a)))(_.bimap(_.compose(_.map(path, ff)), _ => a))
    )

    def combine(
        v: Validator[A, B, C])(implicit F: Functor[C], S: Semigroupal[C]
    ): Validator[A, B, C] = Validator(path, a => (this.f(a), v.f(a)).mapN(_.combine(_)))

    def &&(
        v: Validator[A, B, C])(implicit F: Functor[C], S: Semigroupal[C]
    ): Validator[A, B, C] = combine(v)

    def ||(
        v: Validator[A, B, C])(implicit F: Functor[C], S: Semigroupal[C]
    ): Validator[A, B, C] = Validator(path, a => (f(a), v.f(a)).mapN(_.orElse(_)))

    def combineR(
        f: A => C[Boolean], m1: Message[A, B], ms: Message[A, B]*)(
        implicit F: Functor[C], S: Semigroupal[C]
    ): Validator[A, B, C] = combine(Builder.apply.root(f, m1, ms: _*))

    def combineP[D](f: A => D): PartiallyAppliedCombineP[A, B, C, D] =
        macro ValidatorMacro.combinePImpl[A, B, C, D]

    def combinePR[D](f: A => D): PartiallyAppliedCombinePR[A, B, C, D] =
        macro ValidatorMacro.combinePRImpl[A, B, C, D]
}

object Validator {
    case class PartiallyAppliedCombineP[A, B, C[_], D](iv: Validator[A, B, C], p: PathPart, f: A => D) {
        def apply(
            v: Validator[D, B, C])(implicit F: Functor[C], S: Semigroupal[C]
        ): Validator[A, B, C] = iv.combine(Builder.apply.path(p, f)(v))
    }
    case class PartiallyAppliedCombinePR[A, B, C[_], D](iv: Validator[A, B, C], p: PathPart, f: A => D) {
        def apply(
            fv: D => C[Boolean], m1: Message[D, B], ms: Message[D, B]*)(
            implicit F: Functor[C], S: Semigroupal[C]
        ): Validator[A, B, C] = iv.combine(
            Builder.apply.path(p, f)(Builder.apply[D, B, C].root(fv, m1, ms: _*))
        )
    }
}

