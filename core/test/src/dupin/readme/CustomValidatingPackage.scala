package dupin.readme

import dupin.readme.MessageCustomizationDomainFixture._
import dupin.readme.ReadmeDomainFixture._
import org.scalatest.freespec.AsyncFreeSpec
import scala.concurrent.Future

class CustomValidatingPackage extends AsyncFreeSpec with KindCustomizationDomainFixture {
    "Custom validating package should" - {
        "be correct" in {
            import cats.implicits._
            import dupin.custom._

            val nameService = new NameService

            implicit val nameValidator: CustomValidator[Name] = CustomValidator.root[Name](
                n => nameService.contains(n.value), c => I18nMessage(
                    s"${c.path} should be non empty",
                    "validator.name.empty",
                    List(c.path.toString())
                )
            )

            val validName = Name("Ada")
            val valid: Future[Boolean] = validName.isValid

            valid.map(assert(_))
        }
    }
}
