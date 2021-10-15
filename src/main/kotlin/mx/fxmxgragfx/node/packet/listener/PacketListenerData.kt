package mx.fxmxgragfx.node.packet.listener

import mx.fxmxgragfx.node.packet.Packet
import java.lang.reflect.Method

class PacketListenerData(val instance : Any, val method : Method, private val packetClass : Class<*>) {
    fun matches(packet : Packet) : Boolean {
        return packetClass == packet.javaClass
    }
}