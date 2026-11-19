package com.crossower.valkyrien_mechanic.block;

import com.crossower.valkyrien_mechanic.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.joml.Vector3d;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.ValkyrienSkiesMod;
import org.valkyrienskies.mod.common.util.GameToPhysicsAdapter;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import javax.annotation.Nullable;
import java.util.*;

public class ConnectionSystem
{
    public enum PortType {
        INPUT,
        OUTPUT,
        BOTH;
    }

    public enum SignalType {
        LOGIC(0xFFEA297A),
        BEARING(0xFF3296FF),
        MOTOR(0xFFFF8000),
        SEAT(0xFF0DA02A),
        CONTROL_SEAT(0xFF7DF800),
        CONTROLLER(0xFF7A31C2),
        THRUSTER(0xFFFF8000);

        public final int color;

        SignalType(int color)
        {
            this.color = color;
        }
    }

    public record Port(int id, PortType type, SignalType signal, Set<SignalType> accepts, float x, float y, float z)
    {
        public Port(int id, PortType type, SignalType signal, float x, float y, float z)
        {
            this(id, type, signal, type == PortType.INPUT ? Set.of(signal) : Set.of(), x, y, z);
        }

        public static Port output(int id, SignalType emits, float x, float y, float z)
        {
            return new  Port(id, PortType.OUTPUT, emits, Set.of(), x, y, z);
        }

        public static Port input(int id, SignalType kind , Set<SignalType> accepts, float x, float y, float z)
        {
            return new Port(id, PortType.INPUT, kind, accepts, x, y, z);
        }

        public static Port both(int id, SignalType kind, Set<SignalType> accepts, float x, float y, float z)
        {
            return new Port(id, PortType.BOTH, kind, accepts, x, y, z);
        }
    }

    public interface ConnectableBlockEntity {
        List<Port> getPorts();

        PortLinks getLinks();

        float getOutput(int portId);

        void onLinksChanged();

        default void onPortUse(int portId, ServerPlayer player) {}

        UUID getUid();
    }

    public interface RotationSource
    {
        float getTorque();
        float getMaxSpeed();
        int getDirection();
        float getTargetAngle();
    }

    public record PortRef(BlockPos pos, int id) {

    }

    public static class PortLinks {
        public record Link(BlockPos offset, int sourcePort, long sourceShip, UUID sourceUid)
        {
            public static final long SAME_SPACE = -1;

            public BlockPos resolve(BlockPos self) {
                return sourceShip == SAME_SPACE ? self.offset(offset) : offset;
            }
        }

        private final Map<Integer, Link> links = new HashMap<>();

        public Map<Integer, Link> all() {return links;}
        public void set(int inputPort, BlockPos offset, int sourcePort, long sourceShip, UUID sourceUid) {links.put(inputPort, new Link(offset, sourcePort, sourceShip, sourceUid));}
        public boolean remove(int inputPort)
        {
            return links.remove(inputPort) !=null;
        }

        public void save(CompoundTag tag) {
            ListTag listTag = new ListTag();
            for (Map.Entry<Integer, Link> entry : links.entrySet()) {
                CompoundTag t = new CompoundTag();
                t.putInt("in", entry.getKey());
                t.putInt("src", entry.getValue().sourcePort());
                BlockPos o = entry.getValue().offset();
                t.putIntArray("off", new int[]{o.getX(), o.getY(), o.getZ()});
                t.putLong("ship", entry.getValue().sourceShip());
                if (entry.getValue().sourceUid() != null) t.putUUID("uid", entry.getValue().sourceUid());
                listTag.add(t);

            }
            tag.put("Links", listTag);
        }

        public void load(CompoundTag tag)
        {
            links.clear();
            ListTag list = tag.getList("Links", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++)
            {
                CompoundTag t = list.getCompound(i);
                int[] o = t.getIntArray("off");
                if (o.length == 3)
                {
                    links.put(t.getInt("in"), new Link(new BlockPos(o[0], o[1], o[2]), t.getInt("src"),
                            t.contains("ship") ? t.getLong("ship") : Link.SAME_SPACE,
                            t.hasUUID("uid") ? t.getUUID("uid") : null));
                }
            }
        }


        public static void handleLink(ServerPlayer player, PortRef a, @Nullable PortRef b) {
            if (!player.isHolding(ModItems.CONNECTOR_TOOL.get())) {
                return;
            }
            Level level = player.level();
            if (b == null) disconnect(level, a); else connect(level, a, b);
        }

        public static void handlePortUse(ServerPlayer player, PortRef ref)
        {
            if (!player.isHolding(ModItems.CONNECTOR_TOOL.get())) {return;}
            ConnectableBlockEntity be = find(player.level(), ref);
            if (be != null) be.onPortUse(ref.id(), player);
        }

        public static final int MAX_LINK_DISTANCE = 32;

        public static void connect(Level level, PortRef a, PortRef b)
        {
            if (a.pos().equals(b.pos())) return;
            ConnectableBlockEntity ca = find(level, a), cb = find(level, b);
            if (ca == null || cb == null) return;
            Port pa = portById(ca.getPorts(), a.id()), pb = portById(cb.getPorts(), b.id());
            if (pa == null || pb == null) return;

            boolean aIsSource = canSend(pa);
            Port outPort = aIsSource ? pa : pb;
            Port inPort = aIsSource ? pb : pa;
            if (!canSend(outPort) || !canReceive(inPort)) return;
            if (!inPort.accepts().contains(outPort.signal())) return;

            PortRef in = aIsSource ? b : a, out = aIsSource ? a : b;
            ConnectableBlockEntity inBe = aIsSource ? cb : ca;

            Ship shipIn = VSGameUtilsKt.getShipManagingPos(level, in.pos());
            Ship shipOut = VSGameUtilsKt.getShipManagingPos(level, out.pos());
            if ((shipIn == null) != (shipOut == null)) return;
            if (shipIn != null && shipOut != null && shipIn.getId() != shipOut.getId() && !jointConnected(level, shipIn, shipOut)) { return; }

            if (worldCenter(shipIn, in.pos()).distance(worldCenter(shipOut, out.pos())) > MAX_LINK_DISTANCE) return;

            long spaceIn = shipIn == null ? Link.SAME_SPACE : shipIn.getId();
            long spaceOut = shipOut == null ? Link.SAME_SPACE : shipOut.getId();

            UUID outUid = (aIsSource ? ca : cb).getUid();

            BlockPos stored;
            long storedShip;
            if (spaceIn == spaceOut)
            {
                stored = out.pos().subtract(in.pos());
                storedShip = Link.SAME_SPACE;
            }
            else
            {
                stored = out.pos();
                storedShip = spaceOut;
            }

            Link existing = inBe.getLinks().all().get(in.id());
            boolean sameLink = existing != null && existing.offset().equals(stored)
                    && existing.sourcePort() == out.id() && existing.sourceShip() == storedShip
                    && outUid.equals(existing.sourceUid());
            if (sameLink) inBe.getLinks().remove(in.id());
            else inBe.getLinks().set(in.id(), stored, out.id(), storedShip, outUid);
            inBe.onLinksChanged();
        }

        private static Vector3d worldCenter(Ship ship, BlockPos pos)
        {
            Vector3d local = VectorConversionsMCKt.toJOML(pos.getCenter());
            return ship == null ? local : ship.getTransform().getShipToWorld().transformPosition(local);
        }

        private static boolean jointConnected(Level level, Ship a, Ship b)
        {
            GameToPhysicsAdapter gtpa = ValkyrienSkiesMod.getOrCreateGTPA(VSGameUtilsKt.getDimensionId(level));
            List<Long> connected = gtpa.getAllConnectedShips(a.getId());
            return connected.contains(b.getId());
        }

        private static boolean canSend(Port p)    { return p.type() != PortType.INPUT; }
        private static boolean canReceive(Port p) { return p.type() != PortType.OUTPUT; }

        public static void disconnect(Level level, PortRef ref){
            ConnectableBlockEntity be = find(level, ref);
            if (be == null) return;
            Port p = portById(be.getPorts(), ref.id());
            if (p == null || p.type() == PortType.OUTPUT) return;
            if (be.getLinks().remove(ref.id())) be.onLinksChanged();
        }

        private static ConnectableBlockEntity find(Level level, PortRef ref) {
            if (!level.hasChunkAt(ref.pos())) return null;
            return level.getBlockEntity(ref.pos()) instanceof ConnectableBlockEntity c ? c : null;
        }


    }

    public static Port portById(List<Port> ports, int id)
    {
        for (Port p : ports) if (p.id() == id) return p;
        return null;
    }
}
