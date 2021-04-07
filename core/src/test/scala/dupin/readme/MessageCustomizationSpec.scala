package dupin.readme

import cats.data.NonEmptyChain
import cats.data.Validated
import dupin.readme.MessageCustomizationDomainFixture._
import dupin.readme.ReadmeDomainFixture._
import org.scalatest.WordSpec

trait MessageCustomizationDslFixture {
    import dupin._

    type I18nValidator[R] = Validator[I18nMessage, R, cats.Id]
    def I18nValidator[R] = Validator[I18nMessage, R, cats.Id]
}

trait MessageCustomizationValidatorFixture extends MessageCustomizationDslFixture {
    implicit val nameValidator = I18nValidator[Name].root(_.value.nonEmpty, c => I18nMessage(
        c.path + " should be non empty",
        "validator.name.empty",
        List(c.path.toString())
    ))

    implicit val memberValidator = I18nValidator[Member]
        .combinePI(_.name)
        .combinePR(_.age)(a => a > 18 && a < 40, c => I18nMessage(
            c.path + " should be between 18 and 40",
            "validator.member.age",
            List(c.path.toString())
        ))
}

class MessageCustomizationSpec extends WordSpec with MessageCustomizationValidatorFixture {
    "Message customization validators" should {
        "return custom messages" in {
            import dupin.syntax._

            val invalidMember = Member(Name(""), 0)
            val result = invalidMember.validate

            assert(result == Validated.invalid(NonEmptyChain(
                I18nMessage(
                    ".name should be non empty",
                    "validator.name.empty",
                    List(".name")
                ),
                I18nMessage(
                    ".age should be between 18 and 40",
                    "validator.member.age",
                    List(".age")
                )
            )))
        }
    }
}
