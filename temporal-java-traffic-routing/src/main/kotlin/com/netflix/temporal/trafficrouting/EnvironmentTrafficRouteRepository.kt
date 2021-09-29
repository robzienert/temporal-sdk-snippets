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
package com.netflix.temporal.trafficrouting

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.annotations.VisibleForTesting
import com.netflix.temporal.trafficrouting.constraint.ConstraintEvaluator
import org.slf4j.LoggerFactory
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.EnumerablePropertySource
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 * Exposes the traffic route overrides from the [ConfigurableEnvironment].
 *
 * It is expected that these overrides will be made available through Fast Properties, but they could be defined
 * from within the application config as well.
 */
@Component
public class EnvironmentTrafficRouteRepository(
  private val environment: ConfigurableEnvironment,
  private val constraintEvaluator: ConstraintEvaluator
) : TrafficRouteRepository {

  private val log by lazy { LoggerFactory.getLogger(javaClass) }

  private var hasCached: Boolean = false

  @VisibleForTesting
  internal val cache: ConcurrentMap<String, MutableSet<TrafficRouteProperty>> = ConcurrentHashMap()

  private val mapper = ObjectMapper(YAMLFactory()).registerModule(KotlinModule())

  override fun findMatching(taskQueueName: String): TrafficRouteProperty? {
    // Application startup will try to find matches before Spring's scheduling will have started the cache.
    // Ensure we've populated at least once before proceeding.
    if (!hasCached) {
      refreshCache()
    }

    val properties = cache[taskQueueName]
    if (properties == null || properties.isEmpty()) {
      log.debug("No traffic routing properties loaded in cache")
      return null
    }

    val matching = properties.filter { constraintEvaluator.evaluate(it.constraints) }
    if (matching.size > 1) {
      log.warn("Multiple traffic routing properties matched for '$taskQueueName', selecting none: $matching")
      return null
    }

    if (matching.isEmpty()) {
      log.debug("No matching traffic routing properties matched for '$taskQueueName'")
      return null
    }

    return matching.first()
  }

  override fun listAll(): List<TrafficRouteProperty> {
    return cache.values.flatten()
  }

  @VisibleForTesting
  @Scheduled(fixedDelay = 10_000)
  internal fun refreshCache() {
    val newCache = environment.propertySources
      .filterIsInstance<EnumerablePropertySource<*>>()
      .flatMap { it.propertyNames.toList() }
      .filter { it.startsWith("temporal.routing.") && it != "temporal.routing.enabled" }
      .mapNotNull { propertyName ->
        environment.getProperty(propertyName)?.let { propertyValue ->
          val result = try {
            mapper.readValue<TrafficRouteProperty>(propertyValue)
          } catch (e: JsonProcessingException) {
            log.error("Traffic route property invalid '$propertyName'", e)
            null
          }

          val taskQueueName = extractTaskQueueNameFromProperty(propertyName)
          if (taskQueueName == null) {
            null
          } else {
            result?.apply { originalTaskQueueName = taskQueueName }
          }
        }
      }
      .groupBy { it.originalTaskQueueName }
      .toMap()

    writeCache(newCache)
    hasCached = true
  }

  private fun writeCache(data: Map<String, List<TrafficRouteProperty>>) {
    cache.keys.removeIf { !data.keys.contains(it) }
    data.forEach { (key, properties) ->
      val current = cache[key]
      if (current == null) {
        cache[key] = properties.toMutableSet()
        return
      }
      current.removeIf { !properties.contains(it) }
      current.addAll(properties)
    }
  }

  // scheme: "temporal.routing.{task-queue}.{user}"
  private fun extractTaskQueueNameFromProperty(propertyKey: String): String? =
    propertyKey.split('.').let {
      if (it.size != 4) {
        log.warn(
          "Failed to extract task queue from routing property '$propertyKey': " +
            "Must use 'temporal.routing.{task-queue-name}.{identity}' format"
        )
        return null
      }
      return it[2]
    }
}
