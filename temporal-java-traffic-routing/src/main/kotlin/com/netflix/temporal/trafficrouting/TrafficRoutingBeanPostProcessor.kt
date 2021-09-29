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

import com.netflix.temporal.trafficrouting.laptop.routingLaptopTaskQueueNamerBeanName
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor
import org.springframework.core.Ordered.LOWEST_PRECEDENCE
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

/**
 * Modifies the [BeanDefinitionRegistry] to make traffic-routing related [TaskQueueNamer]s the primary
 * bean, rather than other task queue namers that would conflict with this module's functionality.
 */
@Order(LOWEST_PRECEDENCE)
@Component
public class TrafficRoutingBeanPostProcessor : BeanDefinitionRegistryPostProcessor {

  private val log by lazy { LoggerFactory.getLogger(javaClass) }

  private val preferredTaskQueueNamers = listOf(
    routingLaptopTaskQueueNamerBeanName,
    routingTaskQueueNamerBeanName
  )

  override fun postProcessBeanFactory(beanFactory: ConfigurableListableBeanFactory) {
  }

  override fun postProcessBeanDefinitionRegistry(registry: BeanDefinitionRegistry) {
    // Make other TaskQueueNamer beans _not_ primary, if they were originally marked this way.
    registry.beanDefinitionNames
      .filter { it.toLowerCase().endsWith("taskqueuenamer") && !preferredTaskQueueNamers.contains(it) }
      .map { registry.getBeanDefinition(it) }
      .forEach {
        if (it.isPrimary) {
          log.debug("Marking ${it.beanClassName} as not Primary in favor of a Traffic Routing TaskQueueNamer")
          it.isPrimary = false
        }
      }

    // Ensure correct traffic routing TaskQueueNamer is Primary
    if (registry.containsBeanDefinition(routingLaptopTaskQueueNamerBeanName)) {
      log.debug("Setting '$routingLaptopTaskQueueNamerBeanName' to primary TaskQueueNamer")
      registry.getBeanDefinition(routingLaptopTaskQueueNamerBeanName).isPrimary = true
    } else {
      log.debug("Setting '$routingTaskQueueNamerBeanName' to primary TaskQueueNamer")
      registry.getBeanDefinition(routingTaskQueueNamerBeanName).isPrimary = true
    }
  }
}
