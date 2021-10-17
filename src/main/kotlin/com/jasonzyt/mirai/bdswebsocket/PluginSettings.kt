package com.jasonzyt.mirai.bdswebsocket

import com.jasonzyt.mirai.bdswebsocket.utils.MD5Crypt
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value
import kotlinx.serialization.*

data class Password(val key: String, val iv: String)

@Serializable
data class ServerConfig(
    @ValueDescription("服务器唯一标识")
    val serverID: String = "",
    @ValueDescription("服务器名称")
    val serverName: String = "server",
    @ValueDescription("WebSocket服务器URI")
    val wsUri: String = "ws://127.0.0.1:11451",
    @ValueDescription("是否允许成员自助添加白名单")
    val selfAddWhitelist: Boolean = false,
    @ValueDescription("是否转发游戏聊天")
    val forwardChat: Boolean = true,
    @ValueDescription("是否转发游戏命令")
    val forwardCmd: Boolean = true,
    @ValueDescription("是否转发进入游戏")
    val forwardJoin: Boolean = true,
    @ValueDescription("是否转发退出游戏")
    val forwardLeft: Boolean = true,
    @ValueDescription("加密密码(建议强密码)")
    val password: String = "",
    @ValueDescription("转发目标群")
    val targetGroups: MutableList<Long> = mutableListOf(1145141919810),
    @ValueDescription("Bot命令监听群(唯一)")
    val listenGroup: Long = 11451444
) {
    fun getPassword(): Password {
        val md5 = MD5Crypt.encrypt(password)
        return Password(md5.substring(0, 15), md5.substring(16))
    }
}

object PluginSettings : AutoSavePluginConfig("settings") {
    @ValueDescription("BDS插件WebSocket服务器列表")
    var servers by value(mutableListOf(ServerConfig()))
    @ValueDescription("是否允许成员改绑XboxID")
    val allowRebind by value(false)
    @ValueDescription("转发聊天信息格式")
    val chatFormat by value("{name} >> {chat}")
    @ValueDescription("转发游戏命令格式")
    val cmdFormat by value("{name} (试图)执行命令 {cmd}")
    @ValueDescription("转发玩家进服格式")
    val joinFormat by value("玩家 {name} 进入了服务器, Xuid为{xuid}")
    @ValueDescription("转发玩家离开格式")
    val leftFormat by value("玩家 {name} 离开了服务器, Xuid为{xuid}")
    @ValueDescription("管理员列表(QQ)")
    val admins by value(mutableListOf(1145141919810))
    @ValueDescription("负责转发消息Bot的QQ(当登录了两个及两个以上Bot时需要填写,仅登录了一个不需要填写)")
    val bot: Long? = null

    fun getServerConfigByListenGroup(group: Long): ServerConfig? {
        for (server in servers) {
            if (server.listenGroup == group) return server
        }
        return null
    }
    fun getServerConfigByID(id: String): ServerConfig? {
        for (server in servers) {
            if (server.serverID == id) return server
        }
        return null
    }
    fun getServerConfigByName(name: String): ServerConfig? {
        for (server in servers) {
            if (server.serverID == name) return server
        }
        return null
    }
}