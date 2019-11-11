package dupin.core

import cats.data.NonEmptyList

trait Result[+L, +R] {
    def either: Either[NonEmptyList[L], R]

    def list: List[L] = either.left.map(_.toList).left.getOrElse(Nil)

    def recoverWith[LL >: L, RR >: R](f: NonEmptyList[LL] => Result[LL, RR]): Result[LL, RR] = this match {
        case Success(_) => this
        case Fail(a) => f(a)
    }

    def map[RR](f: R => RR): Result[L, RR] = this match {
        case Success(a) => Success(f(a))
        case Fail(_) => this.asInstanceOf[Result[L, RR]]
    }

    def leftMap[LL](f: L => LL): Result[LL, R] = this match {
        case Success(_) => this.asInstanceOf[Result[LL, R]]
        case Fail(a) => Fail(a.map(f))
    }

    def rightMap[RR](f: R => RR): Result[L, RR] = map(f)

    def bimap[LL, RR](fl: L => LL, fr: R => RR): Result[LL, RR] = leftMap(fl).rightMap(fr)

    def combine[LL >: L, RR >: R](a: Result[LL, RR]): Result[LL, RR] = this match {
        case Success(_) => a
        case Fail(v) => Fail(v ++ a.list)
    }

    def orElse[LL >: L, RR >: R](a: Result[LL, RR]): Result[LL, RR] = this match {
        case v@Success(_) => v
        case Fail(_) => a
    }
}

case class Success[L, R](a: R) extends Result[L, R] {
    override def either: Either[NonEmptyList[L], R] = Right(a)
}

case class Fail[L, R](a: NonEmptyList[L]) extends Result[L, R] {
    override def either: Either[NonEmptyList[L], R] = Left(a)
}