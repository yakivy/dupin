package dupin.core

import cats.Applicative
import cats.Functor
import cats.Monoid
import cats.data.NonEmptyChain
import cats.data.Validated
import cats.data.ValidatedNec
import cats.implicits._
import dupin.MessageBuilder

case class Validator[F[_], E, A](
    run: A => F[ValidatedNec[MessageBuilder[A, E], A]]
) extends ValidatorBinCompat[F, E, A] {
    def validate(a: A)(implicit F: Functor[F]): F[ValidatedNec[E, A]] =
        F.map(run(a))(_.leftMap(_.map(_.apply(Context(Root, a)))))

    def handleErrorWith(
        f: NonEmptyChain[MessageBuilder[A, E]] => ValidatedNec[MessageBuilder[A, E], A])(
        implicit F: Functor[F]
    ): Validator[F, E, A] = Validator(a => F.map(run(a))(
        Validated.catsDataApplicativeErrorForValidated[NonEmptyChain[MessageBuilder[A, E]]].handleErrorWith(_)(f)
    ))

    def leftMap[EE](
        f: NonEmptyChain[MessageBuilder[A, E]] => NonEmptyChain[MessageBuilder[A, EE]])(
        implicit F: Functor[F]
    ): Validator[F, EE, A] = Validator(a => F.map(run(a))(_.leftMap(f)))

    /**
     * Replaces failure messages with supplied values
     */
    def leftAs[EE](
        m1: MessageBuilder[A, EE], ms: MessageBuilder[A, EE]*)(
        implicit F: Functor[F]
    ): Validator[F, EE, A] = leftMap(_ => NonEmptyChain(m1, ms: _*))

    /**
     * Composes validator with explicit field path
     */
    def composeEP[AA](p: Path, f: AA => A)(implicit F: Functor[F]): Validator[F, E, AA] = new Validator(
        a => F.map(run(f(a)))(_.bimap(_.map(_.compose(_.mapP(p, f))), _ => a))
    )

    def compose[AA](f: AA => A)(implicit F: Functor[F]): Validator[F, E, AA] = composeEP(Root, f)

    def composeK[AF[_]](implicit VC: ValidatorComposeK[F, AF]): Validator[F, E, AF[A]] = VC(this)

    def combine(v: Validator[F, E, A])(implicit A: Applicative[F]): Validator[F, E, A] =
        Validator(a => (this.run(a), v.run(a)).mapN(_ product _).map(_.map(_ => a)))

    def orElse(v: Validator[F, E, A])(implicit A: Applicative[F]): Validator[F, E, A] =
        Validator(a => (this.run(a), v.run(a)).mapN(_ orElse _))

    /**
     * Alias for [[combine]] with `$` operator priority
     */
    def &&(v: Validator[F, E, A])(implicit A: Applicative[F]): Validator[F, E, A] = combine(v)

    /**
     * Alias for [[orElse]] with `|` operator priority
     */
    def ||(v: Validator[F, E, A])(implicit A: Applicative[F]): Validator[F, E, A] = orElse(v)

    /**
     * Combines with root validator passed by separate arguments.
     */
    def combineR(
        f: A => F[Boolean], m: MessageBuilder[A, E])(implicit A: Applicative[F]
    ): Validator[F, E, A] = combine(Validator[F, E].root(f, m))

    /**
     * Combines with field validator using explicit path.
     */
    def combineEP[AA](
        p: Path, f: A => AA)(v: Validator[F, E, AA])(implicit A: Applicative[F]
    ): Validator[F, E, A] = combine(v.composeEP(p, f))
}

object Validator extends ValidatorInstances {
    def apply[F[_], E]: PartiallyAppliedConstructor[F, E] = PartiallyAppliedConstructor[F, E]()

    implicit final def validatorInstances[F[_] : Applicative, E, A]: Monoid[Validator[F, E, A]] =
        new Monoid[Validator[F, E, A]] {
            override val empty: Validator[F, E, A] = Validator[F, E].success[A]
            override def combine(x: Validator[F, E, A], y: Validator[F, E, A]): Validator[F, E, A] =
                x combine y
        }

    case class PartiallyAppliedConstructor[F[_], E]() extends PartiallyAppliedConstructorBinCompat[F, E] {
        def apply[A](implicit V: Validator[F, E, A]): Validator[F, E, A] = V

        /**
         * Creates a validator that always returns success result.
         */
        def success[A](implicit A: Applicative[F]): Validator[F, E, A] =
            Validator[F, E, A](a => A.pure(Validated.Valid(a)))

        /**
         * Creates a validator that always returns fail result.
         */
        def failure[A](
            m: MessageBuilder[A, E], ms: MessageBuilder[A, E]*)(
            implicit A: Applicative[F]
        ): Validator[F, E, A] = Validator[F, E, A](_ => A.pure(Validated.Invalid(NonEmptyChain(m, ms: _*))))

        /**
         * Creates a root validator from given arguments.
         */
        def root[A](f: A => F[Boolean], m: MessageBuilder[A, E])(implicit F: Functor[F]): Validator[F, E, A] =
            Validator(a => F.map(f(a))(if (_) Validated.Valid(a) else Validated.invalidNec(m)))
    }

    case class PartiallyAppliedCombineP[F[_], E, A, AA](iv: Validator[F, E, A], p: PathPart, f: A => AA) {
        def apply(
            v: Validator[F, E, AA])(implicit A: Applicative[F]
        ): Validator[F, E, A] = iv.combineEP(p :: Root, f)(v)
    }

    case class PartiallyAppliedCombinePK[F[_], E, A, AF[_], AA](
        iv: Validator[F, E, A], p: PathPart, f: A => AF[AA]
    ) {
        def apply(
            v: Validator[F, E, AA])(implicit A: Applicative[F], F: ValidatorComposeK[F, AF]
        ): Validator[F, E, A] = iv.combineEP(p :: Root, f)(v.composeK[AF])
    }

    case class PartiallyAppliedCombinePR[F[_], E, A, AA](iv: Validator[F, E, A], p: PathPart, f: A => AA) {
        def apply(
            fv: AA => F[Boolean], m: MessageBuilder[AA, E])(implicit A: Applicative[F]
        ): Validator[F, E, A] = iv.combineEP(p :: Root, f)(Validator[F, E].root(fv, m))
    }
}

