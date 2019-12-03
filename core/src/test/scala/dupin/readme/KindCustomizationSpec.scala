package dupin.readme

import cats.data.NonEmptyList
import org.scalatest.WordSpec
import scala.concurrent.Await
import scala.concurrent.duration.Duration


trait KindCustomizationDomainFixture extends ReadmeDomainFixture {
    import scala.concurrent.Future

    class NameService {
        def contains(name: Name): Future[Boolean] =
            // Emulation of DB call
            Future.successful(name != "")
    }
}

trait KindCustomizationDslFixture extends KindCustomizationDomainFixture {
    import dupin.all._
    import scala.concurrent.Future

    type FutureValidator[R] = Validator[String, R, Future]
    def FutureValidator[R] = Validator[String, R, Future]
}

trait KindCustomizationValidatorFixture extends KindCustomizationDslFixture {
    import cats.implicits._
    import scala.concurrent.ExecutionContext.Implicits.global
    import scala.concurrent.Future

    val nameService = new NameService

    implicit val nameValidator = FutureValidator[Name](nameService.contains, _.path + " should be non empty")

    implicit val memberValidator = FutureValidator[Member]
        .combineP(_.name)(implicitly)
        .combinePR(_.age)(a => Future.successful(a > 18 && a < 40), _.path + " should be between 18 and 40")
}

trait KindCustomizationValidatingFixture extends KindCustomizationValidatorFixture {
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
            assert(Await.result(messages, Duration.Inf) == Left(NonEmptyList.of(
                ".name should be non empty",
                ".age should be between 18 and 40"
            )))
        }
    }
}
