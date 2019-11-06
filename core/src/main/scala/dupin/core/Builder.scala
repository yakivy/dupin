package dupin.core

import cats.Functor
import cats.Semigroupal
import cats.data.NonEmptyList
import dupin.core.Builder.PartiallyAppliedCombinePR
import dupin.core.Builder.PartiallyAppliedPath
import dupin.dsl.Message
import scala.language.experimental.macros

class Builder[L, R, F[_]] {
    def root(
        f: R => F[Boolean], m1: Message[R, L], ms: Message[R, L]*)(
        implicit F: Functor[F]
    ): Validator[L, R, F] = Validator(Root, a => F.map(f(a)) {
        if (_) Success(a) else Fail(NonEmptyList(m1, ms.toList))
    })

    def path[RR](f: R => RR): PartiallyAppliedPath[L, R, F, RR] =
        macro BuilderMacro.pathImpl[L, R, F, RR]

    def path[RR](
        p: PathPart, f: R => RR)(v: Validator[L, RR, F])(implicit F: Functor[F]
    ): Validator[L, R, F] = Validator(p :: v.path, v.compose(f).f)

    def apply(
        f: R => F[Boolean], m1: Message[R, L], ms: Message[R, L]*)(
        implicit F: Functor[F]
    ): Validator[L, R, F] = root(f, m1, ms: _*)

    def combineR(
        f: R => F[Boolean], m1: Message[R, L], ms: Message[R, L]*)(
        implicit F: Functor[F], S: Semigroupal[F]
    ): Validator[L, R, F] = root(f, m1, ms: _*)

    def combineP[RR](f: R => RR): PartiallyAppliedPath[L, R, F, RR] =
        macro BuilderMacro.pathImpl[L, R, F, RR]

    def combinePR[RR](f: R => RR): PartiallyAppliedCombinePR[L, R, F, RR] =
        macro BuilderMacro.combinePRImpl[L, R, F, RR]
}

object Builder {
    case class PartiallyAppliedPath[L, R, F[_], RR](p: PathPart, f: R => RR) {
        def apply(v: Validator[L, RR, F])(implicit F: Functor[F]): Validator[L, R, F] =
            Builder.apply.path(p, f)(v)
    }
    case class PartiallyAppliedCombinePR[L, R, F[_], RR](p: PathPart, f: R => RR) {
        def apply(
            fv: RR => F[Boolean], m1: Message[RR, L], ms: Message[RR, L]*)(implicit F: Functor[F]
        ): Validator[L, R, F] = Builder.apply.path(p, f)(Builder.apply.root(fv, m1, ms: _*))
    }
    def apply[L, R, F[_]]: Builder[L, R, F] = new Builder()
}
