package blockrenderer6343.client.renderer;

import blockrenderer6343.BlockRenderer6343;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.world.World;

import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

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
    private final IntBuffer viewport = GLAllocation.createDirectIntBuffer(16);

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
            fbo = new Framebuffer(resolutionWidth, resolutionHeight, true) {
                @Override
                public void createFramebuffer(int p_147605_1_, int p_147605_2_) {
                    //fight with MixinFrameBuffer swapping depth buffer when Angelica is present
                    // by re-overwriting method with Vanilla FBO
                    useDepth = true;
                    this.framebufferWidth = p_147605_1_;
                    this.framebufferHeight = p_147605_2_;
                    this.framebufferTextureWidth = p_147605_1_;
                    this.framebufferTextureHeight = p_147605_2_;

                    if (!OpenGlHelper.isFramebufferEnabled())
                    {
                        this.framebufferClear();
                    }
                    else
                    {
                        this.framebufferObject = OpenGlHelper.func_153165_e();
                        this.framebufferTexture = TextureUtil.glGenTextures();

                        if (this.useDepth)
                        {
                            this.depthBuffer = OpenGlHelper.func_153185_f();
                        }

                        this.setFramebufferFilter(9728);
                        GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.framebufferTexture);
                        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, this.framebufferTextureWidth, this.framebufferTextureHeight, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer)null);
                        OpenGlHelper.func_153171_g(OpenGlHelper.field_153198_e, this.framebufferObject);
                        OpenGlHelper.func_153188_a(OpenGlHelper.field_153198_e, OpenGlHelper.field_153200_g, 3553, this.framebufferTexture, 0);

                        if (this.useDepth)
                        {
                            OpenGlHelper.func_153176_h(OpenGlHelper.field_153199_f, this.depthBuffer);
                            if (net.minecraftforge.client.MinecraftForgeClient.getStencilBits() == 0)
                            {
                                OpenGlHelper.func_153186_a(OpenGlHelper.field_153199_f, 33190, this.framebufferTextureWidth, this.framebufferTextureHeight);
                                OpenGlHelper.func_153190_b(OpenGlHelper.field_153198_e, OpenGlHelper.field_153201_h, OpenGlHelper.field_153199_f, this.depthBuffer);
                            }
                            else
                            {
                                OpenGlHelper.func_153186_a(OpenGlHelper.field_153199_f, org.lwjgl.opengl.EXTPackedDepthStencil.GL_DEPTH24_STENCIL8_EXT, this.framebufferTextureWidth, this.framebufferTextureHeight);
                                OpenGlHelper.func_153190_b(OpenGlHelper.field_153198_e, org.lwjgl.opengl.EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT, OpenGlHelper.field_153199_f, this.depthBuffer);
                                OpenGlHelper.func_153190_b(OpenGlHelper.field_153198_e, org.lwjgl.opengl.EXTFramebufferObject.GL_STENCIL_ATTACHMENT_EXT, OpenGlHelper.field_153199_f, this.depthBuffer);
                            }
                        }

                        this.framebufferClear();
                        this.unbindFramebufferTexture();
                    }
                }
            };
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

    @Override
    public void render(int x, int y, int width, int height, int mouseX, int mouseY) {
        render(x, y, width, height, (float) mouseX, (float) mouseY);
    }

    private int bindFBO() {
        int lastID = GL11.glGetInteger(EXTFramebufferObject.GL_FRAMEBUFFER_BINDING_EXT);
        GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);
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

        GL11.glViewport(viewport.get(0), viewport.get(1), viewport.get(2), viewport.get(3));
    }

    public void releaseFBO() {
        if (fbo != null) {
            fbo.deleteFramebuffer();
        }
        fbo = null;
    }
}
