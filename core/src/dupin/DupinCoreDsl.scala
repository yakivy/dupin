package dupin

trait DupinCoreDsl {
    type Validator[F[_], E, A] = core.Validator[F, E, A]
    val Validator = core.Validator

    type Context[+A] = core.Context[A]
    type MessageBuilder[-A, +E] = Context[A] => E
    type PathPart = core.PathPart
    type Path = core.Path
    val Path = core.Path
}
