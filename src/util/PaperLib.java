//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.jedk1.jedcore.util;

import com.jedk1.jedcore.JedCore;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

public class PaperLib {
    private static Environment ENVIRONMENT;

    public static CompletableFuture<Chunk> getChunkAtAsync(Location location) {
        return ENVIRONMENT.getChunkAtAsync(location);
    }

    public static CompletableFuture<Chunk> getChunkAtAsync(Block block) {
        return ENVIRONMENT.getChunkAtAsync(block);
    }

    public static CompletableFuture<Boolean> teleportAsync(Entity entity, Location location) {
        return ENVIRONMENT.teleportAsync(entity, location);
    }

    public static CompletableFuture<Boolean> teleportAsync(Entity entity, Location location, PlayerTeleportEvent.TeleportCause cause) {
        return ENVIRONMENT.teleportAsync(entity, location, cause);
    }

    static {
        if (JedCore.isFolia()) {
            ENVIRONMENT = new Folia();
        } else if (JedCore.isPaper()) {
            ENVIRONMENT = new Paper();
        } else {
            ENVIRONMENT = new Spigot();
        }

    }

    private interface Environment {
        default CompletableFuture<Chunk> getChunkAtAsync(Location location) {
            return this.getChunkAtAsync(location.getBlock());
        }

        CompletableFuture<Chunk> getChunkAtAsync(Block var1);

        default CompletableFuture<Boolean> teleportAsync(Entity entity, Location location) {
            return this.teleportAsync(entity, location, TeleportCause.PLUGIN);
        }

        CompletableFuture<Boolean> teleportAsync(Entity var1, Location var2, PlayerTeleportEvent.TeleportCause var3);
    }

    static class Spigot implements Environment {
        public CompletableFuture<Chunk> getChunkAtAsync(Block block) {
            return CompletableFuture.completedFuture(block.getChunk());
        }

        public CompletableFuture<Boolean> teleportAsync(Entity entity, Location location, PlayerTeleportEvent.TeleportCause cause) {
            entity.teleport(location, cause);
            return CompletableFuture.completedFuture(true);
        }
    }

    static class Paper implements Environment {
        public CompletableFuture<Chunk> getChunkAtAsync(Block block) {
            return block.getWorld().getChunkAtAsync(block);
        }

        public CompletableFuture<Boolean> teleportAsync(Entity entity, Location location, PlayerTeleportEvent.TeleportCause cause) {
            return entity.teleportAsync(location, cause);
        }
    }

    static class Folia implements Environment {
        public CompletableFuture<Chunk> getChunkAtAsync(Block block) {
            CompletableFuture<Chunk> future = new CompletableFuture();
            ThreadUtil.ensureLocation(block.getLocation(), () -> {
                Chunk chunk = block.getWorld().getChunkAt(block);
                future.complete(chunk);
            });
            return future;
        }

        public CompletableFuture<Boolean> teleportAsync(Entity entity, Location location, PlayerTeleportEvent.TeleportCause cause) {
            return entity.teleportAsync(location, cause);
        }
    }
}
