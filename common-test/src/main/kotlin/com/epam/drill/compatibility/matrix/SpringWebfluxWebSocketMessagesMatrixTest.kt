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

import java.net.URI
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.reactive.socket.client.WebSocketClient
import org.springframework.web.socket.config.annotation.EnableWebSocket
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink
import reactor.core.publisher.Mono
import com.epam.drill.compatibility.context.TestRequestHolder
import com.epam.drill.compatibility.context.DrillRequest

@RunWith(SpringRunner::class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [SpringWebfluxWebSocketMessagesMatrixTest.TestWebSocketServerConfig::class]
)
abstract class SpringWebfluxWebSocketMessagesMatrixTest : SpringCommonWebSocketMessagesMatrixTest() {

    @Autowired
    lateinit var webSocketClient: WebSocketClient

    override fun callWebSocketEndpoint(payloadType: String, body: String, count: Int) = TestWebSocketClientHandler().run {
        webSocketClient.execute(URI("ws://localhost:$serverPort"), this).subscribe()
        while (this.session?.isOpen != true) Thread.sleep(100)
        Thread.sleep(500)
        (0 until count).map(body::plus).forEach {
            TestRequestHolder.store(DrillRequest("$it-session", mapOf("drill-data" to "$it-data")))
            when (payloadType) {
                "text" -> this.sendingEmitter.next(it)
                "binary" -> this.sendingEmitter.next(it.encodeToByteArray())
            }
            Thread.sleep(100)
            TestRequestHolder.remove()
        }
        Thread.sleep(2000)
        this.session!!.close().block()
        this.incomingMessages to this.incomingContexts
    }

    @Configuration
    @EnableWebSocket
    @EnableAutoConfiguration
    open class TestWebSocketServerConfig {
        @Bean
        open fun testWebSocketHandler() = TestWebSocketServerHandler()
        @Bean
        open fun handlerMapping() = SimpleUrlHandlerMapping(mapOf("/" to testWebSocketHandler()), -1)
    }

    @Configuration
    abstract class AbstractTestWebSocketClientConfig {
        @Bean
        abstract fun testWebSocketClient(): WebSocketClient
    }

    open class TestWebSocketServerHandler : WebSocketHandler, TestRequestEndpoint {
        override val incomingMessages = mutableListOf<String>()
        override val incomingContexts = mutableListOf<DrillRequest?>()
        override fun handle(session: WebSocketSession): Mono<Void> = session.receive()
            .map(WebSocketMessage::getPayloadAsText)
            .doOnNext(incomingMessages::add)
            .doOnNext { incomingContexts.add(TestRequestHolder.retrieve()) }
            .map { "$it-response" }
            .doOnNext { TestRequestHolder.store(DrillRequest("$it-session", mapOf("drill-data" to "$it-data"))) }
            .map(session::textMessage)
            .let(session::send)
    }

    protected class TestWebSocketClientHandler : WebSocketHandler, TestRequestEndpoint {
        override val incomingMessages = mutableListOf<String>()
        override val incomingContexts = mutableListOf<DrillRequest?>()
        var session: WebSocketSession? = null
        lateinit var sendingEmitter: FluxSink<Any>
        private val sendingMessages: Flux<Any> = Flux.create { this.sendingEmitter = it; }
        override fun handle(session: WebSocketSession): Mono<Void> {
            this.session = session
            val input = session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .map(incomingMessages::add)
                .doOnNext { incomingContexts.add(TestRequestHolder.retrieve()) }
                .then()
            val output = sendingMessages.map {
                if (it is ByteArray) {
                    session.binaryMessage { factory -> factory.wrap(it) }
                } else {
                    session.textMessage(it.toString())
                }
            }
            return Mono.zip(input, session.send(output)).then()
        }
    }

}
