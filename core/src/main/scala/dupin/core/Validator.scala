package dupin.core

import cats.Applicative
import cats.data.NonEmptyChain
import cats.data.Validated
import cats.data.ValidatedNec
import cats.implicits._
import dupin.MessageBuilder
import dupin.core.Validator._
import scala.language.experimental.macros

class Validator[L, R, F[_]](val f: R => F[ValidatedNec[MessageBuilder[R, L], R]])(implicit A: Applicative[F]) {
    def validate(a: R): F[ValidatedNec[L, R]] =
        A.map(f(a))(_.leftMap(_.map(_.apply(Context(Root, a)))))

    def recoverWith(
        ff: NonEmptyChain[MessageBuilder[R, L]] => ValidatedNec[MessageBuilder[R, L], R]
    ): Validator[L, R, F] = new Validator(a => A.map(f(a))(_.handleErrorWith(ff)))

    /**
     * Recovers validation result with a failure. It is useful for replacing validation message.
     */
    def recoverWithF(
        m1: MessageBuilder[R, L], ms: MessageBuilder[R, L]*
    ): Validator[L, R, F] = recoverWith(_ => Validated.Invalid(NonEmptyChain(m1, ms: _*)))

    def composeP[RR](p: Path, ff: RR => R): Validator[L, RR, F] = new Validator(
        a => A.map(f(ff(a)))(_.bimap(_.map(_.compose(_.map(p, ff))), _ => a))
    )

    def compose[RR](f: RR => R): Validator[L, RR, F] = composeP(Root, f)

    def combine(
        v: Validator[L, R, F]
    ): Validator[L, R, F] = new Validator(a => (this.f(a), v.f(a)).mapN(_ product _).map(_.map(_ => a)))

    def orElse(
        v: Validator[L, R, F]
    ): Validator[L, R, F] = new Validator(a => (this.f(a), v.f(a)).mapN(_ orElse _))

    /**
     * Alias for [[combine]] with `$` operator priority
     */
    def &&(v: Validator[L, R, F]): Validator[L, R, F] = combine(v)

    /**
     * Alias for [[orElse]] with `|` operator priority
     */
    def ||(v: Validator[L, R, F]): Validator[L, R, F] = orElse(v)

    /**
     * Combines with root validator passed by separate arguments.
     */
    def combineR(
        f: R => F[Boolean], m: MessageBuilder[R, L]
    ): Validator[L, R, F] = combine(
        new Validator(a => A.map(f(a))(if (_) Validated.Valid(a) else Validated.invalidNec(m)))
    )

    /**
     * Combines with field validator using explicit path.
     */
    def combineP[RR](p: Path, f: R => RR)(v: Validator[L, RR, F]): Validator[L, R, F] =
        combine(v.composeP(p, f))

    /**
     * Combines with field validator using macros generated path.
     */
    def combineP[RR](f: R => RR): PartiallyAppliedCombineP[L, R, F, RR] =
        macro ValidatorMacro.combinePImpl[L, R, F, RR]

    /**
     * Combines with field validator passed by separate arguments using macros generated path.
     */
    def combinePR[RR](f: R => RR): PartiallyAppliedCombinePR[L, R, F, RR] =
        macro ValidatorMacro.combinePRImpl[L, R, F, RR]

    /**
     * Combines with implicit field validator using macros generated path
     */
    def combinePI[RR](f: R => RR)(implicit v: Validator[L, RR, F]): Validator[L, R, F] =
        macro ValidatorMacro.combinePIImpl[L, R, F, RR]

    /**
     * Combines with implicit validators for all fields that have accessors using macros generated path
     */
    def combineD: Validator[L, R, F] =
        macro ValidatorMacro.combineDImpl[L, R, F]
}

/**
 * It is often being used as a partially applied constructor for [[Validator]]
 */
case class SuccessValidator[L, R, F[_]]()(implicit A: Applicative[F])
    extends Validator[L, R, F](a => A.pure(Validated.Valid(a))) {

    override def combine(v: Validator[L, R, F]): Validator[L, R, F] = v

    override def orElse(v: Validator[L, R, F]): Validator[L, R, F] = this

    //Partially applied constructors:

    /**
     * Creates a validator that always returns success result.
     */
    def success: Validator[L, R, F] = this

    /**
     * Creates a validator that always returns fail result.
     */
    def fail(m: MessageBuilder[R, L]): Validator[L, R, F] = FailValidator(m)

    /**
     * Creates a root validator from arguments from given arguments.
     * Alias for [[combineR]]
     */
    def root(f: R => F[Boolean], m: MessageBuilder[R, L]): Validator[L, R, F] = combineR(f, m)

    /**
     * Creates a field validator from root validator using explicit path.
     * Alias for [[combineP(path: Path, f: R => RR)(v: Validator[L, RR, F])]]
     */
    def path[RR](p: Path, f: R => RR)(v: Validator[L, RR, F]): Validator[L, R, F] = combineP(p, f)(v)

    /**
     * Creates a field validator from root validator using macros generated path.
     * Alias for [[combineP(f: R => RR)]]
     */
    def path[RR](f: R => RR): PartiallyAppliedCombineP[L, R, F, RR] =
        macro ValidatorMacro.combinePImpl[L, R, F, RR]

    /**
     * Creates a field validator from root validator passed as separate arguments using macros generated path.
     * Alias for [[combinePR]]
     */
    def pathR[RR](f: R => RR): PartiallyAppliedCombinePR[L, R, F, RR] =
        macro ValidatorMacro.combinePRImpl[L, R, F, RR]

    /**
     * Creates a field validator from implicit validators for all fields that have accessors
     * using macros generated path.
     * Alias for [[combineD]]
     */
    def derive: Validator[L, R, F] =
        macro ValidatorMacro.combineDImpl[L, R, F]
}

case class FailValidator[L, R, F[_]](m: MessageBuilder[R, L])(implicit A: Applicative[F])
    extends Validator[L, R, F](_ => A.pure(Validated.invalidNec(m)))

object Validator {
    def apply[L, R, F[_]](implicit A: Applicative[F]): SuccessValidator[L, R, F] = SuccessValidator()

    case class PartiallyAppliedCombineP[L, R, F[_], RR](iv: Validator[L, R, F], p: PathPart, f: R => RR) {
        def apply(
            v: Validator[L, RR, F])(implicit A: Applicative[F]
        ): Validator[L, R, F] = iv.combineP(p :: Root, f)(v)
    }

    case class PartiallyAppliedCombinePR[L, R, F[_], RR](iv: Validator[L, R, F], p: PathPart, f: R => RR) {
        def apply(
            fv: RR => F[Boolean], m: MessageBuilder[RR, L])(implicit A: Applicative[F]
        ): Validator[L, R, F] = iv.combineP(p :: Root, f)(Validator[L, RR, F].root(fv, m))
    }
}

