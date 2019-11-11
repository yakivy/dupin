package dupin.syntax

import cats.Functor
import dupin.core.Result
import dupin.core.Validator

trait ValidatorSyntax {
    implicit def validatableOps[R](value: R): ValidatableOps[R] = new ValidatableOps(value)
}

class ValidatableOps[R](val value: R) extends AnyVal {
    def validate[L, F[_]](implicit V: Validator[L, R, F], F: Functor[F]): F[Result[L, R]] = V.validate(value)
}