/*
 * Copyright 2020 Netflix, Inc.
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
package com.netflix.temporal.trafficrouting

import com.netflix.temporal.core.convention.TaskQueueNamer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

public const val routingTaskQueueNamerBeanName: String = "routingTaskQueueNamer"

/**
 * Deployed worker traffic routing task queue namer.
 */
@Component(routingTaskQueueNamerBeanName)
public class RoutingTaskQueueNamer(
  private val repository: TrafficRouteRepository,
  private val strategies: List<TaskQueueRouteStrategy>
) : TaskQueueNamer {

  private val log by lazy { LoggerFactory.getLogger(javaClass) }

  public override fun name(name: String): String {
    val property = repository.findMatching(name) ?: return name
    return strategies.of(property.strategy, log)?.resolveTaskQueueName(property) ?: name
  }
}
