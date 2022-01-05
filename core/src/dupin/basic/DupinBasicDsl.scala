package dupin.basic

import cats.Id
import dupin.DupinCoreDsl

trait DupinBasicDsl extends DupinCoreDsl with DupinBasicDslBinCompat {
    type BasicValidator[A] = Validator[Id, String, A]
    val BasicValidator = Validator[Id, String]

    type BasicMessageBuilder[-A] = MessageBuilder[A, String]
}
