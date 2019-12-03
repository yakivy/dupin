package dupin.readme

import org.scalatest.WordSpec

trait PredefinedValidatorsFixture {
    import dupin.all._

    def min(value: Int) = BaseValidator[Int](_ > value, _.path + " should be grater then " + value)
    def max(value: Int) = BaseValidator[Int](_ < value, _.path + " should be less then " + value)
}

trait PredefinedValidatorsValidatingFixture extends PredefinedValidatorsFixture with ReadmeDomainFixture {
    import dupin.all._

    implicit val memberValidator = BaseValidator[Member].path(_.age)(min(18) && max(40))

    val invalidMember = Member(Name("Ada"), 0)
    val messages = invalidMember.validate.list
}

class PredefinedValidatorsSpec extends WordSpec with PredefinedValidatorsValidatingFixture {
    "Predefined validators" should {
        "be correct" in {
            assert(messages == List(".age should be grater then 18"))
        }
    }
}
