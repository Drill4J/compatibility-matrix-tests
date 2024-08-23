/**
 * Copyright 2020 - 2022 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.drill.compatibility.stubs

import kotlinx.serialization.Serializable

@Serializable
data class ServerData(
    val tests: Map<String, List<TestInfo>>,
    val sessions: List<SessionPayload>,
)

data class TestData(
    val testClass: String,
    val testName: String,
    val testResult: TestResult,
    val testParams: List<String> = emptyList(),
)

@Serializable
class AddTestsPayload(
    val sessionId: String,
    val tests: List<TestInfo> = emptyList(),
)

@Serializable
data class TestInfo(
    val groupId: String,
    val id: String,
    val testDefinitionId: String,
    val result: TestResult,
    val startedAt: Long,
    val finishedAt: Long,
    val details: TestDetails,
)

@Serializable
data class Label(
    val name: String,
    val value: String,
)

@Serializable
data class TestDetails @JvmOverloads constructor(
    val engine: String = "",
    val path: String = "",
    val testName: String = "",
    val params: Map<String, String> = emptyMap(),
    val metadata: Map<String, String> = emptyMap(),
    val labels: Set<Label> = emptySet(),
) : Comparable<TestDetails> {

    override fun compareTo(other: TestDetails): Int {
        return toString().compareTo(other.toString())
    }

    override fun toString(): String {
        return "engine='$engine', path='$path', testName='$testName', params=$params"
    }
}

enum class TestResult {
    PASSED,
    FAILED,
    ERROR,
    SKIPPED,
    UNKNOWN
}

@Serializable
class SessionPayload(
    val id: String,
    val groupId: String,
    val testTaskId: String,
    val startedAt: String
)

@Serializable
data class SessionIdPayload(
    val sessionId: String
)