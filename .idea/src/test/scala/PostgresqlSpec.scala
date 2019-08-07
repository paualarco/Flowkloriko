import java.sql.DriverManager

class PostgresqlSpec extends FlatSpec with ForAllTestContainer  {

  override val container = PostgreSQLContainer()

  "PostgreSQL container" should "be started" in {
    Class.forName(container.driverClassName)
    val connection = DriverManager.getConnection(container.jdbcUrl, container.username, container.password)

  }
}