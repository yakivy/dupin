package dupin

import cats.Applicative

trait DupinCoreDsl {
    type Validator[F[_], E, A] = core.Validator[F, E, A]
    def Validator[F[_], E, A](implicit A: Applicative[F]) = core.Validator[F, E, A]

    type MessageBuilder[-A, +E] = core.Context[A] => E
}
