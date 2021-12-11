package dupin

trait DupinCoreDsl {
    type Validator[F[_], E, A] = core.Validator[F, E, A]
    def Validator[F[_], E] = core.Validator[F, E]

    type MessageBuilder[-A, +E] = core.Context[A] => E
}
