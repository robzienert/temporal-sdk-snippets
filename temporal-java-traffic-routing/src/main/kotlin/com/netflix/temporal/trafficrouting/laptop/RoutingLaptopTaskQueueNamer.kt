/*
 * Copyright 2021 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.temporal.trafficrouting.laptop

import com.netflix.temporal.core.convention.TaskQueueNamer
import com.netflix.temporal.trafficrouting.TaskQueueRouteStrategy
import com.netflix.temporal.trafficrouting.TrafficRouteRepository
import com.netflix.temporal.trafficrouting.of
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.springframework.core.env.get
import org.springframework.stereotype.Component
import java.lang.IllegalStateException

public const val routingLaptopTaskQueueNamerBeanName: String = "routingLaptopTaskQueueNamer"

/**
 * Handles traffic routing task queue naming from the laptop's perspective.
 *
 * If a task queue cannot be resolved, a default laptop name will be created, instead of returning the original
 * task queue name.
 */
@Component(routingLaptopTaskQueueNamerBeanName)
@Profile("laptop")
public class RoutingLaptopTaskQueueNamer(
  private val trafficRouteRepository: TrafficRouteRepository,
  private val trafficRouteStrategies: List<TaskQueueRouteStrategy>,
  private val environment: Environment
) : TaskQueueNamer {

  private val log = LoggerFactory.getLogger(javaClass)

  private val temporalClusterName = resolveTemporalClusterName()

  override fun name(name: String): String {
    val property = trafficRouteRepository.findMatching(name) ?: return name.toLaptopName()
    return trafficRouteStrategies.of(property.strategy, log)?.resolveTaskQueueName(property) ?: name.toLaptopName()
  }

  /**
   * Assumes that laptop users have a name that matches their email.
   *
   * We don't actually do anything with regards to auth or anything by email, so it doesn't totally matter if it
   * doesn't line up 100% of the time.
   */
  private fun String.toLaptopName(): String {
    val user = System.getProperty("user.name")
    return "$user@netflix.com/$this"
  }

  private fun resolveTemporalClusterName(): String {
    return environment["netflix.environment"]
      ?: throw IllegalStateException("Unable to resolve netflix environment")
  }
}
