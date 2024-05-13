import sbt.*

object AppDependencies {

  private val playBootstrapVersion = "8.6.0"
  private val mongoVersion = "1.9.0"

  private val compile = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"             %% "bootstrap-backend-play-30"        % playBootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-30"               % mongoVersion,
    "uk.gov.hmrc"             %% "domain-play-30"                   % "9.0.0"
  )

  private val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"   % playBootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-30"  % mongoVersion
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test

}
