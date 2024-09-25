import sbt._

object AppDependencies {

  private val bootstrapVersion = "9.5.0"
  private val catsVersion      = "2.6.1"

  val compile = Seq(
    "uk.gov.hmrc"   %% "bootstrap-backend-play-30" % bootstrapVersion,
    "org.typelevel" %% "cats-core"                 % catsVersion,
    "io.lemonlabs"  %% "scala-uri"                 % "4.0.3",
    "com.beachape"     %% "enumeratum-play"           % "1.8.0"
  )

  val test = Seq(
    "uk.gov.hmrc"         %% "bootstrap-test-play-30" % bootstrapVersion,
    "org.mockito"         %% "mockito-scala"          % "1.17.31",
    "org.typelevel"       %% "cats-core"              % catsVersion,
    "io.swagger.parser.v3" % "swagger-parser-v3"      % "2.1.14"
  ).map(_ % Test)

  val it   = Seq.empty
}
