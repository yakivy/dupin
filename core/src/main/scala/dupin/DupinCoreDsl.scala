package dupin

trait DupinCoreDsl {
    type Validator[L, R, F[_]] = core.Validator[L, R, F]
    def Validator[L, R, F[_]] = core.Builder[L, R, F]

    type MessageBuilder[-R, +L] = core.Context[R] => L
}
