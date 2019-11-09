package dupin.core

import scala.collection.AbstractSeq
import scala.collection.immutable.LinearSeq

sealed abstract class Path extends AbstractSeq[PathPart] with LinearSeq[PathPart] {
    def ::(part: PathPart): Path = new ::(part, this)
    def :::(path: Path): Path = path.foldRight(this)(_ :: _)
    override def apply(n: Int): PathPart = {
        val rest = drop(n)
        if (n < 0 || rest.isEmpty) throw new IndexOutOfBoundsException("" + n)
        rest.head
    }
}
final case class ::(override val head: PathPart, override val tail: Path) extends Path {
    override def length: Int = 1 + tail.length
}
case object Root extends Path {
    override def head: Nothing = throw new NoSuchElementException("head of empty path")
    override def tail: Nothing = throw new NoSuchElementException("tail of empty path")
    override def length: Int = 0
}

trait PathPart {
    def value: String
}

case class FieldPart(value: String) extends PathPart
