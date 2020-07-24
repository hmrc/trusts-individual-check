/*
 * Copyright 2020 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package connectors

import config.AppConfig
import javax.inject.Inject
import models.{IdMatchApiRequest, IdMatchApiResponse}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import scala.concurrent.{ExecutionContext, Future}

class IdentityMatchConnector @Inject()(val http: HttpClient, val appConfig: AppConfig)
                                      (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext){

  private val postUrl = s"${appConfig.idMatchHost}/${appConfig.idMatchEndpoint}"

  def matchId(nino: String, surname: String, forename: String, birthDate: String): Future[IdMatchApiResponse] = {
    val request = IdMatchApiRequest(nino = nino, surname = surname, forename = forename, birthDate = birthDate)
    http.POST[IdMatchApiRequest, IdMatchApiResponse](postUrl, request)
  }
}
