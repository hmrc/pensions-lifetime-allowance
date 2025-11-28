import sbt.*

object AppDependencies {

  import play.sbt.PlayImport.*

  val boostrapVersion = "9.19.0"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-backend-play-30" % boostrapVersion,
    "uk.gov.hmrc" %% "domain-play-30"            % "11.0.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% "bootstrap-test-play-30" % boostrapVersion,
    "org.scalatestplus" %% "scalacheck-1-18"        % "3.2.19.0"
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test
}
