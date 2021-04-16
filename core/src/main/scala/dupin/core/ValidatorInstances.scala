package dupin.core

trait ValidatorInstances {
    implicit def validatorForTraverse[F[_], E, A, FA[_]](
        implicit v: Validator[F, E, A], composeK: ValidatorComposeK[F, FA]
    ): Validator[F, E, FA[A]] = composeK(v)
}
