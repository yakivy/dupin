package dupin.readme

object MessageCustomizationDomainFixture {
    case class I18nMessage(
        description: String,
        key: String,
        params: List[String]
    )
}
