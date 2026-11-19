package com.crossower.valkyrien_mechanic.block.entity;
import com.crossower.valkyrien_mechanic.block.ConnectionSystem;
import com.crossower.valkyrien_mechanic.block.PortBlockEntity;
import com.crossower.valkyrien_mechanic.block.custom.BearingBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.*;
import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.core.api.world.PhysLevel;
import org.valkyrienskies.core.impl.program.VSCoreClientImpl;
import org.valkyrienskies.core.internal.ShipTeleportData;
import org.valkyrienskies.core.internal.joints.VSJoint;
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

import java.lang.Math;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import static com.crossower.valkyrien_mechanic.block.custom.BearingBlock.getHeadDirection;
//TODO: I hate this, need to make it at least a bit organized
public class BearingBlockEntity extends PortBlockEntity implements BlockEntityPhysicsListener
{
    private volatile long headShipID = -1;
    private volatile int jointID = -1;

    private VSRevoluteJoint pendingJoint = null;
    private long jointSendAt = -1;

    private BlockPos headBlock = null;
    private int jointgen = 0;

    @Override
    public List<ConnectionSystem.Port> getPorts() {
        BlockState state = this.getBlockState();
        AttachFace face = state.getValue(BearingBlock.FACE);
        Direction dir = state.getValue(BearingBlock.FACING);

        return List.of(ConnectionSystem.Port.input(
                0,
                ConnectionSystem.SignalType.BEARING,
                Set.of(ConnectionSystem.SignalType.MOTOR, ConnectionSystem.SignalType.CONTROL_SEAT, ConnectionSystem.SignalType.CONTROLLER),
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

    private static final double TORQUE_SCALE = 7.5;
    private static final double SOFT_ZONE = 1.0;
    private static final double RESPONSE_TIME = 0.25;
    private static final double BRAKE_MAX_ACCEL = 150.0;

    private static final double STEER_OMEGA_N = 12.0;
    private static final double STEER_DAMPING = 1.0;
    private static final double STEER_ACCEL_MAX = 25.0;
    private static final double HOLD_DEADBAND = 0.04;

    private static final double STEER_TORQUE_PER_MASS = 2000.0;
    private static final double BREAKAWAY_PER_MASS = 800.0;
    private static final double BREAKAWAY_OMEGA = 0.05;

    @Override
    public void serverTick()
    {
        super.serverTick();
        if (!(level instanceof ServerLevel serverLevel)) return;

        BlockPos support = worldPosition.relative(getHeadDirection(getBlockState()).getOpposite());
        if (level.hasChunkAt(support) && level.getBlockState(support).isAir())
        {
            level.destroyBlock(worldPosition, true);
            return;
        }

        if (headBlock != null && level.hasChunkAt(headBlock) && level.getBlockState(headBlock).isAir())
        {
            removeJoint();
            return;
        }

        BlockPos headPos = worldPosition.relative(getHeadDirection(getBlockState()));
        if (level.getGameTime() % 100 == 0)
        {
            Ship base = VSGameUtilsKt.getShipManagingPos(level, worldPosition);
        }

        if (headShipID == -1)
        {
            if (!level.getBlockState(headPos).isAir()) createHead(headPos);
        }
        else if (find(serverLevel, headShipID) == null)
        {
            removeJoint();
        }

        if (pendingJoint != null && level.getGameTime() >= jointSendAt)
        {
            VSRevoluteJoint joint = pendingJoint;
            pendingJoint = null;
            GameToPhysicsAdapter gtpa = ValkyrienSkiesMod.getOrCreateGTPA(VSGameUtilsKt.getDimensionId(level));
            final int gen = jointgen;
            gtpa.addJoint(joint, 2, (id) ->
            {
                if (gen != jointgen) return;
                jointID = id;
            });
        }
        updateDrive();
    }

    private void updateDrive()
    {
        Direction dir = getHeadDirection(getBlockState());
        Vector3d axisB = new Vector3d(dir.getStepX(), dir.getStepY(), dir.getStepZ());
        axisInBearing = axisB;

        float torque = 0, maxSpeed = 0, angle = Float.NaN;
        boolean braking = false;
        int engineDir = 0;
        if (getSource(0) instanceof ConnectionSystem.RotationSource r)
        {
            torque = r.getTorque();
            maxSpeed = r.getMaxSpeed();
            engineDir = r.getDirection();
            angle = r.getTargetAngle() * bearingDirection;
            braking = torque <= 0;
        }
        //else if (isSignalControlled(0)) Drive;
        float direction = engineDir * bearingDirection;
        Drive next = new Drive(torque, maxSpeed, direction, angle, braking);
        if (!next.equals(drive)) drive = next;

    }

    private int dbgTick = 0;

    @Override
    public void physTick(@Nullable PhysShip physShip, @NotNull PhysLevel physLevel)
    {
        try
        {
            long headId = headShipID;
            Drive d = drive;
            if (physShip == null || headShipID == -1) return;
            if (!d.braking() && d.torque() <= 0) return;

            PhysShip head = physLevel.getShipById(headId);
            if (head == null) return;

            Vector3d axisH = new Vector3d(axisInHead);

            Vector3d wHead = new Vector3d(head.getAngularVelocity());
            Vector3d wBase = physShip != null ? new Vector3d(physShip.getAngularVelocity()) : new Vector3d(0, 0, 0);
            Vector3d axisW = physShip != null ? physShip.getTransform().getRotation().transform(new Vector3d(axisInBearing)) :  new Vector3d(axisInBearing);
            double omega = new Vector3d(wHead).sub(wBase).dot(axisW);
            double inertia = axisInertia(head, axisH);
            double tau;
            if (d.braking())
            {
                double accel = Mth.clamp(-omega / RESPONSE_TIME, -BRAKE_MAX_ACCEL, BRAKE_MAX_ACCEL);
                tau = accel * inertia;
            }
            else if (Double.isNaN(d.angle()))
            {
                double target = d.direction() * d.maxSpeed() * 2 * Math.PI / 60.0;
                tau = Mth.clamp((target - omega) / SOFT_ZONE, -1.0, 1.0) * d.torque() * TORQUE_SCALE;
            }
            else
            {
                double err = Math.toRadians(d.angle()) - currentSteerAngle(physShip, head);
                double accel = STEER_OMEGA_N * STEER_OMEGA_N * err - 2 * STEER_DAMPING * STEER_OMEGA_N * omega;
                double budget = STEER_TORQUE_PER_MASS * head.getMass();
                tau = Mth.clamp(accel * inertia, -budget, budget);
                if (Math.abs(omega) < BREAKAWAY_OMEGA && Math.abs(err) > HOLD_DEADBAND)
                    tau = Math.copySign(Math.max(Math.abs(tau), BREAKAWAY_PER_MASS * head.getMass()), err);
            }

            head.applyModelTorque(new Vector3d(axisH).mul(tau));
            if (physShip != null) physShip.applyModelTorque(new Vector3d(axisInBearing).mul(-tau));
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
    }
    private double axisInertia(PhysShip head, Vector3d axisH)
    {
        Vector3d v =  head.getMomentOfInertia().transform(new Vector3d(axisH));
        return v.dot(axisH);
    }

    @Override
    public void onPortUse(int portId, ServerPlayer player)
    {
        bearingDirection = -bearingDirection;
        setChanged();
        player.displayClientMessage(Component.literal("Bearing Direction : " + (bearingDirection > 0 ? "counter clockwise" : "clockwise")), true);

    }

    @Override
    protected void saveAdditional(CompoundTag tag)
    {
        super.saveAdditional(tag);
        tag.putLong("headShipID", headShipID);
        tag.putInt("hingeID", jointID);
        tag.putInt("bearingDirection", bearingDirection);
        tag.putDouble("restX", restRelative.x);
        tag.putDouble("restY", restRelative.y);
        tag.putDouble("restZ", restRelative.z);
        tag.putDouble("restW", restRelative.w);
        tag.putDouble("ahX", axisInHead.x);
        tag.putDouble("ahY", axisInHead.y);
        tag.putDouble("ahZ", axisInHead.z);
        if (headBlock != null) tag.putLong("headBlock", headBlock.asLong());
    }

    @Override
    public void load(CompoundTag tag)
    {
        super.load(tag);
        headShipID = tag.contains("headShipID") ? tag.getLong("headShipID") : -1;
        jointID = tag.contains("hingeID") ? tag.getInt("hingeID") : -1;
        bearingDirection = tag.contains("bearingDirection") ? tag.getInt("bearingDirection") : 1;
        if (tag.contains("restX")) {
            restRelative = new Quaterniond(tag.getDouble("restX"), tag.getDouble("restY"),
                    tag.getDouble("restZ"), tag.getDouble("restW"));
        }
        if (tag.contains("ahX")) {
            axisInHead = new Vector3d(tag.getDouble("ahX"), tag.getDouble("ahY"), tag.getDouble("ahZ"));
        }
        headBlock = tag.contains("headBlock") ? BlockPos.of(tag.getLong("headBlock")) : null;
    }

    @Override
    public void setDimension(@NotNull String s)
    {

    }

    private volatile Quaterniond restRelative = new Quaterniond();
    private volatile boolean restCalibrated =  false;
    private boolean restPersisted = false;

    private double currentSteerAngle(PhysShip base, PhysShip head)
    {
        Quaterniond baseRot = base != null ? (Quaterniond)base.getTransform().getRotation() : new Quaterniond();
        Quaterniond rel = new Quaterniond(baseRot).invert().mul(head.getTransform().getRotation());
        Quaterniond delta = new Quaterniond(restRelative).invert().mul(rel);
        double s = delta.x * axisInBearing.x + delta.y * axisInBearing.y + delta.z * axisInBearing.z;
        return 2 * Math.atan2(s, delta.w);
    }

    //ENGINE STUFF
    private record Drive(float torque, float maxSpeed, float direction, float angle, boolean braking) {

    }
    private volatile Drive drive = new Drive(0f, 0f, 0f, Float.NaN, false);
    private int bearingDirection = 1;
    private volatile Vector3d axisInBearing = new Vector3d(0, 1, 0);
    private volatile Vector3d axisInHead = new Vector3d(0, 1, 0);
    private volatile Vector3d axisWorld = new Vector3d(0, 1, 0);


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

        if (bearingShip == null) {
            System.out.println("Bearing ship: NULL (static world)");
        } else {
            System.out.println("Bearing ship ID: " + bearingShip.getId());
            System.out.println("Bearing ship position: " +
                    bearingShip.getTransform().getPositionInWorld());
        }

        ServerShip headShip = assemble((ServerLevel) level, headPos);
        if (headShip == null) return;

        headShipID = headShip.getId();
        Vector3dc c = headShip.getTransform().getPositionInShip();
        headBlock = new BlockPos(Mth.floor(c.x()), Mth.floor(c.y()), Mth.floor(c.z()));
        createJoint(level, bearingShip, headShip);
        setChanged();
    }

    private void createJoint(Level level, @Nullable Ship bearingShip, Ship headShip)
    {
        Direction dir = getHeadDirection(getBlockState());
        Vector3d axis = new Vector3d(dir.getStepX(), dir.getStepY(), dir.getStepZ());

        Vector3d center = VectorConversionsMCKt.toJOML(worldPosition.getCenter());
        Vector3d boundary = new Vector3d(center).fma(0.5, axis);
        Vector3d pivot0 = new Vector3d(center).fma(-0.5, axis);

        Vector3d boundaryWorld;
        Vector3d pivot0World;

        if (bearingShip != null)
        {
            boundaryWorld = bearingShip.getTransform().getShipToWorld().transformPosition(new Vector3d(boundary));
            pivot0World = bearingShip.getTransform().getShipToWorld().transformPosition(new Vector3d(pivot0));
        } else {
            boundaryWorld = new Vector3d(boundary);
            pivot0World = new Vector3d(pivot0);
        }

        Vector3d pivot1 = headShip.getTransform().getWorldToShip().transformPosition(new Vector3d(boundaryWorld));

        Vector3d shift = new Vector3d(pivot0World).sub(boundaryWorld);
        teleportHead(headShip, shift);

        Quaterniond frame0 = new Quaterniond().rotationTo(new Vector3d(1, 0, 0), axis);

        Quaterniond baseRot = bearingShip != null ? new Quaterniond(bearingShip.getTransform().getRotation()) : new Quaterniond();

        Quaterniond frame1 = new Quaterniond(headShip.getTransform().getRotation()).invert().mul(baseRot).mul(frame0);
        restRelative = new Quaterniond(frame0).mul(new Quaterniond(frame1).invert());
        axisInHead = new Vector3d(1, 0, 0).rotate(frame1);

        Long bearingShipId = bearingShip != null ? bearingShip.getId() : null;
        //if (bearingShipId == -1L) System.out.println("Bearing ship equeals to null");

        VSRevoluteJoint joint = (VSRevoluteJoint) new VSRevoluteJoint(bearingShipId, new VSJointPose(pivot0, frame0),
                headShip.getId(), new VSJointPose(pivot1, frame1), null,
                1e-25, null, null, null,
                null, true).serialized();

        pendingJoint = joint;
        jointSendAt = level.getGameTime() + 3;
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

    public void removeJoint()
    {
        if (!(level instanceof ServerLevel serverLevel)) return;
        pendingJoint = null;
        jointgen++;
        if (jointID != -1) {
            GameToPhysicsAdapter gtpa = ValkyrienSkiesMod.getOrCreateGTPA(VSGameUtilsKt.getDimensionId(level));
            gtpa.removeJoint(jointID);
        }

        jointID = -1;
        headShipID = -1;
        headBlock = null;
        setChanged();
    }
}
