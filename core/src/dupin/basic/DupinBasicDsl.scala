package dupin.basic

import cats.Id
import dupin.DupinCoreDsl

trait DupinBasicDsl extends DupinCoreDsl {
    type BasicValidator[A] = Validator[Id, String, A]
    val BasicValidator = Validator[Id, String]

    type BasicParser[A, R] = Parser[Id, String, A, R]
    val BasicParser = Parser[Id, String]

    type BasicMessageBuilder[-A] = MessageBuilder[A, String]
}
