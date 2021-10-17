package com.jasonzyt.mirai.bdswebsocket.ws

import com.google.gson.*
import com.jasonzyt.mirai.bdswebsocket.*
import com.jasonzyt.mirai.bdswebsocket.utils.AESCrypt
import java.util.*
import kotlin.random.Random

class RawMessage(
    val data: String = "",
    val encrypted: Boolean = true,
    val mode: String = "AES/CBC/PKCS5Padding"
) {
    var origin: String? = null

    companion object {
        fun fromMessage(message: Message, password: Password): RawMessage? {
            val ori = message.toString()
            val result = RawMessage(message.encrypt(password))
            result.origin = ori
            return result
        }

        fun fromJson(json: String): RawMessage? {
            return try {
                Gson().fromJson(json, RawMessage::class.java)
            } catch (e: JsonSyntaxException) {
                PluginMain.logger.error(e)
                null
            }
        }
    }

    fun decrypt(password: Password): Message? {
        return Message.fromJson(AESCrypt.decrypt(data, password.key, password.iv))
    }

    override fun toString(): String {
        return Gson().toJson(this)
    }
}

class Message {
    companion object {
        const val EVENT_GAMECHAT = "gameChat"
        const val EVENT_GAMECMD  = "gameCmd"
        const val EVENT_QQCHAT   = "QQChat"
        const val EVENT_QQCMD    = "QQCmd"
        const val EVENT_JOIN     = "join"
        const val EVENT_LEFT     = "left"
        const val TYPE_PLAYER_LIST = "getPlayerList"
        const val TYPE_STATS_QUERY = "statsQuery"
        const val TYPE_XUIDDB_GET  = "getXuid"
        const val TYPE_ADD_WHITELIST    = "addWhitelist"
        const val TYPE_REMOVE_WHITELIST = "addWhitelist"

        fun fromJson(json: String): Message? {
            return try {
                Gson().fromJson(json, Message::class.java)
            } catch (e: JsonSyntaxException) {
                PluginMain.logger.error(e)
                null
            }
        }

        private fun randomString(len: Int): String {
            var result = ""
            val str = "1234567890qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM"
            val rand = Random(Date().time)
            for (i in 0..len) {
                val randInt = rand.nextInt(0, 61)
                result += str[randInt]
            }
            return result
        }

        fun newMessage(type: String, data: Data): Message {
            val msg = Message()
            msg.type = type
            msg.data = data
            return msg
        }

        fun newEvent(event: String, data: Data): Message {
            val msg = Message()
            msg.event = event
            msg.data = data
            return msg
        }
    }

    var id: String = randomString(15)
    var event: String? = null
    var type: String? = null
    class Data {
        var isFakePlayer: Boolean? = null
        var xuid: Long? = null
        var nick: String? = null
        var name: String? = null
        var msg: String?  = null
        var cmd: String?  = null
        var success: Boolean? = null
        var reason: String? = null
    }
    var data: Data = Data()

    fun encrypt(password: Password): String {
        val text = this.toString()
        return AESCrypt.encrypt(text, password.key, password.iv)
    }

    override fun toString(): String {
        return Gson().toJson(this)
    }
}