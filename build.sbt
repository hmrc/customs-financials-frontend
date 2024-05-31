import uk.gov.hmrc.DefaultBuildSettings.{itSettings, targetJvm}
import play.core.PlayVersion.current

val appName = "customs-financials-frontend"

val silencerVersion = "1.7.16"
val bootstrapVersion = "8.5.0"
val scala2_13_12 = "2.13.12"

val scalaStyleConfigFile = "scalastyle-config.xml"
val testScalaStyleConfigFile = "test-scalastyle-config.xml"
val testDirectory = "test"

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := scala2_13_12

lazy val scalastyleSettings = Seq(scalastyleConfig := baseDirectory.value /  scalaStyleConfigFile,
  (Test / scalastyleConfig) := baseDirectory.value/ testDirectory /  testScalaStyleConfigFile)

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test")
  .settings(itSettings())
  .settings(libraryDependencies ++= Seq("uk.gov.hmrc" %% "bootstrap-test-play-30" % bootstrapVersion % Test))

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
    scalacOptions ++= Seq(
      "-Wunused:imports",
      "-Wunused:patvars",
      "-Wunused:implicits",
      "-Wunused:explicits",
      "-Wunused:privates",
      "-P:silencer:pathFilters=target/.*",
      "-P:silencer:pathFilters=routes",
      "-P:silencer:pathFilters=views;routes"),

    Test / scalacOptions ++= Seq(
      "-Wunused:imports",
      "-Wunused:params",
      "-Wunused:patvars",
      "-Wunused:implicits",
      "-Wunused:explicits",
      "-Wunused:privates"),
    libraryDependencies ++= Seq(
      compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
      "com.github.ghik" % "silencer-lib" % silencerVersion % Provided cross CrossVersion.full
    )
  ).settings(resolvers += Resolver.jcenterRepo)

val compileDependencies = Seq(
  "uk.gov.hmrc" %% "bootstrap-frontend-play-30" % bootstrapVersion,
  "uk.gov.hmrc" %% "play-partials-play-30"      % "9.1.0",
  "uk.gov.hmrc" %% "play-frontend-hmrc-play-30" % "9.4.0",
  ws,
  "org.typelevel" %% "cats-core" % "2.10.0",
  "uk.gov.hmrc" %% "tax-year" % "4.0.0",
  "org.webjars.npm" % "moment" % "2.30.1"
)

val testDependencies = Seq(
  "uk.gov.hmrc" %% "bootstrap-test-play-30" % bootstrapVersion % Test,
  "org.scalatest" %% "scalatest" % "3.2.18" % Test,
  "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % Test,
  "org.jsoup" % "jsoup" % "1.17.2" % Test,
  "org.playframework" %% "play-test" % current % Test,
  "com.vladsch.flexmark" % "flexmark-all" % "0.64.8" % Test,
  "org.mockito" %% "mockito-scala-scalatest" % "1.17.31" % Test,
  "uk.gov.hmrc" %% "tax-year" % "4.0.0" % Test
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

addCommandAlias("runAllChecks", ";clean;compile;coverage;test;it/test;scalastyle;Test/scalastyle;coverageReport")
