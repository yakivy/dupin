package dupin.core

case class Context[+A](path: Path, value: A) {
    def map[B](prefix: Path, f: A => B): Context[B] =
        Context(prefix ::: path, f(value))
}
