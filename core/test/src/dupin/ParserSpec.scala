package dupin

import cats.data.Chain
import cats.data.Ior
import cats.data.NonEmptyChain
import cats.data.Validated
import cats.implicits._
import dupin.basic._
import dupin.core.FieldPart
import org.scalatest.freespec.AsyncFreeSpec
import scala.concurrent.Future
import scala.util.Try

class ParserSpec extends AsyncFreeSpec {
    val stringC: String => Boolean = _ != "invalid string"
    val invalidStringM: BasicMessageBuilder[Any] = c => s"${c.path} is invalid"
    "Id parser" - {
        "created from root should" - {
            val vds = "valid string"
            val ivds = "invalid string"
            val p1 = BasicValidator.root(stringC, invalidStringM).toParser
            val p2 = BasicParser.idRoot[String](Option(_).filter(stringC), invalidStringM)

            "return success result" in {
                val r = Ior.right(vds)
                assert(p1.parse(vds) == r)
                assert(p2.parse(vds) == r)
            }

            "return fail result" in {
                val r = Ior.leftNec(". is invalid")
                assert(p1.parse(ivds) == r)
                assert(p2.parse(ivds) == r)
            }
        }
        "when created from simple type" - {
            "should be able to handle error" in {
                val p1 = BasicParser.idFailure[Int](_ => "failure")
                val p2 = p1.handleErrorWith(_ => BasicParser.idSuccess[Int])
                assert(p1.parse(1) == Ior.leftNec("failure"))
                assert(p2.parse(1) == Ior.right(1))
            }
            "should be able to map error" in {
                val p1 = BasicParser.idFailure[Int](_ => "failure")
                val p2 = p1.mapError(_ => "custom failure")
                assert(p1.parse(1) == Ior.leftNec("failure"))
                assert(p2.parse(1) == Ior.leftNec("custom failure"))
            }
        }
        "when created from list" - {
            "should return fail result" in {
                val p1 = BasicParser.idRoot[Int](Option(_).filter(_ < 2), c => s"${c.path} value `${c.value}` is wrong")
                val p2 = p1.liftToTraverseP[List]
                val p3 = p1.liftToTraverseCombiningP[List]

                assert(p1.parse(1) == Ior.right(1))
                assert(p1.parse(3) == Ior.leftNec(". value `3` is wrong"))
                assert(p2.parse(List(1, 2, 3)) == Ior.left(
                    NonEmptyChain(
                        ".[1] value `2` is wrong",
                        ".[2] value `3` is wrong",
                    )
                ))
                assert(p3.parse(List(1, 2, 3)) == Ior.both(
                    NonEmptyChain(
                        ".[1] value `2` is wrong",
                        ".[2] value `3` is wrong",
                    ),
                    List(1),
                ))
            }
            "should not throw stack overflow exception" in {
                val p1 = BasicParser.idSuccess[Int]
                val p2 = p1.liftToTraverseP[List]
                val p3 = p1.liftToTraverseCombiningP[Chain]

                assert(p2.parse(List.fill(1)(1000000)).isRight)
                assert(p3.parse(Chain(List.fill(1)(1000000):_*)).isRight)
            }
        }
    }

    "One field parser when" - {
        case class FromOneFieldDataStructure(value: String)
        case class ToOneFieldDataStructure(value: String)
        val vfds = FromOneFieldDataStructure("valid string")
        val vtds = ToOneFieldDataStructure("valid string")
        val ivfds = FromOneFieldDataStructure("invalid string")

        "created from root should" - {
            val p = BasicParser.root[FromOneFieldDataStructure, ToOneFieldDataStructure](
                Option(_).map(_.value).filter(stringC).map(ToOneFieldDataStructure.apply),
                invalidStringM,
            )

            "return success result" in {
                val r = Ior.right(vtds)
                assert(p.parse(vfds) == r)
            }

            "return fail result" in {
                val r = Ior.leftNec(". is invalid")
                assert(p.parse(ivfds) == r)
            }
        }
/*
        val c: String => Boolean = _ != "invalid string"
        "created from explicit field path should" - {
            val p = Path(FieldPart("value"))
            val bv = BasicValidator.root[String](c, m)
            val v1 = bv.comapPE[ToOneFieldDataStructure](p, _.value)
            val v2 = BasicValidator.success[ToOneFieldDataStructure].combinePE(p, _.value)(bv)

            "return success result" in {
                val r = Validated.validNec(vfds)
                assert(v1.validate(vfds) == r)
                assert(v2.validate(vfds) == r)
            }

            "return fail result" in {
                val r = Validated.invalidNec(".value is invalid")
                assert(v1.validate(ivfds) == r)
                assert(v2.validate(ivfds) == r)
            }
        }

        "created from macros field path should" - {
            val v1 = BasicValidator.success[ToOneFieldDataStructure].combinePR(_.value)(c, m)
            implicit val bv: BasicValidator[String] = BasicValidator.root(c, m)
            val v2 = bv.comapP[ToOneFieldDataStructure](_.value)
            val v3 = BasicValidator.success[ToOneFieldDataStructure].combineP(_.value)(bv)
            val v4 = BasicValidator.success[ToOneFieldDataStructure].combinePI(_.value)

            "return success result" in {
                val r = Validated.validNec(vfds)
                assert(v1.validate(vfds) == r)
                assert(v2.validate(vfds) == r)
                assert(v3.validate(vfds) == r)
                assert(v4.validate(vfds) == r)
            }

            "return fail result" in {
                val r = Validated.invalidNec(".value is invalid")
                assert(v1.validate(ivfds) == r)
                assert(v2.validate(ivfds) == r)
                assert(v3.validate(ivfds) == r)
                assert(v4.validate(ivfds) == r)
            }
        }

        "created from macros lifted field path should" - {
            case class OneLiftedFieldDataStructure(value: Option[String])

            import cats.implicits._
            implicit val bv: BasicValidator[String] = BasicValidator.root(c, m)
            val v1 = bv.liftToTraverseP[Option].comapP[OneLiftedFieldDataStructure](_.value)
            val v2 = BasicValidator.success[OneLiftedFieldDataStructure].combinePI(_.value)

            "return success result" in {
                val ds1 = OneLiftedFieldDataStructure(Option("valid string"))
                val r1 = Validated.validNec(ds1)
                assert(v1.validate(ds1) == r1)
                assert(v2.validate(ds1) == r1)

                val ds2 = OneLiftedFieldDataStructure(None)
                val r2 = Validated.validNec(ds2)
                assert(v1.validate(ds2) == r2)
                assert(v2.validate(ds2) == r2)
            }

            "return fail result" in {
                val ds = OneLiftedFieldDataStructure(Option("invalid string"))
                val r = Validated.invalidNec(".value.[0] is invalid")
                assert(v1.validate(ds) == r)
                assert(v2.validate(ds) == r)
                assert(v2.validate(ds) == r)
            }
        }

        "effect should" - {
            type FutureValidator[A] = Validator[Future, String, A]
            val FutureValidator = Validator[Future, String]

            val c1 = c.andThen(Future.successful)
            implicit val bv: FutureValidator[String] = FutureValidator.rootF(c1, m)
            val v1 = bv.comapP[ToOneFieldDataStructure](_.value)
            val v2 = FutureValidator.success[String].combineRF(c1, m).comapP[ToOneFieldDataStructure](_.value)
            val v3 = FutureValidator.success[String].combineR(c, m).comapP[ToOneFieldDataStructure](_.value)
            val v4 = FutureValidator.success[ToOneFieldDataStructure].combinePRF(_.value)(c1, m)
            val v5 = FutureValidator.success[ToOneFieldDataStructure].combinePR(_.value)(c, m)

            "return success result" in {
                val er = Validated.validNec(vfds)
                (
                    v1.validate(vfds), v2.validate(vfds), v3.validate(vfds), v4.validate(vfds), v5.validate(vfds)
                ).mapN { (vr1, vr2, vr3, vr4, vr5) =>
                    assert(vr1 == er)
                    assert(vr2 == er)
                    assert(vr3 == er)
                    assert(vr4 == er)
                    assert(vr5 == er)
                }
            }

            "return fail result" in {
                val er = Validated.invalidNec(".value is invalid")
                (
                    v1.validate(ivfds), v2.validate(ivfds), v3.validate(ivfds), v4.validate(ivfds), v5.validate(ivfds)
                ).mapN { (vr1, vr2, vr3, vr4, vr5) =>
                    assert(vr1 == er)
                    assert(vr2 == er)
                    assert(vr3 == er)
                    assert(vr4 == er)
                    assert(vr5 == er)
                }
            }
        }*/
    }

    /*"Two field validator when" - {
        case class TwoFieldDataStructure(v1: String, v2: Int)
        val m: BasicMessageBuilder[Any] = c => s"${c.path} is invalid"
        val c1: String => Boolean = _ != "invalid string"
        val c2: Int => Boolean = _ != 0

        "created from root should" - {
            val rc1: TwoFieldDataStructure => Boolean = c1.compose[TwoFieldDataStructure](_.v1)
            val rc2: TwoFieldDataStructure => Boolean = c2.compose[TwoFieldDataStructure](_.v2)
            val v1 = BasicValidator.root(rc1, m) combine BasicValidator.root(rc2, m)
            val v2 = BasicValidator.success[TwoFieldDataStructure].combineR(rc1, m).combineR(rc2, m)

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

        "created in two stages should" - {
            val v = BasicValidator.root(c1, m).comapP[TwoFieldDataStructure](_.v1) andThen
                BasicValidator.root(c2, m).comapP[TwoFieldDataStructure](_.v2)

            "return success result with two successful checks" in {
                val ds = TwoFieldDataStructure("valid string", 1)
                val r = Validated.validNec(ds)
                assert(v.validate(ds) == r)
            }

            "return fail result with first fail check" in {
                val ds = TwoFieldDataStructure("invalid string", 1)
                val r = Validated.invalidNec(".v1 is invalid")
                assert(v.validate(ds) == r)
            }

            "return fail result with second fail check" in {
                val ds = TwoFieldDataStructure("valid string", 0)
                val r = Validated.invalidNec(".v2 is invalid")
                assert(v.validate(ds) == r)
            }

            "return only first fail result with two fail checks" in {
                val ds = TwoFieldDataStructure("invalid string", 0)
                val r = Validated.invalidNec(".v1 is invalid")
                assert(v.validate(ds) == r)
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
            val v1 = BasicValidator.success[FirstLayerDataStructure]
                .combinePI(_.v1)
                .combinePR(_.v2)(c2, m)
            val v2 = BasicValidator.success[FirstLayerDataStructure]
                .combinePR(_.v1.v)(c1, m)
                .combinePR(_.v2)(c2, m)

            "return success result with two successful checks" in {
                val ds = FirstLayerDataStructure(SecondLayerDataStructure("valid string"), 1)
                val r = Validated.validNec(ds)
                assert(v1.validate(ds) == r)
                assert(v2.validate(ds) == r)
            }

            "return fail result with first fail check" in {
                val ds = FirstLayerDataStructure(SecondLayerDataStructure("invalid string"), 1)
                val r = Validated.invalidNec(".v1.v is invalid")
                assert(v1.validate(ds) == r)
                assert(v2.validate(ds) == r)
            }

            "return fail result with second fail check" in {
                val ds = FirstLayerDataStructure(SecondLayerDataStructure("valid string"), 0)
                val r = Validated.invalidNec(".v2 is invalid")
                assert(v1.validate(ds) == r)
                assert(v2.validate(ds) == r)
            }

            "return fail result with two fail checks" in {
                val ds = FirstLayerDataStructure(SecondLayerDataStructure("invalid string"), 0)
                val r = Validated.Invalid(NonEmptyChain(".v1.v is invalid", ".v2 is invalid"))
                assert(v1.validate(ds) == r)
                assert(v2.validate(ds) == r)
            }
        }

        "created from context should" - {
            val c3: Int => String => Boolean = fv => sv => !Try(sv.toInt).toOption.contains(fv)
            val m1: BasicMessageBuilder[Any] = c => s"${c.path} cannot be equal to value from context"
            def vi2 = (c: FirstLayerDataStructure) => BasicValidator
                .success[SecondLayerDataStructure]
                .combinePR(_.v)(c3(c.v2), m1)

            val v1 = BasicValidator.context[FirstLayerDataStructure](vi2(_).comapP(_.v1))
            val v2 = BasicValidator.success[FirstLayerDataStructure].combinePC(_.v1)(vi2)
            val v3 = BasicValidator.success[FirstLayerDataStructure].combineC(c => vi2(c).comapP(_.v1))

            "return success result" in {
                val ds = FirstLayerDataStructure(SecondLayerDataStructure("valid string"), 1)
                val r = Validated.validNec(ds)
                assert(v1.validate(ds) == r)
                assert(v2.validate(ds) == r)
                assert(v3.validate(ds) == r)
            }
            "return fail result" in {
                val ds = FirstLayerDataStructure(SecondLayerDataStructure("1"), 1)
                val r = Validated.Invalid(NonEmptyChain(".v1.v cannot be equal to value from context"))
                assert(v1.validate(ds) == r)
                assert(v2.validate(ds) == r)
                assert(v3.validate(ds) == r)
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
    }*/
}
