package dev.ukry.node.packet

import com.google.gson.JsonObject

interface Packet {
    fun serialize() : JsonObject//non ? because no is for skiddy kids :)
    fun deserialize(`object` : JsonObject)
}