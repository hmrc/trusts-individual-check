import play.core.PlayVersion.current
import sbt._

object AppDependencies {

  val compile = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"             %% "bootstrap-backend-play-28"        % "5.24.0",
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-28"               % "0.70.0",
    "uk.gov.hmrc"             %% "domain"                           % "6.2.0-play-28",
    "com.amazonaws"            % "aws-java-sdk-s3"                  % "1.11.1016",
    "com.enragedginger"       %% "akka-quartz-scheduler"            % "1.9.0-akka-2.6.x"
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"   % "5.24.0",
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-28"  % "0.70.0",
    "org.scalatest"           %% "scalatest"                % "3.1.4",
    "com.typesafe.play"       %% "play-test"                % current,
    "com.vladsch.flexmark"    %  "flexmark-all"             % "0.35.10",
    "org.scalatestplus.play"  %% "scalatestplus-play"       % "5.1.0",
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
