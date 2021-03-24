package dupin.readme

import dupin.readme.MessageCustomizationDomainFixture._
import dupin.readme.ReadmeDomainFixture._
import org.scalatest.WordSpec
import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.Duration

class CustomValidatingPackage extends WordSpec with KindCustomizationDomainFixture {
    "Custom validating package" should {
        "be correct" in {
            import dupin.custom._
            import scala.concurrent.ExecutionContext.Implicits.global

            val nameService = new NameService

            implicit val nameValidator = CustomValidator[Name].root(
                n => nameService.contains(n.value), c => I18nMessage(
                    c.path + " should be non empty",
                    "validator.name.empty",
                    List(c.path.toString())
                )
            )

            val validName = Name("Ada")
            val valid: Future[Boolean] = validName.isValid

            assert(Await.result(valid, Duration.Inf))
        }
    }
}
