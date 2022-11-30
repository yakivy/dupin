package dupin.readme

import cats.data.Validated
import dupin.readme.ReadmeDomainFixture._
import org.scalatest.freespec.AnyFreeSpec

trait PredefinedValidatorsFixture {
    import dupin.basic.all._

    def min(value: Int) = BasicValidator.root[Int](_ > value, c => s"${c.path} should be greater than $value")
    def max(value: Int) = BasicValidator.root[Int](_ < value, c => s"${c.path} should be less than $value")
}

class PredefinedValidatorsSpec extends AnyFreeSpec with PredefinedValidatorsFixture {
    "Predefined validators should" - {
        "be correct" in {
            import cats._
            import dupin.basic.all._

            implicit val memberValidator: BasicValidator[Member] = BasicValidator
                .success[Member]
                .combineP(_.age)(min(18) && max(40).failureAs(_ => "updated validation message"))

            val invalidMember = Member(Name("Ada"), 0)
            val result = invalidMember.validate

            assert(result == Validated.invalidNec(".age should be greater than 18"))
        }
    }
}
