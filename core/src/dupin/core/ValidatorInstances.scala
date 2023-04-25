package dupin.core

import cats._

trait ValidatorInstances {
    implicit final def validatorContravariantMonoidal[F[_]: Applicative, E]: ContravariantMonoidal[Validator[F, E, *]] =
        new ContravariantMonoidal[Validator[F, E, *]] {
            override def unit: Validator[F, E, Unit] = Validator[F, E].success[Unit]
            override def product[A, B](fa: Validator[F, E, A], fb: Validator[F, E, B]): Validator[F, E, (A, B)] =
                fa.product(fb)
            override def contramap[A, B](fa: Validator[F, E, A])(f: B => A): Validator[F, E, B] = fa.comap(f)
        }

    implicit final def validatorMonoidK[F[_]: Applicative, E]: MonoidK[Validator[F, E, *]] =
        new MonoidK[Validator[F, E, *]] {
            override def empty[A]: Validator[F, E, A] = Validator[F, E].success[A]
            override def combineK[A](x: Validator[F, E, A], y: Validator[F, E, A]): Validator[F, E, A] =
                x.combine(y)
        }

    implicit final def validatorLiftedToTraverseP[F[_], E, A, G[_]](implicit
        v: Validator[F, E, A],
        F: Applicative[F],
        G: Traverse[G],
    ): Validator[F, E, G[A]] = v.liftToTraverseP[G]
}
