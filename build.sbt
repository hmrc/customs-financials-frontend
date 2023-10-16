import play.core.PlayVersion.current
import uk.gov.hmrc.DefaultBuildSettings.{integrationTestSettings, targetJvm}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings

val appName = "customs-financials-frontend"

val silencerVersion = "1.17.13"
val bootstrapVersion = "7.22.0"

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
  .settings(
    scalaVersion := "2.13.8",
    targetJvm := "jvm-11",
    fork in Test := false,
    parallelExecution in Test := false,
    TwirlKeys.templateImports ++= Seq(
      "play.twirl.api.HtmlFormat",
      "play.twirl.api.HtmlFormat._",
      "utils.ViewUtils._",
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
  "uk.gov.hmrc" %% "bootstrap-frontend-play-28" % bootstrapVersion,
  "uk.gov.hmrc" %% "play-partials" % "8.4.0-play-28",
  "uk.gov.hmrc" %% "play-frontend-hmrc" % "7.23.0-play-28",
  ws,
  "org.typelevel" %% "cats-core" % "2.9.0",
  "uk.gov.hmrc" %% "tax-year" % "3.3.0",
  "org.webjars.npm" % "moment" % "2.29.4",
  "com.typesafe.play" %% "play-json-joda" % "2.9.4"
)

val testDependencies = Seq(
  "uk.gov.hmrc" %% "bootstrap-test-play-28" % bootstrapVersion % Test,
  "org.scalatest" %% "scalatest" % "3.2.16" % Test,
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % "test, it",
  "org.jsoup" % "jsoup" % "1.16.1" % Test,
  "com.typesafe.play" %% "play-test" % current % Test,
  "com.vladsch.flexmark" % "flexmark-all" % "0.64.8" % "test, it",
  "org.mockito" %% "mockito-scala-scalatest" % "1.17.14" % "test, it",
  "uk.gov.hmrc" %% "play-frontend-hmrc" % "7.23.0-play-28" % Test,
  "uk.gov.hmrc" %% "tax-year" % "3.3.0" % Test
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