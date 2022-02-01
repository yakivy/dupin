## Dupin
[![Maven Central](https://img.shields.io/maven-central/v/com.github.yakivy/dupin-core_2.12.svg)](https://mvnrepository.com/search?q=dupin)
[![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/https/oss.sonatype.org/com.github.yakivy/dupin-core_2.13.svg)](https://oss.sonatype.org/content/repositories/snapshots/com/github/yakivy/dupin-core_2.13/)
[![Build Status](https://travis-ci.com/yakivy/dupin.svg?branch=master)](https://travis-ci.com/yakivy/dupin)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
<a href="https://typelevel.org/cats/"><img src="https://typelevel.org/cats/img/cats-badge.svg" height="40px" align="right" alt="Cats friendly" /></a>

Dupin is a minimal, idiomatic, customizable validation Scala library.

### Table of contents
1. [Motivation](#motivation)
2. [Quick start](#quick-start)
3. [Predefined validators](#predefined-validators)
4. [Customization](#message-customization)
    1. [Message customization](#message-customization)
    2. [Kind customization](#kind-customization)
    3. [Custom validating package](#custom-validating-package)
5. [Changelog](#changelog)

### Motivation

You may find Dupin useful if you...
- need to return something richer than `String` as validation message
- want to use custom data kind for validation (`Future`, `IO`, etc...)
- use [cats](https://typelevel.org/cats/) and like their API style
- need Scala 3, Scala JS or Scala Native support

### Quick start
Add cats and dupin dependencies to the build file, let's assume you are using sbt:
```scala
libraryDependencies += Seq(
    "org.typelevel" %% "cats-core" % "2.6.1",
    "com.github.yakivy" %% "dupin-core" % "0.2.0",
)
```
Describe the domain:
```scala
case class Name(value: String)
case class Member(name: Name, age: Int)
case class Team(name: Name, members: Seq[Member])
```
Define some validators:
```scala
import cats._
import dupin.basic.all._

//validator for simple type or value class
implicit val nameValidator: BasicValidator[Name] = BasicValidator
    .root[Name](_.value.nonEmpty, c => s"${c.path} should be non empty")

//idiomatic validator for complex type
implicit val memberValidator: BasicValidator[Member] =
    nameValidator.comapP[Member](_.name) combine
    BasicValidator.root[Int](a => a > 18 && a < 40, c => s"${c.path} should be between 18 and 40").comapP[Member](_.age)

//same validator but with combination helpers for better type resolving
val alternativeMemberValidator: BasicValidator[Member] = BasicValidator.success[Member]
    .combineP(_.name)(nameValidator)
    .combinePR(_.age)(a => a > 18 && a < 40, c => s"${c.path} should be between 18 and 40")

//derived validator
implicit val teamValidator: BasicValidator[Team] = BasicValidator.derive[Team]
    .combineR(_.members.size <= 8, _ => "team should be fed with two pizzas!")
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

val valid = validTeam.isValid
val result = invalidTeam.validate

assert(valid)
assert(result == Validated.invalid(NonEmptyChain(
    ".members.[0].name should be non empty",
    ".members.[0].age should be between 18 and 40",
    ".name should be non empty",
    "team should be fed with two pizzas!"
)))
```

### Predefined validators

The more validators you have, the more logic can be reused without writing validators from the scratch. Let's define common validators for minimum and maximum `Int` value:
```scala
import dupin.basic.all._

def min(value: Int) = BasicValidator.root[Int](_ > value, c => s"${c.path} should be greater than $value")
def max(value: Int) = BasicValidator.root[Int](_ < value, c => s"${c.path} should be less than $value")
``` 
And since validators can be combined, you can create validators from other validators:
```scala
import cats._
import dupin.basic.all._

implicit val memberValidator: BasicValidator[Member] = BasicValidator.success[Member]
    .combineP(_.age)(min(18) && max(40))

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
As `BasicValidator[A]` is just a type alias for `Validator[Id, String, A]`, you can define own validator type with builder:

```scala
import dupin._

type I18nValidator[A] = Validator[cats.Id, I18nMessage, A]
val I18nValidator = Validator[cats.Id, I18nMessage]
```
And start creating validators with custom messages:
```scala
import cats._

implicit val nameValidator: I18nValidator[Name] = I18nValidator.root[Name](
    _.value.nonEmpty, c => I18nMessage(
        s"${c.path} should be non empty",
        "validator.name.empty",
        List(c.path.toString())
    )
)

implicit val memberValidator: I18nValidator[Member] = I18nValidator.success[Member]
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

### Kind customization

For example you want to allow only using of limited list of names and they are stored in the database:
```scala
import scala.concurrent.Future

class NameService {
    private val allowedNames = Set("Ada")
    def contains(name: String): Future[Boolean] =
        // Emulation of DB call
        Future.successful(allowedNames(name))
}
```
So to be able to handle checks that returns `Future[Boolean]`, you just need to define your own validator type with builder:

```scala
import dupin._
import scala.concurrent.Future

type FutureValidator[A] = Validator[Future, String, A]
val FutureValidator = Validator[Future, String]
``` 
Then you can create validators with generic dsl (don't forget to import required type classes, as minimum `Functor[Future]`):
```scala
import cats.implicits._
import scala.concurrent.Future

val nameService = new NameService

implicit val nameValidator: FutureValidator[Name] = FutureValidator.root[Name](
    n => nameService.contains(n.value), c => s"${c.path} should be non empty"
)

implicit val memberValidator: FutureValidator[Member] = FutureValidator.success[Member]
    .combinePI(_.name)
    .combinePR(_.age)(a => Future.successful(a > 18 && a < 40), c => s"${c.path} should be between 18 and 40")
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

To avoid imports boilerplate and isolating all customizations you can define your own dupin package:

```scala
package object custom extends DupinCoreDsl with DupinSyntax {
    type CustomValidator[A] = Validator[Future, I18nMessage, A]
    val CustomValidator = Validator[Future, I18nMessage]
}
```
Then you can start using your own validator type with single import:
```scala
import cats.implicits._
import dupin.custom._

val nameService = new NameService

implicit val nameValidator: CustomValidator[Name] = CustomValidator.root[Name](
    n => nameService.contains(n.value), c => I18nMessage(
        s"${c.path} should be non empty",
        "validator.name.empty",
        List(c.path.toString())
    )
)

val validName = Name("Ada")
val valid: Future[Boolean] = validName.isValid

valid.map(assert(_))
```

### Changelog

#### 0.3.1:
- optimize a naive implementation of `ValidatorComapToP.validatorComapToPForTraverse` that threw StackOverflowException for long lists

---

<center>Finalize API</center>

---

#### 0.3.0:
- rename `dupin.Validator.compose` to `dupin.Validator.comap`, similar to `cats.Contravariant.contramap`
- rename `dupin.Validator.combinePK` to `dupin.Validator.combinePL`, where `L` stands for "lifted" to reflect method signature
- minor refactorings

#### 0.2.0:
- migrate to mill build tool
- add Scala 3, Scala JS and Scala Native support
- expose validator monoid instance `Monoid[Validator[F, E, A]]`
- rename `dupin.base` package to `dupin.basic`
- various refactorings and cleanups