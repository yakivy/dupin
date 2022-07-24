package dupin

import dupin.core.FieldPart
import dupin.core.IndexPart
import org.scalatest.freespec.AnyFreeSpec

class PathSpec extends AnyFreeSpec {
    "Path should pass basic checks" in {
        val namePart = FieldPart("name")
        val firstPart = IndexPart("0")
        assert(Path.empty ++ Path.empty == Path.empty)
        assert(Path(namePart) ++ Path(firstPart) == Path(namePart, firstPart))
        assert(namePart +: Path.empty == Path(namePart))
        assert(Path.empty :+ namePart == Path(namePart))
        assert(Path.empty.toString() == ".")
        assert(Path(namePart).toString() == ".name")
        assert(Path(namePart, firstPart).toString() == ".name.[0]")
    }
}
