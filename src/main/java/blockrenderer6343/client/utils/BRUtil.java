package blockrenderer6343.client.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.StatCollector;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import com.github.vfyjxf.nee.network.NEENetworkHandler;
import com.github.vfyjxf.nee.network.packet.PacketNEIPatternRecipe;
import com.gtnewhorizon.gtnhlib.blockpos.BlockPos;
import com.gtnewhorizon.gtnhlib.util.CoordinatePacker;
import com.gtnewhorizon.structurelib.StructureLib;
import com.gtnewhorizon.structurelib.alignment.constructable.ConstructableUtility;
import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;
import com.gtnewhorizon.structurelib.item.ItemConstructableTrigger;
import com.gtnewhorizon.structurelib.structure.AutoPlaceEnvironment;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.mojang.authlib.GameProfile;

import blockrenderer6343.BlockRenderer6343;
import blockrenderer6343.api.utils.CreativeItemSource;
import blockrenderer6343.client.renderer.WorldSceneRenderer;
import blockrenderer6343.client.world.ClientFakePlayer;
import blockrenderer6343.client.world.DummyWorld;
import codechicken.lib.math.MathHelper;
import codechicken.nei.NEIClientUtils;
import codechicken.nei.recipe.GuiRecipe;

public class BRUtil {

    public static final ClientFakePlayer FAKE_PLAYER = new ClientFakePlayer(
            DummyWorld.INSTANCE,
            new GameProfile(UUID.randomUUID(), "MultiblockBuilder"));
    public static final AutoPlaceEnvironment AUTO_PLACE_ENVIRONMENT = AutoPlaceEnvironment
            .fromLegacy(CreativeItemSource.instance, FAKE_PLAYER, a -> {});

    public static AutoPlaceEnvironment getBuildEnvironment() {
        return AUTO_PLACE_ENVIRONMENT;
    }

    public static void projectMultiblock(ItemStack buildStack, ItemStack multiStack, int blocksBelowController) {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        World baseWorld = Minecraft.getMinecraft().theWorld;
        Vec3 lookVec = player.getLookVec();
        MovingObjectPosition lookingPos = player.rayTrace(10, 1);
        int blockX, blockY, blockZ;
        if (lookingPos == null || lookingPos.typeOfHit == MovingObjectPosition.MovingObjectType.MISS) {
            blockX = MathHelper.floor_double(player.posX + lookVec.xCoord * 2);
            blockZ = MathHelper.floor_double(player.posZ + lookVec.zCoord * 2);
            blockY = baseWorld.getPrecipitationHeight(blockX, blockZ) + blocksBelowController;
        } else {
            blockX = lookingPos.blockX;
            blockY = lookingPos.blockY + blocksBelowController + 1;
            blockZ = lookingPos.blockZ;
        }
        ItemStack copy = multiStack.copy();

        if (!baseWorld.isAirBlock(blockX, blockY, blockZ)) {
            player.addChatMessage(new ChatComponentText(StatCollector.translateToLocal("blockrenderer6343.no_space")));
            return;
        }

        // noinspection ConstantConditions
        if (!copy.getItem().onItemUse(copy, player, baseWorld, blockX, blockY, blockZ, 0, blockX, blockY - 1, blockZ)) {
            player.addChatMessage(new ChatComponentText(StatCollector.translateToLocal("blockrenderer6343.no_block")));
            return;
        }
        ConstructableUtility.handle(buildStack, player, baseWorld, blockX, blockY, blockZ, 0);
        baseWorld.setBlockToAir(blockX, blockY, blockZ);
        baseWorld.removeTileEntity(blockX, blockY, blockZ);
    }

    public static void neiOverlay(WorldSceneRenderer renderer) {
        if (!BlockRenderer6343.isNEELoaded) return;
        NBTTagCompound recipeInputs = new NBTTagCompound();
        GuiRecipe<?> currentScreen = (GuiRecipe<?>) Minecraft.getMinecraft().currentScreen;
        Minecraft.getMinecraft().displayGuiScreen(currentScreen.firstGui);
        List<ItemStack> ingredients = getIngredients(renderer);
        for (int i = 0; i < ingredients.size(); i++) {
            ItemStack itemStack = ingredients.get(i);
            if (itemStack != null) {
                NBTTagCompound itemStackNBT = new NBTTagCompound();
                itemStack.writeToNBT(itemStackNBT);
                itemStackNBT.setInteger("Count", itemStack.stackSize);
                recipeInputs.setTag("#" + i, itemStackNBT);
            }
        }
        NEENetworkHandler.getInstance().sendToServer(new PacketNEIPatternRecipe(recipeInputs, new NBTTagCompound()));
    }

    public static void copyToHologram(ItemStack triggerStack) {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        ItemStack stack = player.getHeldItem();
        if (stack == null || !(stack.getItem() instanceof ItemConstructableTrigger)) {
            player.addChatMessage(
                    new ChatComponentText(StatCollector.translateToLocal("blockrenderer6343.no_projector")));
        } else {
            StructureLib.instance().proxy().uploadChannels(triggerStack);
        }
    }

    public static List<ItemStack> getIngredients(WorldSceneRenderer renderer) {
        List<ItemStack> ingredients = new ArrayList<>();
        for (long renderedBlock : renderer.renderedBlocks) {
            int x = CoordinatePacker.unpackX(renderedBlock);
            int y = CoordinatePacker.unpackY(renderedBlock);
            int z = CoordinatePacker.unpackZ(renderedBlock);
            Block block = renderer.world.getBlock(x, y, z);
            if (block.equals(Blocks.air)) continue;
            int meta = renderer.world.getBlockMetadata(x, y, z);
            int qty = block.quantityDropped(renderer.world.rand);
            ArrayList<ItemStack> itemStacks = new ArrayList<>();
            if (qty != 1) {
                itemStacks.add(new ItemStack(block));
            } else {
                itemStacks = block.getDrops(renderer.world, x, y, z, meta, 0);
            }
            boolean added = false;
            for (ItemStack ingredient : ingredients) {
                if (NEIClientUtils.areStacksSameTypeWithNBT(ingredient, itemStacks.get(0))) {
                    ingredient.stackSize++;
                    added = true;
                    break;
                }
            }
            if (!added) ingredients.add(itemStacks.get(0));
        }

        return ingredients;
    }

    public static List<List<ItemStack>> scanCandidates(IConstructable multi,
            @Nullable IStructureElement<IConstructable> element, ItemStack buildStack, BlockPos block) {
        if (element == null) return Collections.emptyList();
        IStructureElement.BlocksToPlace blocksToPlace = element.getBlocksToPlace(
                multi,
                DummyWorld.INSTANCE,
                block.x,
                block.y,
                block.z,
                buildStack,
                getBuildEnvironment());
        if (blocksToPlace == null) return Collections.emptyList();

        Set<ItemStack> rawCandidates = CreativeItemSource.instance
                .takeEverythingMatches(blocksToPlace.getPredicate(), false, 0).keySet();
        List<List<ItemStack>> stackedCandidates = new ArrayList<>();
        for (ItemStack rawCandidate : rawCandidates) {
            boolean added = false;
            for (List<ItemStack> stackedCandidate : stackedCandidates) {
                List<String> firstCandidateTooltip = stackedCandidate.get(0).getTooltip(FAKE_PLAYER, false);
                List<String> rawCandidateTooltip = rawCandidate.getTooltip(FAKE_PLAYER, false);
                if (firstCandidateTooltip.size() > 1 && rawCandidateTooltip.size() > 1
                        && firstCandidateTooltip.get(1).equals(rawCandidateTooltip.get(1))) {
                    stackedCandidate.add(rawCandidate);
                    added = true;
                    break;
                }
            }
            if (!added) {
                List<ItemStack> newStackedCandidate = new ArrayList<>();
                newStackedCandidate.add(rawCandidate);
                stackedCandidates.add(newStackedCandidate);
            }
        }

        return stackedCandidates;
    }

    public static long hashStack(ItemStack stack) {
        // noinspection ConstantConditions
        return stack.getItem().hashCode() * 31L + stack.getItemDamage() * 31L;
    }

    public static void drawCenteredScaledString(String text, double textX, double textY, int fontColor,
            double fontScale) {
        drawScaledString(text, textX, textY, fontColor, true, true, fontScale);
    }

    public static void drawScaledString(String text, double textX, double textY, int fontColor, boolean centered,
            boolean fontShadow, double fontScale) {
        final FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;

        GL11.glPushMatrix();

        if (fontScale != 1.0) {
            textX /= fontScale;
            textY /= fontScale;
            GL11.glScaled(fontScale, fontScale, 0);
        }

        double dTextX = textX - (double) (int) textX;
        double dTextY = textY - (double) (int) textY;
        double textWidth = fontRenderer.getStringWidth(text);
        GL11.glTranslated(dTextX, dTextY, 0.0);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        if (fontShadow) {
            fontRenderer.drawStringWithShadow(
                    text,
                    (centered ? (int) (textX - textWidth / 2.0) : (int) textX),
                    (int) textY,
                    fontColor);
        } else {
            fontRenderer.drawString(
                    text,
                    (centered ? (int) (textX - textWidth / 2.0) : (int) textX),
                    (int) textY,
                    fontColor);
        }
        GL11.glPopMatrix();
    }
}
