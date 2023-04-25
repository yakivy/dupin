package dupin.readme

import cats.data.Ior
import cats.data.IorNec
import cats.data.NonEmptyChain
import cats.data.NonEmptyList
import org.scalatest.freespec.AnyFreeSpecLike

trait ComplexExampleFixture extends AnyFreeSpecLike {
    case class RawTermModel(
        term: String,
        mistakes: List[String],
    )
    case class Term(value: String) {
        override def toString: String = value
    }
    case class TermModel(
        term: Term,
        mistakes: NonEmptyList[Term],
    )

    type R[A] = Either[String, A]
    object TermRepository {
        def contains(term: Term): R[Boolean] = {
            if (term == Term("exists")) Right(true)
            else if (term == Term("error")) Left("error")
            else Right(false)
        }
    }

    def parse(rawModels: List[RawTermModel]): R[IorNec[String, List[TermModel]]]

    "Complex example parsers should" - {
        val validRawTermWith1Mistake = RawTermModel("term1", List("mistake1"))
        val validRawTermWith2Mistakes = RawTermModel("term2", List("mistake21", "mistake22"))
        val parsedTermWith1Mistake = TermModel(Term("term1"), NonEmptyList.one(Term("mistake1")))
        val parsedTermWith2Mistakes = TermModel(Term("term2"), NonEmptyList.of(Term("mistake21"), Term("mistake22")))
        "return empty list if empty models were passed" in {
            val models = List.empty
            assert(parse(models) == Right(Ior.right(List.empty)))
        }
        "return valid models" in {
            val models = List(
                validRawTermWith1Mistake,
                validRawTermWith2Mistakes
            )
            assert(parse(models) == Right(Ior.right(List(
                parsedTermWith1Mistake,
                parsedTermWith2Mistakes
            ))))
        }
        "filter out term that is not a word" in {
            val models = List(
                validRawTermWith1Mistake,
                RawTermModel("invalid term", validRawTermWith2Mistakes.mistakes),
            )
            assert(parse(models) == Right(Ior.both(
                NonEmptyChain.one(".[1].term: cannot parse string 'invalid term' to a term"),
                List(
                    parsedTermWith1Mistake,
                )
            )))
        }
        "filter out term with two invalid fields" in {
            val models = List(
                validRawTermWith1Mistake,
                RawTermModel("invalid term", List("invalid mistake")),
            )
            assert(parse(models) == Right(Ior.both(
                NonEmptyChain(
                    ".[1].term: cannot parse string 'invalid term' to a term",
                    ".[1].mistakes.[0]: cannot parse string 'invalid mistake' to a term",
                ),
                List(
                    parsedTermWith1Mistake,
                )
            )))
        }
        "filter out term that exists in repository" in {
            val models = List(
                validRawTermWith1Mistake,
                RawTermModel("exists", validRawTermWith2Mistakes.mistakes),
            )
            assert(parse(models) == Right(Ior.both(
                NonEmptyChain.one(".[1].term: term 'exists' already exists"),
                List(
                    parsedTermWith1Mistake,
                )
            )))
        }
        "filter out mistake that is not a word" in {
            val models = List(
                validRawTermWith1Mistake,
                RawTermModel(validRawTermWith2Mistakes.term, "invalid mistake" :: validRawTermWith2Mistakes.mistakes),
            )
            assert(parse(models) == Right(Ior.both(
                NonEmptyChain.one(".[1].mistakes.[0]: cannot parse string 'invalid mistake' to a term"),
                List(
                    parsedTermWith1Mistake,
                    parsedTermWith2Mistakes,
                )
            )))
        }
        "filter out duplicated terms" in {
            val models = List(
                validRawTermWith1Mistake,
                RawTermModel(validRawTermWith1Mistake.term, validRawTermWith2Mistakes.mistakes)
            )
            assert(parse(models) == Right(Ior.both(
                NonEmptyChain(
                    ".[1].term: term 'term1' is duplicate",
                ),
                List(
                    parsedTermWith1Mistake,
                )
            )))
        }
        "filter out duplicated mistakes" in {
            val models = List(
                RawTermModel(validRawTermWith1Mistake.term, "mistake1" :: validRawTermWith1Mistake.mistakes),
                RawTermModel(validRawTermWith2Mistakes.term, "mistake1" :: validRawTermWith2Mistakes.mistakes)
            )
            assert(parse(models) == Right(Ior.both(
                NonEmptyChain(
                    ".[0].mistakes.[1]: mistake 'mistake1' is duplicate",
                    ".[1].mistakes.[0]: mistake 'mistake1' is duplicate",
                ),
                List(
                    parsedTermWith1Mistake,
                    parsedTermWith2Mistakes,
                )
            )))
        }
        "filter out mistake that duplicates term" in {
            val models = List(
                RawTermModel(validRawTermWith1Mistake.term, "term2" :: validRawTermWith1Mistake.mistakes),
                validRawTermWith2Mistakes,
            )
            assert(parse(models) == Right(Ior.both(
                NonEmptyChain(
                    ".[0].mistakes.[0]: mistake 'term2' is duplicate",
                ),
                List(
                    parsedTermWith1Mistake,
                    parsedTermWith2Mistakes,
                )
            )))
        }
        "filter out term if mistakes are empty" in {
            val models = List(
                validRawTermWith1Mistake,
                RawTermModel("term2", List.empty),
            )
            assert(parse(models) == Right(Ior.both(
                NonEmptyChain.one(".[1].mistakes: cannot be empty"),
                List(
                    parsedTermWith1Mistake,
                )
            )))
        }
        "filter out term with invalid mistakes only" in {
            val models = List(
                validRawTermWith1Mistake,
                RawTermModel(validRawTermWith2Mistakes.term, List("invalid mistake 1", "invalid mistake 2"))
            )
            assert(parse(models) == Right(Ior.both(
                NonEmptyChain(
                    ".[1].mistakes.[0]: cannot parse string 'invalid mistake 1' to a term",
                    ".[1].mistakes.[1]: cannot parse string 'invalid mistake 2' to a term",
                    ".[1].mistakes: cannot be empty",
                ),
                List(
                    parsedTermWith1Mistake,
                )
            )))
        }
        "return error if repository returns error" in {
            val models = List(RawTermModel("error", List.empty))
            assert(parse(models) == Left("error"))
        }
    }
}
