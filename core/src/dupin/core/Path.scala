package dupin.core

import cats.data.Chain

case class Path(chain: Chain[PathPart]) {
    def concat(p: Path): Path = Path(chain.concat(p.chain))
    def ++(p: Path): Path = concat(p)
    def prepend(p: PathPart): Path = Path(chain.prepend(p))
    def +:(p: PathPart): Path = prepend(p)
    def append(p: PathPart): Path = Path(chain.append(p))
    def :+(p: PathPart): Path = append(p)
    override def toString: String = chain.iterator.mkString(".", ".", "")
}

object Path {
    val empty: Path = Path(Chain.empty)
    def apply(elems: PathPart*): Path = Path(Chain(elems: _*))
}
