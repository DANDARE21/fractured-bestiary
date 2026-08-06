package net.dandare21.fracturedutils.ping;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public class HudPing {
    private final String id;
    private final String label;
    private final double x;
    private final double y;
    private final double z;
    private final String dimension;
    private final int color;
    private final String icon;
    private final String creator;

    public HudPing(String id, String label, double x, double y, double z, String dimension, int color, String icon, String creator) {
        this.id = id;
        this.label = label != null ? label : id;
        this.x = x;
        this.y = y;
        this.z = z;
        this.dimension = dimension != null ? dimension : "minecraft:overworld";
        this.color = color != 0 ? color : 0xFF00E5FF; // Default vibrant cyan
        this.icon = icon != null ? icon : "default";
        this.creator = creator != null ? creator : "Server";
    }

    public HudPing(FriendlyByteBuf buf) {
        this.id = buf.readUtf();
        this.label = buf.readUtf();
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
        this.dimension = buf.readUtf();
        this.color = buf.readInt();
        this.icon = buf.readUtf();
        this.creator = buf.readUtf();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.id);
        buf.writeUtf(this.label);
        buf.writeDouble(this.x);
        buf.writeDouble(this.y);
        buf.writeDouble(this.z);
        buf.writeUtf(this.dimension);
        buf.writeInt(this.color);
        buf.writeUtf(this.icon);
        buf.writeUtf(this.creator);
    }

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id);
        tag.putString("label", label);
        tag.putDouble("x", x);
        tag.putDouble("y", y);
        tag.putDouble("z", z);
        tag.putString("dimension", dimension);
        tag.putInt("color", color);
        tag.putString("icon", icon);
        tag.putString("creator", creator);
        return tag;
    }

    public static HudPing fromNBT(CompoundTag tag) {
        return new HudPing(
                tag.getString("id"),
                tag.getString("label"),
                tag.getDouble("x"),
                tag.getDouble("y"),
                tag.getDouble("z"),
                tag.getString("dimension"),
                tag.getInt("color"),
                tag.getString("icon"),
                tag.getString("creator")
        );
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public String getDimension() {
        return dimension;
    }

    public int getColor() {
        return color;
    }

    public String getIcon() {
        return icon;
    }

    public String getCreator() {
        return creator;
    }
}
