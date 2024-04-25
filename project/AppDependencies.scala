import sbt._

object AppDependencies {

  private val bootstrapVersion = "8.4.0"
  

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-30"  % bootstrapVersion
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"     % bootstrapVersion,
    "org.mockito"             %% "mockito-scala"              % "1.17.31"
    
  ).map(_ % Test)

  val it = Seq.empty
}
