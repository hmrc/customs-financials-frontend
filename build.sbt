import play.core.PlayVersion.current
import uk.gov.hmrc.DefaultBuildSettings.{targetJvm, itSettings}

val appName = "customs-financials-frontend"

val silencerVersion = "1.17.13"
val bootstrapVersion = "7.22.0"
val scala2_13_8 = "2.13.8"

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := scala2_13_8

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test")
  .settings(itSettings())
  .settings(libraryDependencies ++= Seq("uk.gov.hmrc" %% "bootstrap-test-play-28" % bootstrapVersion % Test))

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin)
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)
  .settings(
    PlayKeys.playDefaultPort := 9876,
    libraryDependencies ++= compileDependencies ++ testDependencies,
    retrieveManaged := true,
  )
  .settings(scoverageSettings *)
  .settings(
    targetJvm := "jvm-11",
    Test / fork := false,
    Test/ parallelExecution := false,
    TwirlKeys.templateImports ++= Seq(
      "play.twirl.api.HtmlFormat",
      "play.twirl.api.HtmlFormat._",
      "utils.ViewUtils._",
      "controllers._",
      "domain._"
    ),
    routesImport ++= Seq("domain._"),
    scalacOptions += "-P:silencer:pathFilters=views;routes",
    libraryDependencies ++= Seq(
      compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
      "com.github.ghik" % "silencer-lib" % silencerVersion % Provided cross CrossVersion.full
    )
  )
  .configs(IntegrationTest)
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
  "uk.gov.hmrc" %% "tax-year" % "3.3.0" % Test
)

lazy val compileScalastyle = taskKey[Unit]("compileScalastyle")
compileScalastyle := (Compile / scalastyle).toTask("").value
(Compile / compile) := ((Compile / compile) dependsOn compileScalastyle).value

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    ScoverageKeys.coverageExcludedPackages := List("<empty>"
      , "Reverse.*"
      , ".*(BuildInfo|Routes|testOnly).*").mkString(";"),
    ScoverageKeys.coverageMinimumStmtTotal := 90,
    ScoverageKeys.coverageMinimumBranchTotal := 90,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}
