import uk.gov.hmrc.DefaultBuildSettings.{itSettings, targetJvm}

val appName = "trusts-individual-check"

ThisBuild / scalaVersion := "3.9.0"
ThisBuild / majorVersion := 1
ThisBuild / targetJvm := "jvm-21"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) // Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(
    PlayKeys.playDefaultPort := 9846,
    CodeCoverageSettings(),
    scalacOptions ++= Seq("-feature", "-Wconf:src=routes/.*:s"),
    libraryDependencies ++= AppDependencies()
  )

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test") // the "test->test" allows reusing test code and test dependencies
  .settings(itSettings())

addCommandAlias("scalafmtAll", "all scalafmtSbt scalafmt Test/scalafmt it/Test/scalafmt")
