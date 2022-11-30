package dupin.core

import cats._
import cats.data._
import cats.implicits._
import dupin._

/**
 * A type class that defines how to validate an instance of `A`.
 * For a better understanding can be thought of as a `A => F[ValidatedNec[E, A]]` function.
 */
final class Validator[F[_], E, A] private (
    val runF: Context[A] => F[ValidatedNec[E, Unit]]
) extends ValidatorBinCompat[F, E, A] {
    def apply(a: A)(implicit F: Functor[F]): F[ValidatedNec[E, A]] = validate(a)

    def validate(a: A)(implicit F: Functor[F]): F[ValidatedNec[E, A]] =
        runF(Context(Path.empty, a)).map(_.map(_ => a))

    def handleErrorWith(
        f: NonEmptyChain[E] => Validator[F, E, A]
    )(implicit
        F: Monad[F]
    ): Validator[F, E, A] = Validator[F, E].runF(c =>
        runF(c).flatMap {
            case r @ Validated.Valid(_) => F.pure(r)
            case Validated.Invalid(a) => f(a).runF(c)
        }
    )

    def mapError[EE](f: E => EE)(implicit F: Functor[F]): Validator[F, EE, A] = Validator[F, EE].runF(c =>
        runF(c).map(_.leftMap(_.map(f)))
    )

    /**
     * Replaces failure messages with supplied values.
     * Optimized version of `.handleErrorWith(_ => Validator.failure(m1, ms))`
     */
    def failureAs[EE](
        m1: MessageBuilder[A, EE],
        ms: MessageBuilder[A, EE]*
    )(implicit
        F: Functor[F]
    ): Validator[F, EE, A] = Validator[F, EE].runF(c =>
        runF(c).map(_.leftMap(_ => NonEmptyChain(m1(c), ms.map(_(c)): _*)))
    )

    def mapK[G[_]](f: F ~> G): Validator[G, E, A] = Validator[G, E].runF(a => f(runF(a)))

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
    def comap[AA](f: AA => A)(implicit F: Functor[F]): Validator[F, E, AA] = comapPE(Path.empty, f)

    /**
     * Contravariant map with explicit path prefix
     */
    def comapPE[AA](p: Path, f: AA => A)(implicit F: Functor[F]): Validator[F, E, AA] = Validator[F, E].runF(c =>
        runF(Context(c.path ++ p, f(c.value)))
    )

    /**
     * Contravariant map that lifts `A` to higher-kinded type `G` including path changes
     */
    def comapToP[G[_]](implicit C: ValidatorComapToP[F, E, A, G]): Validator[F, E, G[A]] = C(this)

    def product[B](
        v: Validator[F, E, B]
    )(implicit FF: Functor[F], FS: Semigroupal[F]): Validator[F, E, (A, B)] = Validator[F, E].runF(a =>
        (this.runF(a.copy(value = a.value._1)), v.runF(a.copy(value = a.value._2))).mapN(_ combine _)
    )

    /**
     * Combines two validators of the same type into one.
     * If first validator fails, second one is not invoked.
     * Example:
     * {{{
     * scala> val value = "value"
     * scala> val v1 = dupin.basic.BasicValidator.failure[String](_ => "failure1")
     * scala> val v2 = dupin.basic.BasicValidator.failure[String](_ => "failure2")
     * scala> val v3 = dupin.basic.BasicValidator.success[String]
     *
     * scala> (v1 andThen v2).validate(value)
     * res0: cats.Id[cats.data.ValidatedNec[String,User]] = Invalid(Chain(failure1))
     *
     * scala> (v3 andThen v2).validate(value)
     * res1: cats.Id[cats.data.ValidatedNec[String,User]] = Invalid(Chain(failure2))
     * }}}
     */
    def andThen(v: Validator[F, E, A])(implicit F: Monad[F]): Validator[F, E, A] = Validator[F, E].runF(c =>
        runF(c).flatMap {
            case Validated.Valid(_) => v.runF(c)
            case r @ Validated.Invalid(_) => F.pure(r)
        }
    )

    /**
     * Combines two validators of the same type into one.
     * If either validator fails, error is returned. If both validators fail, errors from both validators are returned.
     * Example:
     * {{{
     * scala> val value = "value"
     * scala> val v1 = dupin.basic.BasicValidator.failure[String](_ => "failure1")
     * scala> val v2 = dupin.basic.BasicValidator.failure[String](_ => "failure2")
     * scala> val v3 = dupin.basic.BasicValidator.success[String]
     *
     * scala> (v1 combine v2).validate(value)
     * res0: cats.Id[cats.data.ValidatedNec[String,User]] = Invalid(Chain(failure1, failure2))
     *
     * scala> (v3 combine v2).validate(value)
     * res1: cats.Id[cats.data.ValidatedNec[String,User]] = Invalid(Chain(failure2))
     * }}}
     */
    def combine(v: Validator[F, E, A])(implicit F: Applicative[F]): Validator[F, E, A] =
        Validator[F, E].runF(a => (this.runF(a), v.runF(a)).mapN(_ combine _))

    /**
     * Combines two validators of the same type into one.
     * If either validator fails, success is returned. If both validators fail, errors from right validator are returned.
     * Example:
     * {{{
     * scala> val value = "value"
     * scala> val v1 = dupin.basic.BasicValidator.failure[String](_ => "failure1")
     * scala> val v2 = dupin.basic.BasicValidator.failure[String](_ => "failure2")
     * scala> val v3 = dupin.basic.BasicValidator.success[String]
     *
     * scala> (v1 orElse v2).validate(value)
     * res0: cats.Id[cats.data.ValidatedNec[String,User]] = Invalid(Chain(failure2))
     *
     * scala> (v3 orElse v2).validate(value)
     * res1: cats.Id[cats.data.ValidatedNec[String,User]] = Valid(value)
     * }}}
     */
    def orElse(v: Validator[F, E, A])(implicit F: Applicative[F]): Validator[F, E, A] =
        Validator[F, E].runF(a => (this.runF(a), v.runF(a)).mapN(_ orElse _))

    /**
     * Alias for [[combine]] with `$` operator priority
     */
    def &&(v: Validator[F, E, A])(implicit F: Applicative[F]): Validator[F, E, A] = combine(v)

    /**
     * Alias for [[orElse]] with `|` operator priority
     */
    def ||(v: Validator[F, E, A])(implicit F: Applicative[F]): Validator[F, E, A] = orElse(v)

    /**
     * Combines `this` with validator from context.
     *
     * @see [[combine]]
     */
    def combineC(f: A => Validator[F, E, A])(implicit F: Applicative[F]): Validator[F, E, A] =
        combine(Validator[F, E].context(f))

    /**
     * Combines `this` with root validator passed by separate arguments.
     *
     * @see [[combine]]
     */
    def combineR(f: A => Boolean, m: MessageBuilder[A, E])(implicit F: Applicative[F]): Validator[F, E, A] =
        combine(Validator[F, E].root(f, m))

    def combineRF(f: A => F[Boolean], m: MessageBuilder[A, E])(implicit F: Applicative[F]): Validator[F, E, A] =
        combine(Validator[F, E].rootF(f, m))

    /**
     * Combines `this` with field validator using explicit path.
     *
     * @see [[combine]]
     */
    def combinePE[AA](
        p: Path,
        f: A => AA
    )(
        v: Validator[F, E, AA]
    )(implicit
        F: Applicative[F]
    ): Validator[F, E, A] = combine(v.comapPE(p, f))
}

object Validator extends ValidatorInstances {
    def apply[F[_], E]: PartiallyAppliedConstructor[F, E] = PartiallyAppliedConstructor[F, E]()

    case class PartiallyAppliedConstructor[F[_], E]() extends PartiallyAppliedConstructorBinCompat[F, E] {
        def apply[A](implicit V: Validator[F, E, A]): Validator[F, E, A] = V

        def runF[A](runF: Context[A] => F[ValidatedNec[E, Unit]]): Validator[F, E, A] =
            new Validator(runF)

        def run[A](run: Context[A] => ValidatedNec[E, Unit])(implicit F: Applicative[F]): Validator[F, E, A] =
            runF(run andThen F.pure)

        /**
         * Creates a validator that always returns success result.
         */
        def success[A](implicit F: Applicative[F]): Validator[F, E, A] =
            run[A](_ => Validated.Valid(()))

        /**
         * Creates a validator that always returns fail result.
         */
        def failure[A](
            m: MessageBuilder[A, E],
            ms: MessageBuilder[A, E]*
        )(implicit
            F: Applicative[F]
        ): Validator[F, E, A] = run[A](c => Validated.Invalid(NonEmptyChain(m(c), ms.map(_(c)): _*)))

        /**
         * Creates validator from context.
         */
        def context[A](f: A => Validator[F, E, A]): Validator[F, E, A] =
            runF(a => f(a.value).runF(a))

        /**
         * Creates a root validator from given arguments.
         */
        def root[A](f: A => Boolean, m: MessageBuilder[A, E])(implicit F: Applicative[F]): Validator[F, E, A] =
            rootF(f andThen F.pure, m)

        def rootF[A](f: A => F[Boolean], m: MessageBuilder[A, E])(implicit F: Functor[F]): Validator[F, E, A] =
            runF(c => F.map(f(c.value))(if (_) Validated.Valid(()) else Validated.invalidNec(m(c))))
    }

    case class PartiallyAppliedCombineP[F[_], E, A, AA](iv: Validator[F, E, A], p: Path, f: A => AA) {
        def apply(v: Validator[F, E, AA])(implicit A: Applicative[F]): Validator[F, E, A] =
            iv.combinePE(p, f)(v)
    }

    case class PartiallyAppliedCombinePC[F[_], E, A, AA](iv: Validator[F, E, A], p: Path, f: A => AA) {
        def apply(vf: A => Validator[F, E, AA])(implicit A: Applicative[F]): Validator[F, E, A] =
            iv.combineC(a => vf(a).comapPE(p, f))
    }

    case class PartiallyAppliedCombinePL[F[_], E, A, G[_], AA](
        iv: Validator[F, E, A],
        p: Path,
        f: A => G[AA]
    ) {
        def apply(
            v: Validator[F, E, AA]
        )(implicit
            F: Applicative[F],
            C: ValidatorComapToP[F, E, AA, G]
        ): Validator[F, E, A] = iv.combinePE(p, f)(v.comapToP[G])
    }

    case class PartiallyAppliedCombinePRF[F[_], E, A, AA](iv: Validator[F, E, A], p: Path, f: A => AA) {
        def apply(
            fv: AA => F[Boolean],
            m: MessageBuilder[AA, E]
        )(implicit
            F: Applicative[F]
        ): Validator[F, E, A] = iv.combinePE(p, f)(Validator[F, E].rootF(fv, m))
    }

    case class PartiallyAppliedCombinePR[F[_], E, A, AA](iv: Validator[F, E, A], p: Path, f: A => AA) {
        def apply(
            fv: AA => Boolean,
            m: MessageBuilder[AA, E]
        )(implicit
            F: Applicative[F]
        ): Validator[F, E, A] = iv.combinePE(p, f)(Validator[F, E].root(fv, m))
    }
}
