package dupin

import cats.laws.discipline.ExhaustiveCheck
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.Checkers
import org.typelevel.discipline.scalatest.WordSpecDiscipline

class CustomLawSpec extends AnyWordSpec with WordSpecDiscipline with Checkers {
    implicit def exhaustiveCheckForContext[A: ExhaustiveCheck]: ExhaustiveCheck[Context[A]] =
        ExhaustiveCheck.instance(ExhaustiveCheck[A].allValues.map(Context(Path.empty, _)))
}
