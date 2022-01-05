package dupin.readme

import cats.data.NonEmptyChain
import cats.data.Validated
import cats.data.ValidatedNec
import dupin.readme.ReadmeDomainFixture._
import org.scalatest.freespec.AsyncFreeSpec
import scala.concurrent.Future

trait KindCustomizationDslFixture extends KindCustomizationDomainFixture {
    import dupin._
    import scala.concurrent.Future

    type FutureValidator[A] = Validator[Future, String, A]
    val FutureValidator = Validator[Future, String]
}

trait KindCustomizationValidatorFixture extends AsyncFreeSpec with KindCustomizationDslFixture {
    import cats.implicits._
    import scala.concurrent.Future

    val nameService = new NameService

    implicit val nameValidator: FutureValidator[Name] = FutureValidator.root[Name](
        n => nameService.contains(n.value), c => s"${c.path} should be non empty"
    )

    implicit val memberValidator: FutureValidator[Member] = FutureValidator.success[Member]
        .combinePI(_.name)
        .combinePR(_.age)(a => Future.successful(a > 18 && a < 40), c => s"${c.path} should be between 18 and 40")
}

class KindCustomizationSpec extends KindCustomizationValidatorFixture {
    "Kind customization validators should" - {
        "return custom kind" in {
            import dupin.syntax._

            val invalidMember = Member(Name(""), 0)
            val result: Future[ValidatedNec[String, Member]] = invalidMember.validate

            result.map(r => assert(r == Validated.invalid(NonEmptyChain(
                ".name should be non empty",
                ".age should be between 18 and 40"
            ))))
        }
    }
}
