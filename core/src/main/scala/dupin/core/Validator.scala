package dupin.core

import cats.Applicative
import cats.data.NonEmptyChain
import cats.data.Validated
import cats.data.ValidatedNec
import cats.implicits._
import dupin.MessageBuilder
import dupin.core.Validator.PartiallyAppliedCombineP
import dupin.core.Validator.PartiallyAppliedCombinePR
import scala.language.experimental.macros

class Validator[F[_], E, A](val f: A => F[ValidatedNec[MessageBuilder[A, E], A]])(implicit A: Applicative[F]) {
    def validate(a: A): F[ValidatedNec[E, A]] =
        A.map(f(a))(_.leftMap(_.map(_.apply(Context(Root, a)))))

    def recoverWith(
        ff: NonEmptyChain[MessageBuilder[A, E]] => ValidatedNec[MessageBuilder[A, E], A]
    ): Validator[F, E, A] = new Validator(a => A.map(f(a))(
        Validated.catsDataApplicativeErrorForValidated[NonEmptyChain[MessageBuilder[A, E]]].handleErrorWith(_)(ff)
    ))

    /**
     * Recovers validation result with a failure. It is useful for replacing validation message.
     */
    def recoverWithF(
        m1: MessageBuilder[A, E], ms: MessageBuilder[A, E]*
    ): Validator[F, E, A] = recoverWith(_ => Validated.Invalid(NonEmptyChain(m1, ms: _*)))

    def composeP[AA](p: Path, ff: AA => A): Validator[F, E, AA] = new Validator(
        a => A.map(f(ff(a)))(_.bimap(_.map(_.compose(_.map(p, ff))), _ => a))
    )

    def compose[AA](f: AA => A): Validator[F, E, AA] = composeP(Root, f)

    def combine(
        v: Validator[F, E, A]
    ): Validator[F, E, A] = new Validator(a => (this.f(a), v.f(a)).mapN(_ product _).map(_.map(_ => a)))

    def orElse(
        v: Validator[F, E, A]
    ): Validator[F, E, A] = new Validator(a => (this.f(a), v.f(a)).mapN(_ orElse _))

    /**
     * Alias for [[combine]] with `$` operator priority
     */
    def &&(v: Validator[F, E, A]): Validator[F, E, A] = combine(v)

    /**
     * Alias for [[orElse]] with `|` operator priority
     */
    def ||(v: Validator[F, E, A]): Validator[F, E, A] = orElse(v)

    /**
     * Combines with root validator passed by separate arguments.
     */
    def combineR(
        f: A => F[Boolean], m: MessageBuilder[A, E]
    ): Validator[F, E, A] = combine(
        new Validator(a => A.map(f(a))(if (_) Validated.Valid(a) else Validated.invalidNec(m)))
    )

    /**
     * Combines with field validator using explicit path.
     */
    def combineP[AA](p: Path, f: A => AA)(v: Validator[F, E, AA]): Validator[F, E, A] =
        combine(v.composeP(p, f))

    /**
     * Combines with field validator using macros generated path.
     */
    def combineP[AA](f: A => AA): PartiallyAppliedCombineP[F, E, A, AA] =
        macro ValidatorMacro.combinePImpl[F, E, A, AA]

    /**
     * Combines with field validator passed by separate arguments using macros generated path.
     */
    def combinePR[AA](f: A => AA): PartiallyAppliedCombinePR[F, E, A, AA] =
        macro ValidatorMacro.combinePRImpl[F, E, A, AA]

    /**
     * Combines with implicit field validator using macros generated path
     */
    def combinePI[AA](f: A => AA)(implicit v: Validator[F, E, AA]): Validator[F, E, A] =
        macro ValidatorMacro.combinePIImpl[F, E, A, AA]

    /**
     * Combines with implicit validators for all fields that have accessors using macros generated path
     */
    def combineD: Validator[F, E, A] =
        macro ValidatorMacro.combineDImpl[F, E, A]
}

/**
 * It is often being used as a partially applied constructor for [[Validator]]
 */
case class SuccessValidator[F[_], E, A]()(implicit A: Applicative[F])
    extends Validator[F, E, A](a => A.pure(Validated.Valid(a))) {

    override def combine(v: Validator[F, E, A]): Validator[F, E, A] = v

    override def orElse(v: Validator[F, E, A]): Validator[F, E, A] = this

    //Partially applied constructors:

    /**
     * Creates a validator that always returns success result.
     */
    def success: Validator[F, E, A] = this

    /**
     * Creates a validator that always returns fail result.
     */
    def fail(m: MessageBuilder[A, E]): Validator[F, E, A] = FailValidator(m)

    /**
     * Creates a root validator from given arguments.
     * Alias for [[combineR]]
     */
    def root(f: A => F[Boolean], m: MessageBuilder[A, E]): Validator[F, E, A] = combineR(f, m)

    /**
     * Creates a field validator from root validator using explicit path.
     * Alias for [[combineP(path: Path, f: A => AA)(v: Validator[F, E, AA])]]
     */
    def path[AA](p: Path, f: A => AA)(v: Validator[F, E, AA]): Validator[F, E, A] = combineP(p, f)(v)

    /**
     * Creates a field validator from root validator using macros generated path.
     * Alias for [[combineP(f: A => AA)]]
     */
    def path[AA](f: A => AA): PartiallyAppliedCombineP[F, E, A, AA] =
        macro ValidatorMacro.combinePImpl[F, E, A, AA]

    /**
     * Creates a field validator from root validator passed as separate arguments using macros generated path.
     * Alias for [[combinePR]]
     */
    def pathR[AA](f: A => AA): PartiallyAppliedCombinePR[F, E, A, AA] =
        macro ValidatorMacro.combinePRImpl[F, E, A, AA]

    /**
     * Creates a root validator from implicit validators for all fields that have accessors
     * using macros generated path.
     * Alias for [[combineD]]
     */
    def derive: Validator[F, E, A] =
        macro ValidatorMacro.combineDImpl[F, E, A]
}

case class FailValidator[F[_], E, A](m: MessageBuilder[A, E])(implicit A: Applicative[F])
    extends Validator[F, E, A](_ => A.pure(Validated.invalidNec(m)))

object Validator extends ValidatorInstances {
    def apply[F[_], E, A](implicit A: Applicative[F]): SuccessValidator[F, E, A] = SuccessValidator()

    case class PartiallyAppliedCombineP[F[_], E, A, AA](iv: Validator[F, E, A], p: PathPart, f: A => AA) {
        def apply(
            v: Validator[F, E, AA])(implicit A: Applicative[F]
        ): Validator[F, E, A] = iv.combineP(p :: Root, f)(v)
    }

    case class PartiallyAppliedCombinePR[F[_], E, A, AA](iv: Validator[F, E, A], p: PathPart, f: A => AA) {
        def apply(
            fv: AA => F[Boolean], m: MessageBuilder[AA, E])(implicit A: Applicative[F]
        ): Validator[F, E, A] = iv.combineP(p :: Root, f)(Validator[F, E, AA].root(fv, m))
    }
}

