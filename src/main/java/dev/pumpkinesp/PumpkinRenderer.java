package dev.pumpkinesp;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import com.mojang.blaze3d.vertex.PoseStack;

/**
 * Through-wall pumpkin ESP for 1.21.11 (Fabric + Yarn).
 * Depth test is forced off so boxes/tracers draw over terrain.
 */
public final class PumpkinRenderer {
    private static final float R = 1.0f;
    private static final float G = 0.45f;
    private static final float B = 0.0f;
    private static final float A = 1.0f;

    private PumpkinRenderer() {}

    public static void render(WorldRenderContext ctx) {
        if (!PumpkinEspMod.enabled) return;
        if (!PumpkinEspMod.boxes && !PumpkinEspMod.tracers) return;
        if (PumpkinScanner.FOUND.isEmpty()) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        Vec3 cam = ctx.camera().getPosition();
        PoseStack matrices = ctx.matrixStack();
        if (matrices == null) return;

        matrices.pushPose();
        matrices.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f matrix = matrices.last().pose();

        // Through walls
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glLineWidth(3.0f);

        Tesselator tesselator = Tesselator.getInstance();

        if (PumpkinEspMod.boxes) {
            BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
            for (BlockPos pos : PumpkinScanner.FOUND) {
                if (!client.player.blockPosition().closerThan(pos, PumpkinEspMod.range)) continue;
                drawBox(buffer, matrix, pos.getX(), pos.getY(), pos.getZ());
            }
            // build + draw
            var mesh = buffer.buildOrThrow();
            // Immediate-mode fallback via BufferUploader if available
            try {
                net.minecraft.client.renderer.RenderType.lines().draw(mesh);
            } catch (Throwable t) {
                try {
                    com.mojang.blaze3d.vertex.BufferUploader.drawWithShader(mesh);
                } catch (Throwable ignored) {
                    mesh.close();
                }
            }
        }

        if (PumpkinEspMod.tracers) {
            BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
            Vec3 eye = client.player.getEyePosition(1.0f);
            for (BlockPos pos : PumpkinScanner.FOUND) {
                if (!client.player.blockPosition().closerThan(pos, PumpkinEspMod.range)) continue;
                float x = pos.getX() + 0.5f;
                float y = pos.getY() + 0.5f;
                float z = pos.getZ() + 0.5f;
                buffer.addVertex(matrix, (float) eye.x, (float) eye.y, (float) eye.z).setColor(R, G, B, 0.8f);
                buffer.addVertex(matrix, x, y, z).setColor(R, G, B, 0.8f);
            }
            var mesh = buffer.buildOrThrow();
            try {
                com.mojang.blaze3d.vertex.BufferUploader.drawWithShader(mesh);
            } catch (Throwable t) {
                mesh.close();
            }
        }

        GL11.glLineWidth(1.0f);
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        matrices.popPose();
    }

    private static void drawBox(BufferBuilder buffer, Matrix4f m, int x, int y, int z) {
        float x0 = x, y0 = y, z0 = z, x1 = x + 1, y1 = y + 1, z1 = z + 1;
        line(buffer, m, x0, y0, z0, x1, y0, z0);
        line(buffer, m, x1, y0, z0, x1, y0, z1);
        line(buffer, m, x1, y0, z1, x0, y0, z1);
        line(buffer, m, x0, y0, z1, x0, y0, z0);
        line(buffer, m, x0, y1, z0, x1, y1, z0);
        line(buffer, m, x1, y1, z0, x1, y1, z1);
        line(buffer, m, x1, y1, z1, x0, y1, z1);
        line(buffer, m, x0, y1, z1, x0, y1, z0);
        line(buffer, m, x0, y0, z0, x0, y1, z0);
        line(buffer, m, x1, y0, z0, x1, y1, z0);
        line(buffer, m, x1, y0, z1, x1, y1, z1);
        line(buffer, m, x0, y0, z1, x0, y1, z1);
    }

    private static void line(BufferBuilder b, Matrix4f m, float x1, float y1, float z1, float x2, float y2, float z2) {
        b.addVertex(m, x1, y1, z1).setColor(R, G, B, A);
        b.addVertex(m, x2, y2, z2).setColor(R, G, B, A);
    }
}
