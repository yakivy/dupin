package dupin

import cats._
import cats.data._
import cats.implicits._
import cats.laws.discipline.ExhaustiveCheck
import cats.laws.discipline.SemigroupalTests.Isomorphisms
import cats.laws.discipline._
import cats.laws.discipline.arbitrary._
import cats.laws.discipline.eq._
import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary._
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.Checkers
import org.typelevel.discipline.scalatest.WordSpecDiscipline

class ParserLawSpec extends CustomLawSpec {
    implicit def isomorphismsForParser[F[_], E, A](implicit
        V: Invariant[Parser[F, E, A, *]]
    ): Isomorphisms[Parser[F, E, A, *]] = Isomorphisms.invariant[Parser[F, E, A, *]]

    implicit def catsLawsArbitraryForParser[F[_] : Functor, E, A, B](implicit
        B: Arbitrary[A => F[IorNec[E, B]]]
    ): Arbitrary[Parser[F, E, A, B]] =
        Arbitrary(B.arbitrary.map(f => Parser[F, E].runF(c => f(c.value))))

    implicit def parserEq[F[_], E, A, B](implicit
        A: ExhaustiveCheck[Context[A]],
        B: Eq[F[IorNec[E, B]]],
    ): Eq[Parser[F, E, A, B]] =
        Eq.by[Parser[F, E, A, B], Context[A] => F[IorNec[E, B]]](p => c => p.runF(c))

    checkAll(
        "Parser[Option, String, MiniInt, *].MonadTests",
        MonadTests[Parser[Option, String, MiniInt, *]].monad[Int, Int, Int]
    )
    checkAll(
        "Parser[Option, String, MiniInt, *].ParallelTests",
        ParallelTests[Parser[Option, String, MiniInt, *]].parallel[MiniInt, MiniInt]
    )
    checkAll(
        "Parser[Option, String, *, *].ArrowTests",
        ArrowTests[Parser[Option, String, *, *]].arrow[MiniInt, Boolean, MiniInt, Boolean, MiniInt, Boolean]
    )
}
