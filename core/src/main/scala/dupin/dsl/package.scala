package dupin

import cats.Id
import cats.instances.function._
import cats.syntax.functor._
import scala.concurrent.Future

package object dsl {
    type Validator[L, R, F[_]] = core.Validator[L, R, F]
    def Validator[L, R, F[_]] = core.Builder[L, R, F]

    type BaseValidator[R] = Validator[String, R, Id]
    def BaseValidator[R] = Validator[String, R, Id]

    type FutureValidator[R] = Validator[String, R, Future]
    def FutureValidator[R] = Validator[String, R, Future]

    type Message[-R, +L] = core.Context[R] => L
    type BaseMessage[-R] = Message[R, String]

    implicit def idF1Conversion[A, B](a: A => B): A => Id[B] = a.map(b => b)
}
