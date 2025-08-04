package com.jedk1.jedcore.util;

import com.jedk1.jedcore.JedCore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class SchedulerUtil {
    
    private static final boolean THREAD_UTIL_AVAILABLE;
    private static final Class<?> THREAD_UTIL_CLASS;
    
    static {
        Class<?> threadUtilClass = null;
        try {
            threadUtilClass = Class.forName("com.projectkorra.projectkorra.util.ThreadUtil");
        } catch (ClassNotFoundException e) {
            // ThreadUtil not available, will use Bukkit scheduler
        }
        THREAD_UTIL_CLASS = threadUtilClass;
        THREAD_UTIL_AVAILABLE = THREAD_UTIL_CLASS != null;
    }

    public static void runGlobalLater(Runnable runnable, long delay) {
        if (THREAD_UTIL_AVAILABLE) {
            try {
                THREAD_UTIL_CLASS.getMethod("runGlobalLater", Runnable.class, long.class)
                    .invoke(null, runnable, delay);
            } catch (Exception e) {
                // Fallback to Bukkit scheduler
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        runnable.run();
                    }
                }.runTaskLater(JedCore.plugin, delay);
            }
        } else {
            new BukkitRunnable() {
                @Override
                public void run() {
                    runnable.run();
                }
            }.runTaskLater(JedCore.plugin, delay);
        }
    }

    public static void runLocationLater(Location location, Runnable runnable, long delay) {
        if (THREAD_UTIL_AVAILABLE) {
            try {
                THREAD_UTIL_CLASS.getMethod("ensureLocationLater", Location.class, Runnable.class, long.class)
                    .invoke(null, location, runnable, delay);
            } catch (Exception e) {
                // Fallback to Bukkit scheduler
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        runnable.run();
                    }
                }.runTaskLater(JedCore.plugin, delay);
            }
        } else {
            new BukkitRunnable() {
                @Override
                public void run() {
                    runnable.run();
                }
            }.runTaskLater(JedCore.plugin, delay);
        }
    }

    public static void runEntityLater(Entity entity, Runnable runnable, long delay) {
        if (THREAD_UTIL_AVAILABLE) {
            try {
                THREAD_UTIL_CLASS.getMethod("ensureEntityLater", Entity.class, Runnable.class, long.class)
                    .invoke(null, entity, runnable, delay);
            } catch (Exception e) {
                // Fallback to Bukkit scheduler
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        runnable.run();
                    }
                }.runTaskLater(JedCore.plugin, delay);
            }
        } else {
            new BukkitRunnable() {
                @Override
                public void run() {
                    runnable.run();
                }
            }.runTaskLater(JedCore.plugin, delay);
        }
    }

    public static Object runGlobalTimer(Runnable runnable, long delay, long period) {
        if (THREAD_UTIL_AVAILABLE) {
            try {
                return THREAD_UTIL_CLASS.getMethod("runGlobalTimer", Runnable.class, long.class, long.class)
                    .invoke(null, runnable, delay, period);
            } catch (Exception e) {
                // Fallback to Bukkit scheduler
                return new BukkitRunnable() {
                    @Override
                    public void run() {
                        runnable.run();
                    }
                }.runTaskTimer(JedCore.plugin, delay, period);
            }
        } else {
            return new BukkitRunnable() {
                @Override
                public void run() {
                    runnable.run();
                }
            }.runTaskTimer(JedCore.plugin, delay, period);
        }
    }

    public static Object runLocationTimer(Location location, Runnable runnable, long delay, long period) {
        if (THREAD_UTIL_AVAILABLE) {
            try {
                return THREAD_UTIL_CLASS.getMethod("ensureLocationTimer", Location.class, Runnable.class, long.class, long.class)
                    .invoke(null, location, runnable, delay, period);
            } catch (Exception e) {
                // Fallback to Bukkit scheduler
                return new BukkitRunnable() {
                    @Override
                    public void run() {
                        runnable.run();
                    }
                }.runTaskTimer(JedCore.plugin, delay, period);
            }
        } else {
            return new BukkitRunnable() {
                @Override
                public void run() {
                    runnable.run();
                }
            }.runTaskTimer(JedCore.plugin, delay, period);
        }
    }

    public static Object runEntityTimer(Entity entity, Runnable runnable, long delay, long period) {
        if (THREAD_UTIL_AVAILABLE) {
            try {
                return THREAD_UTIL_CLASS.getMethod("ensureEntityTimer", Entity.class, Runnable.class, long.class, long.class)
                    .invoke(null, entity, runnable, delay, period);
            } catch (Exception e) {
                // Fallback to Bukkit scheduler
                return new BukkitRunnable() {
                    @Override
                    public void run() {
                        runnable.run();
                    }
                }.runTaskTimer(JedCore.plugin, delay, period);
            }
        } else {
            return new BukkitRunnable() {
                @Override
                public void run() {
                    runnable.run();
                }
            }.runTaskTimer(JedCore.plugin, delay, period);
        }
    }

    public static void runAsync(Runnable runnable) {
        if (THREAD_UTIL_AVAILABLE) {
            try {
                THREAD_UTIL_CLASS.getMethod("runAsync", Runnable.class)
                    .invoke(null, runnable);
            } catch (Exception e) {
                // Fallback to Bukkit scheduler
                Bukkit.getScheduler().runTaskAsynchronously(JedCore.plugin, runnable);
            }
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(JedCore.plugin, runnable);
        }
    }

    public static Object runAsyncTimer(Runnable runnable, long delay, long period) {
        if (THREAD_UTIL_AVAILABLE) {
            try {
                return THREAD_UTIL_CLASS.getMethod("runAsyncTimer", Runnable.class, long.class, long.class)
                    .invoke(null, runnable, delay, period);
            } catch (Exception e) {
                // Fallback to Bukkit scheduler
                return Bukkit.getScheduler().runTaskTimerAsynchronously(JedCore.plugin, runnable, delay, period);
            }
        } else {
            return Bukkit.getScheduler().runTaskTimerAsynchronously(JedCore.plugin, runnable, delay, period);
        }
    }

    public static void cancelTimerTask(Object task) {
        if (THREAD_UTIL_AVAILABLE) {
            try {
                THREAD_UTIL_CLASS.getMethod("cancelTimerTask", Object.class)
                    .invoke(null, task);
            } catch (Exception e) {
                // Fallback to Bukkit task cancellation
                if (task instanceof BukkitTask) {
                    ((BukkitTask) task).cancel();
                }
            }
        } else {
            if (task instanceof BukkitTask) {
                ((BukkitTask) task).cancel();
            }
        }
    }

    public static boolean isThreadUtilAvailable() {
        return THREAD_UTIL_AVAILABLE;
    }

    public static void ensureEntity(Entity entity, Runnable runnable) {
        if (THREAD_UTIL_AVAILABLE) {
            try {
                THREAD_UTIL_CLASS.getMethod("ensureEntity", Entity.class, Runnable.class)
                    .invoke(null, entity, runnable);
            } catch (Exception e) {
                // Fallback to global execution
                runnable.run();
            }
        } else {
            // Fallback to global execution
            runnable.run();
        }
    }
}
