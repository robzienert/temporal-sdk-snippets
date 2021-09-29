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
package com.netflix.temporal.trafficrouting.constraint

import org.slf4j.MDC
import org.springframework.stereotype.Component

private const val MDC_RULE_PREFIX = "mdc/"

/**
 * Matches constraints based on MDC values.
 *
 * The evaluator will look for constraint rules that have a key that starts with [MDC_RULE_PREFIX].
 * This evaluator is case-sensitive, and accepts `null` values for constraining on the the _absence_
 * of a particular key.
 */
@Component
public class MdcConstraintEvaluator : ConstraintEvaluator {

  override fun evaluate(constraints: TrafficRouteConstraints): Boolean {
    var includeMatch = true
    if (constraints.include.hasMdcRules()) {
      includeMatch = constraints.include.matchesMdc()
    }

    var excludeMatch = false
    if (constraints.exclude.hasMdcRules()) {
      excludeMatch = constraints.exclude.matchesMdc()
    }

    return includeMatch && !excludeMatch
  }

  private fun Map<String, String?>.hasMdcRules(): Boolean {
    return keys.any { it.startsWith(MDC_RULE_PREFIX) }
  }

  private fun Map<String, String?>.matchesMdc(): Boolean {
    return entries
      .filter { it.key.startsWith(MDC_RULE_PREFIX) }
      .all {
        val mdcValue = MDC.get(it.key.substring(4))
        val expectedValue = it.value

        if (mdcValue == null && expectedValue == null) {
          true
        } else {
          mdcValue == expectedValue
        }
      }
  }
}
