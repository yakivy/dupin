package dupin.core

import cats._
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

    def mapK[G[_]](f: F ~> G): Validator[G, E, A] = Validator(a => f(run(a)))

    /**
     * Contravariant map without path changes. Example:
     * {{{
     * scala> case class User(age: Int)
     * scala> val user = User(1)
     * scala> val validator = dupin.basic.BasicValidator.failure[Int](c => s"${c.path} is wrong")
     *
     * scala> validator.comap[User](_.age).validate(user)
     * res0: cats.Id[cats.data.ValidatedNec[String,User]] = Invalid(Chain(. is wrong))
     *
     * scala> validator.comapP[User](_.age).validate(user)
     * res1: cats.Id[cats.data.ValidatedNec[String,User]] = Invalid(Chain(.age is wrong))
     * }}}
     */
    def comap[AA](f: AA => A)(implicit F: Functor[F]): Validator[F, E, AA] = comapPE(Root, f)

    /**
     * Contravariant map with explicit path prefix
     */
    def comapPE[AA](p: Path, f: AA => A)(implicit F: Functor[F]): Validator[F, E, AA] = new Validator(
        a => F.map(run(f(a)))(_.bimap(_.map(_.compose(_.mapP(p, f))), _ => a))
    )

    /**
     * Contravariant map that lifts `A` to higher-kinded type `G` including path changes
     */
    def comapToP[G[_]](implicit C: ValidatorComapToP[F, E, A, G]): Validator[F, E, G[A]] = C(this)

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
    def combinePE[AA](
        p: Path, f: A => AA)(v: Validator[F, E, AA])(implicit A: Applicative[F]
    ): Validator[F, E, A] = combine(v.comapPE(p, f))
}

object Validator extends ValidatorInstances {
    def apply[F[_], E]: PartiallyAppliedConstructor[F, E] = PartiallyAppliedConstructor[F, E]()

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
        ): Validator[F, E, A] = iv.combinePE(p :: Root, f)(v)
    }

    case class PartiallyAppliedCombinePL[F[_], E, A, G[_], AA](
        iv: Validator[F, E, A], p: PathPart, f: A => G[AA]
    ) {
        def apply(
            v: Validator[F, E, AA])(implicit A: Applicative[F], C: ValidatorComapToP[F, E, AA, G]
        ): Validator[F, E, A] = iv.combinePE(p :: Root, f)(v.comapToP[G])
    }

    case class PartiallyAppliedCombinePR[F[_], E, A, AA](iv: Validator[F, E, A], p: PathPart, f: A => AA) {
        def apply(
            fv: AA => F[Boolean], m: MessageBuilder[AA, E])(implicit A: Applicative[F]
        ): Validator[F, E, A] = iv.combinePE(p :: Root, f)(Validator[F, E].root(fv, m))
    }
}

