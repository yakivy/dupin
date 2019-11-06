package dupin.core

case class Context[+R](path: Path, value: R) {
    def map[RR](prefix: Path, f: R => RR): Context[RR] =
        Context(prefix ::: path, f(value))
}
