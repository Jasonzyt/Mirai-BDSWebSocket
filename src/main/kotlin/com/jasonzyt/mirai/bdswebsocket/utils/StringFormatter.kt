package com.jasonzyt.mirai.bdswebsocket.utils

class StringFormatter(var string: String) {
    fun use(key: String, str: String): StringFormatter {
        string.replace("{$key}", str).also { string = it }
        return this
    }
}