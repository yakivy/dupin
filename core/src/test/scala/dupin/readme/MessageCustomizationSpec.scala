package dupin.readme

import org.scalatest.WordSpec

trait MessageCustomizationDslFixture extends MessageCustomizationDomainFixture {
    import dupin.all._

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
        .combineP(_.name)(implicitly)
        .combinePR(_.age)(a => a > 18 && a < 40, c => I18nMessage(
            c.path + " should be between 18 and 40",
            "validator.member.age",
            List(c.path.toString())
        ))
}

trait MessageCustomizationValidatingFixture extends MessageCustomizationValidatorFixture {
    import dupin.all._

    val invalidMember = Member(Name(""), 0)
    val messages: List[I18nMessage] = invalidMember.validate.list
}

class MessageCustomizationSpec extends WordSpec with MessageCustomizationValidatingFixture {
    "Message customization validators" should {
        "return custom messages" in {
            assert(messages == List(
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
            ))
        }
    }
}
