package dupin

import org.scalatest.Assertion
import org.scalatest.exceptions.TestFailedException
import org.scalatest.freespec.AsyncFreeSpec
import org.scalactic.source.Position
import scala.util.matching.Regex

class CustomSpec extends AsyncFreeSpec {
    def assertCompilationErrorMessagePattern(
        compilesAssert: => Assertion,
        pattern: Regex,
    )(implicit
        pos: Position,
    ): Assertion = {
        try {
            compilesAssert
            fail("Compilation was successful")
        } catch {
            case e: TestFailedException =>
                val from = "but got the following type error: \""
                val to = "\", for code:"
                val message = e.getMessage().substring(
                    e.getMessage().indexOf(from) + from.size,
                    e.getMessage().indexOf(to),
                )
                assert(
                    pattern.pattern.matcher(message).matches(),
                    s"""\nCompilation error "$message" does not match "$pattern" pattern""",
                )
        }
    }
}
