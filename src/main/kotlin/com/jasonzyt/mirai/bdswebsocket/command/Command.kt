package com.jasonzyt.mirai.bdswebsocket.command

import com.jasonzyt.mirai.bdswebsocket.GameInfo
import com.jasonzyt.mirai.bdswebsocket.PluginData
import com.jasonzyt.mirai.bdswebsocket.PluginSettings
import com.jasonzyt.mirai.bdswebsocket.ws.MessageBuilder
import com.jasonzyt.mirai.bdswebsocket.ws.RawMessage
import com.jasonzyt.mirai.bdswebsocket.ws.clients
import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.ParseResults
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.LongArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.LiteralCommandNode
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.User
import kotlin.reflect.full.functions

data class CommandSource(val src: User, val contact: Contact?)

class CommandRoot(val name: String, val desc: String) {
    var literal: LiteralArgumentBuilder<CommandSource> = literal(name)
    var node: LiteralCommandNode<CommandSource>? = null

    fun overload(cmd: (CommandContext<CommandSource>) -> Int) {
        literal.executes(Command(cmd))
    }

    fun overload(argNames: List<String>, argTypes: List<ArgumentType<*>>,
                 cmd: ((CommandContext<CommandSource>) -> Int)? = null) {
        var last: RequiredArgumentBuilder<CommandSource, *> =
            argument(argNames[argNames.size - 1], argTypes[argTypes.size - 1])
        var first: RequiredArgumentBuilder<CommandSource, *> = last
        if (cmd != null) last.executes(Command(cmd))
        for (i in argNames.size - 2 downTo 0) {
            first = argument(argNames[i], argTypes[i])
            first.then(last)
            last = first
        }
        literal.then(first)
    }
}

object CommandUtil {
    var commands = mutableMapOf<String, CommandRoot>()
    private var dispatcher = CommandDispatcher<CommandSource>()

    fun getRootUsage(): String {
        var result = ""
        for (cmd in commands)
            result += "+ ${cmd.key} - ${cmd.value.desc}\n"
        return result
    }

    fun getUsage(cmd: String, src: CommandSource): Array<String>? {
        return dispatcher.getAllUsage(commands[cmd]?.node, src, true)
    }

    fun parse(text: String, src: CommandSource): ParseResults<CommandSource>? {
        return dispatcher.parse(text, src)
    }

    fun execute(text: String, src: CommandSource): Int {
        return dispatcher.execute(parse(text, src))
    }

    fun execute(results: ParseResults<CommandSource>): Int {
        return dispatcher.execute(results)
    }

    fun register(root: CommandRoot) {
        commands[root.name] = root
        root.node = dispatcher.register(root.literal)
    }
}

enum class CommandID(val id: Int) {
    Help1(0),
    Help2(1),
    GetXboxID(2),
    BindXboxID1(3),
    BindXboxID2(4),
    WhiteList(5),
    SelfAddWhiteList(6);

    companion object {
        fun fromInt(id: Int) = CommandID.values().first { it.id == id }
    }
}

object CommandRegistry {
    val commandNames = hashMapOf(
        "Help" to "help",
        "BindXbox" to "绑定",
        "GetXboxID" to "查ID",
        "GetQQ" to "查QQ",
        "SelfAddWhiteList" to "自助加白",
        "WhiteList" to "白名单"
    )
    private val commandDesc = hashMapOf(
        "Help" to "获取帮助信息",
        "BindXbox" to "绑定XboxID",
        "GetXboxID" to "查询某人的XboxID",
        "GetQQ" to "查XboxID对应的QQ号",
        "SelfAddWhiteList" to "自助添加白名单",
        "WhiteList" to "白名单操作"
    )

    fun registerAll() {
        val clazz = CommandRegistry::class
        val ignore = listOf("registerAll", "equals", "hashCode", "toString")
        for (func in clazz.functions) {
            if (ignore.indexOf(func.name) == -1) {
                func.call(this)
            }
        }
    }

    fun registerHelp() {
        val root = CommandRoot(commandNames["Help"]!!, commandDesc["Help"]!!)
        root.overload { return@overload CommandID.Help1.id }
        root.overload(listOf("Command"),
            listOf(EnumArgumentType.enumArg(commandNames.values.toList()))) {
            return@overload CommandID.Help2.id
        }
        CommandUtil.register(root)
    }

    fun registerGetXboxID() {
        val root = CommandRoot(commandNames["GetXboxID"]!!, commandDesc["GetXboxID"]!!)
        root.overload(listOf("Target"), listOf(QQArgumentType.qq())) {
            return@overload CommandID.GetXboxID.id
        }
        CommandUtil.register(root)
    }

    fun registerBindXboxID() {
        val root = CommandRoot(commandNames["BindXboxID"]!!, commandDesc["BindXboxID"]!!)
        root.overload(listOf("XboxID"), listOf(StringArgumentType.greedyString())) {
            return@overload CommandID.BindXboxID1.id
        }
        root.overload(listOf("Target", "XboxID"),
            listOf(QQArgumentType.qq(), StringArgumentType.greedyString())) {
            return@overload CommandID.BindXboxID2.id
        }
        CommandUtil.register(root)
    }

    fun registerWhiteList() {
        val root = CommandRoot(commandNames["WhiteList"]!!, commandDesc["WhiteList"]!!)
        root.overload(listOf("Operate", "Name"),
            listOf(EnumArgumentType.enumArg(listOf("+", "-", "add", "remove")))) {
            return@overload CommandID.WhiteList.id
        }
        CommandUtil.register(root)
    }

    fun registerSelfAddWhiteList() {
        val root = CommandRoot(commandNames["SelfAddWhiteList"]!!, commandDesc["SelfAddWhiteList"]!!)
        root.overload { return@overload CommandID.SelfAddWhiteList.id }
        CommandUtil.register(root)
    }
}

object CommandCallbacks {
    // Check permission
    private fun isAdmin(src: CommandSource): Boolean {
        if (PluginSettings.admins.indexOf(src.src.id) == -1)
            return false
        return true
    }

    // Help
    suspend fun getHelp(context: CommandContext<CommandSource>) {
        val src = context.source
        val usage = CommandUtil.getRootUsage() + "\n详细内容请发送\"help <命令>\""
        src.contact?.sendMessage(usage)
    }
    suspend fun getHelpOfCommand(context: CommandContext<CommandSource>) {
        val src = context.source
        val idx = context.getArgument("Command", Int::class.java)
        val cmd = CommandRegistry.commandNames.values.toList()[idx]
        val root = CommandUtil.commands[cmd]
        if (root != null) {
            val content = CommandUtil.getUsage(cmd, src)
            var result = "${root.name} - ${root.desc}\n"
            if (content != null) {
                for (usage in content) {
                    result += "- ${root.name} $usage\n"
                }
                result.dropLast(1)
                src.contact?.sendMessage(result)
                return
            }
        }
        src.contact?.sendMessage("找不到此命令")
    }

    // Get Xbox ID
    suspend fun getXboxID(context: CommandContext<CommandSource>) {
        val src = context.source
        val qq = context.getArgument("Target", Long::class.java)
        val res = PluginData.gameInfo[qq]
        if (res == null) {
            src.contact?.sendMessage("该用户还未绑定XboxID")
            return
        }
        src.contact?.sendMessage("QQ: $qq\nXbox: ${res.name}\nXuid: ${res.xuid}")
    }

    // Bind Xbox ID
    suspend fun bindXboxID(context: CommandContext<CommandSource>) {
        val src = context.source
        val name = context.getArgument("XboxID", String::class.java)
        val server = PluginSettings.getServerConfigByListenGroup(src.contact?.id!!)
        if (server != null) {
            val info = GameInfo(name, -1)
            val msg = MessageBuilder.getXuid(name)
            val id = msg.id
            val client = clients[server.serverName]
            val res = client?.send(RawMessage.fromMessage(msg, server.getPassword())!!)
            if (res == true) {
                val response = client.waitForMessage(id)
                if (response != null) {
                    info.xuid = response.data.xuid!!
                }
            }
            PluginData.gameInfo[src.src.id] = info
            if (info.xuid == -1L) {
                src.contact.sendMessage("绑定成功! (请登陆游戏以完善Xuid信息)")
            }
            else {
                src.contact.sendMessage("绑定成功! Xuid: ${info.xuid}")
            }
        }
    }
    suspend fun bindXboxIDAdmin(context: CommandContext<CommandSource>) {
        val src = context.source
        if (!isAdmin(src)) {
            src.contact?.sendMessage("您没有权限执行此操作")
        }
        val target = context.getArgument("Target", Long::class.java)
        val name = context.getArgument("XboxID", String::class.java)
        val server = PluginSettings.getServerConfigByListenGroup(src.contact?.id!!)
        if (server != null) {
            val info = GameInfo(name, -1)
            val msg = MessageBuilder.getXuid(name)
            val id = msg.id
            val client = clients[server.serverName]
            val res = client?.send(RawMessage.fromMessage(msg, server.getPassword())!!)
            if (res == true) {
                val response = client.waitForMessage(id)
                if (response != null) {
                    info.xuid = response.data.xuid!!
                }
            }
            PluginData.gameInfo[target] = info
            if (info.xuid == -1L) {
                src.contact.sendMessage("绑定成功! (Xuid信息未完善)")
            }
            else {
                src.contact.sendMessage("绑定成功! Xuid: ${info.xuid}")
            }
        }
    }

    // WhiteList
    suspend fun whiteList(context: CommandContext<CommandSource>) {
        val src = context.source
        val operate = context.getArgument("Operate", Int::class.java)
        val name = context.getArgument("Name", String::class.java)

    }
}