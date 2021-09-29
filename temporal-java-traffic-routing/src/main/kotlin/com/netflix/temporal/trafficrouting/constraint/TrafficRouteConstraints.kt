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

/**
 * A constraint for the task queue traffic route property, providing the main mechanism for targeting
 * specific tasks for rerouting..
 *
 * All [include] and [exclude] rules must evaluate successfully for the overall constraint to be satisfied.
 */
public data class TrafficRouteConstraints(
  val include: Map<String, String?> = mapOf(),
  val exclude: Map<String, String?> = mapOf()
)
