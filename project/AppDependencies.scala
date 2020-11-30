import play.core.PlayVersion.current
import sbt._

object AppDependencies {

  val compile = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"             %% "bootstrap-backend-play-27"        % "2.25.0",
    "org.reactivemongo"       %% "play2-reactivemongo"              % "0.20.13-play27",
    "uk.gov.hmrc"             %% "domain"                           % "5.10.0-play-27",
    "com.amazonaws"           % "aws-java-sdk-s3"                   % "1.11.820",
    "com.enragedginger"       %% "akka-quartz-scheduler"            % "1.8.4-akka-2.6.x",
    "uk.gov.hmrc"             %% "mongo-lock"                       % "6.23.0-play-27"
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-27"   % "2.25.0",
    "org.scalatest"           %% "scalatest"                % "3.1.2",
    "com.typesafe.play"       %% "play-test"                % current,
    "com.vladsch.flexmark"    %  "flexmark-all"             % "0.35.10",
    "org.scalatestplus.play"  %% "scalatestplus-play"       % "4.0.3",
    "org.scalatestplus"       %% "mockito-3-3"              % "3.2.0.0",
    "org.scalacheck"          %% "scalacheck"               % "1.14.1",
    "com.github.tomakehurst"  %  "wiremock-standalone"      % "2.27.1"
  ).map(_ % "test,it")

  def apply(): Seq[ModuleID] = compile ++ test

  val akkaVersion = "2.6.7"
  val akkaHttpVersion = "10.1.12"

  val overrides = Seq(
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.typesafe.akka" %% "akka-protobuf" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-http-core" % akkaHttpVersion
  )
}
