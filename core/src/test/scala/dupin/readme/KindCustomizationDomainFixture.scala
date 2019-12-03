package dupin.readme

trait KindCustomizationDomainFixture {
    import scala.concurrent.Future

    class NameService {
        private val allowedNames = Set("Ada")
        def contains(name: String): Future[Boolean] =
        // Emulation of DB call
            Future.successful(allowedNames(name))
    }
}