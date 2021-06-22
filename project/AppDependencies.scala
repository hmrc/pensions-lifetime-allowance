import sbt._

object AppDependencies {

  import play.sbt.PlayImport._
  import play.core.PlayVersion

  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-backend-play-28" % "5.3.0",
    "uk.gov.hmrc" %% "domain" % "5.11.0-play-27"
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test: Seq[ModuleID] = ???
  }

  object Test {
    def apply(): Seq[ModuleID] = new TestDependencies {
      override lazy val test = Seq(
      "org.mockito"                   %   "mockito-core"              %   "3.7.7"           % scope,
      "com.typesafe.play"             %%  "play-test"                 % PlayVersion.current % scope,
      "com.fasterxml.jackson.module"  %%  "jackson-module-scala"      % "2.12.2"            % scope,
      "com.github.tomakehurst"        %   "wiremock"                  % "2.26.3"            % scope,
      "com.github.tomakehurst"        %   "wiremock-jre8"             % "2.26.3"            % scope,
      "com.vladsch.flexmark"          %   "flexmark-all"              % "0.35.10"           % scope,
      "org.scalatestplus"             %%  "scalatestplus-mockito"     % "1.0.0-M2"          % scope,
      "org.scalatestplus.play"        %%  "scalatestplus-play"        % "5.1.0"             % scope,
      "org.scalatestplus"             %%  "scalatestplus-scalacheck"  % "3.1.0.0-RC2"       % scope,
      "org.pegdown"                   %   "pegdown"                   % "1.6.0"             % scope,
      "org.jsoup"                     %   "jsoup"                     % "1.13.1"            % scope,
      "org.mockito"                   %   "mockito-all"               % "1.10.19"           % scope
      )
    }.test
  }

  def apply(): Seq[ModuleID] = compile ++ Test()
}