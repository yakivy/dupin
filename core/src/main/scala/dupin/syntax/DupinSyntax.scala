package dupin.syntax

import cats.Functor
import cats.data.ValidatedNec
import dupin.core.Validator

trait DupinSyntax {
    implicit def validatableOps[R](value: R): ValidatableOps[R] = new ValidatableOps(value)
}

class ValidatableOps[R](val value: R) extends AnyVal {
    def validate[L, F[_]](implicit V: Validator[L, R, F]): F[ValidatedNec[L, R]] = V.validate(value)
    def isValid[L, F[_]](implicit V: Validator[L, R, F], F: Functor[F]): F[Boolean] =
        F.map(V.validate(value))(_.isValid)
}