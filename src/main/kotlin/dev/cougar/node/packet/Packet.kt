package dev.cougar.node.packet

import com.google.gson.JsonObject
import java.math.BigInteger
import java.nio.charset.StandardCharsets

abstract class Packet {
    fun id() : Int = BigInteger(javaClass.simpleName.toByteArray(StandardCharsets.UTF_8)).toInt()
    abstract fun serialize() : JsonObject
    abstract fun deserialize(`object` : JsonObject)
}