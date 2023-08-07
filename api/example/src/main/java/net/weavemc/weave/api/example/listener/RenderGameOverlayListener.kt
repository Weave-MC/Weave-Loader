package net.weavemc.weave.api.example.listener

import net.minecraft.client.renderer.GlStateManager
import net.weavemc.weave.api.event.RenderGameOverlayEvent
import net.weavemc.weave.api.event.SubscribeEvent
import org.lwjgl.opengl.GL11.*

class RenderGameOverlayListener {
    @SubscribeEvent
    fun onRender(event: RenderGameOverlayEvent.Post) {
        GlStateManager.disableTexture2D()
        GlStateManager.color(1f, 0f, 0f)
        glBegin(GL_QUADS)
        glVertex2f(0f, 0f)
        glVertex2f(0f, 50f)
        glVertex2f(50f, 50f)
        glVertex2f(50f, 0f)
        glEnd()
        GlStateManager.color(1f, 1f, 1f)
        GlStateManager.enableTexture2D()
    }
}
