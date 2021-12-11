package dupin.readme

object ReadmeDomainFixture {
    case class Name(value: String)
    case class Member(name: Name, age: Int)
    case class Team(name: Name, members: List[Member])
}
