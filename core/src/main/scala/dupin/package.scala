import cats.Id
import cats.instances.function._
import cats.syntax.functor._
import scala.concurrent.Future

package object dupin {
    type Validator[A, B, C[_]] = core.Validator[A, B, C]
    def Validator[A, B, C[_]] = core.Builder[A, B, C]

    type BaseValidator[A] = Validator[A, String, Id]
    def BaseValidator[A] = Validator[A, String, Id]

    type FutureValidator[A] = Validator[A, String, Future]
    def FutureValidator[A] = Validator[A, String, Future]

    type Message[-A, +B] = core.Context[A] => B
    type BaseMessage[-A] = Message[A, String]

    implicit def idF1Conversion[A, B](a: A => B): A => Id[B] = a.map(b => b)
}
