package dev.cougar.node.packet.handler

import java.lang.Exception

class PacketExceptionHandler {
    fun onException(e : Exception) {
        println("Failed to send packet")
        e.printStackTrace()
    }
}