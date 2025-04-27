package blockrenderer6343.client.renderer;

import blockrenderer6343.BlockRenderer6343;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.world.World;

import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: KilaBash, backported by Quarri6343
 * @Date: 2021/08/23
 * @Description: It looks similar to {@link ImmediateWorldSceneRenderer}, but totally different.
 *               It uses FBO and is more universality and efficient(X).
 *               FBO can be rendered anywhere more flexibly, not just in the GUI.
 *               If you have scene rendering needs, you will love this FBO renderer.
 *               TODO OP_LIST might be used in the future to further improve performance.
 */
@SideOnly(Side.CLIENT)
public class FBOWorldSceneRenderer extends WorldSceneRenderer {

    private int resolutionWidth = 1080;
    private int resolutionHeight = 1080;
    private Framebuffer fbo;

    public FBOWorldSceneRenderer(World world, int resolutionWidth, int resolutionHeight) {
        super(world);
        setFBOSize(resolutionWidth, resolutionHeight);
    }

    public FBOWorldSceneRenderer(World world, Framebuffer fbo) {
        super(world);
        this.fbo = fbo;
    }

    public int getResolutionWidth() {
        return resolutionWidth;
    }

    public int getResolutionHeight() {
        return resolutionHeight;
    }

    /***
     * This will modify the size of the FBO. You'd better know what you're doing before you call it.
     */
    public void setFBOSize(int resolutionWidth, int resolutionHeight) {
        this.resolutionWidth = resolutionWidth;
        this.resolutionHeight = resolutionHeight;
        releaseFBO();
        try {
            fbo = new Framebuffer(resolutionWidth, resolutionHeight, true);
        } catch (Exception e) {
            BlockRenderer6343.error("Failed to Resize!");
        }
    }

    public void render(float x, float y, float width, float height, float mouseX, float mouseY) {
        // bind to FBO
        int lastID = bindFBO();
        super.render(0, 0, this.resolutionWidth, this.resolutionHeight, (int) (this.resolutionWidth * mouseX / width),
                (int) (this.resolutionHeight * (1 - mouseY / height)));
        // unbind FBO
        unbindFBO(lastID);

        // bind FBO as texture
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        lastID = GL11.glGetInteger(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, fbo.framebufferTexture);
        GL11.glColor4f(1, 1, 1, 1);

        // render rect with FBO texture
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();

        tessellator.addVertexWithUV(x + width, y + height, 0, 1, 0);
        tessellator.addVertexWithUV(x + width, y, 0, 1, 1);
        tessellator.addVertexWithUV(x, y, 0, 0, 1);
        tessellator.addVertexWithUV(x, y + height, 0, 0, 0);
        tessellator.draw();

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, lastID);
    }

    public void render(float x, float y, float width, float height, int mouseX, int mouseY) {
        render(x, y, width, height, (float) mouseX, (float) mouseY);
    }

    private int bindFBO() {
        int lastID = GL11.glGetInteger(EXTFramebufferObject.GL_FRAMEBUFFER_BINDING_EXT);
        fbo.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);
        fbo.framebufferClear();
        fbo.bindFramebuffer(true);
        GL11.glPushMatrix();
        return lastID;
    }

    private void unbindFBO(int lastID) {
        GL11.glPopMatrix();
        fbo.unbindFramebufferTexture();
        OpenGlHelper.func_153171_g(OpenGlHelper.field_153198_e, lastID); // glBindFramebuffer GL_FRAMEBUFFER
    }

    public void releaseFBO() {
        if (fbo != null) {
            fbo.deleteFramebuffer();
        }
        fbo = null;
    }
}
