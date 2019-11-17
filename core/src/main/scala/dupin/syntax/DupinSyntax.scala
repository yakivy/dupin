package dupin.syntax

import cats.Functor
import dupin.core.Result
import dupin.core.Validator

trait DupinSyntax {
    implicit def validatableOps[R](value: R): ValidatableOps[R] = new ValidatableOps(value)
}

class ValidatableOps[R](val value: R) extends AnyVal {
    def validate[L, F[_]](implicit V: Validator[L, R, F], F: Functor[F]): F[Result[L, R]] = V.validate(value)
    def isValid[L, F[_]](implicit V: Validator[L, R, F], F: Functor[F]): F[Boolean] =
        F.map(V.validate(value))(_.either.isRight)
}