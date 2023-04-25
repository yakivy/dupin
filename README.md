## Dupin
[![Maven Central](https://img.shields.io/maven-central/v/com.github.yakivy/dupin-core_2.12.svg)](https://mvnrepository.com/search?q=dupin)
[![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/https/oss.sonatype.org/com.github.yakivy/dupin-core_2.13.svg)](https://oss.sonatype.org/content/repositories/snapshots/com/github/yakivy/dupin-core_2.13/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
<a href="https://typelevel.org/cats/"><img src="https://typelevel.org/cats/img/cats-badge.svg" height="40px" align="right" alt="Cats friendly" /></a>

Dupin is a minimal, idiomatic, customizable validation Scala library.

You may find Dupin useful if you...
- want a transparent and composable validation approach
- need to return something richer than `String` as validation message
- use effectful logic inside validator (`Future`, `IO`, etc...)
- like [parse don't validate](https://lexi-lambda.github.io/blog/2019/11/05/parse-don-t-validate/) style
- have [cats](https://typelevel.org/cats/) dependency and like their API style
- need Scala 3, Scala JS or Scala Native support

Library is built around two type classes:
- `Validator[F[_], E, A]` - is a self-sufficient validator for type `A`, represents a function `A => F[ValidatedNec[E, Unit]]`
- `Parser[F[_], E, A, B]` - is a parser from type `A` to type `B`, represents a function `A => F[IorNec[E, B]]`

### Table of contents
1. [Quick start](#quick-start)
   1. [Validate](#validate)
   2. [Parse](#parse)
2. [Predefined validators](#predefined-validators)
3. [Message customization](#message-customization)
4. [Effectful validation](#effectful-validation)
5. [Custom validating package](#custom-validating-package)
6. [Complex example](#complex-example)
7. [Roadmap](#roadmap)
8. [Changelog](#changelog)

### Quick start
Add cats and dupin dependencies to the build file, let's assume you are using sbt:
```scala
libraryDependencies += Seq(
    "org.typelevel" %% "cats-core" % "2.9.0",
    "com.github.yakivy" %% "dupin-core" % "0.6.0",
)
```
Describe the domain:
```scala
case class Name(value: String)
case class Member(name: Name, age: Int)
case class Team(name: Name, members: Seq[Member])
```

#### Validate

Define validators:
```scala
import cats._
import dupin.basic.all._

//validator for simple type or value class
implicit val nameValidator: BasicValidator[Name] = BasicValidator
    .root[Name](_.value.nonEmpty, c => s"${c.path} should be non empty")

//idiomatic validator for complex type
implicit val memberValidator: BasicValidator[Member] =
    nameValidator.comapP[Member](_.name) combine
    BasicValidator.root[Int](
        a => a > 18 && a < 40,
        c => s"${c.path} should be between 18 and 40"
    ).comapP[Member](_.age)

//same validator but with combination helpers for better type resolving
val alternativeMemberValidator: BasicValidator[Member] = BasicValidator
    .success[Member]
    .combineP(_.name)(nameValidator)
    .combinePR(_.age)(a => a > 18 && a < 40, c => s"${c.path} should be between 18 and 40")

//derived validator
implicit val teamValidator: BasicValidator[Team] = BasicValidator
    .derive[Team]
    .combineR(_.members.size <= 8, _ => "team should be fed with two pizzas!")

//two stage validator
val failingTeamValidator: BasicValidator[Team] = teamValidator
    .andThen(BasicValidator.failure[Team](_ => "validation error after heavy computations"))
```
Validate them all:
```scala
import dupin.basic.all._

val validTeam = Team(
    Name("Bears"),
    List(
        Member(Name("Yakiv"), 26),
        Member(Name("Myroslav"), 31),
        Member(Name("Andrii"), 25)
    )
)

val invalidTeam = Team(
    Name(""),
    Member(Name(""), 0) :: (1 to 10).map(_ => Member(Name("Valid name"), 20)).toList
)

assert(validTeam.isValid)
assert(invalidTeam.validate == Validated.invalid(NonEmptyChain(
    ".members.[0].name should be non empty",
    ".members.[0].age should be between 18 and 40",
    ".name should be non empty",
    "team should be fed with two pizzas!",
)))
assert(failingTeamValidator.validate(validTeam) == Validated.invalid(NonEmptyChain(
    "validation error after heavy computations",
)))
assert(failingTeamValidator.validate(invalidTeam) == Validated.invalid(NonEmptyChain(
    ".members.[0].name should be non empty",
    ".members.[0].age should be between 18 and 40",
    ".name should be non empty",
    "team should be fed with two pizzas!",
)))
```

#### Parse

Enrich the domain with raw models to parse:
```scala
case class RawMember(name: String, age: Int)
case class RawTeam(name: String, members: List[RawMember])
```

Define parsers:
```scala
import cats._
import cats.implicits._
import dupin.basic.all._

// parser for simple type or value class
implicit val nameParser: BasicParser[String, Name] = BasicParser.root[String, Name](
    Option(_).filter(_.nonEmpty).map(Name.apply),
    c => s"${c.path} should be non empty",
)

//idiomatic parser for complex type
implicit val memberParser: BasicParser[RawMember, Member] =
    (
        nameParser.comapP[RawMember](_.name),
        BasicParser.idRoot[Int](
            Option(_).filter(a => a > 18 && a < 40),
            c => s"${c.path} should be between 18 and 40",
        ).comapP[RawMember](_.age),
    )
        .parMapN(Member.apply)

implicit val teamParser: BasicParser[RawTeam, Team] =
    (
        nameParser.comapP[RawTeam](_.name),
        memberParser.liftToTraverseCombiningP[List].comapP[RawTeam](_.members),
    )
        .parMapN(Team.apply)
        .andThen(
            //if you need identity parser that filters out value by condition,
            //you can simply create a validator and convert it to parser
            BasicValidator
                .root[Team](_.members.size <= 8, _ => "team should be fed with two pizzas!")
                .toParser
        )
```

Parse them all:
```scala
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
```

### Predefined validators

It also might be useful to extract and reuse validators for common types. Let's define validators for minimum and maximum `Int` value:
```scala
import dupin.basic.all._

def min(value: Int) = BasicValidator.root[Int](_ > value, c => s"${c.path} should be greater than $value")
def max(value: Int) = BasicValidator.root[Int](_ < value, c => s"${c.path} should be less than $value")
``` 
And since validators can be combined, you can use them to create more complex validators:
```scala
import cats._
import dupin.basic.all._

implicit val memberValidator: BasicValidator[Member] = BasicValidator
    .success[Member]
    .combineP(_.age)(min(18) && max(40).failureAs(_ => "updated validation message"))

val invalidMember = Member(Name("Ada"), 0)
val result = invalidMember.validate

assert(result == Validated.invalidNec(".age should be greater than 18"))
```

### Message customization

But not many real projects use strings as validation messages, for example you want to support internationalization:
```scala
case class I18nMessage(
    description: String,
    key: String,
    params: List[String]
)
```
`BasicValidator[A]` is simply a type alias for `Validator[Id, String, A]`, so you can define own validator type with partially applied builder:

```scala
import dupin._

type I18nValidator[A] = Validator[cats.Id, I18nMessage, A]
val I18nValidator = Validator[cats.Id, I18nMessage]
```
And start creating validators with custom messages:
```scala
import cats._

implicit val nameValidator: I18nValidator[Name] = I18nValidator.root[Name](
    _.value.nonEmpty,
    c => I18nMessage(
        s"${c.path} should be non empty",
        "validator.name.empty",
        List(c.path.toString())
    )
)

implicit val memberValidator: I18nValidator[Member] = I18nValidator
    .success[Member]
    .combinePI(_.name)
    .combinePR(_.age)(a => a > 18 && a < 40, c => I18nMessage(
        s"${c.path} should be between 18 and 40",
        "validator.member.age",
        List(c.path.toString())
    ))
```
Validation messages will look like:
```scala
import dupin.syntax._

val invalidMember = Member(Name(""), 0)
val result = invalidMember.validate

assert(result == Validated.invalid(NonEmptyChain(
    I18nMessage(
        ".name should be non empty",
        "validator.name.empty",
        List(".name")
    ),
    I18nMessage(
        ".age should be between 18 and 40",
        "validator.member.age",
        List(".age")
    )
)))
```

### Effectful validation

For example, you want to allow only a limited list of names and it is stored in the database:
```scala
import scala.concurrent.Future

class NameService {
    private val allowedNames = Set("Ada")
    def contains(name: String): Future[Boolean] =
        // Emulation of DB call
        Future.successful(allowedNames(name))
}
```
So to be able to handle checks that return `Future[Boolean]`, you just need to define your own validator type with partially applied builder:
```scala
import dupin._
import scala.concurrent.Future

type FutureValidator[A] = Validator[Future, String, A]
val FutureValidator = Validator[Future, String]
``` 
Then you can create validators with generic DSL (don't forget to import required type classes, as minimum `Functor[Future]`):
```scala
import cats.implicits._
import scala.concurrent.Future

val nameService = new NameService

implicit val nameValidator: FutureValidator[Name] = FutureValidator.rootF[Name](
    n => nameService.contains(n.value),
    c => s"${c.path} should be non empty"
)

implicit val memberValidator: FutureValidator[Member] = FutureValidator
    .success[Member]
    .combinePI(_.name)
    .combinePR(_.age)(a => a > 18 && a < 40, c => s"${c.path} should be between 18 and 40")
```
Validation result will look like:
```scala
import dupin.syntax._

val invalidMember = Member(Name(""), 0)
val result: Future[ValidatedNec[String, Member]] = invalidMember.validate

result.map(r => assert(r == Validated.invalid(NonEmptyChain(
    ".name should be non empty",
    ".age should be between 18 and 40"
))))
```

### Custom validating package

To avoid imports boilerplate and isolating all customizations, you can define your own dupin package:
```scala
package object custom extends DupinCoreDsl with DupinSyntax {
    type CustomValidator[A] = Validator[Future, I18nMessage, A]
    val CustomValidator = Validator[Future, I18nMessage]

    type CustomParser[A, B] = Parser[Future, I18nMessage, A, B]
    val CustomParser = Parser[Future, I18nMessage]
}
```
Then you can start using custom validator type with a single import:
```scala
import cats.implicits._
import dupin.custom._

val nameService = new NameService

implicit val nameValidator: CustomValidator[Name] = CustomValidator.rootF[Name](
    n => nameService.contains(n.value),
    c => I18nMessage(
        s"${c.path} should be non empty",
        "validator.name.empty",
        List(c.path.toString())
    )
)

val validName = Name("Ada")
val valid: Future[Boolean] = validName.isValid

valid.map(assert(_))
```

### Complex example

Let's assume that you need to build a method that receives a list of raw term models (each model is a product of term itself and a list of mistakes that people often make when typing this term, for example: "calendar" -> ["calender", "celender"]) and parses them before saving to the database. Here are some requirements:
- suggested raw model:
```scala
case class RawTermModel(
    term: String,
    mistakes: List[String],
)
```
- term and mistake should be a single word
- term and mistake should not exist in the database:
```scala
type R[A] = Either[String, A]
trait TermRepository {
    def contains(term: Term): R[Boolean] = ...
}
```
- terms should be unique among other terms in the list
- mistakes should be unique among other mistakes and terms in the list
- parsed model should have as minimum one mistake
- suggested final model:
```scala
case class Term(value: String)
case class TermModel(
    term: Term,
    mistakes: NonEmptyList[Term],
)
```
- if validation error occurs in term - skip the model and continue parsing
- if validation error occurs in mistake - skip the mistake only and continue parsing
- all validation errors should be collected and returned after parsing

So the parser from `RawTermModel` to `TermModel`, considering the requirements above, will look like:
```scala
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
        val validTerms = mutable.Set.from(ms.view.map(_.term))
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
```

(full list of test cases can be found [here](https://github.com/yakivy/dupin/blob/master/core/test/src/dupin/readme/ComplexExampleFixture.scala))

### Roadmap
- add unzip from index for validator/parser
- enrich parser tests with validator cases
- optimize `Parser.liftToTraverseCombiningP`, `combineK` is often slow, for example for lists
- add complex example without parser for comparison

### Changelog

#### 0.6.0
- add Parser type
- replace implicit conversion (`comapToP`, ...) with explicit lift methods (`liftToTraverseP`, ...)
- a couple of minor fixes

#### 0.5.0
- simplify internal validator function
- expose validator contravariant monoidal instance `ContravariantMonoidal[Validator[F, E, *]]`

#### 0.4.1
- update Scala Native and Scala JS versions

#### 0.4.0
- add Scala 3 support for Scala Native
- optimize path concatenation
- separate F Validator methods (like `rootF`)
- add Validator methods with context (like `combineC`)

#### 0.3.1:
- optimize a naive implementation of `ValidatorComapToP.validatorComapToPForTraverse` that threw StackOverflowException for long lists

#### 0.3.0:
- rename `dupin.Validator.compose` to `dupin.Validator.comap`, similar to `cats.Contravariant.contramap`
- rename `dupin.Validator.combinePK` to `dupin.Validator.combinePL`, where `L` stands for "lifted" to reflect method signature
- minor refactorings

#### 0.2.0:
- migrate to mill build tool
- add Scala 3, Scala JS and Scala Native support
- expose validator monoid instance `MonoidK[Validator[F, E, *]]`
- rename `dupin.base` package to `dupin.basic`
- various refactorings and cleanups