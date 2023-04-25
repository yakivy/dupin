package dupin

import cats._
import cats.data.ValidatedNec
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

class ValidatorLawSpec extends CustomLawSpec {
    implicit def isomorphismsForValidator[F[_], E](implicit
        V: Invariant[Validator[F, E, *]]
    ): Isomorphisms[Validator[F, E, *]] = Isomorphisms.invariant[Validator[F, E, *]]

    implicit def catsLawsArbitraryForValidator[F[_]: Functor, E, A](implicit
        V: Arbitrary[A => F[ValidatedNec[E, Unit]]]
    ): Arbitrary[Validator[F, E, A]] =
        Arbitrary(V.arbitrary.map(f => Validator[F, E].runF(c => f(c.value))))

    implicit def validatorEq[F[_], E, A](implicit
        A: ExhaustiveCheck[Context[A]],
        FE: Eq[F[ValidatedNec[E, Unit]]],
    ): Eq[Validator[F, E, A]] =
        Eq.by[Validator[F, E, A], Context[A] => F[ValidatedNec[E, Unit]]](validator => c => validator.runF(c))

    checkAll(
        "Validator[Option, String, *].MonoidKTests",
        MonoidKTests[Validator[Option, String, *]].monoidK[MiniInt]
    )
    checkAll(
        "Validator[Option, String, *].ContravariantMonoidalTests",
        ContravariantMonoidalTests[Validator[Option, String, *]].contravariantMonoidal[MiniInt, Boolean, Boolean]
    )
}
