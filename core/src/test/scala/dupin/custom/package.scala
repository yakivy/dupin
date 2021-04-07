package dupin

import cats.Applicative
import dupin.readme.MessageCustomizationDomainFixture._
import dupin.syntax.DupinSyntax
import scala.concurrent.Future

package object custom extends DupinCoreDsl with DupinSyntax {
    type CustomValidator[R] = Validator[I18nMessage, R, Future]
    def CustomValidator[R](implicit A: Applicative[Future]) = Validator[I18nMessage, R, Future]
}
