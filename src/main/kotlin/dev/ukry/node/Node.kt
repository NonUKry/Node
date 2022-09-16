package dev.ukry.node

import com.google.gson.JsonParser
import dev.ukry.node.listener.PacketData
import dev.ukry.node.listener.PacketListener
import dev.ukry.node.packet.Packet
import dev.ukry.node.packet.handler.IncomingPacketHandler
import org.objenesis.ObjenesisStd
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPubSub
import java.util.concurrent.ForkJoinPool
import java.util.logging.Level
import java.util.logging.Logger


@Suppress("unused")//Just for aesthetic
class Node(private val channel: String, host: String, port: Int = 6379, password: String? = null) {

    private val jedisPool: JedisPool
    private var jedisPubSub: JedisPubSub? = null

    private val packetListeners: MutableList<PacketData>
    private val idToType: MutableMap<String, Class<out Packet>> = HashMap()
    private val typeToId: MutableMap<Class<out Packet>, String> = HashMap()

    private val constructorMaker = ObjenesisStd()
    private val logger = Logger.getLogger(javaClass.name)

    fun sendPacket(packet: Packet) {
        try {
            val `object` = packet.serialize()
            jedisPool.resource.use { jedis ->
                try {
                    jedis.publish(channel, packet::class.java.name + ";" + `object`.toString())
                } catch (ex: Exception) {
                    logger.log(Level.WARNING, "[Node] Failed publishing a packet with the id ${packet::class.java.name}..", ex)
                }
            }
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Error sending the packet ${packet::class.java.name}", e)
        }
    }

    fun findPacket(id : String): Packet? {
        if (!idToType.containsKey(id)) {
            println("No registered")
            return null
        }
        try {
            val clazz = idToType[id]!!
            return constructorMaker.newInstance(clazz)
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Error building the packet $id", e)
        }
        throw IllegalStateException("Could not create new instance of packet type")
    }

    fun registerPacket(clazz: Class<out Packet>) {
        val id = clazz.name
        check(!(idToType.containsKey(id) || typeToId.containsKey(clazz))) { "That packet has already been registered" }
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
                    packetListeners.add(PacketData(packetListener, method, packetClass))
                }
            }
        }
    }

    private fun setupPubSub() {
        logger.info("[Node] Initializing the node..")
        jedisPubSub = object : JedisPubSub() {
            override fun onMessage(channel: String, message: String) {
                if (channel.equals(this@Node.channel, ignoreCase = true)) {
                    try {
                        val args = message.split(";".toRegex()).toTypedArray()
                        val packet = findPacket(args[0])
                        if (packet != null) {
                            packet.deserialize(JsonParser.parseString(args[1]).asJsonObject)
                            for (data in packetListeners) {
                                if (data.matches(packet)) {
                                    data.method.invoke(data.instance, packet)
                                }
                            }
                        } else {
                            println(message)
                            println("Null packet")
                        }
                    } catch (e: Exception) {
                        logger.log(Level.WARNING, "[Node] Failed to handle message", e)
                    }
                }
            }
        }
        ForkJoinPool.commonPool().execute {
            try {
                jedisPool.resource.use { jedis ->
                    jedis.subscribe(jedisPubSub, channel)
                    logger.info("[Node] Successfully subscribing to channel $channel..")
                }
            } catch (exception: Exception) {
                logger.log(Level.WARNING, "[Node] Failed to subscribe to channel..", exception)
            }
        }
    }

    init {
        packetListeners = ArrayList()
        jedisPool = JedisPool(host, port)
        if (password != null && password != "") {
            jedisPool.resource.use { jedis ->
                jedis.auth(password)
                logger.info("[Node] Authenticating..")
            }
        }
        setupPubSub()
    }
}