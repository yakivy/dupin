package dupin.basic

import cats.Id

trait DupinBasicDslBinCompat {
    implicit def idF1Conversion[A, B](f: A => B): A => Id[B] = a => f(a)
}
