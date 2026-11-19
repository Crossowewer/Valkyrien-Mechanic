package com.crossower.valkyrien_mechanic.block;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class PortBlockEntity extends BlockEntity implements ConnectionSystem.ConnectableBlockEntity
{
    private final ConnectionSystem.PortLinks links = new ConnectionSystem.PortLinks();
    private UUID uid = UUID.randomUUID();
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

    @Override protected void saveAdditional(CompoundTag tag)
    {
        super.saveAdditional(tag);
        links.save(tag);
        tag.putUUID("uid", uid);
        tag.putIntArray("lost", lostPorts.stream().mapToInt(Integer::intValue).toArray());
    }

    @Override public void load(CompoundTag tag)
    {
        super.load(tag);
        links.load(tag);
        if (tag.hasUUID("uid")) uid = tag.getUUID("uid");
        for (int p : tag.getIntArray("lost")) lostPorts.add(p);
    }

    @Override
    public UUID getUid() {return uid;}

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

    private void validateLinks()
    {
        if (level == null) return;
        boolean changed = false;
        for (Integer inPort : new ArrayList<>(getLinks().all().keySet()))
        {
            ConnectionSystem.PortLinks.Link link = getLinks().all().get(inPort);
            BlockPos srcPos = link.resolve(worldPosition);
            if (!level.hasChunkAt(srcPos)) continue;
            boolean valid = level.getBlockEntity(srcPos) instanceof ConnectionSystem.ConnectableBlockEntity src && src.getUid().equals(link.sourceUid());
            if (valid) {
                lostPorts.remove(inPort);
            }
            else {
                getLinks().remove(inPort);
                lostPorts.add(inPort);
                lastInputs.put(inPort, 0f);
                onInputChanged(inPort, 0f);
                changed = true;
            }
        }
        if (changed) onLinksChanged();
    }

    public void serverTick()
    {
        if (level == null) return;
        validateLinks();
        for (ConnectionSystem.Port port : getPorts())
        {
            if (port.type() == ConnectionSystem.PortType.OUTPUT) continue;
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
        BlockPos srcPos = link.resolve(worldPosition);
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

    @Override
    public void onPortUse(int portId, ServerPlayer player) {
        if (getLinks().remove(portId)) onLinksChanged();
    }

    @Nullable
    protected ConnectionSystem.ConnectableBlockEntity getSource(int inputPortID)
    {
        ConnectionSystem.PortLinks.Link link = getLinks().all().get(inputPortID);
        if (link == null || level == null) return null;
        BlockPos srcPos = link.resolve(worldPosition);
        if (!level.hasChunkAt(srcPos)) return null;
        return level.getBlockEntity(srcPos) instanceof  ConnectionSystem.ConnectableBlockEntity con ? con : null;
    }

    //Losing ports fix (thx bricky)
    private final Set<Integer> lostPorts = new HashSet<>();

    protected boolean isSignalControlled(int portId) {
        return getLinks().all().containsKey(portId) || lostPorts.contains(portId);
    }
}
