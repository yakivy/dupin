package dupin.readme

import cats.data.IorNec
import cats.data.NonEmptyList
import cats.implicits._
import dupin.basic._
import org.scalatest.freespec.AnyFreeSpec
import scala.collection.mutable

class ComplexExampleWithParserSpec extends AnyFreeSpec with ComplexExampleFixture {
    //validation types to handle repository effect `R`
    type CustomValidator[A] = Validator[R, String, A]
    val CustomValidator = Validator[R, String]
    type CustomParser[A, B] = Parser[R, String, A, B]
    val CustomParser = Parser[R, String]

    //parsers per requirement:

    //term and mistake should be a single word
    val termParser = CustomParser
        .root[String, Term](
            Option(_).filter(_.matches("\\w+")).map(Term.apply),
            c => s"${c.path}: cannot parse string '${c.value}' to a term"
        )

    //term and mistake should not exist in the database
    val repositoryTermParser = CustomValidator
        .rootF[Term](
            TermRepository.contains(_).map(!_),
            c => s"${c.path}: term '${c.value}' already exists"
        )
        .toParser

    //intermediate model to aggregate parsed terms
    case class HalfParsedTermModel(
        term: Term,
        mistakes: List[Term],
    )

    //terms should be unique among other terms in the list
    val uniqueTermsParser = CustomParser
        //define list level context where terms should be unique
        .idContext[List[HalfParsedTermModel]] { _ =>
            val validTerms = mutable.Set.empty[Term]
            CustomValidator
                .root[Term](validTerms.add, c => s"${c.path}: term '${c.value}' is duplicate")
                .comapP[HalfParsedTermModel](_.term)
                .toParser
                //lift parser to `List` accumulating errors
                .liftToTraverseCombiningP[List]
        }

    //mistakes should be unique among other mistakes and terms in the list
    val uniqueTermsMistakesParser = CustomParser
        .idContext[List[HalfParsedTermModel]] { ms =>
            val validTerms = ms.view.map(_.term).to(mutable.Set)
            CustomParser
                .idContext[HalfParsedTermModel] { m =>
                    CustomValidator
                        .root[Term](validTerms.add, c => s"${c.path}: mistake '${c.value}' is duplicate")
                        .toParser
                        .liftToTraverseCombiningP[List]
                        .comapP[HalfParsedTermModel](_.mistakes)
                        //update model with unique mistakes
                        .map(a => m.copy(mistakes = a))
                }
                .liftToTraverseCombiningP[List]
        }

    //parsed model should have as minimum one mistake
    def nelParser[A] = CustomParser
        .root[List[A], NonEmptyList[A]](_.toNel, c => s"${c.path}: cannot be empty")
    val halfToFullModelParser = CustomParser
        .context[HalfParsedTermModel, TermModel](m =>
            nelParser[Term]
                .comapP[HalfParsedTermModel](_.mistakes)
                .map(mistakes => TermModel(m.term, mistakes))
        )

    //combine all parsers together
    val modelsParser = (
        termParser
            .andThen(repositoryTermParser)
            .comapP[RawTermModel](_.term),
        termParser
            .andThen(repositoryTermParser)
            .liftToTraverseCombiningP[List]
            .comapP[RawTermModel](_.mistakes),
    )
        .parMapN(HalfParsedTermModel.apply)
        .liftToTraverseCombiningP[List]
        .andThen(uniqueTermsParser)
        .andThen(uniqueTermsMistakesParser)
        .andThen(halfToFullModelParser.liftToTraverseCombiningP[List])

    override def parse(rawModels: List[RawTermModel]): R[IorNec[String, List[TermModel]]] =
        modelsParser(rawModels)
}
