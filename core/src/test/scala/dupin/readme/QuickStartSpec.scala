package dupin.readme

import cats.data.NonEmptyChain
import cats.data.Validated
import org.scalatest.WordSpec
import dupin.readme.ReadmeDomainFixture._

trait QuickStartValidatorFixture {
    import dupin.all._
    import cats.implicits._

    implicit val nameValidator = BaseValidator[Name].root(_.value.nonEmpty, _.path + " should be non empty")

    implicit val memberValidator = BaseValidator[Member]
        .combineP(_.name)(nameValidator)
        .combinePR(_.age)(a => a > 18 && a < 40, _.path + " should be between 18 and 40")

    implicit val teamValidator = BaseValidator[Team]
        .combinePI(_.name)(nameValidator)
        .combineP(_.members)(element(memberValidator))
        .combineR(_.members.size <= 8, _ => "team should be fed with two pizzas!")
}

class QuickStartSpec extends WordSpec with QuickStartValidatorFixture {
    "Readme validators" should {
        "be correct" in {
            import dupin.all._

            val validTeam = Team(
                Name("bears"),
                List(
                    Member(Name("Yakiv"), 26),
                    Member(Name("Myroslav"), 31),
                    Member(Name("Andrii"), 25)
                )
            )

            val invalidTeam = Team(
                Name(""),
                Member(Name(""), 0) :: (1 to 10).map(_ => Member(Name("Valid name"), 20)).toList
            )

            val valid = validTeam.isValid
            val result = invalidTeam.validate

            assert(valid)
            assert(result == Validated.invalid(NonEmptyChain(
                ".name should be non empty",
                ".members.[0].name should be non empty",
                ".members.[0].age should be between 18 and 40",
                "team should be fed with two pizzas!"
            )))
        }
    }
}