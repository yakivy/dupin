package dupin.base

import cats.Id
import dupin.DupinCoreDsl

trait DupinBaseDsl extends DupinCoreDsl {
    type BaseValidator[A] = Validator[Id, String, A]
    def BaseValidator[A] = Validator[Id, String, A]

    type BaseMessageBuilder[-A] = MessageBuilder[A, String]

    implicit def idF1Conversion[A, B](f: A => B): A => Id[B] = a => f(a)
}
