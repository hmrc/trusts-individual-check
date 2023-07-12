import play.core.PlayVersion.current
import sbt._

object AppDependencies {

  val scoverageVersion = "2.0.7"
  val mongoVersion = "0.70.0"

  val compile = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"             %% "bootstrap-backend-play-28"        % "7.19.0",
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-28"               % mongoVersion,
    "uk.gov.hmrc"             %% "domain"                           % "8.3.0-play-28",
    "com.amazonaws"            % "aws-java-sdk-s3"                  % "1.12.501",
    "com.enragedginger"       %% "akka-quartz-scheduler"            % "1.9.3-akka-2.6.x"
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"   % "7.19.0",
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-28"  % mongoVersion,
    "org.scalatest"           %% "scalatest"                % "3.2.16",
    "com.typesafe.play"       %% "play-test"                % current,
    "com.vladsch.flexmark"    %  "flexmark-all"             % "0.64.8",
    "org.scalatestplus.play"  %% "scalatestplus-play"       % "5.1.0",
    "org.scalatestplus"       %% "mockito-3-3"              % "3.2.2.0",
    "org.scalacheck"          %% "scalacheck"               % "1.17.0",
    "com.github.tomakehurst"  %  "wiremock-standalone"      % "2.27.2",
  ).map(_ % "test,it")

  def apply(): Seq[ModuleID] = compile ++ test

  val akkaVersion = "2.6.12"
  val akkaHttpVersion = "10.2.3"

  val overrides: Seq[ModuleID] = Seq(
    "com.typesafe.akka" %% "akka-stream_2.12" % akkaVersion,
    "com.typesafe.akka" %% "akka-protobuf_2.12" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j_2.12" % akkaVersion,
    "com.typesafe.akka" %% "akka-actor_2.12" % akkaVersion,
    "com.typesafe.akka" %% "akka-http-core_2.12" % akkaHttpVersion,
    "commons-codec" % "commons-codec" % "1.12",
    "org.scoverage" %% "scalac-scoverage-runtime" % scoverageVersion,
    "org.scoverage" %% "scalac-scoverage-plugin" % scoverageVersion
  )
}
