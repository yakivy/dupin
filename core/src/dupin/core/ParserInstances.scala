package dupin.core

import cats._
import cats.arrow.ArrowChoice
import cats.arrow.FunctionK
import cats.data.IorT
import cats.data.NonEmptyChain
import cats.implicits._

trait ParserInstance0 {
    implicit final def parserMonad[F[_]: Monad, E, A]: Monad[Parser[F, E, A, *]] =
        new Monad[Parser[F, E, A, *]] {
            override def pure[B](b: B): Parser[F, E, A, B] = Parser[F, E].success(b)
            override def flatMap[B, C](fa: Parser[F, E, A, B])(f: B => Parser[F, E, A, C]): Parser[F, E, A, C] =
                fa.flatMap(f)
            override def tailRecM[B, C](b: B)(f: B => Parser[F, E, A, Either[B, C]]): Parser[F, E, A, C] =
                Parser[F, E].runF[A, C] { a =>
                    FlatMap[IorT[F, NonEmptyChain[E], *]].tailRecM(b)(b => IorT(f(b).runF(a))).value
                }
        }

    implicit def parserParallelWithSequentialEffect[F0[_]: Monad, E, A]: Parallel.Aux[Parser[F0, E, A, *], Parser[F0, E, A, *]] =
        new Parallel[Parser[F0, E, A, *]] {
            type F[x] = Parser[F0, E, A, x]
            private val identityK: Parser[F0, E, A, *] ~> Parser[F0, E, A, *] = FunctionK.id[Parser[F0, E, A, *]]
            private val underlyingParallel: Parallel.Aux[IorT[F0, NonEmptyChain[E], *], IorT[F0, NonEmptyChain[E], *]] =
                IorT.catsDataParallelForIorTWithSequentialEffect[F0, NonEmptyChain[E]]

            def parallel: Parser[F0, E, A, *] ~> Parser[F0, E, A, *] = identityK
            def sequential: Parser[F0, E, A, *] ~> Parser[F0, E, A, *] = identityK

            val applicative: Applicative[Parser[F0, E, A, *]] = new Applicative[Parser[F0, E, A, *]] {
                def pure[B](b: B): Parser[F0, E, A, B] = Parser[F0, E].success(b)
                def ap[B, C](ff: Parser[F0, E, A, B => C])(fa: Parser[F0, E, A, B]): Parser[F0, E, A, C] =
                    Parser[F0, E].runF[A, C](c =>
                        underlyingParallel.applicative.ap(IorT(ff.runF(c)))(IorT(fa.runF(c))).value
                    )
            }

            lazy val monad: Monad[Parser[F0, E, A, *]] = Monad[Parser[F0, E, A, *]]
        }
}

trait ParserInstances extends ParserInstance0 {
    implicit def parserArrow[F[_]: Monad, E]: ArrowChoice[Parser[F, E, *, *]] =
        new ArrowChoice[Parser[F, E, *, *]] {
            def choose[A, B, C, D](
                f: Parser[F, E, A, C]
            )(g: Parser[F, E, B, D]): Parser[F, E, Either[A, B], Either[C, D]] = Parser[F, E].runF(c =>
                c.value match {
                    case Left(a) => f.map(Either.left[C, D](_)).runF(c.copy(value = a))
                    case Right(b) => g.map(Either.right[C, D](_)).runF(c.copy(value = b))
                }
            )

            def lift[A, B](f: A => B): Parser[F, E, A, B] = Parser[F, E].run(c => f(c.value).rightIor)

            def first[A, B, C](fa: Parser[F, E, A, B]): Parser[F, E, (A, C), (B, C)] =
                Parser[F, E].runF(c => fa.map(_ -> c.value._2).runF(c.copy(value = c.value._1)))

            def compose[A, B, C](f: Parser[F, E, B, C], g: Parser[F, E, A, B]): Parser[F, E, A, C] = f.compose(g)
        }

    implicit final def parserLiftedToTraverseP[F[_], E, A, B, G[_]](implicit
        p: Parser[F, E, A, B],
        F: Applicative[F],
        G: Traverse[G],
    ): Parser[F, E, G[A], G[B]] = p.liftToTraverseP[G]
}
