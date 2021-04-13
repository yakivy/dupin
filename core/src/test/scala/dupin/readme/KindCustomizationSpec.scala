package dupin.readme

import cats.data.NonEmptyChain
import cats.data.Validated
import cats.data.ValidatedNec
import dupin.readme.ReadmeDomainFixture._
import org.scalatest.WordSpec
import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.Duration

trait KindCustomizationDslFixture extends KindCustomizationDomainFixture {
    import cats.Applicative
    import dupin._
    import scala.concurrent.Future

    type FutureValidator[A] = Validator[Future, String, A]
    def FutureValidator[A](implicit A: Applicative[Future]) = Validator[Future, String, A]
}

trait KindCustomizationValidatorFixture extends KindCustomizationDslFixture {
    import cats.implicits._
    import scala.concurrent.ExecutionContext.Implicits.global
    import scala.concurrent.Future

    val nameService = new NameService

    implicit val nameValidator = FutureValidator[Name].root(
        n => nameService.contains(n.value), _.path + " should be non empty"
    )

    implicit val memberValidator = FutureValidator[Member]
        .combinePI(_.name)
        .combinePR(_.age)(a => Future.successful(a > 18 && a < 40), _.path + " should be between 18 and 40")
}

class KindCustomizationSpec extends WordSpec with KindCustomizationValidatorFixture {
    "Kind customization validators" should {
        "return custom kind" in {
            import dupin.syntax._

            val invalidMember = Member(Name(""), 0)
            val result: Future[ValidatedNec[String, Member]] = invalidMember.validate

            assert(Await.result(result, Duration.Inf) == Validated.invalid(NonEmptyChain(
                ".name should be non empty",
                ".age should be between 18 and 40"
            )))
        }
    }
}
