package com.jasonzyt.mirai.bdswebsocket

import com.jasonzyt.mirai.bdswebsocket.command.*
import com.jasonzyt.mirai.bdswebsocket.command.CommandID.*
import com.jasonzyt.mirai.bdswebsocket.ws.*
import com.mojang.brigadier.exceptions.CommandSyntaxException
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.Listener
import net.mamoe.mirai.event.events.MessageEvent
import java.net.ConnectException
import java.net.URI
import java.util.*

object PluginMain : KotlinPlugin(
    @OptIn(ConsoleExperimentalApi::class)
    JvmPluginDescription.loadFromResource()
) {
    private lateinit var messageListener: Listener<MessageEvent>
    private lateinit var messageForwarder: Job

    private fun onMessageListener() {
        val scope = GlobalEventChannel.parentScope(this)
        messageListener = scope.subscribeAlways {
            var server: ServerConfig? = null
            var check = false
            if (subject.id == sender.id) {
                if (PluginData.gameInfo[sender.id] != null)
                    check = true
            }
            else {
                for (ser in PluginSettings.servers) {
                    if (ser.listenGroup == subject.id) {
                        check = true
                        server = ser
                    }
                }
            }
            try {
                if (check) {
                    val data = CommandUtil.parse(message.serializeToMiraiCode(), CommandSource(sender, subject))
                    if (data != null) {
                        val context = data.context.build("")
                        when (CommandID.fromInt(CommandUtil.execute(data))) {
                            Help1       -> CommandCallbacks.getHelp(context)
                            Help2       -> CommandCallbacks.getHelpOfCommand(context)
                            GetXboxID   -> CommandCallbacks.getXboxID(context)
                            BindXboxID1 -> CommandCallbacks.bindXboxID(context)
                            BindXboxID2 -> CommandCallbacks.bindXboxIDAdmin(context)
                            WhiteList   -> TODO()
                            SelfAddWhiteList -> TODO()
                        }
                    }
                }
            }
            catch (e: IllegalArgumentException) {
                subject.sendMessage(e.message!!)
            }
            catch (e: CommandSyntaxException) {
                if (e.cursor == 0) {
                    if (server != null) {
                        val msg = MessageBuilder.sendChatEvent(sender, message.contentToString())
                        clients[server.serverName]?.send(RawMessage(msg.encrypt(server.getPassword())))
                    }
                }
                else subject.sendMessage(e.message!!)
            }
        }
    }

    private fun onNudgeListener() {

    }

    override fun onEnable() {
        GlobalEventChannel.exceptionHandler { e -> logger.error(e) }
        PluginSettings.reload()
        PluginData.reload()
        logger.info("Plugin Loaded! Author: Jasonzyt")
        for (ser in PluginSettings.servers)
            clients[ser.serverName] = WebSocketClient(URI(ser.wsUri), ser)
        Timer().schedule(AutoConnectWebSocket(), Date(), 5000)
        onMessageListener()
        CommandRegistry.registerAll()
        messageForwarder = GlobalScope.launch {

        }
    }

    override fun onDisable() {
        messageListener.complete()
    }
}
