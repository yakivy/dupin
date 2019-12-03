package dupin.readme

import org.scalatest.WordSpec
import scala.concurrent.Await
import scala.concurrent.duration.Duration

class CustomValidatingPackage extends WordSpec with KindCustomizationDomainFixture {
    "Custom validating package" should {
        "be correct" in {
            import dupin.custom._
            import scala.concurrent.ExecutionContext.Implicits.global

            val nameService = new NameService

            implicit val nameValidator = CustomValidator[Name](
                n => nameService.contains(n.value), c => I18nMessage(
                    c.path + " should be non empty",
                    "validator.name.empty",
                    List(c.path.toString())
                )
            )

            val validName = Name("Ada")

            assert(Await.result(validName.isValid, Duration.Inf))
        }
    }
}
