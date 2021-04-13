package dupin.syntax

import cats.Functor
import cats.data.ValidatedNec
import dupin.core.Validator

trait DupinSyntax {
    implicit def validatableOps[A](value: A): ValidatableOps[A] = new ValidatableOps(value)
}

class ValidatableOps[A](val value: A) extends AnyVal {
    def validate[F[_], E](implicit V: Validator[F, E, A]): F[ValidatedNec[E, A]] = V.validate(value)
    def isValid[F[_], E](implicit V: Validator[F, E, A], F: Functor[F]): F[Boolean] =
        F.map(V.validate(value))(_.isValid)
}