package com.jasonzyt.mirai.bdswebsocket.command

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.exceptions.CommandExceptionType
import com.mojang.brigadier.exceptions.CommandSyntaxException

class NoSpaceStringArgumentType: ArgumentType<String> {
    override fun parse(reader: StringReader): String {
        val start: Int = reader.cursor
        while (reader.canRead() && reader.peek() != ' ') {
            reader.skip()
        }
        return reader.string.substring(start, reader.cursor)
    }

    override fun toString(): String {
        return "string"
    }

    override fun getExamples(): MutableCollection<String> {
        return mutableListOf("word", "[word_with&any*symbols]")
    }

    companion object {
        fun stringWithoutSpace(): NoSpaceStringArgumentType {
            return NoSpaceStringArgumentType()
        }
    }
}

class QQArgumentType: ArgumentType<Long> {
    override fun parse(reader: StringReader?): Long? {
        if (reader != null) {
            val start: Int = reader.cursor
            var flag = true
            while (reader.canRead() && reader.peek() != ' ') {
                if (flag && !reader.peek().isDigit())
                    flag = false
                reader.skip()
            }
            val code = reader.string.substring(start, reader.cursor)
            if (flag) return code.toLong()
            val list = code.substring(1, code.length - 1).split(':')
            if (list[1] == "at")
                return list[2].toLong()
        }
        return null
    }

    override fun getExamples(): MutableCollection<String> {
        return mutableListOf("1145141919", "@at", "[mirai:at:100000]")
    }

    companion object {
        fun qq(): QQArgumentType {
            return QQArgumentType()
        }
    }
}

class EnumArgumentType(private val values: List<String>): ArgumentType<Int> {
    override fun parse(reader: StringReader?): Int {
        if (reader != null) {
            val start: Int = reader.cursor
            while (reader.canRead() && reader.peek() != ' ') {
                reader.skip()
            }
            val str = reader.string.substring(start, reader.cursor)
            return values.indexOf(str)
        }
        return -1
    }

    override fun getExamples(): MutableCollection<String> {
        return values.toMutableList()
    }

    companion object {
        fun enumArg(values: List<String>): EnumArgumentType {
            return EnumArgumentType(values)
        }
    }
}