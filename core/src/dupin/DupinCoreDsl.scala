package dupin

trait DupinCoreDsl {
    type Validator[F[_], E, A] = core.Validator[F, E, A]
    val Validator = core.Validator

    type Parser[F[_], E, A, B] = core.Parser[F, E, A, B]
    type IdParser[F[_], E, A] = Parser[F, E, A, A]
    val Parser = core.Parser

    type Context[+A] = core.Context[A]
    val Context = core.Context
    type MessageBuilder[-A, +E] = Context[A] => E
    type PathPart = core.PathPart
    type Path = core.Path
    val Path = core.Path
}
