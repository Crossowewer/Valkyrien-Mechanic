package com.crossower.valkyrien_mechanic.block;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.sql.ClientInfoStatus;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PortBlockEntity extends BlockEntity implements ConnectionSystem.ConnectableBlockEntity
{
    private final ConnectionSystem.PortLinks links = new ConnectionSystem.PortLinks();
    private final Map<Integer, Float> lastInputs = new HashMap<>();

    protected PortBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState)
    {
        super(type, pos, blockState);
    }

    @Override
    public List<ConnectionSystem.Port> getPorts()
    {
        return List.of();
    }

    @Override
    public ConnectionSystem.PortLinks getLinks()
    {
        return links;
    }

    @Override
    public void onLinksChanged()
    {
        setChanged();
        if (level != null)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override protected void saveAdditional(CompoundTag tag) {super.saveAdditional(tag); links.save(tag);}
    @Override public void load(CompoundTag tag) {super.load(tag); links.load(tag);}

    @Override public CompoundTag getUpdateTag() { CompoundTag tag = super.getUpdateTag(); links.save(tag); return tag; }

    @Override public Packet<ClientGamePacketListener> getUpdatePacket()
    {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public float getOutput(int portId)
    {
        return 0f;
    }

    protected void onInputChanged(int portId, float value)
    {

    }

    public void serverTick()
    {
        if (level == null) return;
        for (ConnectionSystem.Port port : getPorts())
        {
            if (port.type() != ConnectionSystem.PortType.INPUT) continue;
            float value = readInput(port.id());
            if (value != lastInputs.getOrDefault(port.id(), 0f)) {
                lastInputs.put(port.id(), value);
                onInputChanged(port.id(), value);
            }
        }
    }

    private float readInput(int portId) {
        ConnectionSystem.PortLinks.Link link = getLinks().all().get(portId);
        if (link == null) return 0f;
        BlockPos srcPos = worldPosition.offset(link.offset());
        if (!level.hasChunkAt(srcPos)) return lastInputs.getOrDefault(portId, 0f);
        if (level.getBlockEntity(srcPos) instanceof ConnectionSystem.ConnectableBlockEntity src) {
            return src.getOutput(link.sourcePort());
        }
        return 0f;
    }

    public static <T extends BlockEntity>BlockEntityTicker<T> serverTicker(Level level) {
        if (level.isClientSide) return null;
        return (l, pos, state, be) -> {
            if (be instanceof PortBlockEntity p) {
                p.serverTick();
            }
        };
    }

}
