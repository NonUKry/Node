package mx.fxmxgragfx.node.packet

import com.google.gson.JsonObject

interface Packet {
    fun id() : Int
    fun serialize() : JsonObject
    fun deserialize(`object` : JsonObject)
}