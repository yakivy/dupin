package dupin.readme

import cats.data.Validated
import dupin.readme.ReadmeDomainFixture._
import org.scalatest.WordSpec

trait PredefinedValidatorsFixture {
    import dupin.base.all._

    def min(value: Int) = BaseValidator[Int].root(_ > value, _.path + " should be grater than " + value)
    def max(value: Int) = BaseValidator[Int].root(_ < value, _.path + " should be less than " + value)
}

class PredefinedValidatorsSpec extends WordSpec with PredefinedValidatorsFixture {
    "Predefined validators" should {
        "be correct" in {
            import dupin.base.all._

            implicit val memberValidator = BaseValidator[Member].path(_.age)(min(18) && max(40))

            val invalidMember = Member(Name("Ada"), 0)
            val result = invalidMember.validate

            assert(result == Validated.invalidNec(".age should be grater than 18"))
        }
    }
}
