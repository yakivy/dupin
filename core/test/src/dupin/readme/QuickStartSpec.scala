package dupin.readme

import cats.data.NonEmptyChain
import cats.data.Validated
import org.scalatest.freespec.AnyFreeSpec
import dupin.readme.ReadmeDomainFixture._

trait QuickStartValidatorFixture {
    import cats._
    import dupin.basic.all._

    //validator for simple type or value class
    implicit val nameValidator: BasicValidator[Name] = BasicValidator
        .root[Name](_.value.nonEmpty, c => s"${c.path} should be non empty")

    //idiomatic validator for complex type
    implicit val memberValidator: BasicValidator[Member] =
        nameValidator.composeP[Member](_.name) combine
        BasicValidator.root[Int](a => a > 18 && a < 40, c => s"${c.path} should be between 18 and 40").composeP[Member](_.age)

    //same validator but with combination helpers for better type resolving
    val alternativeMemberValidator: BasicValidator[Member] = BasicValidator.success[Member]
        .combineP(_.name)(nameValidator)
        .combinePR(_.age)(a => a > 18 && a < 40, c => s"${c.path} should be between 18 and 40")

    //derived validator
    implicit val teamValidator: BasicValidator[Team] = BasicValidator.derive[Team]
        .combineR(_.members.size <= 8, _ => "team should be fed with two pizzas!")
}

class QuickStartSpec extends AnyFreeSpec with QuickStartValidatorFixture {
    "Readme validators should" - {
        "be correct" in {
            import dupin.basic.all._

            val validTeam = Team(
                Name("Bears"),
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
                ".members.[0].name should be non empty",
                ".members.[0].age should be between 18 and 40",
                ".name should be non empty",
                "team should be fed with two pizzas!"
            )))
        }
    }
}