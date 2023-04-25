package dupin.core

import cats._
import cats.data._
import cats.implicits._
import dupin._

/**
 * A type class that defines how to parse an instance of `A` to an instance of `B`.
 * Can be thought of as a `A => F[IorNec[E, B]]` function.
 */
final class Parser[F[_], E, A, B] private (
    val runF: Context[A] => F[IorNec[E, B]]
) extends ParserBinCompat[F, E, A, B] {
    def apply(a: A): F[IorNec[E, B]] = parse(a)

    def parse(a: A): F[IorNec[E, B]] = runF(Context(Path.empty, a))

    def handleErrorWith(
        f: NonEmptyChain[E] => Parser[F, E, A, B]
    )(implicit
        F: Monad[F]
    ): Parser[F, E, A, B] = Parser[F, E].runF(c =>
        ApplicativeError[IorT[F, NonEmptyChain[E], *], NonEmptyChain[E]]
            .handleErrorWith(IorT(runF(c)))(e => IorT(f(e).runF(c))).value
    )

    def mapError[EE](f: E => EE)(implicit F: Functor[F]): Parser[F, EE, A, B] = Parser[F, EE].runF(c =>
        runF(c).map(_.leftMap(_.map(f)))
    )

    def map[C](f: B => C)(implicit F: Functor[F]): Parser[F, E, A, C] = Parser[F, E].runF(c =>
        runF(Context(c.path, c.value)).map(_.map(f))
    )

    def flatMap[C](f: B => Parser[F, E, A, C])(implicit F: Monad[F]): Parser[F, E, A, C] = Parser[F, E].runF(c =>
        IorT(runF(Context(c.path, c.value))).flatMapF(f(_).runF(c)).value
    )

    /**
     * Contravariant map without path changes. Example:
     * {{{
     * scala> case class User(age: Int)
     * scala> val user = User(1)
     * scala> val parser = dupin.basic.BasicParser.idFailure[Int](c => s"${c.path} is wrong")
     *
     * scala> parser.comap[User](_.age).parse(user)
     * res0: cats.Id[cats.data.IorNec[String,Int]] = Left(Chain(. is wrong))
     *
     * scala> parser.comapP[User](_.age).parse(user)
     * res1: cats.Id[cats.data.IorNec[String,Int]] = Left(Chain(.age is wrong))
     * }}}
     */
    def comap[AA](f: AA => A): Parser[F, E, AA, B] = comapPE(Path.empty, f)

    /**
     * Contravariant map with explicit path prefix.
     */
    def comapPE[AA](p: Path, f: AA => A): Parser[F, E, AA, B] = Parser[F, E].runF(c =>
        runF(Context(c.path ++ p, f(c.value)))
    )

    /**
     * Lifts parser to `G[_]` type using `Traverse` instance, adds index as path prefix.
     * Example:
     * {{{
     * scala> case class Name(value: String)
     * scala> val rawNames = List("", "Doe")
     * scala> val parser = dupin.basic.BasicParser.root[String, Name](
     *   Option(_).filterNot(_.isEmpty).map(Name.apply),
     *   c => s"${c.path} is not a name",
     * )
     *
     * scala> parser.liftToTraverseP[List].parse(rawNames)
     * res0: cats.Id[cats.data.IorNec[String,List[Name]]] = Left(Chain(.[0] is not a name))
     *
     * scala> parser.liftToTraverseCombiningP[List].parse(rawNames)
     * res1: cats.Id[cats.data.IorNec[String,List[Name]]] = Both(Chain(.[0] is not a name),List(Name(Doe)))
     * }}}
     */
    def liftToTraverseP[G[_]](implicit
        F: Applicative[F],
        GT: Traverse[G],
    ): Parser[F, E, G[A], G[B]] = Parser[F, E].runF[G[A], G[B]](c => c
        .value
        .mapWithIndex((a, i) => this.comapPE[G[A]](Path(IndexPart(i.toString)), _ => a).runF(c))
        .sequence
        .map(Parallel.parSequence[G, IorNec[E, *], B])

    )

    /**
     * Lifts parser to `G[_]` type using `Traverse` instance, adds index as path prefix,
     * combines each individual parser result using `MonoidK` instance, therefore allows to skip failures.
     *
     * @see [[liftToTraverseP]]
     */
    def liftToTraverseCombiningP[G[_]](implicit
        F: Applicative[F],
        GT: Traverse[G],
        GA: Applicative[G],
        GM: MonoidK[G],
    ): Parser[F, E, G[A], G[B]] = Parser[F, E].runF[G[A], G[B]](c => c
        .value
        .mapWithIndex((a, i) => this.comapPE[G[A]](Path(IndexPart(i.toString)), _ => a).runF(c))
        .sequence
        .map(GA
            .map(_)(_.map(_.pure[G]))
            .foldLeft(Ior.right[NonEmptyChain[E], G[B]](GM.empty))(_.combine(_)(_ combineK _, _ combineK _))
        )
    )

    def andThen[C](p: Parser[F, E, B, C])(implicit F: Monad[F]): Parser[F, E, A, C] = Parser[F, E].runF(c =>
        runF(c).flatMap {
            case Ior.Both(a, b) => p.runF(Context(c.path, b)).map(_.addLeft(a)((x, y) => y ++ x))
            case Ior.Right(b) => p.runF(Context(c.path, b))
            case r@Ior.Left(_) => F.pure(r)
        }
    )

    def compose[Z](p: Parser[F, E, Z, A])(implicit F: Monad[F]): Parser[F, E, Z, B] = p.andThen(this)
}

object Parser extends ParserInstances {
    def apply[F[_], E]: PartiallyAppliedConstructor[F, E] = PartiallyAppliedConstructor[F, E]()

    case class PartiallyAppliedConstructor[F[_], E]() {
        def apply[A, B](implicit B: Parser[F, E, A, B]): Parser[F, E, A, B] = B

        def runF[A, B](runF: Context[A] => F[IorNec[E, B]]): Parser[F, E, A, B] =
            new Parser(runF)

        def run[A, B](run: Context[A] => IorNec[E, B])(implicit F: Applicative[F]): Parser[F, E, A, B] =
            runF(run andThen F.pure)

        def success[A, B](b: B)(implicit F: Applicative[F]): Parser[F, E, A, B] =
            run[A, B](_ => Ior.right(b))

        def idSuccess[A](implicit F: Applicative[F]): IdParser[F, E, A] = run[A, A](c => Ior.right(c.value))

        def failure[A, B](
            m: MessageBuilder[A, E],
            ms: MessageBuilder[A, E]*
        )(implicit
            F: Applicative[F],
        ): Parser[F, E, A, B] = run[A, B](c => Ior.Left(NonEmptyChain(m(c), ms.map(_(c)): _*)))

        def idFailure[A](
            m: MessageBuilder[A, E],
            ms: MessageBuilder[A, E]*
        )(implicit
            F: Applicative[F],
        ): IdParser[F, E, A] = failure[A, A](m, ms: _*)

        /**
         * Creates parser from context.
         */
        def context[A, B](f: A => Parser[F, E, A, B]): Parser[F, E, A, B] =
            runF(a => f(a.value).runF(a))

        def idContext[A](f: A => Parser[F, E, A, A]): Parser[F, E, A, A] = context(f)

        def rootF[A, B](f: A => F[Option[B]], m: MessageBuilder[A, E])(implicit F: Functor[F]): Parser[F, E, A, B] =
            runF(c => f(c.value).map(_.fold(Ior.leftNec[E, B](m(c)))(Ior.right)))

        /**
         * Creates a root parser from given arguments.
         */
        def root[A, B](f: A => Option[B], m: MessageBuilder[A, E])(implicit F: Applicative[F]): Parser[F, E, A, B] =
            rootF(f andThen F.pure, m)

        def idRoot[A](f: A => Option[A], m: MessageBuilder[A, E])(implicit F: Applicative[F]): IdParser[F, E, A] =
            root(f, m)
    }
}
