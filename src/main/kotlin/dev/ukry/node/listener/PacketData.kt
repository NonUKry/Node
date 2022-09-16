package dev.ukry.node.listener

import dev.ukry.node.packet.Packet
import java.lang.reflect.Method

class PacketData(val instance : Any, val method : Method, private val packetClass : Class<*>) {
    fun matches(packet : Packet) : Boolean {
        return packetClass == packet.javaClass
    }
}