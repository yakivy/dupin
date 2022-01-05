package dupin

trait DupinCoreDsl {
    type Validator[F[_], E, A] = core.Validator[F, E, A]
    val Validator = core.Validator

    type MessageBuilder[-A, +E] = core.Context[A] => E
}
