package dupin

import cats.Id

package object base {
    type BaseValidator[R] = Validator[String, R, Id]
    def BaseValidator[R] = Validator[String, R, Id]

    type BaseMessage[-R] = Message[R, String]

    implicit def idF1Conversion[A, B](f: A => B): A => Id[B] = a => f(a)
}
