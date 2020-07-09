package dupin.core

import cats.Applicative
import cats.data.NonEmptyList
import cats.syntax.contravariantSemigroupal._
import dupin.MessageBuilder
import dupin.core.Validator._
import scala.language.experimental.macros

class Validator[L, R, F[_]](val f: R => F[Result[MessageBuilder[R, L], R]])(implicit A: Applicative[F]) {
    def validate(a: R): F[Result[L, R]] =
        A.map(f(a))(_.leftMap(_.apply(Context(Root, a))))

    def recover(
        ff: NonEmptyList[MessageBuilder[R, L]] => Result[MessageBuilder[R, L], R]
    ): Validator[L, R, F] = new Validator(a => A.map(f(a))(_.recoverWith(ff)))

    def recoverAsF(
        m1: MessageBuilder[R, L], ms: MessageBuilder[R, L]*
    ): Validator[L, R, F] = recover(_ => Fail(NonEmptyList(m1, ms.toList)))

    def composeP[RR](p: Path, ff: RR => R): Validator[L, RR, F] = new Validator(
        a => A.map(f(ff(a)))(_.bimap(_.compose(_.map(p, ff)), _ => a))
    )

    def compose[RR](f: RR => R): Validator[L, RR, F] = composeP(Root, f)

    def combine(
        v: Validator[L, R, F]
    ): Validator[L, R, F] = new Validator(a => (this.f(a), v.f(a)).mapN(_ combine _))

    def orElse(
        v: Validator[L, R, F]
    ): Validator[L, R, F] = new Validator(a => (this.f(a), v.f(a)).mapN(_ orElse _))

    def &&(
        v: Validator[L, R, F]
    ): Validator[L, R, F] = combine(v)

    def ||(
        v: Validator[L, R, F]
    ): Validator[L, R, F] = orElse(v)

    def combineR(
        f: R => F[Boolean], m: MessageBuilder[R, L]
    ): Validator[L, R, F] = combine(Validator[L, R, F].root(f, m))

    def combineP[RR](f: R => RR): PartiallyAppliedCombineP[L, R, F, RR] =
        macro ValidatorMacro.combinePImpl[L, R, F, RR]

    def combinePR[RR](f: R => RR): PartiallyAppliedCombinePR[L, R, F, RR] =
        macro ValidatorMacro.combinePRImpl[L, R, F, RR]
}

case class SuccessValidator[L, R, F[_]]()(implicit A: Applicative[F])
    extends Validator[L, R, F](a => A.pure(Success(a))) {

    override def combine(v: Validator[L, R, F]): Validator[L, R, F] = v

    override def orElse(v: Validator[L, R, F]): Validator[L, R, F] = this

    def success: Validator[L, R, F] = this

    def fail(m: MessageBuilder[R, L]): Validator[L, R, F] = FailValidator(m)

    def root(f: R => F[Boolean], m: MessageBuilder[R, L]): Validator[L, R, F] =
        new Validator(a => A.map(f(a))(if (_) Success(a) else Fail(NonEmptyList(m, Nil))))

    def path[RR](p: Path, f: R => RR)(v: Validator[L, RR, F]): Validator[L, R, F] =
        v.composeP(p, f)

    def path[RR](f: R => RR): PartiallyAppliedCombineP[L, R, F, RR] =
        macro ValidatorMacro.combinePImpl[L, R, F, RR]
}

case class FailValidator[L, R, F[_]](m: MessageBuilder[R, L])(implicit A: Applicative[F])
    extends Validator[L, R, F](_ => A.pure(Fail(NonEmptyList(m, Nil))))

object Validator {
    def apply[L, R, F[_]](implicit A: Applicative[F]): SuccessValidator[L, R, F] = new SuccessValidator()

    case class PartiallyAppliedCombineP[L, R, F[_], RR](iv: Validator[L, R, F], p: PathPart, f: R => RR) {
        def apply(
            v: Validator[L, RR, F])(implicit A: Applicative[F]
        ): Validator[L, R, F] = iv.combine(Validator[L, R, F].path(p :: Root, f)(v))
    }

    case class PartiallyAppliedCombinePR[L, R, F[_], RR](iv: Validator[L, R, F], p: PathPart, f: R => RR) {
        def apply(
            fv: RR => F[Boolean], m: MessageBuilder[RR, L])(implicit A: Applicative[F]
        ): Validator[L, R, F] = iv.combine(
            Validator[L, R, F].path(p :: Root, f)(Validator[L, RR, F].root(fv, m))
        )
    }
}

