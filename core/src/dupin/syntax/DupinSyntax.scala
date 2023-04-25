package dupin.syntax

import cats.Functor
import cats.data.IorNec
import cats.data.ValidatedNec
import dupin.core.Parser
import dupin.core.Validator

trait DupinSyntax {
    implicit def validatableOps[A](value: A): ValidatableOps[A] = new ValidatableOps(value)
    implicit def parsableOps[A](value: A): ParsableOps[A] = new ParsableOps(value)
}

class ValidatableOps[A](val value: A) extends AnyVal {
    def validate[F[_], E](implicit V: Validator[F, E, A], F: Functor[F]): F[ValidatedNec[E, A]] =
        V.validate(value)

    def isValid[F[_], E](implicit V: Validator[F, E, A], F: Functor[F]): F[Boolean] =
        F.map(V.validate(value))(_.isValid)
}

class ParsableOps[A](val value: A) extends AnyVal {
    def parse[F[_], E, B](implicit A: Parser[F, E, A, B]): F[IorNec[E, B]] =
        A.parse(value)
}
