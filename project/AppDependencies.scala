import play.core.PlayVersion.current
import sbt._

object AppDependencies {

  val compile = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"             %% "bootstrap-backend-play-27"        % "5.8.0",
    "org.reactivemongo"       %% "play2-reactivemongo"              % "0.20.3-play27",
    "uk.gov.hmrc"             %% "domain"                           % "5.11.0-play-27",
    "com.amazonaws"            % "aws-java-sdk-s3"                  % "1.11.1016",
    "com.enragedginger"       %% "akka-quartz-scheduler"            % "1.9.0-akka-2.6.x",
    "uk.gov.hmrc"             %% "mongo-lock"                       % "6.24.0-play-27"
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-27"   % "5.3.0",
    "org.scalatest"           %% "scalatest"                % "3.1.4",
    "com.typesafe.play"       %% "play-test"                % current,
    "com.vladsch.flexmark"    %  "flexmark-all"             % "0.35.10",
    "org.scalatestplus.play"  %% "scalatestplus-play"       % "4.0.3",
    "org.scalatestplus"       %% "mockito-3-3"              % "3.2.2.0",
    "org.scalacheck"          %% "scalacheck"               % "1.14.3",
    "com.github.tomakehurst"  %  "wiremock-standalone"      % "2.27.2"
  ).map(_ % "test,it")

  def apply(): Seq[ModuleID] = compile ++ test

  val akkaVersion = "2.6.12"
  val akkaHttpVersion = "10.2.3"

  val overrides = Seq(
    "com.typesafe.akka" %% "akka-stream_2.12" % akkaVersion,
    "com.typesafe.akka" %% "akka-protobuf_2.12" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j_2.12" % akkaVersion,
    "com.typesafe.akka" %% "akka-actor_2.12" % akkaVersion,
    "com.typesafe.akka" %% "akka-http-core_2.12" % akkaHttpVersion,
    "commons-codec" % "commons-codec" % "1.12"
  )
}
