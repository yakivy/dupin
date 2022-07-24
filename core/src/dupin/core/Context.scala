package dupin.core

case class Context[+A](path: Path, value: A) {
    def mapP[AA](path: Path, f: A => AA): Context[AA] = {
        Context(this.path ++ path, f(value))
    }
}
