package dupin.readme

trait ReadmeDomainFixture {
    case class Name(value: String)
    case class Member(name: Name, age: Int)
    case class Team(name: Name, members: List[Member])
}
