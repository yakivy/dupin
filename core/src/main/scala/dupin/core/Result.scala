package dupin.core

import cats.data.NonEmptyList

trait Result[+L, +R] {
    def either: Either[NonEmptyList[L], R]

    def messages[LL >: L](m1: LL, ms: LL*): Result[LL, R]

    def messages: List[L] = either.left.map(_.toList).left.getOrElse(Nil)

    def map[A](f: R => A): Result[L, A] = this match {
        case Success(a) => Success(f(a))
        case Fail(_) => this.asInstanceOf[Result[L, A]]
    }

    def leftMap[LL](f: L => LL): Result[LL, R] = this match {
        case Success(_) => this.asInstanceOf[Result[LL, R]]
        case Fail(a) => Fail(a.map(f))
    }

    def rightMap[RR](f: R => RR): Result[L, RR] = map(f)

    def bimap[LL, RR](fl: L => LL, fr: R => RR): Result[LL, RR] = leftMap(fl).rightMap(fr)

    def combine[LL >: L, RR >: R](a: Result[LL, RR]): Result[LL, RR] = this match {
        case Success(_) => a
        case Fail(v) => Fail(v ++ a.messages)
    }

    def orElse[LL >: L, RR >: R](a: Result[LL, RR]): Result[LL, RR] = this match {
        case v@Success(_) => v
        case Fail(_) => a
    }
}

case class Success[L, R](a: R) extends Result[L, R] {
    override def either: Either[NonEmptyList[L], R] = Right(a)
    override def messages[A >: L](m1: A, ms: A*): Result[A, R] = this
}

case class Fail[L, R](a: NonEmptyList[L]) extends Result[L, R] {
    override def either: Either[NonEmptyList[L], R] = Left(a)
    override def messages[A >: L](m1: A, ms: A*): Result[A, R] = Fail(NonEmptyList(m1, ms.toList))
}