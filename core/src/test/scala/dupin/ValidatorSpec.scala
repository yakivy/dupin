package dupin

import cats.data.NonEmptyList
import dupin.base._
import dupin.core.Fail
import dupin.core.FieldPart
import dupin.core.Root
import dupin.core.Success
import org.scalatest.WordSpec

class ValidatorSpec extends WordSpec {
    "One field validator" when {
        case class OneFieldDataStructure(value: String)
        val m: BaseMessageBuilder[Any] = _.path + " is invalid"

        "created from root" should {
            val c: OneFieldDataStructure => Boolean = _.value != "invalid string"
            val v1 = BaseValidator[OneFieldDataStructure].root(c, m)
            val v2 = BaseValidator[OneFieldDataStructure].combineR(c, m)

            "return success result" in {
                val ds = OneFieldDataStructure("valid string")
                val r = Success(ds)
                assert(v1.validate(ds) == r)
                assert(v2.validate(ds) == r)
            }

            "return fail result" in {
                val ds = OneFieldDataStructure("invalid string")
                val r = Fail(NonEmptyList(". is invalid", Nil))
                assert(v1.validate(ds) == r)
                assert(v2.validate(ds) == r)
            }
        }

        val c: String => Boolean = _ != "invalid string"
        "created from explicit field path" should {
            val p = FieldPart("value") :: Root
            val v = BaseValidator[OneFieldDataStructure].path(p, _.value)(BaseValidator[String].root(c, m))

            "return success result" in {
                val ds = OneFieldDataStructure("valid string")
                val r = Success(ds)
                assert(v.validate(ds) == r)
            }

            "return fail result" in {
                val ds = OneFieldDataStructure("invalid string")
                val r = Fail(NonEmptyList(".value is invalid", Nil))
                assert(v.validate(ds) == r)
            }
        }

        "created from macros field path" should {
            val v1 = BaseValidator[OneFieldDataStructure].combinePR(_.value)(c, m)
            implicit val bv = BaseValidator[String].root(c, m)
            val v2 = BaseValidator[OneFieldDataStructure].path(_.value)(bv)
            val v3 = BaseValidator[OneFieldDataStructure].combineP(_.value)(bv)
            val v4 = BaseValidator[OneFieldDataStructure].combinePI(_.value)

            "return success result" in {
                val ds = OneFieldDataStructure("valid string")
                val r = Success(ds)
                assert(v1.validate(ds) == r)
                assert(v2.validate(ds) == r)
                assert(v3.validate(ds) == r)
                assert(v4.validate(ds) == r)
            }

            "return fail result" in {
                val ds = OneFieldDataStructure("invalid string")
                val r = Fail(NonEmptyList(".value is invalid", Nil))
                assert(v1.validate(ds) == r)
                assert(v2.validate(ds) == r)
                assert(v3.validate(ds) == r)
                assert(v4.validate(ds) == r)
            }
        }
    }

    "Two field validator" when {
        case class TwoFieldDataStructure(v1: String, v2: Int)
        val m: BaseMessageBuilder[Any] = _.path + " is invalid"

        "created from root" should {
            val c1: TwoFieldDataStructure => Boolean = _.v1 != "invalid string"
            val c2: TwoFieldDataStructure => Boolean = _.v2 != 0
            val v = BaseValidator[TwoFieldDataStructure].combineR(c1, m).combineR(c2, m)

            "return success result with two successful checks" in {
                val ds = TwoFieldDataStructure("valid string", 1)
                val r = Success(ds)
                assert(v.validate(ds) == r)
            }

            "return fail result with first fail check" in {
                val ds = TwoFieldDataStructure("invalid string", 1)
                val r = Fail(NonEmptyList(". is invalid", Nil))
                assert(v.validate(ds) == r)
            }

            "return fail result with second fail check" in {
                val ds = TwoFieldDataStructure("valid string", 0)
                val r = Fail(NonEmptyList(". is invalid", Nil))
                assert(v.validate(ds) == r)
            }

            "return fail result with two fail checks" in {
                val ds = TwoFieldDataStructure("invalid string", 0)
                val r = Fail(NonEmptyList(". is invalid", List(". is invalid")))
                assert(v.validate(ds) == r)
            }
        }

        "created from field path" should {
            val c1: String => Boolean = _ != "invalid string"
            val c2: Int => Boolean = _ != 0
            val v1 = BaseValidator[TwoFieldDataStructure]
                .combinePR(_.v1)(c1, m)
                .combinePR(_.v2)(c2, m)
            implicit val bv1 = BaseValidator[String].root(c1, m)
            implicit val bv2 = BaseValidator[Int].root(c2, m)
            val v2 = BaseValidator[TwoFieldDataStructure]
                .combineP(_.v1)(bv1)
                .combineP(_.v2)(bv2)
            val v3 = BaseValidator[TwoFieldDataStructure]
                .combinePI(_.v1)
                .combinePI(_.v2)

            "return success result with two successful checks" in {
                val ds = TwoFieldDataStructure("valid string", 1)
                val r = Success(ds)
                assert(v1.validate(ds) == r)
                assert(v2.validate(ds) == r)
                assert(v3.validate(ds) == r)
            }

            "return fail result with first fail check" in {
                val ds = TwoFieldDataStructure("invalid string", 1)
                val r = Fail(NonEmptyList(".v1 is invalid", Nil))
                assert(v1.validate(ds) == r)
                assert(v2.validate(ds) == r)
                assert(v3.validate(ds) == r)
            }

            "return fail result with second fail check" in {
                val ds = TwoFieldDataStructure("valid string", 0)
                val r = Fail(NonEmptyList(".v2 is invalid", Nil))
                assert(v1.validate(ds) == r)
                assert(v2.validate(ds) == r)
                assert(v3.validate(ds) == r)
            }

            "return fail result with two fail checks" in {
                val ds = TwoFieldDataStructure("invalid string", 0)
                val r = Fail(NonEmptyList(".v1 is invalid", List(".v2 is invalid")))
                assert(v1.validate(ds) == r)
                assert(v2.validate(ds) == r)
                assert(v3.validate(ds) == r)
            }
        }
    }

    "Two layer validator" when {
        case class SecondLayerDataStructure(v: String)
        case class FirstLayerDataStructure(v1: SecondLayerDataStructure, v2: Int)
        val m: BaseMessageBuilder[Any] = _.path + " is invalid"

        val c1: String => Boolean = _ != "invalid string"
        val c2: Int => Boolean = _ != 0
        implicit val vi1 = BaseValidator[SecondLayerDataStructure].pathR(_.v)(c1, m)

        "created from field path" should {
            val v = BaseValidator[FirstLayerDataStructure]
                .combinePI(_.v1)
                .combinePR(_.v2)(c2, m)

            "return success result with two successful checks" in {
                val ds = FirstLayerDataStructure(SecondLayerDataStructure("valid string"), 1)
                val r = Success(ds)
                assert(v.validate(ds) == r)
            }

            "return fail result with first fail check" in {
                val ds = FirstLayerDataStructure(SecondLayerDataStructure("invalid string"), 1)
                val r = Fail(NonEmptyList(".v1.v is invalid", Nil))
                assert(v.validate(ds) == r)
            }

            "return fail result with second fail check" in {
                val ds = FirstLayerDataStructure(SecondLayerDataStructure("valid string"), 0)
                val r = Fail(NonEmptyList(".v2 is invalid", Nil))
                assert(v.validate(ds) == r)
            }

            "return fail result with two fail checks" in {
                val ds = FirstLayerDataStructure(SecondLayerDataStructure("invalid string"), 0)
                val r = Fail(NonEmptyList(".v1.v is invalid", ".v2 is invalid" :: Nil))
                assert(v.validate(ds) == r)
            }
        }

        "derived from validator type" should {
            implicit val vi2 = BaseValidator[Int].root(c2, m)

            val v1 = BaseValidator[FirstLayerDataStructure].derive
            //scala bug can't use them in same compilation unit
            //val v2 = BaseValidator[FirstLayerDataStructure].combineD

            "return success result with two successful checks" in {
                val ds = FirstLayerDataStructure(SecondLayerDataStructure("validdd string"), 1)
                val r = Success(ds)
                assert(v1.validate(ds) == r)
            }

            "return fail result with first fail check" in {
                val ds = FirstLayerDataStructure(SecondLayerDataStructure("invalid string"), 1)
                val r = Fail(NonEmptyList(".v1.v is invalid", Nil))
                assert(v1.validate(ds) == r)
            }

            "return fail result with second fail check" in {
                val ds = FirstLayerDataStructure(SecondLayerDataStructure("valid string"), 0)
                val r = Fail(NonEmptyList(".v2 is invalid", Nil))
                assert(v1.validate(ds) == r)
            }

            "return fail result with two fail checks" in {
                val ds = FirstLayerDataStructure(SecondLayerDataStructure("invalid string"), 0)
                val r = Fail(NonEmptyList(".v1.v is invalid", ".v2 is invalid" :: Nil))
                assert(v1.validate(ds) == r)
            }
        }
    }
}
