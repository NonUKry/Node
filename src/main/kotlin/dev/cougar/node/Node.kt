package dev.cougar.node

import com.google.gson.JsonParser
import dev.cougar.node.packet.Packet
import dev.cougar.node.packet.handler.IncomingPacketHandler
import dev.cougar.node.packet.handler.PacketExceptionHandler
import dev.cougar.node.packet.listener.PacketListener
import dev.cougar.node.packet.listener.PacketListenerData
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPubSub
import java.util.concurrent.ForkJoinPool

@Suppress("USELESS_ELVIS", "unused", "DEPRECATION")
class Node(private val channel: String, host: String?, port: Int, password: String?) {
    private val jedisPool: JedisPool
    private var jedisPubSub: JedisPubSub? = null
    private val packetListeners: MutableList<PacketListenerData>
    private val idToType: MutableMap<Int, Class<out Packet>> = HashMap()
    private val typeToId: MutableMap<Class<out Packet>, Int> = HashMap()
    @JvmOverloads
    fun sendPacket(packet: Packet, exceptionHandler: PacketExceptionHandler? = null) {
        try {
            val `object` = packet.serialize()
                ?: throw IllegalStateException("Packet cannot generate null serialized data")
            jedisPool.resource.use { jedis ->
                try {
                    jedis.publish(channel, packet.id().toString() + ";" + `object`.toString())
                } catch (ex: Exception) {
                    println("[Node] Failed publishing a packet with the id ${packet.id()}..")
                    ex.printStackTrace()
                }
            }
        } catch (e: Exception) {
            exceptionHandler?.onException(e)
        }
    }

    fun buildPacket(id: Int): Packet? {
        if (!idToType.containsKey(id)) {
            return null
        }
        try {
            return idToType[id]!!.newInstance() as Packet
        } catch (e: Exception) {
            e.printStackTrace()
        }
        throw IllegalStateException("Could not create new instance of packet type")
    }

    fun registerPacket(clazz: Class<out Packet>) {
        val id = clazz.getDeclaredMethod("id").invoke(clazz.newInstance()) as Int
        check(!(idToType.containsKey(id) || typeToId.containsKey(clazz))) { "A packet with that ID has already been registered" }
        idToType[id] = clazz
        typeToId[clazz] = id
    }

    fun registerListenerClass(inputClass : Class<out PacketListener>, vararg args : Any) {
        this.registerListener(inputClass.constructors.first().newInstance(*args) as PacketListener)
    }

    fun registerListenerClass(inputClass : Class<out PacketListener>) {
        this.registerListener(inputClass.newInstance())
    }

    private fun registerListener(packetListener: PacketListener) {
        for (method in packetListener.javaClass.declaredMethods) {
            if (method.getDeclaredAnnotation(IncomingPacketHandler::class.java) != null) {
                var packetClass: Class<*>? = null
                if (method.parameters.isNotEmpty()) {
                    if (Packet::class.java.isAssignableFrom(method.parameters[0].type)) {
                        packetClass = method.parameters[0].type
                    }
                }
                if (packetClass != null) {
                    packetListeners.add(PacketListenerData(packetListener, method, packetClass))
                }
            }
        }
    }

    private fun setupPubSub() {
        println("[Node] Initializing the node..")
        jedisPubSub = object : JedisPubSub() {
            override fun onMessage(channel: String, message: String) {
                if (channel.equals(this@Node.channel, ignoreCase = true)) {
                    try {
                        val args = message.split(";".toRegex()).toTypedArray()
                        val id = Integer.valueOf(args[0])
                        val packet = buildPacket(id)
                        if (packet != null) {
                            packet.deserialize(PARSER.parse(args[1]).asJsonObject)
                            for (data in packetListeners) {
                                if (data.matches(packet)) {
                                    data.method.invoke(data.instance, packet)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        println("[Node] Failed to handle message")
                        e.printStackTrace()
                    }
                }
            }
        }
        ForkJoinPool.commonPool().execute {
            try {
                jedisPool.resource.use { jedis ->
                    jedis.subscribe(jedisPubSub, channel)
                    println("[Node] Successfully subscribing to channel..")
                }
            } catch (exception: Exception) {
                println("[Node] Failed to subscribe to channel..")
                exception.printStackTrace()
            }
        }
    }

    companion object {
        private val PARSER = JsonParser()
    }

    init {
        packetListeners = ArrayList()
        jedisPool = JedisPool(host, port)
        if (password != null && password != "") {
            jedisPool.resource.use { jedis ->
                jedis.auth(password)
                println("[Node] Authenticating..")
            }
        }
        setupPubSub()
    }
}