package dupin.core

case class Context[+A](path: Path, value: A)
