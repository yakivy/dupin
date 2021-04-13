package dupin.core

case class Context[+A](path: Path, value: A) {
    def map[AA](prefix: Path, f: A => AA): Context[AA] =
        Context(prefix ::: path, f(value))
}
