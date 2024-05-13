/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.tradergoodsprofiles.metrics

import com.codahale.metrics.{MetricRegistry, Timer}

import java.util.concurrent.atomic.AtomicBoolean
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

trait MetricsSupport {

  val metricsRegistry: MetricRegistry

  def histo(metricKey: String) =
    metricsRegistry.histogram(metricKey)

  def counter(metricsKey: String) =
    metricsRegistry.counter(metricsKey)

  class MetricsTimer(metricKey: String) {
    val timerContext: Timer.Context = metricsRegistry.timer(s"$metricKey-timer").time()
    val successCounter              = metricsRegistry.counter(s"$metricKey-success-counter")
    val failureCounter              = metricsRegistry.counter(s"$metricKey-failed-counter")
    val timerRunning                = new AtomicBoolean(true)

    def completeWithSuccess(): Unit =
      if (timerRunning.compareAndSet(true, false)) {
        timerContext.stop()
        successCounter.inc()
      }

    def completeWithFailure(): Unit =
      if (timerRunning.compareAndSet(true, false)) {
        timerContext.stop()
        failureCounter.inc()
      }

    def processWithTimer[T](timer: Timer.Context)(f: => Future[T])(implicit ec: ExecutionContext): Future[T] =
      f map { data =>
        timer.stop()
        data
      } recover { case e =>
        timer.stop()
        throw e
      }

  }

  /** Execute a block of code with a metrics timer.
    *
    * Intended for use in connectors that call other microservices.
    *
    * It's expected that the user of this method might want to handle
    * connector failures gracefully and therefore they are given a [[MetricsTimer]]
    * to optionally record whether the call was a success or a failure.
    *
    * If the user does not specify otherwise the status of the result Future is
    * used to determine whether the block was successful or not.
    *
    * @param metricKey The id of the metric to be collected
    * @param block The block of code to execute asynchronously
    * @param ec The [[scala.concurrent.ExecutionContext]] on which the block of code should run
    * @return The result of the block of code
    */
  def withMetricsTimerAsync[T](
    metricKey: String
  )(block: MetricsTimer => Future[T])(implicit ec: ExecutionContext): Future[T] =
    withMetricsTimer(metricKey) { timer =>
      val result = block(timer)

      // Clean up timer if the user doesn't
      result.foreach(_ => timer.completeWithSuccess())

      // Clean up timer on unhandled exceptions
      result.failed.foreach(_ => timer.completeWithFailure())

      result
    }

  def withMetricsTimer[T](metricKey: String)(block: MetricsTimer => T): T = {
    val timer = new MetricsTimer(metricKey)

    try block(timer)
    catch {
      case NonFatal(e) =>
        timer.completeWithFailure()
        throw e
    }
  }
}
