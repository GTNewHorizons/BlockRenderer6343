package blockrenderer6343.client.world;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.stats.StatBase;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.DamageSource;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;

import com.mojang.authlib.GameProfile;

import blockrenderer6343.integration.nei.GuiMultiblockHandler;

public class ClientFakePlayer extends EntityPlayer {

    private static final String CHANNEL_KEY = "structurelib.autoplace.warning.no_explicit_channel";

    public ClientFakePlayer(World world, GameProfile name) {
        super(world, name);
    }

    @Override
    public void addChatMessage(IChatComponent message) {}

    @Override
    public boolean canCommandSenderUseCommand(int i, String s) {
        return false;
    }

    @Override
    public ChunkCoordinates getPlayerCoordinates() {
        return new ChunkCoordinates(0, 0, 0);
    }

    @Override
    public void addChatComponentMessage(IChatComponent message) {
        if (message instanceof ChatComponentTranslation msg && msg.getKey().equals(CHANNEL_KEY)) {
            GuiMultiblockHandler.channels.add(msg.getFormatArgs()[0].toString());
        }
    }

    @Override
    public void addStat(StatBase par1StatBase, int par2) {}

    @Override
    public void openGui(Object mod, int modGuiId, World world, int x, int y, int z) {}

    @Override
    public boolean isEntityInvulnerable() {
        return true;
    }

    @Override
    public boolean canAttackPlayer(EntityPlayer player) {
        return false;
    }

    @Override
    public void onDeath(DamageSource source) {
        return;
    }

    @Override
    public void onUpdate() {
        return;
    }

    @Override
    public void travelToDimension(int dim) {
        return;
    }
}
