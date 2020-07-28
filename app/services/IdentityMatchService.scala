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

package services

import config.AppConfig
import connectors.IdentityMatchConnector
import exceptions.LimitException
import javax.inject.Inject
import models.{IdMatchApiResponseFailure, IdMatchApiResponseSuccess, IdMatchResponse}
import play.api.i18n.Messages
import repositories.IndividualCheckRepository
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class IdentityMatchService @Inject()(val connector: IdentityMatchConnector,
                                     val repository: IndividualCheckRepository,
                                     val appConfig: AppConfig)
                                    (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext){

  def matchId(id: String, nino: String, surname: String, forename: String, birthDate: String): Future[IdMatchResponse] = {

    val attemptCount: Future[Int] = repository.getCounter(id)

    val apiResponse: Future[Either[IdMatchApiResponseFailure, IdMatchApiResponseSuccess]] = attemptCount.flatMap(count =>
        if(count < appConfig.maxIdAttempts) {
          repository.setCounter(id = id, attempts = count + 1)
          Try {
            connector.matchId(nino, surname, forename, birthDate)
          } match {
            case Success(value) => value
            case Failure(exception) => throw new Exception("Something went wrong")
          }
        } else {
          Future.failed(new LimitException(s"Individual check - retry limit reached (${appConfig.maxIdAttempts})"))
        })

    val response = apiResponse.map {
      case Right(x) =>
        println(x)
        repository.setCounter(id = id, attempts = 0)
        IdMatchResponse(id = id, idMatch = x.individualMatch)
      case Left(x) => throw new Exception("Something went wrong")
    }

    response
  }
}
