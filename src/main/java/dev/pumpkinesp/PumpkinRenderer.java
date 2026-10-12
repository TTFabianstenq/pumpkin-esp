package dev.pumpkinesp;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * Draws orange ESP boxes (and optional tracers) for found pumpkins.
 */
public final class PumpkinRenderer {
    private static final float R = 1.0f;
    private static final float G = 0.55f;
    private static final float B = 0.0f;
    private static final float A = 0.85f;

    private PumpkinRenderer() {}

    public static void render(WorldRenderContext ctx) {
        if (!PumpkinEspMod.enabled) return;
        if (!PumpkinEspMod.boxes && !PumpkinEspMod.tracers) return;
        if (PumpkinScanner.FOUND.isEmpty()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        Vec3d cam = ctx.camera().getPos();
        MatrixStack matrices = ctx.matrixStack();
        matrices.push();
        matrices.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        Tessellator tessellator = Tessellator.getInstance();

        if (PumpkinEspMod.boxes) {
            BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            for (BlockPos pos : PumpkinScanner.FOUND) {
                if (!client.player.getBlockPos().isWithinDistance(pos, PumpkinEspMod.range)) continue;
                drawBoxOutline(buffer, matrix, pos.getX(), pos.getY(), pos.getZ());
            }
            net.minecraft.client.render.BufferRenderer.drawWithGlobalProgram(buffer.end());
        }

        if (PumpkinEspMod.tracers) {
            BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            Vec3d eye = client.player.getEyePos();
            for (BlockPos pos : PumpkinScanner.FOUND) {
                if (!client.player.getBlockPos().isWithinDistance(pos, PumpkinEspMod.range)) continue;
                float x = pos.getX() + 0.5f;
                float y = pos.getY() + 0.5f;
                float z = pos.getZ() + 0.5f;
                buffer.vertex(matrix, (float) eye.x, (float) eye.y, (float) eye.z).color(R, G, B, 0.6f);
                buffer.vertex(matrix, x, y, z).color(R, G, B, 0.6f);
            }
            net.minecraft.client.render.BufferRenderer.drawWithGlobalProgram(buffer.end());
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        matrices.pop();
    }

    private static void drawBoxOutline(BufferBuilder buffer, Matrix4f matrix, int x, int y, int z) {
        float x0 = x, y0 = y, z0 = z;
        float x1 = x + 1, y1 = y + 1, z1 = z + 1;

        line(buffer, matrix, x0, y0, z0, x1, y0, z0);
        line(buffer, matrix, x1, y0, z0, x1, y0, z1);
        line(buffer, matrix, x1, y0, z1, x0, y0, z1);
        line(buffer, matrix, x0, y0, z1, x0, y0, z0);
        line(buffer, matrix, x0, y1, z0, x1, y1, z0);
        line(buffer, matrix, x1, y1, z0, x1, y1, z1);
        line(buffer, matrix, x1, y1, z1, x0, y1, z1);
        line(buffer, matrix, x0, y1, z1, x0, y1, z0);
        line(buffer, matrix, x0, y0, z0, x0, y1, z0);
        line(buffer, matrix, x1, y0, z0, x1, y1, z0);
        line(buffer, matrix, x1, y0, z1, x1, y1, z1);
        line(buffer, matrix, x0, y0, z1, x0, y1, z1);
    }

    private static void line(BufferBuilder buffer, Matrix4f matrix,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2) {
        buffer.vertex(matrix, x1, y1, z1).color(R, G, B, A);
        buffer.vertex(matrix, x2, y2, z2).color(R, G, B, A);
    }
}
