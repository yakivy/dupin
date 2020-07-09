package dupin

import cats.Applicative

trait DupinCoreDsl {
    type Validator[L, R, F[_]] = core.Validator[L, R, F]
    def Validator[L, R, F[_]](implicit A: Applicative[F]) = core.Validator[L, R, F]

    type MessageBuilder[-R, +L] = core.Context[R] => L
}
