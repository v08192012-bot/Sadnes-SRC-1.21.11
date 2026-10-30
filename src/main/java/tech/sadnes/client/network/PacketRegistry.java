package tech.sadnes.client.network;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public final class PacketRegistry {
    private static final Map<Long, Map<Integer, PacketType<?>>> BUCKETS = new HashMap<>();
    private static final Map<Class<?>, PacketType<?>> BY_CLASS = new HashMap<>();
    private static volatile Map<Long, Map<Integer, PacketType<?>>> frozenBuckets;
    private static volatile Map<Class<?>, PacketType<?>> frozenByClass;

    private PacketRegistry() {
    }

    private static long bucketKey(ProtocolState protocolState, PacketDirection packetDirection) {
        return (long)protocolState.ordinal() << 1 | packetDirection.ordinal();
    }

    static <P extends Packet<?>> PacketType<P> register(PacketType<P> packetType) {
        addToBucket(packetType);
        if (BY_CLASS.putIfAbsent(packetType.clazz(), packetType) != null) {
            throw new IllegalStateException("Duplicate packet class " + packetType.clazz().getName());
        } else {
            return packetType;
        }
    }

    static <P extends Packet<?>> PacketType<P> registerDecodeOnly(PacketType<P> packetType) {
        addToBucket(packetType);
        return packetType;
    }

    private static void addToBucket(PacketType<?> packetType) {
        if (frozenBuckets != null) {
            throw new IllegalStateException("registry frozen");
        }

        Map map = BUCKETS.computeIfAbsent(bucketKey(packetType.state(), packetType.direction()), longValue -> new HashMap<>());
        if (map.putIfAbsent(packetType.id(), packetType) != null) {
            throw new IllegalStateException("Duplicate id " + packetType.id() + " in " + packetType.state() + "/" + packetType.direction());
        }
    }

    static void freeze() {
        for (Entry<Long, Map<Integer, PacketType<?>>> entry : BUCKETS.entrySet()) {
            Map<Integer, PacketType<?>> map = (Map<Integer, PacketType<?>>)entry.getValue();
            int i = map.keySet().stream().mapToInt(Integer::intValue).max().orElse(-1);
            if (map.size() != i + 1) {
                throw new IllegalStateException("Non-contiguous ids in bucket " + entry.getKey());
            }

            for (int i2 = 0; i2 <= i; i2++) {
                if (!map.containsKey(i2)) {
                    throw new IllegalStateException("Non-contiguous ids in bucket " + entry.getKey() + ", missing " + i2);
                }
            }
        }

        HashMap hashMap = new HashMap();
        BUCKETS.forEach((longValue, map2) -> hashMap.put(longValue, Map.copyOf((Map<? extends Integer, ? extends PacketType<?>>)map2)));
        frozenBuckets = Map.copyOf(hashMap);
        frozenByClass = Map.copyOf(BY_CLASS);
    }

    public static PacketType<?> getType(ProtocolState protocolState, PacketDirection packetDirection, int i) {
        Map map = frozenBuckets.get(bucketKey(protocolState, packetDirection));
        PacketType packetType = map == null ? null : (PacketType)map.get(i);
        if (packetType == null) {
            throw new ProtocolException("Unknown packet id " + i + " in " + protocolState + "/" + packetDirection);
        } else {
            return packetType;
        }
    }

    public static int getId(Packet<?> packet) {
        PacketType packetType = frozenByClass.get(packet.getClass());
        if (packetType == null) {
            throw new ProtocolException("Unregistered packet " + packet.getClass().getName());
        } else {
            return packetType.id();
        }
    }
}
