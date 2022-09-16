# Node
Node to send packages with redis

This shit is so easy to use and implement

# Maven implement
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
<dependencies>
    <dependency>
        <groupId>dev.ukry</groupId>
        <artifactId>node</artifactId>
        <version>2.0-STABLE</version>
        <scope>compile</scope>
    </dependency>
</dependencies>
```

# Kotlin Usage

### Initialize the node
```kotlin
package example.pkg

class NodeExample {
    
   companion object {
      @JvmStatic
      fun main(args : Array<String>) {
          val node = Node("TestChannel", "127.0.0.1", 6379, null)
      }
  }
}
```

### Creating a custom packet
```kotlin
package example.pkg

import com.google.gson.JsonObject
import dev.ukry.node.packet.Packet

class ExampleCustomPacket(var msg : String) : Packet {
    override fun serialize(): JsonObject {
        val obj = JsonObject()
        obj.addProperty("message", msg)
        return obj
    }

    override fun deserialize(obj: JsonObject) {
        this.msg = obj.get("message").asString
    }
}
```

### Create a packet listener
```kotlin
package example.pkg

import dev.ukry.node.listener.PacketListener
import dev.ukry.node.packet.handler.IncomingPacketHandler

class ExamplePacketListener : PacketListener {
    
    @IncomingPacketHandler
    fun onPacketReceived(packet : ExampleCustomPacket) {
        println("Received this mg: ${packet.msg}")
    }
}
```

### Register the packet listener
```kotlin
package example.pkg

class NodeExample {
    
   companion object {
      @JvmStatic
      fun main(args : Array<String>) {
          val node = Node("TestChannel", "127.0.0.1", 6379, null)
          node.registerListenerClass(EmxaplePacketListener::class.java)
      }
  }
}
```

# Java Usage

### Initialize the node

```java
package example.pkg;

import dev.ukry.node.Node;

public class NodeExample {

    public static void main(String[] args) {
        Node node = new Node("TestChannel", "127.0.0.1", 6379, false);
    }
}
```

### Creating a custom packet

```java
package example.pkg;

import com.google.gson.JsonObject;
import dev.ukry.node.packet.Packet;
import org.jetbrains.annotations.NotNull;

public class ExampleCustomPacket implements Packet {

    private String msg;

    public ExampleCustomPacket(String msg) {
        this.msg = msg;
    }

    @NotNull
    @Override
    public JsonObject serialize() {
        JsonObject object = new JsonObject();
        object.addProperty("message", msg);
        return object;
    }

    @Override
    public void deserialize(@NotNull JsonObject object) {
        this.msg = object.get("message").getAsString();
    }

    public String getMsg() {
        return this.msg;
    }
}
```

### Create a packet listener

```java
package example.pkg;

import dev.ukry.node.listener.PacketListener;
import dev.ukry.node.packet.handler.IncomingPacketHandler;

public class ExamplePacketListener implements PacketListener {

    @IncomingPacketHandler
    public void onPacketReceived(ExampleCustomPacket packet) {
        System.out.println(packet.getMsg());
    }
}
```

### Register the packet listener

```java
package example.pkg;

import dev.ukry.node.ExamplePacketListener;
import dev.ukry.node.Node;

public class NodeExample {

    public static void main(String[] args) {
        Node node = new Node("TestChannel", "127.0.0.1", 6379, false);
        node.registerListenerClass(ExamplePacketListener.class);
    }
}
```

## And ready :)