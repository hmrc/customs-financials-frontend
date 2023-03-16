import play.core.PlayVersion.current
import uk.gov.hmrc.DefaultBuildSettings.{integrationTestSettings, targetJvm}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings

val appName = "customs-financials-frontend"

val silencerVersion = "1.7.0"


lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin)
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)
  .settings(
    PlayKeys.playDefaultPort := 9876,
    libraryDependencies ++= compileDependencies ++ testDependencies,
    retrieveManaged := true,
    majorVersion := 0
  )
  .settings(scoverageSettings: _*)
  .settings(publishingSettings: _*)
  .settings(
    scalaVersion := "2.12.11",
    targetJvm := "jvm-1.8",
    fork in Test := false,
    parallelExecution in Test := false,
    TwirlKeys.templateImports ++= Seq(
      "play.twirl.api.HtmlFormat",
      "play.twirl.api.HtmlFormat._",
      "controllers._",
      "domain._"
    ),
    routesImport ++= Seq("domain._"),
    // ***************
    // Use the silencer plugin to suppress warnings from unused imports in compiled twirl templates
    scalacOptions += "-P:silencer:pathFilters=views;routes",
    libraryDependencies ++= Seq(
      compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
      "com.github.ghik" % "silencer-lib" % silencerVersion % Provided cross CrossVersion.full
    )
    // ***************
  )
  .configs(IntegrationTest)
  .settings(integrationTestSettings(): _*)
  .settings(
    resolvers += Resolver.jcenterRepo
  )

val compileDependencies = Seq(
  "uk.gov.hmrc" %% "bootstrap-frontend-play-28" % "7.14.0",
  "uk.gov.hmrc" %% "play-partials" % "8.2.0-play-28",
  "uk.gov.hmrc" %% "govuk-template" % "5.79.0-play-28",
  "uk.gov.hmrc" %% "play-frontend-hmrc" % "6.8.0-play-28",
  ws,
  "uk.gov.hmrc" %% "reactive-circuit-breaker" % "3.5.0",
  "org.typelevel" %% "cats-core" % "2.3.0",
  "uk.gov.hmrc" %% "tax-year" % "1.6.0",
  "org.webjars.npm" % "moment" % "2.29.0",
  "com.typesafe.play" %% "play-json-joda" % "2.9.2"
)

val testDependencies = Seq(
 "uk.gov.hmrc" %% "bootstrap-test-play-28" % "7.14.0" % Test,
"org.scalatest" %% "scalatest" % "3.2.9" % Test,
"org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % "test, it",
"org.jsoup" % "jsoup" % "1.10.2" % Test,
"com.typesafe.play" %% "play-test" % current % Test,
"com.vladsch.flexmark" % "flexmark-all" % "0.35.10" % "test, it",
"org.mockito" %% "mockito-scala-scalatest" % "1.16.46" % "test, it"
)

lazy val compileScalastyle = taskKey[Unit]("compileScalastyle")
compileScalastyle := scalastyle.in(Compile).toTask("").value
(compile in Compile) := ((compile in Compile) dependsOn compileScalastyle).value

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    ScoverageKeys.coverageExcludedPackages := List("<empty>"
      , "Reverse.*"
      , ".*(BuildInfo|Routes|testOnly).*").mkString(";"),
    ScoverageKeys.coverageMinimum := 80,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}