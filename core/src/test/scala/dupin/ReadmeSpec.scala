package dupin

import org.scalatest.WordSpec

trait ReadmeDomainFixture {
    case class Name(value: String)
    case class Member(name: Name, age: Int)
    case class Team(name: Name, members: List[Member])
}

trait ReadmeValidatorFixture extends ReadmeDomainFixture {
    import dupin.all._

    implicit val nameValidator = BaseValidator[Name](_.value.nonEmpty, _.path + " should be non empty")

    implicit val memberValidator = BaseValidator[Member]
        .combineP(_.name)(implicitly)
        .combinePR(_.age)(a => a > 18 && a < 40, _.path + " should be between 18 and 40")

    implicit val teamValidator = BaseValidator[Team]
        .combineP(_.name)(implicitly)
        .combineP(_.members)(implicitly)
        .combineR(_.members.size <= 8, _ => "team should be fed with two pizzas!")
}

trait ReadmeValidatingFixture extends ReadmeValidatorFixture {
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
        Member(Name(""), -1) :: (1 to 10).map(_ => Member(Name("Valid name"), 20)).toList
    )

    val a = validTeam.validate.either
    val b = validTeam.isValid
    val c = invalidTeam.validate.list
}

class ReadmeValidatingSpec extends WordSpec with ReadmeValidatingFixture {
    "Readme validators" should {
        "be correct" in {
            assert(a == Right(validTeam))
            assert(b)
            assert(c == List(
                ".name should be non empty",
                ".members.[0].name should be non empty",
                ".members.[0].age should be between 18 and 40",
                "team should be fed with two pizzas!"
            ))
        }
    }
}