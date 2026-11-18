package blockrenderer6343.client.renderer;

import java.lang.ref.WeakReference;

import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraftforge.client.event.GuiOpenEvent;

import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;

@EventBusSubscriber(side = Side.CLIENT)
public class PreviewGeometryCache implements IResourceManagerReloadListener {

    private static final PreviewGeometryCache INSTANCE = new PreviewGeometryCache();
    private WeakReference<Object> owner = new WeakReference<>(null);
    private int displayList;
    private long revision;

    public static PreviewGeometryCache getInstance() {
        return INSTANCE;
    }

    public void render(Object scene, long geometryRevision, Runnable drawGeometry) {
        if (displayList == 0 || owner.get() != scene || revision != geometryRevision) {
            invalidate();
            displayList = GLAllocation.generateDisplayLists(1);
            boolean compiled = false;
            GL11.glNewList(displayList, GL11.GL_COMPILE);
            try {
                drawGeometry.run();
                compiled = true;
            } finally {
                GL11.glEndList();
                if (!compiled) invalidate();
            }
            owner = new WeakReference<>(scene);
            revision = geometryRevision;
        }
        GL11.glCallList(displayList);
    }

    public void invalidate() {
        if (displayList != 0) {
            GLAllocation.deleteDisplayLists(displayList);
            displayList = 0;
        }
        owner.clear();
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        invalidate();
    }

    @SubscribeEvent
    public static void onGuiOpen(GuiOpenEvent event) {
        INSTANCE.invalidate();
    }
}
