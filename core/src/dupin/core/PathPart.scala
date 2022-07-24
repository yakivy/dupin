package dupin.core

sealed trait PathPart {
    def value: String
    override def toString: String = value
}

case class FieldPart(value: String) extends PathPart

case class IndexPart(index: String) extends PathPart {
    val value = s"[$index]"
}
