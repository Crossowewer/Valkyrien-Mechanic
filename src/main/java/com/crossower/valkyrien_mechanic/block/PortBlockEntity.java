package com.crossower.valkyrien_mechanic.block;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.sql.ClientInfoStatus;
import java.util.List;

public class PortBlockEntity extends BlockEntity implements ConnectionSystem.ConnectableBlockEntity
{
    private final ConnectionSystem.PortLinks links = new ConnectionSystem.PortLinks();

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

}
