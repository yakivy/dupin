package dupin.readme

import cats.data.NonEmptyChain
import cats.data.Validated
import dupin.readme.ReadmeDomainFixture._
import org.scalatest.WordSpec

trait DerivationValidatorFixture {
    import dupin.all._

    implicit val nameValidator = BaseValidator[Name]
        .root(_.value.nonEmpty, _.path + " should be non empty")
    implicit val ageValidator = BaseValidator[Int]
        .root(a => a > 18 && a < 40, _.path + " should be between 18 and 40")

    implicit val memberValidator = BaseValidator[Member].derive
}

class DerivationSpec extends WordSpec with DerivationValidatorFixture {
    "Derived validators" should {
        "be correct" in {
            import dupin.all._

            val validMember = Member(Name("Yakiv"), 27)
            val invalidMember = Member(Name(""), 0)

            val valid = validMember.isValid
            val result = invalidMember.validate

            assert(valid)
            assert(result == Validated.invalid(NonEmptyChain(
                ".name should be non empty",
                ".age should be between 18 and 40"
            )))
        }
    }
}