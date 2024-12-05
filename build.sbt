import uk.gov.hmrc.DefaultBuildSettings.itSettings

val appName = "customs-financials-frontend"

val silencerVersion = "1.7.16"
val bootstrapVersion = "9.0.0"
val scala3_3_3 = "3.3.3"

val scalaStyleConfigFile = "scalastyle-config.xml"
val testScalaStyleConfigFile = "test-scalastyle-config.xml"
val testDirectory = "test"

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := scala3_3_3

lazy val scalastyleSettings = Seq(scalastyleConfig := baseDirectory.value /  scalaStyleConfigFile,
  (Test / scalastyleConfig) := baseDirectory.value/ testDirectory /  testScalaStyleConfigFile)

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test")
  .settings(itSettings())
  .settings(libraryDependencies ++= Seq("uk.gov.hmrc" %% "bootstrap-test-play-30" % bootstrapVersion % Test))

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)
  .settings(
    PlayKeys.playDefaultPort := 9876,
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    retrieveManaged := true,
  )
  .settings(scoverageSettings *)
  .settings(
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
    scalacOptions := scalacOptions.value.diff(Seq("-Wunused:all")),
    Test / scalacOptions ++= Seq(
      "-Wunused:imports",
      "-Wunused:params",
      "-Wunused:implicits",
      "-Wunused:explicits",
      "-Wunused:privates"),
    libraryDependencies ++= Seq(
      compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.for3Use2_13With("", ".12")),
      "com.github.ghik" % "silencer-lib" % silencerVersion % Provided cross CrossVersion.for3Use2_13With("",".12")
    )
  ).settings(resolvers += Resolver.jcenterRepo)

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
    ScoverageKeys.coverageFailOnMinimum := false,
    ScoverageKeys.coverageHighlighting := true
  )
}

addCommandAlias("runAllChecks", ";clean;compile;coverage;test;it/test;scalastyle;Test/scalastyle;coverageReport")
