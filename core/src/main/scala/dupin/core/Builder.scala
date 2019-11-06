package dupin.core

import cats.Functor
import cats.Semigroupal
import cats.data.NonEmptyList
import dupin.Message
import dupin.core.Builder.PartiallyAppliedCombinePR
import dupin.core.Builder.PartiallyAppliedPath
import scala.language.experimental.macros


class Builder[A, B, C[_]] {
    def root(
        f: A => C[Boolean], m1: Message[A, B], ms: Message[A, B]*)(
        implicit F: Functor[C]
    ): Validator[A, B, C] = Validator(Root, a => F.map(f(a)) {
        if (_) Success(a) else Fail(NonEmptyList(m1, ms.toList))
    })

    def path[D](f: A => D): PartiallyAppliedPath[A, B, C, D] =
        macro BuilderMacro.pathImpl[A, B, C, D]

    def path[D](
        p: PathPart, f: A => D)(v: Validator[D, B, C])(implicit F: Functor[C]
    ): Validator[A, B, C] = Validator(p :: v.path, v.compose(f).f)

    def apply(
        f: A => C[Boolean], m1: Message[A, B], ms: Message[A, B]*)(
        implicit F: Functor[C]
    ): Validator[A, B, C] = root(f, m1, ms: _*)

    def combineR(
        f: A => C[Boolean], m1: Message[A, B], ms: Message[A, B]*)(
        implicit F: Functor[C], S: Semigroupal[C]
    ): Validator[A, B, C] = root(f, m1, ms: _*)

    def combineP[D](f: A => D): PartiallyAppliedPath[A, B, C, D] =
        macro BuilderMacro.pathImpl[A, B, C, D]

    def combinePR[D](f: A => D): PartiallyAppliedCombinePR[A, B, C, D] =
        macro BuilderMacro.combinePRImpl[A, B, C, D]
}

object Builder {
    case class PartiallyAppliedPath[A, B, C[_], D](p: PathPart, f: A => D) {
        def apply(v: Validator[D, B, C])(implicit F: Functor[C]): Validator[A, B, C] =
            Builder[A, B, C].path(p, f)(v)
    }
    case class PartiallyAppliedCombinePR[A, B, C[_], D](p: PathPart, f: A => D) {
        def apply(
            fv: D => C[Boolean], m1: Message[D, B], ms: Message[D, B]*)(implicit F: Functor[C]
        ): Validator[A, B, C] = Builder.apply.path(p, f)(Builder.apply[D, B, C].root(fv, m1, ms: _*))
    }
    def apply[A, B, C[_]]: Builder[A, B, C] = new Builder()
}
