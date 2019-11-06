package dupin.core

import scala.collection.immutable.AbstractSeq
import scala.collection.immutable.LinearSeq

sealed abstract class Path extends AbstractSeq[PathPart] with LinearSeq[PathPart] {
    def ::(part: PathPart): Path = new ::(part, this)
    def :::(path: Path): Path = path.foldRight(this)(_ :: _)
}
final case class ::(override val head: PathPart, override val tail: Path) extends Path
case object Root extends Path {
    override def head: Nothing = throw new NoSuchElementException("head of empty path")
    override def tail: Nothing = throw new NoSuchElementException("tail of empty path")
}

trait PathPart {
    def value: String
}

case class FieldPart(value: String) extends PathPart
