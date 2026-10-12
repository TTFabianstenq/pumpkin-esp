package dev.pumpkinesp;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;

/**
 * Incremental scanner: walks loaded chunks within {@link PumpkinEspMod#range}.
 * Client only has block data for loaded chunks — with RD 16 that covers ~256 blocks.
 */
public final class PumpkinScanner {
    /** Thread-safe list of found pumpkin positions for the renderer. */
    public static final List<BlockPos> FOUND = new CopyOnWriteArrayList<>();

    private static final Set<Long> pendingChunks = new LinkedHashSet<>();
    private static final Set<Long> scannedChunks = new LinkedHashSet<>();
    private static BlockPos lastPlayerChunk = BlockPos.ORIGIN;
    private static int tickCounter;

    private PumpkinScanner() {}

    public static void tick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;

        tickCounter++;
        BlockPos playerPos = client.player.getBlockPos();
        ChunkPos playerChunk = new ChunkPos(playerPos);

        // Refresh chunk queue when player moves to a new chunk or every 100 ticks
        if (!playerPos.isWithinDistance(lastPlayerChunk, 16) || tickCounter % 100 == 0) {
            lastPlayerChunk = playerPos.toImmutable();
            queueNearbyChunks(client, playerChunk, playerPos);
        }

        // Scan a few chunks per tick to avoid lag
        int budget = 2;
        Iterator<Long> it = pendingChunks.iterator();
        while (it.hasNext() && budget-- > 0) {
            long key = it.next();
            it.remove();
            int cx = (int) (key >> 32);
            int cz = (int) key;
            WorldChunk chunk = client.world.getChunkManager().getWorldChunk(cx, cz);
            if (chunk == null) continue;
            scanChunk(chunk, playerPos);
            scannedChunks.add(key);
        }

        // Prune far results every 40 ticks
        if (tickCounter % 40 == 0) {
            pruneFar(playerPos);
        }
    }

    private static void queueNearbyChunks(MinecraftClient client, ChunkPos center, BlockPos playerPos) {
        pendingChunks.clear();
        scannedChunks.clear();
        int chunkRange = (int) Math.ceil(PumpkinEspMod.range / 16.0) + 1;

        for (int dx = -chunkRange; dx <= chunkRange; dx++) {
            for (int dz = -chunkRange; dz <= chunkRange; dz++) {
                int cx = center.x + dx;
                int cz = center.z + dz;
                double blockDist = Math.hypot((cx * 16 + 8) - playerPos.getX(), (cz * 16 + 8) - playerPos.getZ());
                if (blockDist > PumpkinEspMod.range + 24) continue;
                if (client.world.getChunkManager().getWorldChunk(cx, cz) == null) continue;
                pendingChunks.add(pack(cx, cz));
            }
        }

        FOUND.clear();
    }

    private static void scanChunk(WorldChunk chunk, BlockPos playerPos) {
        ChunkPos cp = chunk.getPos();
        int minY = chunk.getBottomY();
        int maxY = minY + chunk.countVerticalSections() * 16;

        BlockPos.Mutable mut = new BlockPos.Mutable();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int wx = cp.getStartX() + x;
                int wz = cp.getStartZ() + z;
                double horiz = Math.hypot(wx - playerPos.getX(), wz - playerPos.getZ());
                if (horiz > PumpkinEspMod.range) continue;

                for (int y = minY; y < maxY; y++) {
                    mut.set(wx, y, wz);
                    if (!playerPos.isWithinDistance(mut, PumpkinEspMod.range)) continue;
                    Block block = chunk.getBlockState(mut).getBlock();
                    if (isPumpkin(block)) {
                        FOUND.add(mut.toImmutable());
                    }
                }
            }
        }
    }

    private static boolean isPumpkin(Block block) {
        return block == Blocks.PUMPKIN
                || block == Blocks.CARVED_PUMPKIN
                || block == Blocks.JACK_O_LANTERN;
    }

    private static void pruneFar(BlockPos playerPos) {
        List<BlockPos> keep = new ArrayList<>(FOUND.size());
        for (BlockPos p : FOUND) {
            if (playerPos.isWithinDistance(p, PumpkinEspMod.range)) {
                keep.add(p);
            }
        }
        FOUND.clear();
        FOUND.addAll(keep);
    }

    private static long pack(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xffffffffL);
    }
}
