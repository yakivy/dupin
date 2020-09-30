package dupin.readme

import dupin.readme.ReadmeDomainFixture._
import org.scalatest.WordSpec
import scala.concurrent.Await
import scala.concurrent.duration.Duration

trait KindCustomizationDslFixture extends KindCustomizationDomainFixture {
    import cats.Applicative
    import dupin.all._
    import scala.concurrent.Future

    type FutureValidator[R] = Validator[String, R, Future]
    def FutureValidator[R](implicit A: Applicative[Future]) = Validator[String, R, Future]
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

trait KindCustomizationValidatingFixture extends KindCustomizationValidatorFixture {
    import cats.data.NonEmptyList
    import cats.implicits._
    import dupin.all._
    import scala.concurrent.ExecutionContext.Implicits.global
    import scala.concurrent.Future

    val invalidMember = Member(Name(""), 0)
    val messages: Future[Either[NonEmptyList[String], Member]] = invalidMember.validate.map(_.either)
}

class KindCustomizationSpec extends WordSpec with KindCustomizationValidatingFixture {
    "Kind customization validators" should {
        "return custom kind" in {
            import cats.data.NonEmptyList

            assert(Await.result(messages, Duration.Inf) == Left(NonEmptyList.of(
                ".name should be non empty",
                ".age should be between 18 and 40"
            )))
        }
    }
}
