package com.crossower.valkyrien_mechanic.block;

import com.crossower.valkyrien_mechanic.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConnectionSystem
{
    public enum PortType {
        INPUT(0xFF3296FF),
        OUTPUT(0xFFEA297A);

        public final int color;

        PortType(int color) {this.color = color;}
    }

    public enum SignalType {
        LOGIC,
        NUMBER;

        public boolean canFeed(SignalType input) {
            return this == input || (this == LOGIC && input == NUMBER);
        }
    }

    public record Port(int id, PortType type, SignalType signal, float x, float y, float z) {

    }

    public interface ConnectableBlockEntity {
        List<Port> getPorts();

        PortLinks getLinks();

        float getOutput(int portId);

        void onLinksChanged();
    }

    public record PortRef(BlockPos pos, int id) {

    }

    public static class PortLinks {
        public record Link(BlockPos offset, int sourcePort)
        {
        }

        private final Map<Integer, Link> links = new HashMap<>();

        public Map<Integer, Link> all() {return links;}
        public void set(int inputPort, BlockPos offset, int sourcePort) {links.put(inputPort, new Link(offset, sourcePort));}
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
                BlockPos o = entry.getValue().offset;
                t.putIntArray("off", new int[]{o.getX(), o.getY(), o.getZ()});
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
                if (o.length == 3) {
                    links.put(t.getInt("in"), new Link(new BlockPos(o[0], o[1], o[2]), t.getInt("src")));
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

        public static final int MAX_LINK_DISTANCE = 32;

        public static void connect(Level level, PortRef a, PortRef b)
        {
            if (a.pos().equals(b.pos())) return;
            if (a.pos().distSqr(b.pos()) > MAX_LINK_DISTANCE * MAX_LINK_DISTANCE) return;

            ConnectableBlockEntity ca = find(level, a), cb = find(level, b);
            if (ca == null || cb == null) return;
            Port pa = portById(ca.getPorts(), a.id()), pb = portById(cb.getPorts(), b.id());
            if (pa == null ||  pb == null || pa.type() == pb.type()) return;

            boolean aIsInput = pa.type() == PortType.INPUT;

            Port inPort = aIsInput ? pa : pb;
            Port outPort = aIsInput ? pb : pa;
            if (!outPort.signal().canFeed(inPort.signal())) return;

            PortRef in = aIsInput ? a : b, out = aIsInput ? b : a;
            ConnectableBlockEntity inBe = aIsInput ? ca : cb;

            BlockPos offset = out.pos().subtract(in.pos());
            ConnectionSystem.PortLinks.Link existing = inBe.getLinks().all().get(in.id());

            if (existing != null && existing.offset().equals(offset) && existing.sourcePort() == out.id())
                inBe.getLinks().remove(in.id());
            else
                inBe.getLinks().set(in.id(), offset, out.id());
            inBe.onLinksChanged();
        }

        public static void disconnect(Level level, PortRef ref){
            ConnectableBlockEntity be = find(level, ref);
            if (be == null) return;
            Port p = portById(be.getPorts(), ref.id());
            if (p == null || p.type() != PortType.INPUT) return;
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
