import sbt._

object AppDependencies {

  import play.sbt.PlayImport._

  val boostrapVersion = "8.4.0"
  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-backend-play-30" % boostrapVersion,
    "uk.gov.hmrc" %% "domain" % "8.0.0-play-28"
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test: Seq[ModuleID] = ???
  }

  object Test {
    def apply(): Seq[ModuleID] = new TestDependencies {
      override lazy val test = Seq(
      "uk.gov.hmrc"                   %%  "bootstrap-test-play-30"    % boostrapVersion     % scope,
      "org.scalatestplus"             %%  "scalatestplus-mockito"     % "1.0.0-M2"          % scope,
      "org.mockito"                   %   "mockito-core"              %   "3.7.7"           % scope
      )
    }.test
  }

  def apply(): Seq[ModuleID] = compile ++ Test()
}