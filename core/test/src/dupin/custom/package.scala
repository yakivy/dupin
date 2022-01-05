package dupin

import dupin.readme.MessageCustomizationDomainFixture._
import dupin.syntax.DupinSyntax
import scala.concurrent.Future

package object custom extends DupinCoreDsl with DupinSyntax {
    type CustomValidator[A] = Validator[Future, I18nMessage, A]
    val CustomValidator = Validator[Future, I18nMessage]
}
