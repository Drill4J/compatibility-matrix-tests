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
package com.epam.drill.compatibility.matrix

import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import java.net.URI
import java.nio.ByteBuffer
import javax.websocket.ClientEndpointConfig
import javax.websocket.CloseReason
import javax.websocket.Endpoint
import javax.websocket.EndpointConfig
import javax.websocket.Session
import org.glassfish.tyrus.client.ClientManager
import com.epam.drill.compatibility.context.TestRequestHolder

abstract class AbstractWebSocketServerTest {

    protected companion object {
        fun attachSessionHeaders(message: String) = TestRequestHolder.retrieve()
            ?.let {
                val headers = it.headers
                    .map { (key, value) -> "${key}=${value}" }
                    .joinToString("\n")
                "$message\nsession-headers:\n$headers"
            }
            ?: message
    }

    protected fun testEmptyHeadersRequest(address: String, type: String) {
        TestRequestHolder.remove()
        val responses = callWebsocketEndpoint(address, type = type)
        assertEquals(10, responses.size)
        responses.forEachIndexed { i, response ->
            assertEquals("test-request-$i", response)
        }
    }

    protected fun testSessionHeadersRequest(address: String, type: String) {
        TestRequestHolder.remove()
        val requestHeaders = mapOf(
            "drill-session-id" to "session-123",
            "drill-header-data" to "test-data"
        )
        val responses = callWebsocketEndpoint(address, requestHeaders, type = type)
        assertEquals(10, responses.size)
        responses.forEachIndexed { i, response ->
            val responseBody = response.split("\nsession-headers:\n")[0]
            val responseHeaders = response.split("\nsession-headers:\n").getOrNull(1)
                ?.lines()
                ?.associate { it.substringBefore("=") to it.substringAfter("=", "") }
            assertEquals("test-request-$i", responseBody)
            assertNotNull(responseHeaders)
            assertEquals("session-123", responseHeaders["drill-session-id"])
            assertEquals("test-data", responseHeaders["drill-header-data"])
        }
    }

    private fun callWebsocketEndpoint(
        endpointAddress: String,
        headers: Map<String, String> = emptyMap(),
        body: String = "test-request-",
        type: String = "text",
        count: Int = 10
    ) = ClientManager.createClient().run {
        val endpoint = ClientWebSocketEndpoint()
        val config = ClientEndpointConfig.Builder.create()
            .configurator(ClientWebSocketEndpointConfigurator(headers)).build()
        val session = this.connectToServer(endpoint, config, URI(endpointAddress))
        when (type) {
            "text" -> (0 until count).map(body::plus).forEach(session.basicRemote::sendText)
            "binary" -> (0 until count).map(body::plus).map(String::encodeToByteArray).map(ByteBuffer::wrap)
                .forEach(session.basicRemote::sendBinary)
        }
        Thread.sleep(1000)
        session.close(CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, CloseReason.CloseCodes.NORMAL_CLOSURE.name))
        endpoint.incomingMessages
    }

    private class ClientWebSocketEndpoint : Endpoint() {
        val incomingMessages = mutableListOf<String>()
        override fun onOpen(session: Session, config: EndpointConfig) {
            session.addMessageHandler(String::class.java, incomingMessages::add)
            session.addMessageHandler(ByteBuffer::class.java) { message ->
                incomingMessages.add(ByteArray(message.limit()).also(message::get).decodeToString())
            }
        }
    }

    private class ClientWebSocketEndpointConfigurator(
        private val drillHeaders: Map<String, String>
    ) : ClientEndpointConfig.Configurator() {
        override fun beforeRequest(headers: MutableMap<String, MutableList<String>>) =
            drillHeaders.forEach { (key, value) -> headers[key] = mutableListOf(value) }
    }

}
