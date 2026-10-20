package com.crossower.valkyrien_mechanic.block.entity;
import com.crossower.valkyrien_mechanic.block.ConnectionSystem;
import com.crossower.valkyrien_mechanic.block.PortBlockEntity;
import com.crossower.valkyrien_mechanic.block.custom.BearingBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.core.api.world.PhysLevel;
import org.valkyrienskies.core.impl.program.VSCoreClientImpl;
import org.valkyrienskies.core.internal.ShipTeleportData;
import org.valkyrienskies.core.internal.joints.VSJointPose;
import org.valkyrienskies.core.internal.joints.VSRevoluteJoint;
import org.valkyrienskies.core.internal.world.VsiServerShipWorld;
import org.valkyrienskies.mod.api.BlockEntityPhysicsListener;
import org.valkyrienskies.mod.api.ValkyrienSkies;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.ValkyrienSkiesMod;
import org.valkyrienskies.mod.common.assembly.ShipAssembler;
import org.valkyrienskies.mod.common.util.GameToPhysicsAdapter;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import static com.crossower.valkyrien_mechanic.block.custom.BearingBlock.getHeadDirection;

public class BearingBlockEntity extends PortBlockEntity implements BlockEntityPhysicsListener
{
    private volatile long headShipID = -1;
    private volatile int jointID = -1;
    private volatile float targetSpeed = 0f;

    @Override
    public List<ConnectionSystem.Port> getPorts() {
        BlockState state = this.getBlockState();
        AttachFace face = state.getValue(BearingBlock.FACE);
        Direction dir = state.getValue(BearingBlock.FACING);

        return List.of(new ConnectionSystem.Port(
                0,
                ConnectionSystem.PortType.INPUT,
                ConnectionSystem.SignalType.NUMBER,
                getX(face, dir),
                getY(face, dir),
                getZ(face, dir)
        ));
    }

    @Override
    public @NotNull String getDimension()
    {
        return "minecraft:dimension:minecraft:overworld";
    }

    public BearingBlockEntity(BlockPos pos, BlockState state)
    {
        super(ModBlockEntities.BEARING_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public void serverTick()
    {
        super.serverTick();
        if (!(level instanceof ServerLevel serverLevel)) return;
        BlockPos headPos = worldPosition.relative(getHeadDirection(getBlockState()));

        if (headShipID == -1) {
           if (!level.getBlockState(headPos).isAir()) createHead(headPos);
        } else if (find(serverLevel, headShipID) == null) {
            headShipID = -1;
            jointID = -1;
            setChanged();
        }
    }

    @Override
    protected void onInputChanged(int portID, float value) {
        targetSpeed = value;
    }

    @Override
    public void physTick(@Nullable PhysShip physShip, @NotNull PhysLevel physLevel)
    {
        if (physShip == null || headShipID == -1) return;
        float speed = targetSpeed;

    }

    @Override
    protected void saveAdditional(CompoundTag tag){
        super.saveAdditional(tag);
        tag.putLong("headShipID", headShipID);
        tag.putInt("hingeID", jointID);
    }

    @Override
    public void load(CompoundTag tag)
    {
        super.load(tag);
        headShipID = tag.contains("headShipID") ? tag.getLong("headShipID") : -1;
        jointID = tag.contains("hingeID") ? tag.getInt("hingeID") : -1;
    }

    @Override
    public void setDimension(@NotNull String s)
    {

    }



    //BLOCK ROTATION/POS STUFF

    protected final static float floor_x = 0.5f, floor_y = 0.1f, floor_z = 0.5f;
    protected final static float ceiling_x = 0.5f, ceiling_y = 0.9f, ceiling_z = 0.5f;
    protected final static float north_x = 0.5f, north_y = 0.5f, north_z = 0.9f;
    protected final static float south_x = 0.5f, south_y = 0.5f, south_z = 0.1f;
    protected final static float east_x = 0.1f, east_y = 0.5f, east_z = 0.5f;
    protected final static float west_x = 0.9f, west_y = 0.5f, west_z = 0.5f;

    private static float getX(AttachFace face, Direction dir){
        if (face == AttachFace.CEILING) return ceiling_x;
        else if (face == AttachFace.FLOOR) return floor_x;
        else if (face == AttachFace.WALL){
            switch (dir){
                case WEST: return west_x;
                case EAST: return east_x;
                case SOUTH: return south_x;
                case NORTH: return north_x;
                default: return 0.1f;
            }
        }
        return 0.1f;
    }
    private static float getY(AttachFace face, Direction dir){
        if (face == AttachFace.CEILING) return ceiling_y;
        else if (face == AttachFace.FLOOR) return floor_y;
        else if (face == AttachFace.WALL){
            switch (dir){
                case WEST: return west_y;
                case EAST: return east_y;
                case SOUTH: return south_y;
                case NORTH: return north_y;
                default: return 0.1f;
            }
        }
        return 0.1f;
    }
    private static float getZ(AttachFace face, Direction dir){
        if (face == AttachFace.CEILING) return ceiling_z;
        else if (face == AttachFace.FLOOR) return floor_z;
        else if (face == AttachFace.WALL){
            switch (dir){
                case WEST: return west_z;
                case EAST: return east_z;
                case SOUTH: return south_z;
                case NORTH: return north_z;
                default: return 0.1f;
            }
        }
        return 0.1f;
    }

    //VS STUFF (not to be confused with vstuff)



    public ServerShip assemble(ServerLevel level, BlockPos pos)
    {
        ServerShip s = ShipAssembler.assembleToShip(level, Set.of(pos), 1f);
        return s;
    }

    static @Nullable ServerShip find(ServerLevel level, long ShipID)
    {
        ServerShip s = VSGameUtilsKt.getShipObjectWorld(level).getAllShips().getById(ShipID);
        return s;
    }

    private int getCoord(String coordLetter, BlockPos pos)
    {
        Direction dir = getHeadDirection(getBlockState());
        switch (coordLetter)
        {
            case "X":
                switch (dir)
                {
                    case WEST:
                        return pos.getX() + 1;
                    case EAST:
                        return pos.getX() - 1;
                    default:
                        return pos.getX();
                }
            case "Y":
                switch (dir)
                {
                    case DOWN:
                        return pos.getY() + 1;
                    case UP:
                        return pos.getY() - 1;
                    default:
                        return pos.getY();
                }
            case "Z":
                switch (dir)
                {
                    case SOUTH:
                        return pos.getZ() - 1;
                    case NORTH:
                        return pos.getZ() + 1;
                    default:
                        return pos.getZ();
                }
                default: return pos.getY();
        }
    }

                private void createHead(BlockPos headPos)
    {
        Ship bearingShip = VSGameUtilsKt.getShipManagingPos(level, worldPosition);
        if (bearingShip == null) return;

        ServerShip headShip = assemble((ServerLevel) level, headPos);
        if (headShip == null) return;

        headShipID = headShip.getId();
        createJoint(level, bearingShip, headShip);
        setChanged();

        System.out.println("head ship: " + find(level.getServer().getLevel(level.dimension()), headShipID));
    }

    private void createJoint(Level level, Ship bearingShip, Ship headShip)
    {

        Direction dir = getHeadDirection(getBlockState());
        Vector3d axis = new Vector3d(dir.getStepX(), dir.getStepY(), dir.getStepZ());

        Vector3d center = VectorConversionsMCKt.toJOML(worldPosition.getCenter());
        Vector3d boundary = new Vector3d(center).fma(0.5, axis);
        Vector3d pivot0 = new Vector3d(center).fma(-0.5, axis);

        Vector3d boundaryWorld = bearingShip.getTransform().getShipToWorld().transformPosition(new Vector3d(boundary));
        Vector3d pivot0World = bearingShip.getTransform().getShipToWorld().transformPosition(new Vector3d(pivot0));

        Vector3d pivot1 = headShip.getTransform().getWorldToShip().transformPosition(new Vector3d(boundaryWorld));

        Vector3d shift = new Vector3d(pivot0World).sub(boundaryWorld);
        teleportHead(headShip, shift);

        Quaterniond frame0 = new Quaterniond().rotationTo(new Vector3d(1, 0, 0), axis);
        Quaterniond frame1 = new Quaterniond(headShip.getTransform().getRotation()).invert().mul(bearingShip.getTransform().getRotation()).mul(frame0);

        VSRevoluteJoint joint = new VSRevoluteJoint(bearingShip.getId(), new VSJointPose(pivot0, frame0), headShip.getId(), new VSJointPose(pivot1, frame1), null,
                1e-25,
                null, new VSRevoluteJoint.VSRevoluteDriveVelocity(1, true),
                null, null, true);

        GameToPhysicsAdapter gtpa = ValkyrienSkiesMod.getOrCreateGTPA(
                VSGameUtilsKt.getDimensionId(level)
        );

        gtpa.addJoint(joint, 0, (id) ->
        {
            jointID = id;
        });
    }

        private void teleportHead(Ship shipHead, Vector3d shift)
        {
            Vector3d sh = new Vector3d(shipHead.getTransform().getPositionInWorld()).add(shift);
            var teleportData = (ShipTeleportData) ValkyrienSkiesMod.getVsCore().newShipTeleportData(sh, shipHead.getTransform().getRotation(),
                    shipHead.getAngularVelocity(), shipHead.getOmega(), getDimension(), 1d,
                    shipHead.getTransform().getPositionInShip());
            VsiServerShipWorld ssw = (VsiServerShipWorld) ValkyrienSkies.getShipWorld(level.getServer().getLevel(level.dimension()));
            ssw.teleportShip( (ServerShip) shipHead, teleportData);
        }

    private void removeJoint()
    {
        if (!(level instanceof ServerLevel) || jointID == -1) return;
        GameToPhysicsAdapter gtpa = ValkyrienSkiesMod.getOrCreateGTPA(VSGameUtilsKt.getDimensionId(level));
        //
        jointID = -1;
        headShipID = -1;
    }
}
