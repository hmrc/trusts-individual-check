import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings.itSettings

val appName = "trusts-individual-check"

ThisBuild / scalaVersion := "2.13.13"
ThisBuild / majorVersion := 0

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(
    PlayKeys.playDefaultPort := 9846,
    ScoverageKeys.coverageExcludedFiles := "<empty>;.*components.*;.*Routes.*",
    ScoverageKeys.coverageMinimumStmtTotal := 95,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    scalacOptions ++= Seq("-feature", "-Wconf:src=routes/.*:s"),
    libraryDependencies ++= AppDependencies()
  )

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test") // the "test->test" allows reusing test code and test dependencies
  .settings(itSettings())

addCommandAlias("scalastyleAll", "all scalastyle Test/scalastyle it/Test/scalastyle")
