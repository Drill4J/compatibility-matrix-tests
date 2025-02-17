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

import java.net.InetSocketAddress
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler
import mu.KotlinLogging

class Netty4WsServerTest : JavaxWebSocketServerMatrixTest() {

    override val logger = KotlinLogging.logger {}

    override fun withWebSocketAnnotatedEndpoint(block: (String) -> Unit) =
        withWebSocketServer(DefaultProtocolHandlerChannelInitializer(), block)

    override fun withWebSocketInterfaceEndpoint(block: (String) -> Unit) =
        withWebSocketServer(CustomProtocolHandlerChannelInitializer(), block)

    private fun withWebSocketServer(initializer: ChannelInitializer<SocketChannel>, block: (String) -> Unit) {
        val bossGroup = NioEventLoopGroup()
        val workerGroup = NioEventLoopGroup()
        lateinit var serverChannel: Channel
        try {
            serverChannel = ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel::class.java)
                .childHandler(initializer)
                .bind(InetSocketAddress(0))
                .sync()
                .channel()
            val address = serverChannel.localAddress() as InetSocketAddress
            block("ws://localhost:${address.port}")
        } finally {
            serverChannel.close().sync()
            workerGroup.shutdownGracefully().sync()
            bossGroup.shutdownGracefully().sync()
        }
    }

    private class DefaultProtocolHandlerChannelInitializer : ChannelInitializer<SocketChannel>() {
        override fun initChannel(ch: SocketChannel) {
            ch.pipeline().addLast(HttpServerCodec())
            ch.pipeline().addLast(WebSocketServerProtocolHandler("/"))
            ch.pipeline().addLast(TextFrameChannelHandler())
            ch.pipeline().addLast(BinaryFrameChannelHandler())
        }
    }

    private class CustomProtocolHandlerChannelInitializer : ChannelInitializer<SocketChannel>() {
        override fun initChannel(ch: SocketChannel) {
            ch.pipeline().addLast(HttpServerCodec())
            ch.pipeline().addLast(HttpWebSocketHandshakeHandler())
        }
    }

    private class HttpWebSocketHandshakeHandler : SimpleChannelInboundHandler<HttpRequest>() {
        override fun channelRead0(ctx: ChannelHandlerContext, msg: HttpRequest) {
            if (HttpHeaderValues.UPGRADE.contentEqualsIgnoreCase(msg.headers().get(HttpHeaderNames.CONNECTION)) &&
                HttpHeaderValues.WEBSOCKET.contentEqualsIgnoreCase(msg.headers().get(HttpHeaderNames.UPGRADE))) {
                ctx.pipeline().remove(this)
                ctx.pipeline().addLast(TextFrameChannelHandler())
                ctx.pipeline().addLast(BinaryFrameChannelHandler())
                val wsUrl = "ws://${msg.headers().get(HttpHeaderNames.HOST)}${msg.uri()}"
                WebSocketServerHandshakerFactory(wsUrl, null, true).newHandshaker(msg)
                    ?.handshake(ctx.channel(), msg)
                    ?: WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel())
            }
        }
    }

    private class TextFrameChannelHandler : SimpleChannelInboundHandler<TextWebSocketFrame>() {
        override fun channelRead0(ctx: ChannelHandlerContext, msg: TextWebSocketFrame) {
            ctx.writeAndFlush(TextWebSocketFrame(attachSessionHeaders(msg.retain().text())))
        }
    }

    private class BinaryFrameChannelHandler : SimpleChannelInboundHandler<BinaryWebSocketFrame>() {
        override fun channelRead0(ctx: ChannelHandlerContext, msg: BinaryWebSocketFrame) {
            val message = msg.retain().content()
            val text = ByteArray(message.readableBytes()).also(message::readBytes).decodeToString()
            val response = attachSessionHeaders(text).encodeToByteArray()
            ctx.writeAndFlush(BinaryWebSocketFrame(Unpooled.copiedBuffer(response)))
        }
    }

}
