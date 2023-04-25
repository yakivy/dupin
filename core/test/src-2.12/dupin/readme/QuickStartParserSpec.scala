package dupin.readme

import cats.data.Ior
import cats.data.NonEmptyChain
import org.scalatest.freespec.AnyFreeSpec
import dupin.readme.ReadmeDomainValidatorFixture._

trait QuickStartParserFixture {
    import cats._
    import cats.implicits._
    import dupin.basic.all._

    case class RawMember(name: String, age: Int)
    case class RawTeam(name: String, members: List[RawMember])

    //parser for simple type or value class
    implicit val nameParser: BasicParser[String, Name] = BasicParser.root[String, Name](
        Option(_).filter(_.nonEmpty).map(Name.apply),
        c => s"${c.path} should be non empty",
    )

    //idiomatic parser for complex type
    implicit val memberParser: BasicParser[RawMember, Member] =
        Parser.parserParallelWithSequentialEffect[cats.Id, String, RawMember].applicative.map2(
            nameParser.comapP[RawMember](_.name),
            BasicParser.idRoot[Int](
                Option(_).filter(a => a > 18 && a < 40),
                c => s"${c.path} should be between 18 and 40",
            ).comapP[RawMember](_.age),
        )(Member.apply)

    implicit val teamParser: BasicParser[RawTeam, Team] =
        Parser.parserParallelWithSequentialEffect[cats.Id, String, RawTeam].applicative.map2(
            nameParser.comapP[RawTeam](_.name),
            memberParser.liftToTraverseCombiningP[List].comapP[RawTeam](_.members),
        )(Team.apply)
            .andThen(
                //if you need id parser that filters out value by condition,
                //you can simply create a validator and convert it to parser
                BasicValidator
                    .root[Team](_.members.size <= 8, _ => "team should be fed with two pizzas!")
                    .toParser
            )
}

class QuickStartParserSpec extends AnyFreeSpec with QuickStartParserFixture {
    "Readme parsers should" - {
        "be correct" in {
            import dupin.basic.all._

            val validTeam = RawTeam(
                "Bears",
                List(
                    RawMember("Yakiv", 26),
                    RawMember("Myroslav", 31),
                    RawMember("Andrii", 25)
                )
            )

            val invalidTeam = RawTeam(
                "",
                RawMember("", 0) :: (1 to 10).map(_ => RawMember("Valid name", 20)).toList
            )

            assert(validTeam.parse == Ior.right(Team(
                Name("Bears"),
                List(
                    Member(Name("Yakiv"), 26),
                    Member(Name("Myroslav"), 31),
                    Member(Name("Andrii"), 25)
                )
            )))
            assert(invalidTeam.parse == Ior.left(NonEmptyChain(
                ".name should be non empty",
                ".members.[0].name should be non empty",
                ".members.[0].age should be between 18 and 40",
            )))
        }
    }
}
