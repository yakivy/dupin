package dupin

import cats.instances.FutureInstances
import dupin.instances.DupinInstances
import dupin.readme.MessageCustomizationDomainFixture
import dupin.syntax.DupinSyntax
import scala.concurrent.Future

package object custom
    extends DupinCoreDsl with DupinInstances with DupinSyntax
        with FutureInstances with MessageCustomizationDomainFixture {
    type CustomValidator[R] = Validator[I18nMessage, R, Future]
    def CustomValidator[R] = Validator[I18nMessage, R, Future]
}
