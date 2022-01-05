package dupin

import cats._
import cats.data.NonEmptyChain
import cats.data.Validated
import dupin.basic._
import dupin.core.FieldPart
import dupin.core.Root
import org.scalatest.freespec.AnyFreeSpec

class ValidatorSpec extends AnyFreeSpec {
    "One field validator when" - {
        case class OneFieldDataStructure(value: String)
        val m: BasicMessageBuilder[Any] = c => s"${c.path} is invalid"

        "created from root should" - {
            val c: OneFieldDataStructure => Boolean = _.value != "invalid string"
            val v1 = BasicValidator.root(c, m)
            val v2 = BasicValidator.success[OneFieldDataStructure].combineR(c, m)

            "return success result" in {
                val ds = OneFieldDataStructure("valid string")
                val r = Validated.validNec(ds)
                assert(v1.validate(ds) == r)
                assert(v2.validate(ds) == r)
            }

            "return fail result" in {
                val ds = OneFieldDataStructure("invalid string")
                val r = Validated.invalidNec(". is invalid")
                assert(v1.validate(ds) == r)
                assert(v2.validate(ds) == r)
            }
        }

        val c: String => Boolean = _ != "invalid string"
        "created from explicit field path should" - {
            val p = FieldPart("value") :: Root
            val bv = BasicValidator.root[String](c, m)
            val v1 = bv.comapPE[OneFieldDataStructure](p, _.value)
            val v2 = BasicValidator.success[OneFieldDataStructure].combinePE(p, _.value)(bv)

            "return success result" in {
                val ds = OneFieldDataStructure("valid string")
                val r = Validated.validNec(ds)
                assert(v1.validate(ds) == r)
                assert(v2.validate(ds) == r)
            }

            "return fail result" in {
                val ds = OneFieldDataStructure("invalid string")
                val r = Validated.invalidNec(".value is invalid")
                assert(v1.validate(ds) == r)
                assert(v2.validate(ds) == r)
            }
        }

        "created from macros field path should" - {
            val v1 = BasicValidator.success[OneFieldDataStructure].combinePR(_.value)(c, m)
            implicit val bv: BasicValidator[String] = BasicValidator.root(c, m)
            val v2 = bv.comapP[OneFieldDataStructure](_.value)
            val v3 = BasicValidator.success[OneFieldDataStructure].combineP(_.value)(bv)
            val v4 = BasicValidator.success[OneFieldDataStructure].combinePI(_.value)

            "return success result" in {
                val ds = OneFieldDataStructure("valid string")
                val r = Validated.validNec(ds)
                assert(v1.validate(ds) == r)
                assert(v2.validate(ds) == r)
                assert(v3.validate(ds) == r)
                assert(v4.validate(ds) == r)
            }

            "return fail result" in {
                val ds = OneFieldDataStructure("invalid string")
                val r = Validated.invalidNec(".value is invalid")
                assert(v1.validate(ds) == r)
                assert(v2.validate(ds) == r)
                assert(v3.validate(ds) == r)
                assert(v4.validate(ds) == r)
            }
        }

        "created from macros lifted field path should" - {
            case class OneLiftedFieldDataStructure(value: Option[String])

            import cats.implicits._
            implicit val bv: BasicValidator[String] = BasicValidator.root(c, m)
            val v1 = bv.comapToP[Option].comapP[OneLiftedFieldDataStructure](_.value)
            val v2 = BasicValidator.success[OneLiftedFieldDataStructure].combinePL(_.value)(bv)
            val v3 = BasicValidator.success[OneLiftedFieldDataStructure].combinePI(_.value)

            "return success result" in {
                val ds1 = OneLiftedFieldDataStructure(Option("valid string"))
                val r1 = Validated.validNec(ds1)
                assert(v1.validate(ds1) == r1)
                assert(v2.validate(ds1) == r1)
                assert(v3.validate(ds1) == r1)

                val ds2 = OneLiftedFieldDataStructure(None)
                val r2 = Validated.validNec(ds2)
                assert(v1.validate(ds2) == r2)
                assert(v2.validate(ds2) == r2)
                assert(v3.validate(ds2) == r2)
            }

            "return fail result" in {
                val ds = OneLiftedFieldDataStructure(Option("invalid string"))
                val r = Validated.invalidNec(".value.[0] is invalid")
                assert(v1.validate(ds) == r)
                assert(v2.validate(ds) == r)
                assert(v3.validate(ds) == r)
            }
        }
    }

    "Two field validator when" - {
        case class TwoFieldDataStructure(v1: String, v2: Int)
        val m: BasicMessageBuilder[Any] = c => s"${c.path} is invalid"

        "created from root should" - {

            val c1: TwoFieldDataStructure => Boolean = _.v1 != "invalid string"
            val c2: TwoFieldDataStructure => Boolean = _.v2 != 0
            val v1 = BasicValidator.root(c1, m) combine BasicValidator.root(c2, m)
            val v2 = BasicValidator.success[TwoFieldDataStructure].combineR(c1, m).combineR(c2, m)

            "return success result with two successful checks" in {
                val ds = TwoFieldDataStructure("valid string", 1)
                val r = Validated.validNec(ds)
                assert(v1.validate(ds) == r)
                assert(v2.validate(ds) == r)
            }

            "return fail result with first fail check" in {
                val ds = TwoFieldDataStructure("invalid string", 1)
                val r = Validated.invalidNec(". is invalid")
                assert(v1.validate(ds) == r)
                assert(v2.validate(ds) == r)
            }

            "return fail result with second fail check" in {
                val ds = TwoFieldDataStructure("valid string", 0)
                val r = Validated.invalidNec(". is invalid")
                assert(v1.validate(ds) == r)
                assert(v2.validate(ds) == r)
            }

            "return fail result with two fail checks" in {
                val ds = TwoFieldDataStructure("invalid string", 0)
                val r = Validated.Invalid(NonEmptyChain(". is invalid", ". is invalid"))
                assert(v1.validate(ds) == r)
                assert(v2.validate(ds) == r)
            }
        }

        "created from field path should" - {
            val c1: String => Boolean = _ != "invalid string"
            val c2: Int => Boolean = _ != 0
            val v1 = BasicValidator.success[TwoFieldDataStructure]
                .combinePR(_.v1)(c1, m)
                .combinePR(_.v2)(c2, m)
            implicit val bv1: BasicValidator[String] = BasicValidator.root[String](c1, m)
            implicit val bv2: BasicValidator[Int] = BasicValidator.root[Int](c2, m)
            val v2 = bv1.comapP[TwoFieldDataStructure](_.v1) combine
                bv2.comapP[TwoFieldDataStructure](_.v2)
            val v3 = BasicValidator.success[TwoFieldDataStructure]
                .combineP(_.v1)(bv1)
                .combineP(_.v2)(bv2)
            val v4 = BasicValidator.success[TwoFieldDataStructure]
                .combinePI(_.v1)
                .combinePI(_.v2)

            "return success result with two successful checks" in {
                val ds = TwoFieldDataStructure("valid string", 1)
                val r = Validated.validNec(ds)
                assert(v1.validate(ds) == r)
                assert(v2.validate(ds) == r)
                assert(v3.validate(ds) == r)
                assert(v4.validate(ds) == r)
            }

            "return fail result with first fail check" in {
                val ds = TwoFieldDataStructure("invalid string", 1)
                val r = Validated.invalidNec(".v1 is invalid")
                assert(v1.validate(ds) == r)
                assert(v2.validate(ds) == r)
                assert(v3.validate(ds) == r)
                assert(v4.validate(ds) == r)
            }

            "return fail result with second fail check" in {
                val ds = TwoFieldDataStructure("valid string", 0)
                val r = Validated.invalidNec(".v2 is invalid")
                assert(v1.validate(ds) == r)
                assert(v2.validate(ds) == r)
                assert(v3.validate(ds) == r)
                assert(v4.validate(ds) == r)
            }

            "return fail result with two fail checks" in {
                val ds = TwoFieldDataStructure("invalid string", 0)
                val r = Validated.Invalid(NonEmptyChain(".v1 is invalid", ".v2 is invalid"))
                assert(v1.validate(ds) == r)
                assert(v2.validate(ds) == r)
                assert(v3.validate(ds) == r)
                assert(v4.validate(ds) == r)
            }
        }
    }

    "Two layer validator when" - {
        case class SecondLayerDataStructure(v: String)
        case class FirstLayerDataStructure(v1: SecondLayerDataStructure, v2: Int)
        val m: BasicMessageBuilder[Any] = c => s"${c.path} is invalid"

        val c1: String => Boolean = _ != "invalid string"
        val c2: Int => Boolean = _ != 0
        implicit val vi1: BasicValidator[SecondLayerDataStructure] = BasicValidator
            .success[SecondLayerDataStructure]
            .combinePR(_.v)(c1, m)

        "created from field path should" - {
            val v = BasicValidator.success[FirstLayerDataStructure]
                .combinePI(_.v1)
                .combinePR(_.v2)(c2, m)

            "return success result with two successful checks" in {
                val ds = FirstLayerDataStructure(SecondLayerDataStructure("valid string"), 1)
                val r = Validated.validNec(ds)
                assert(v.validate(ds) == r)
            }

            "return fail result with first fail check" in {
                val ds = FirstLayerDataStructure(SecondLayerDataStructure("invalid string"), 1)
                val r = Validated.invalidNec(".v1.v is invalid")
                assert(v.validate(ds) == r)
            }

            "return fail result with second fail check" in {
                val ds = FirstLayerDataStructure(SecondLayerDataStructure("valid string"), 0)
                val r = Validated.invalidNec(".v2 is invalid")
                assert(v.validate(ds) == r)
            }

            "return fail result with two fail checks" in {
                val ds = FirstLayerDataStructure(SecondLayerDataStructure("invalid string"), 0)
                val r = Validated.Invalid(NonEmptyChain(".v1.v is invalid", ".v2 is invalid"))
                assert(v.validate(ds) == r)
            }
        }

        "derived from validator type should" - {
            implicit val vi2: BasicValidator[Int] = BasicValidator.root[Int](c2, m)

            val v1 = BasicValidator.derive[FirstLayerDataStructure]

            "return success result with two successful checks" in {
                val ds = FirstLayerDataStructure(SecondLayerDataStructure("validdd string"), 1)
                val r = Validated.validNec(ds)
                assert(v1.validate(ds) == r)
            }

            "return fail result with first fail check" in {
                val ds = FirstLayerDataStructure(SecondLayerDataStructure("invalid string"), 1)
                val r = Validated.invalidNec(".v1.v is invalid")
                assert(v1.validate(ds) == r)
            }

            "return fail result with second fail check" in {
                val ds = FirstLayerDataStructure(SecondLayerDataStructure("valid string"), 0)
                val r = Validated.invalidNec(".v2 is invalid")
                assert(v1.validate(ds) == r)
            }

            "return fail result with two fail checks" in {
                val ds = FirstLayerDataStructure(SecondLayerDataStructure("invalid string"), 0)
                val r = Validated.Invalid(NonEmptyChain(".v1.v is invalid", ".v2 is invalid"))
                assert(v1.validate(ds) == r)
            }
        }
    }
}
