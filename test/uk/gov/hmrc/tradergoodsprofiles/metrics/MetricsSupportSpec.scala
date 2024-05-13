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

import com.codahale.metrics.{Counter, Histogram, MetricRegistry, Timer}
import org.mockito.ArgumentMatchers.{anyString, endsWith}
import org.mockito.Mockito
import org.mockito.Mockito._
import org.scalatest.compatible.Assertion
import org.scalatest.wordspec.AsyncWordSpecLike
import org.scalatestplus.mockito.MockitoSugar.mock

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class MetricsSupportSpec extends AsyncWordSpecLike {

  implicit val ec: ExecutionContext = ExecutionContext.global

  trait MockHasMetrics {
    self: MetricsSupport =>
    val timerContext                                  = mock[Timer.Context]
    val timer                                         = mock[Timer]
    val successCounter                                = mock[Counter]
    val failureCounter                                = mock[Counter]
    val histogram                                     = mock[Histogram]
    override lazy val metricsRegistry: MetricRegistry = mock[MetricRegistry]
    when(metricsRegistry.timer(anyString())) thenReturn timer
    when(metricsRegistry.counter(endsWith("success-counter"))) thenReturn successCounter
    when(metricsRegistry.counter(endsWith("failed-counter"))) thenReturn failureCounter
    when(metricsRegistry.histogram(anyString())) thenReturn histogram
    when(timer.time()) thenReturn timerContext
    when(timerContext.stop()) thenReturn 0L
  }

  private val metricName = "test-metrics"
  class TestMetricsSupport @Inject() extends MetricsSupport with MockHasMetrics

  def withTestMetrics[A](test: TestMetricsSupport => A): A =
    test(new TestMetricsSupport)

  "withMetricsTimerAsync" should {
    "start a timer and increment a counter for a successful future" in withTestMetrics { metrics =>
      metrics
        .withMetricsTimerAsync(metricName)(_ => Future.successful(()))
        .map { _ =>
          verifyCompletedWithSuccess(metricName, metrics)

        }
    }

    "increment success counter for a successful future where completeWithSuccess is called explicitly" in withTestMetrics {
      metrics =>
        metrics
          .withMetricsTimerAsync(metricName) { timer =>
            timer.completeWithSuccess()
            Future.successful(())
          }
          .map(_ => verifyCompletedWithSuccess(metricName, metrics))
    }

    "increment failure counter for a failed future" in withTestMetrics { metrics =>
      metrics
        .withMetricsTimerAsync(metricName)(_ => Future.failed(new Exception))
        .recover { case _ =>
          verifyCompletedWithFailure(metricName, metrics)
        }
    }

    "increment failure counter for a successful future where completeWithFailure is called explicitly" in withTestMetrics {
      metrics =>
        metrics
          .withMetricsTimerAsync(metricName) { timer =>
            timer.completeWithFailure()
            Future.successful(())
          }
          .map(_ => verifyCompletedWithFailure(metricName, metrics))
    }

    "only increment counters once regardless of how many times the user calls complete with success" in withTestMetrics {
      metrics =>
        metrics
          .withMetricsTimerAsync(metricName) { timer =>
            Future(timer.completeWithSuccess())
            Future(timer.completeWithSuccess())
            Future(timer.completeWithSuccess())
            Future(timer.completeWithSuccess())
            Future.successful(())
          }
          .map(_ => verifyCompletedWithSuccess(metricName, metrics))
    }

    "only increment counters once regardless of how many times the user calls complete with failure" in withTestMetrics {
      metrics =>
        metrics
          .withMetricsTimerAsync(metricName) { timer =>
            Future(timer.completeWithFailure())
            Future(timer.completeWithFailure())
            Future(timer.completeWithFailure())
            Future(timer.completeWithFailure())
            timer.completeWithFailure()
            Future.successful(())
          }
          .map(_ => verifyCompletedWithFailure(metricName, metrics))
    }

    "increment failure counter when the user throws an exception constructing their code block" in withTestMetrics {
      metrics =>
        assertThrows[RuntimeException] {
          metrics.withMetricsTimerAsync(metricName)(_ => Future.successful(throw new RuntimeException))
        }

        Future.successful(verifyCompletedWithFailure(metricName, metrics))
    }

  }

  def verifyCompletedWithSuccess(metricName: String, metrics: MockHasMetrics): Assertion = {
    val inOrder = Mockito.inOrder(metrics.timer, metrics.timerContext, metrics.successCounter)
    inOrder.verify(metrics.timer, times(1)).time()
    inOrder.verify(metrics.timerContext, times(1)).stop()
    inOrder.verify(metrics.successCounter, times(1)).inc()
    verifyNoMoreInteractions(metrics.timer)
    verifyNoMoreInteractions(metrics.timerContext)
    verifyNoMoreInteractions(metrics.successCounter)
    verifyNoInteractions(metrics.failureCounter)
    succeed
  }

  def verifyCompletedWithFailure(metricName: String, metrics: MockHasMetrics): Assertion = {
    val inOrder = Mockito.inOrder(metrics.timer, metrics.timerContext, metrics.failureCounter)
    inOrder.verify(metrics.timer, times(1)).time()
    inOrder.verify(metrics.timerContext, times(1)).stop()
    inOrder.verify(metrics.failureCounter, times(1)).inc()
    verifyNoMoreInteractions(metrics.timer)
    verifyNoMoreInteractions(metrics.timerContext)
    verifyNoMoreInteractions(metrics.failureCounter)
    verifyNoInteractions(metrics.successCounter)
    succeed
  }
}
