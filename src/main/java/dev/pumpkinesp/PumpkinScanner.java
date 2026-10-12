package dev.pumpkinesp;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ChunkAccess;

public final class PumpkinScanner {
    public static final List<BlockPos> FOUND = new CopyOnWriteArrayList<>();
    private static final Set<Long> pendingChunks = new LinkedHashSet<>();
    private static BlockPos lastPlayerChunk = BlockPos.ZERO;
    private static int tickCounter;

    private PumpkinScanner() {}

    public static void tick(Minecraft client) {
        if (client.player == null || client.level == null) return;
        tickCounter++;
        BlockPos playerPos = client.player.blockPosition();

        if (!playerPos.closerThan(lastPlayerChunk, 16) || tickCounter % 100 == 0) {
            lastPlayerChunk = playerPos.immutable();
            queueNearbyChunks(client, playerPos);
        }

        int budget = 3;
        Iterator<Long> it = pendingChunks.iterator();
        while (it.hasNext() && budget-- > 0) {
            long key = it.next();
            it.remove();
            int cx = (int) (key >> 32);
            int cz = (int) key;
            LevelChunk chunk = client.level.getChunkSource().getChunk(cx, cz, false);
            if (chunk == null) continue;
            scanChunk(chunk, playerPos);
        }

        if (tickCounter % 40 == 0) pruneFar(playerPos);
    }

    private static void queueNearbyChunks(Minecraft client, BlockPos playerPos) {
        pendingChunks.clear();
        int chunkRange = (int) Math.ceil(PumpkinEspMod.range / 16.0) + 1;
        int pcx = playerPos.getX() >> 4;
        int pcz = playerPos.getZ() >> 4;

        for (int dx = -chunkRange; dx <= chunkRange; dx++) {
            for (int dz = -chunkRange; dz <= chunkRange; dz++) {
                int cx = pcx + dx;
                int cz = pcz + dz;
                double blockDist = Math.hypot((cx * 16 + 8) - playerPos.getX(), (cz * 16 + 8) - playerPos.getZ());
                if (blockDist > PumpkinEspMod.range + 24) continue;
                if (client.level.getChunkSource().getChunk(cx, cz, false) == null) continue;
                pendingChunks.add(((long) cx << 32) | (cz & 0xffffffffL));
            }
        }
        FOUND.clear();
    }

    private static void scanChunk(LevelChunk chunk, BlockPos playerPos) {
        int minY = chunk.getMinY();
        int maxY = chunk.getMaxY();
        int startX = chunk.getPos().getMinBlockX();
        int startZ = chunk.getPos().getMinBlockZ();

        BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int wx = startX + x;
                int wz = startZ + z;
                if (Math.hypot(wx - playerPos.getX(), wz - playerPos.getZ()) > PumpkinEspMod.range) continue;
                for (int y = minY; y < maxY; y++) {
                    mut.set(wx, y, wz);
                    if (!playerPos.closerThan(mut, PumpkinEspMod.range)) continue;
                    Block block = chunk.getBlockState(mut).getBlock();
                    if (block == Blocks.PUMPKIN || block == Blocks.CARVED_PUMPKIN || block == Blocks.JACK_O_LANTERN) {
                        FOUND.add(mut.immutable());
                    }
                }
            }
        }
    }

    private static void pruneFar(BlockPos playerPos) {
        List<BlockPos> keep = new ArrayList<>();
        for (BlockPos p : FOUND) {
            if (playerPos.closerThan(p, PumpkinEspMod.range)) keep.add(p);
        }
        FOUND.clear();
        FOUND.addAll(keep);
    }
}
