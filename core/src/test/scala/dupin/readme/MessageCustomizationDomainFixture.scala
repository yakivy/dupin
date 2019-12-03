package dupin.readme

trait MessageCustomizationDomainFixture extends ReadmeDomainFixture {
    case class I18nMessage(
        description: String,
        key: String,
        params: List[String]
    )
}
