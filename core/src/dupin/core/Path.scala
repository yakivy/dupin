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
    override def toString(): String = reverse.map(_.value).mkString(".", ".", "")
}

final case class ::(override val head: PathPart, override val tail: Path) extends Path {
    override def length: Int = 1 + tail.length
    override def isEmpty: Boolean = false
}

case object Root extends Path {
    override def head: Nothing = throw new NoSuchElementException("head of empty path")
    override def tail: Nothing = throw new NoSuchElementException("tail of empty path")
    override def length: Int = 0
    override def isEmpty: Boolean = true
}
