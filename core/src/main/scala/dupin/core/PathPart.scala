package dupin.core

trait PathPart {
    def value: String
}

case class FieldPart(value: String) extends PathPart

case class IndexPart(index: String) extends PathPart {
    val value = s"[$index]"
}
