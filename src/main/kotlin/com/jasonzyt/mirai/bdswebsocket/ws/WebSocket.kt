package com.jasonzyt.mirai.bdswebsocket.ws

import com.jasonzyt.mirai.bdswebsocket.*
import com.sun.security.ntlm.Server
import kotlinx.coroutines.delay
import net.mamoe.mirai.console.util.CoroutineScopeUtils.childScope
import org.java_websocket.exceptions.WebsocketNotConnectedException
import org.java_websocket.framing.CloseFrame
import org.java_websocket.handshake.ServerHandshake
import java.net.ConnectException
import java.net.URI
import java.util.*

class AutoConnectWebSocket : TimerTask() {
    override fun run() {
        for (cli in clients) {
            cli.value.tryConnect()
        }
    }
}
class WebSocketClient(
    private val serverUri: URI?,
    val serverConfig: ServerConfig
) : org.java_websocket.client.WebSocketClient(serverUri) {

    private val messages = hashMapOf<String, Message>()

    suspend fun waitForMessage(id: String): Message? {
        var i = 0
        while (messages[id] != null && i <= 20) {
            i++
            delay(10)
        }
        return messages[id]
    }

    fun tryConnect(): Boolean {
        return try {
            if (!isOpen) {
                connect()
            }
            true
        } catch (e: ConnectException) {
            false
        }
    }

    fun send(rawMessage: RawMessage): Boolean {
        return try {
            if (isOpen) {
                PluginMain.logger.info("[WebSocket] Sent to $serverUri -> ${rawMessage.origin}")
                send(rawMessage.toString())
                return true
            }
            false
        } catch (e: IllegalArgumentException) {
            false
        } catch (e: WebsocketNotConnectedException) {
            false
        }
    }

    override fun onOpen(handshakedata: ServerHandshake?) {
        PluginMain.logger.info("[WebSocket] WebSocket server connection opened! URI: $serverUri")
    }

    override fun onMessage(message: String?) {
        PluginMain.logger.info("[WebSocket] Received from $serverUri <- $message")

    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        if (code != CloseFrame.ABNORMAL_CLOSE) {
            PluginMain.logger.warning(
                "[WebSocket] WebSocket server connection abnormal closed! URI: $serverUri Reason: $reason"
            )
            return
        }
        PluginMain.logger.info("[WebSocket] WebSocket server connection closed! URI: $serverUri")
    }

    override fun onError(ex: Exception?) {
        PluginMain.logger.error(ex)
    }
}

var clients = hashMapOf<String, WebSocketClient>()