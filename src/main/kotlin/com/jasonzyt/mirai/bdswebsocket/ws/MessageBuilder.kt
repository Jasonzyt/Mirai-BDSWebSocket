package com.jasonzyt.mirai.bdswebsocket.ws

import com.jasonzyt.mirai.bdswebsocket.PluginData
import net.mamoe.mirai.contact.User

object MessageBuilder {
    fun getXuid(name: String): Message {
        val data = Message.Data()
        data.name = name
        return Message.newMessage(Message.TYPE_XUIDDB_GET, data)
    }

    fun addWhiteList(name: String, xuid: Long = -1): Message {
        val data = Message.Data()
        data.name = name
        if (xuid != -1L) data.xuid = xuid
        return Message.newMessage(Message.TYPE_ADD_WHITELIST, data)
    }

    fun removeWhiteList(name: String, xuid: Long = -1): Message {
        val data = Message.Data()
        data.name = name
        if (xuid != -1L) data.xuid = xuid
        return Message.newMessage(Message.TYPE_REMOVE_WHITELIST, data)
    }

    fun sendChatEvent(member: User, msg: String): Message {
        val data = Message.Data()
        data.msg = msg
        data.nick = member.nick
        val info = PluginData.gameInfo[member.id]
        if (info != null) {
            data.name = info.name
            data.xuid = info.xuid
        }
        return Message.newEvent(Message.EVENT_QQCHAT, data)
    }
}