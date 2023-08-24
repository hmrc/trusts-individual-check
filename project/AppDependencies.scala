import sbt.*

object AppDependencies {

  val bootstrapVersion = "7.21.0"
  val mongoVersion = "1.3.0"

  private val compile = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"             %% "bootstrap-backend-play-28"        % bootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-28"               % mongoVersion,
    "uk.gov.hmrc"             %% "domain"                           % "8.3.0-play-28",
    "com.amazonaws"            % "aws-java-sdk-s3"                  % "1.12.536",
    "com.enragedginger"       %% "akka-quartz-scheduler"            % "1.9.3-akka-2.6.x"
  )

  private val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"   % bootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-28"  % mongoVersion,
    "org.scalatest"           %% "scalatest"                % "3.2.16",
    "com.vladsch.flexmark"    %  "flexmark-all"             % "0.64.8",
    "org.scalatestplus"       %% "scalacheck-1-17"          % "3.2.16.0",
    "org.scalatestplus"       %% "mockito-4-11"             % "3.2.16.0",
    "com.github.tomakehurst"  %  "wiremock-standalone"      % "2.27.2",
  ).map(_ % "test,it")

  def apply(): Seq[ModuleID] = compile ++ test

}
