package dupin.readme

import dupin.readme.ReadmeDomainFixture._
import org.scalatest.WordSpec

trait DerivationValidatorFixture {
    import dupin.all._

    implicit val nameValidator = BaseValidator[Name].root(_.value.nonEmpty, _.path + " should be non empty")
    implicit val ageValidator = BaseValidator[Int].root(a => a > 18 && a < 40, _.path + " should be between 18 and 40")

    implicit val memberValidator = BaseValidator[Member].derive
}

trait DerivationValidatingFixture extends DerivationValidatorFixture {
    import dupin.all._

    val validMember = Member(Name("Yakiv"), 27)
    val invalidMember = Member(Name(""), 0)

    val validationResult = validMember.isValid
    val messages = invalidMember.validate.list
}

class DerivationSpec extends WordSpec with DerivationValidatingFixture {
    "Derived validators" should {
        "be correct" in {
            assert(validationResult)
            assert(messages == List(
                ".name should be non empty",
                ".age should be between 18 and 40",
            ))
        }
    }
}