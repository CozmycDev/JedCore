//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.jedk1.jedcore.util;

import com.jedk1.jedcore.JedCore;
import io.papermc.paper.threadedregions.scheduler.RegionScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

public class ThreadUtil {

    public static void ensureMainThread(Runnable runnable) {
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            throw new IllegalStateException("Runnable must be called from the main thread in Folia/Paper");
        }
    }

    public static <T> T EnsureMainThread(Callable<T> supplier) {
        if (Bukkit.isPrimaryThread()) {
            try {
                return supplier.call();
            } catch (Exception e) {
                throw new RuntimeException("Failed to get value on main thread", e);
            }
        } else {
            throw new IllegalStateException("Must be called from the main thread in Folia/Paper");
        }
    }

    public static <T> T ensureMainThread2(Supplier<T> supplier) {
        if (Bukkit.isPrimaryThread()) {
            return supplier.get();
        } else {
            throw new IllegalStateException("Operation must be performed on the main thread.");
        }
    }

    public static void ensureEntity(Entity entity, Runnable runnable) {
        if (JedCore.isFolia()) {
            if (Bukkit.isOwnedByCurrentRegion(entity) || Bukkit.isStopping()) {
                runnable.run();
                return;
            }

            entity.getScheduler().execute(JedCore.plugin, runnable, (Runnable)null, 1L);
        } else {
            if (Bukkit.isPrimaryThread()) {
                runnable.run();
                return;
            }

            Bukkit.getScheduler().runTask(JedCore.plugin, runnable);
        }

    }

    public static void ensureEntityDelay(Entity entity, Runnable runnable, long delay) {
        delay = Math.max(1L, delay);
        if (JedCore.isFolia()) {
            entity.getScheduler().execute(JedCore.plugin, runnable, (Runnable)null, delay);
        } else {
            Bukkit.getScheduler().runTaskLater(JedCore.plugin, runnable, delay);
        }

    }

    public static Object ensureEntityTimer(Entity entity, Runnable runnable, long delay, long repeat) {
        delay = Math.max(1L, delay);
        return JedCore.isFolia() ? entity.getScheduler().runAtFixedRate(JedCore.plugin, (task) -> runnable.run(), (Runnable)null, delay, repeat) : Bukkit.getScheduler().runTaskTimer(JedCore.plugin, runnable, delay, repeat);
    }

    public static void ensureLocation(Location location, Runnable runnable) {
        if (JedCore.isFolia()) {
            if (Bukkit.isOwnedByCurrentRegion(location) || Bukkit.isStopping()) {
                runnable.run();
                return;
            }

            RegionScheduler scheduler = Bukkit.getRegionScheduler();
            scheduler.execute(JedCore.plugin, location, runnable);
        } else {
            if (Bukkit.isPrimaryThread()) {
                runnable.run();
                return;
            }

            Bukkit.getScheduler().runTask(JedCore.plugin, runnable);
        }

    }

    public static void ensureLocationDelay(@NotNull Location location, Runnable runnable, long delay) {
        delay = Math.max(1L, delay);
        if (JedCore.isFolia()) {
            RegionScheduler scheduler = Bukkit.getRegionScheduler();
            scheduler.runDelayed(JedCore.plugin, location, (task) -> runnable.run(), delay);
        } else {
            Bukkit.getScheduler().runTaskLater(JedCore.plugin, runnable, delay);
        }

    }

    public static Object ensureLocationTimer(Location location, Runnable runnable, long delay, long repeat) {
        delay = Math.max(1L, delay);
        if (JedCore.isFolia()) {
            RegionScheduler scheduler = Bukkit.getRegionScheduler();
            return scheduler.runAtFixedRate(JedCore.plugin, location, (task) -> runnable.run(), delay, repeat);
        } else {
            return Bukkit.getScheduler().runTaskLater(JedCore.plugin, runnable, delay);
        }
    }

    public static void runAsync(Runnable runnable) {
        if (JedCore.isFolia()) {
            if (Bukkit.isStopping()) {
                runnable.run();
                return;
            }

            Bukkit.getAsyncScheduler().runNow(JedCore.plugin, (task) -> runnable.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(JedCore.plugin, runnable);
        }

    }

    public static void runAsyncLater(Runnable runnable, long delay) {
        delay = Math.max(1L, delay);
        if (JedCore.isFolia()) {
            Bukkit.getAsyncScheduler().runDelayed(JedCore.plugin, (task) -> runnable.run(), delay * 50L, TimeUnit.MILLISECONDS);
        } else {
            Bukkit.getScheduler().runTaskLater(JedCore.plugin, runnable, delay);
        }

    }

    public static Object runAsyncTimer(Runnable runnable, long delay, long repeat) {
        delay = Math.max(1L, delay);
        return JedCore.isFolia() ? Bukkit.getAsyncScheduler().runAtFixedRate(JedCore.plugin, (task) -> runnable.run(), delay * 50L, repeat * 50L, TimeUnit.MILLISECONDS) : Bukkit.getScheduler().runTaskTimerAsynchronously(JedCore.plugin, runnable, delay, repeat);
    }

    public static void runSync(Runnable runnable) {
        if (JedCore.isFolia()) {
            if (Bukkit.isStopping()) {
                runnable.run();
                return;
            }

            Bukkit.getGlobalRegionScheduler().run(JedCore.plugin, (task) -> runnable.run());
        } else {
            Bukkit.getScheduler().runTask(JedCore.plugin, runnable);
        }

    }

    public static Object runSyncLater(Runnable runnable, long delay) {
        delay = Math.max(1L, delay);
        return JedCore.isFolia() ? Bukkit.getGlobalRegionScheduler().runDelayed(JedCore.plugin, (task) -> runnable.run(), delay) : Bukkit.getScheduler().runTaskLater(JedCore.plugin, runnable, delay);
    }

    public static Object runSyncTimer(Runnable runnable, long delay, long repeat) {
        delay = Math.max(1L, delay);
        return JedCore.isFolia() ? Bukkit.getGlobalRegionScheduler().runAtFixedRate(JedCore.plugin, (task) -> runnable.run(), delay, repeat) : Bukkit.getScheduler().runTaskTimer(JedCore.plugin, runnable, delay, repeat);
    }

    public static boolean cancelTimerTask(Object task) {
        if (task == null) {
            return false;
        } else {
            if (JedCore.isFolia()) {
                if (task instanceof ScheduledTask) {
                    ((ScheduledTask)task).cancel();
                    return true;
                }
            } else if (task instanceof BukkitTask) {
                ((BukkitTask)task).cancel();
                return true;
            }

            return false;
        }
    }

    public static boolean isTaskCancelled(Object task) {
        if (JedCore.isFolia()) {
            if (task instanceof ScheduledTask) {
                return ((ScheduledTask)task).isCancelled();
            }
        } else if (task instanceof BukkitTask) {
            return ((BukkitTask)task).isCancelled();
        }

        return false;
    }
}
