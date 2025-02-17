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
package com.epam.drill.compatibility.http.clients

import com.epam.drill.compatibility.matrix.ClientMatrixTest
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClientBuilder


class ApacheHttpClient4Test: ClientMatrixTest() {

    override fun callHttpEndpoint(
        endpoint: String,
        headers: Map<String, String>,
        body: String
    ): Pair<Map<String, String>, String> = HttpClientBuilder.create().build().run {
        val request = HttpPost(endpoint)
        headers.entries.forEach {
            request.addHeader(it.key, it.value)
        }
        request.entity = StringEntity(body)
        val response = this.execute(request)
        val responseHeaders = response.allHeaders.associate { it.name to it.value }
        val responseBody = response.entity.content.readBytes().decodeToString()
        responseHeaders to responseBody
    }

}