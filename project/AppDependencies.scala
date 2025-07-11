import sbt._

object AppDependencies {

  private val bootstrapVersion = "9.16.0"
  private val catsVersion      = "2.13.0"

  val compile = Seq(
    "uk.gov.hmrc"   %% "bootstrap-backend-play-30" % bootstrapVersion,
    "org.typelevel" %% "cats-core"                 % catsVersion,
    "io.lemonlabs"  %% "scala-uri"                 % "4.0.3",
    "com.beachape"     %% "enumeratum-play"           % "1.8.0"
  )

  val test = Seq(
    "uk.gov.hmrc"         %% "bootstrap-test-play-30" % bootstrapVersion,
    "uk.gov.hmrc.mongo"   %% "hmrc-mongo-test-play-30" % "2.6.0",
    "org.scalatestplus"   %% "mockito-4-11"           % "3.2.17.0",
    "org.typelevel"       %% "cats-core"              % catsVersion,
    "io.swagger.parser.v3" % "swagger-parser-v3"      % "2.1.14",
    "org.mongodb.scala" %% "mongo-scala-driver" % "5.1.0" cross CrossVersion.for3Use2_13
  ).map(_ % Test)

  val it: Seq[Nothing] = Seq.empty
}
