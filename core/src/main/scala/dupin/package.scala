package object dupin {
    type Validator[L, R, F[_]] = core.Validator[L, R, F]
    def Validator[L, R, F[_]] = core.Builder[L, R, F]

    type Message[-R, +L] = core.Context[R] => L
}
