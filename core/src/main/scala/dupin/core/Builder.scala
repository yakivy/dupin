package dupin.core

import cats.Functor
import cats.data.NonEmptyList
import dupin.Message
import dupin.core.Builder.PartiallyAppliedCombinePR
import dupin.core.Builder.PartiallyAppliedPath
import dupin.core.Validator.SingleValidator
import scala.language.experimental.macros

class Builder[L, R, F[_]] {
    def root(f: R => F[Boolean], m: Message[R, L])(implicit F: Functor[F]): Validator[L, R, F] =
        SingleValidator(Root, a => F.map(f(a))(if (_) Success(a) else Fail(NonEmptyList(m, Nil))))

    def apply(f: R => F[Boolean], m: Message[R, L])(implicit F: Functor[F]): Validator[L, R, F] =
        root(f, m)

    def combineR(f: R => F[Boolean], m: Message[R, L])(implicit F: Functor[F]): Validator[L, R, F] =
        root(f, m)

    def composeP[RR](p: Path, f: R => RR)(v: Validator[L, RR, F])(implicit F: Functor[F]): Validator[L, R, F] =
        v.composeP(p, f)

    def path[RR](p: Path, f: R => RR)(v: Validator[L, RR, F])(implicit F: Functor[F]): Validator[L, R, F] =
        composeP(p, f)(v)

    def path[RR](f: R => RR): PartiallyAppliedPath[L, R, F, RR] = macro BuilderMacro.pathImpl[L, R, F, RR]

    def combineP[RR](f: R => RR): PartiallyAppliedPath[L, R, F, RR] = macro BuilderMacro.pathImpl[L, R, F, RR]

    def combinePR[RR](f: R => RR): PartiallyAppliedCombinePR[L, R, F, RR] =
        macro BuilderMacro.combinePRImpl[L, R, F, RR]
}

object Builder {
    case class PartiallyAppliedPath[L, R, F[_], RR](p: PathPart, f: R => RR) {
        def apply(v: Validator[L, RR, F])(implicit F: Functor[F]): Validator[L, R, F] =
            Builder.apply.path(p :: Root, f)(v)
    }
    case class PartiallyAppliedCombinePR[L, R, F[_], RR](p: PathPart, f: R => RR) {
        def apply(fv: RR => F[Boolean], m: Message[RR, L])(implicit F: Functor[F]): Validator[L, R, F] =
            Builder.apply.path(p :: Root, f)(Builder.apply.root(fv, m))
    }
    def apply[L, R, F[_]]: Builder[L, R, F] = new Builder()
}
