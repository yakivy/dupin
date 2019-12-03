package dupin.base

import cats.Id
import dupin.DupinCoreDsl

trait DupinBaseDsl extends DupinCoreDsl {
    type BaseValidator[R] = Validator[String, R, Id]
    def BaseValidator[R] = Validator[String, R, Id]

    type BaseMessageBuilder[-R] = MessageBuilder[R, String]

    implicit def idF1Conversion[A, B](f: A => B): A => Id[B] = a => f(a)
}
