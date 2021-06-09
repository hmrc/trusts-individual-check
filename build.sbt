import play.sbt.routes.RoutesKeys
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings
import sbt.Keys.useSuperShell
import scoverage.ScoverageKeys

val appName = "trusts-individual-check"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(
    majorVersion := 0,
    scalaVersion := "2.12.12",
    SilencerSettings(),
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
  )
  .settings(useSuperShell in ThisBuild := false)
  .settings(publishingSettings: _*)
  .settings(inConfig(IntegrationTest)(itSettings): _*)
  .configs(IntegrationTest)
  .settings(resolvers += Resolver.jcenterRepo)
  .settings(
    PlayKeys.playDefaultPort := 9846,
    RoutesKeys.routesImport += "models._",
    ScoverageKeys.coverageExcludedPackages := "<empty>;scheduler.jobs.*;",
    ScoverageKeys.coverageExcludedFiles := "<empty>;Reverse.*;..*components.*;" +
      ".*Routes.*;.*ControllerConfiguration;.*EncryptedDataModule;.*Modules;.*WorkerConfig;",
    ScoverageKeys.coverageMinimum := 89,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    scalacOptions ++= Seq("-feature"),
  )

lazy val itSettings = Defaults.itSettings ++ Seq(
  unmanagedSourceDirectories   := Seq(
    baseDirectory.value / "it"
  ),
  unmanagedResourceDirectories := Seq(
    baseDirectory.value / "it" / "resources"
  ),
  parallelExecution            := false,
  fork                         := true,
  javaOptions                  ++= Seq(
    "-Dconfig.resource=it.application.conf",
    "-Dlogger.resource=it.logback.xml"
  )
)

dependencyOverrides ++= AppDependencies.overrides
