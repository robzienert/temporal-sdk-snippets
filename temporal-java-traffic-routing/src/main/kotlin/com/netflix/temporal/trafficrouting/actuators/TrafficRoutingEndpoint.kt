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
package com.netflix.temporal.trafficrouting.actuators

import com.netflix.temporal.trafficrouting.TrafficRouteProperty
import com.netflix.temporal.trafficrouting.TrafficRouteRepository
import org.springframework.boot.actuate.endpoint.annotation.Endpoint
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation
import org.springframework.stereotype.Component

/**
 * Provides information on the instance's current traffic routing configuration.
 */
@Component
@Endpoint(id = "temporalTrafficRouting")
public class TrafficRoutingEndpoint(
  private val trafficRouteRepository: TrafficRouteRepository
) {

  @ReadOperation
  internal fun list(): List<TrafficRouteProperty> =
    trafficRouteRepository.listAll()
}
