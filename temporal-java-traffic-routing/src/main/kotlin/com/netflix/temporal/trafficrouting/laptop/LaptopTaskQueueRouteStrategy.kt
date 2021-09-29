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

import com.netflix.temporal.trafficrouting.TaskQueueRouteStrategy
import com.netflix.temporal.trafficrouting.TrafficRouteProperty
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Renames task queues for work that should be routed to a laptop.
 *
 * Must be compatible with [LaptopTaskQueueNamer] in `temporal-java-spring`.
 */
@Component
public class LaptopTaskQueueRouteStrategy : TaskQueueRouteStrategy {

  private val log by lazy { LoggerFactory.getLogger(javaClass) }

  override val type: String = "laptop"

  override fun resolveTaskQueueName(property: TrafficRouteProperty): String {
    val user = property.config["user"]
    if (user.isNullOrBlank()) {
      log.warn("Misconfigured laptop config: Missing 'user', will not override task queue: $property")
      return property.originalTaskQueueName
    }
    return "$user/${property.originalTaskQueueName}"
  }
}
