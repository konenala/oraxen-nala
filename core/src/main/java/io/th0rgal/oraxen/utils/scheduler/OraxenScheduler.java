package io.th0rgal.oraxen.utils.scheduler;

import io.th0rgal.oraxen.OraxenPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * Oraxen Scheduler utility for Folia compatibility
 * Automatically detects if running on Folia or regular Bukkit/Spigot
 * and uses the appropriate scheduling methods.
 */
public class OraxenScheduler {

    private static final boolean IS_FOLIA;
    private static Method globalRegionScheduler_run;
    private static Method globalRegionScheduler_runDelayed;
    private static Method globalRegionScheduler_runAtFixedRate;
    private static Method regionScheduler_run;
    private static Method regionScheduler_runDelayed;
    private static Method regionScheduler_runAtFixedRate;
    private static Method entityScheduler_run;
    private static Method entityScheduler_runDelayed;
    private static Method entityScheduler_runAtFixedRate;
    private static Method asyncScheduler_runNow;
    private static Method asyncScheduler_runDelayed;
    private static Method asyncScheduler_runAtFixedRate;
    private static Object globalRegionScheduler;
    private static Object regionScheduler;
    private static Object asyncScheduler;

    static {
        boolean isFolia = false;
        try {
            Class.forName("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler");
            isFolia = true;

            // Initialize Folia schedulers
            Class<?> serverClass = Class.forName("org.bukkit.Bukkit");
            globalRegionScheduler = serverClass.getMethod("getGlobalRegionScheduler").invoke(null);
            regionScheduler = serverClass.getMethod("getRegionScheduler").invoke(null);
            asyncScheduler = serverClass.getMethod("getAsyncScheduler").invoke(null);

            // Get GlobalRegionScheduler methods
            Class<?> globalSchedulerClass = Class.forName("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler");
            globalRegionScheduler_run = globalSchedulerClass.getMethod("run", Plugin.class, Runnable.class);
            globalRegionScheduler_runDelayed = globalSchedulerClass.getMethod("runDelayed", Plugin.class, Runnable.class, long.class);
            globalRegionScheduler_runAtFixedRate = globalSchedulerClass.getMethod("runAtFixedRate", Plugin.class, Runnable.class, long.class, long.class);

            // Get RegionScheduler methods
            Class<?> regionSchedulerClass = Class.forName("io.papermc.paper.threadedregions.scheduler.RegionScheduler");
            regionScheduler_run = regionSchedulerClass.getMethod("run", Plugin.class, Location.class, Runnable.class);
            regionScheduler_runDelayed = regionSchedulerClass.getMethod("runDelayed", Plugin.class, Location.class, Runnable.class, long.class);
            regionScheduler_runAtFixedRate = regionSchedulerClass.getMethod("runAtFixedRate", Plugin.class, Location.class, Runnable.class, long.class, long.class);

            // Get EntityScheduler methods
            Class<?> entitySchedulerClass = Class.forName("io.papermc.paper.threadedregions.scheduler.EntityScheduler");
            entityScheduler_run = entitySchedulerClass.getMethod("run", Plugin.class, Runnable.class, Runnable.class);
            entityScheduler_runDelayed = entitySchedulerClass.getMethod("runDelayed", Plugin.class, Runnable.class, Runnable.class, long.class);
            entityScheduler_runAtFixedRate = entitySchedulerClass.getMethod("runAtFixedRate", Plugin.class, Runnable.class, Runnable.class, long.class, long.class);

            // Get AsyncScheduler methods
            Class<?> asyncSchedulerClass = Class.forName("io.papermc.paper.threadedregions.scheduler.AsyncScheduler");
            asyncScheduler_runNow = asyncSchedulerClass.getMethod("runNow", Plugin.class, Runnable.class);
            asyncScheduler_runDelayed = asyncSchedulerClass.getMethod("runDelayed", Plugin.class, Runnable.class, long.class, TimeUnit.class);
            asyncScheduler_runAtFixedRate = asyncSchedulerClass.getMethod("runAtFixedRate", Plugin.class, Runnable.class, long.class, long.class, TimeUnit.class);

        } catch (Exception e) {
            // Not Folia, use regular Bukkit scheduler
        }
        IS_FOLIA = isFolia;
    }

    /**
     * Check if the server is running Folia
     * @return true if Folia is detected
     */
    public static boolean isFolia() {
        return IS_FOLIA;
    }

    /**
     * Run a task on the main thread (or global region in Folia)
     */
    public static BukkitTask runTask(Plugin plugin, Runnable task) {
        if (IS_FOLIA) {
            try {
                return (BukkitTask) globalRegionScheduler_run.invoke(globalRegionScheduler, plugin, task);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("Failed to run task on Folia", e);
            }
        } else {
            return Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    /**
     * Run a task with delay on the main thread (or global region in Folia)
     */
    public static BukkitTask runTaskLater(Plugin plugin, Runnable task, long delay) {
        if (IS_FOLIA) {
            try {
                return (BukkitTask) globalRegionScheduler_runDelayed.invoke(globalRegionScheduler, plugin, task, delay);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("Failed to run delayed task on Folia", e);
            }
        } else {
            return Bukkit.getScheduler().runTaskLater(plugin, task, delay);
        }
    }

    /**
     * Run a repeating task on the main thread (or global region in Folia)
     */
    public static BukkitTask runTaskTimer(Plugin plugin, Runnable task, long delay, long period) {
        if (IS_FOLIA) {
            try {
                return (BukkitTask) globalRegionScheduler_runAtFixedRate.invoke(globalRegionScheduler, plugin, task, delay, period);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("Failed to run timer task on Folia", e);
            }
        } else {
            return Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period);
        }
    }

    /**
     * Run a task at a specific location (region-based in Folia)
     */
    public static BukkitTask runTask(Plugin plugin, Location location, Runnable task) {
        if (IS_FOLIA) {
            try {
                return (BukkitTask) regionScheduler_run.invoke(regionScheduler, plugin, location, task);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("Failed to run location-based task on Folia", e);
            }
        } else {
            return Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    /**
     * Run a delayed task at a specific location (region-based in Folia)
     */
    public static BukkitTask runTaskLater(Plugin plugin, Location location, Runnable task, long delay) {
        if (IS_FOLIA) {
            try {
                return (BukkitTask) regionScheduler_runDelayed.invoke(regionScheduler, plugin, location, task, delay);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("Failed to run delayed location-based task on Folia", e);
            }
        } else {
            return Bukkit.getScheduler().runTaskLater(plugin, task, delay);
        }
    }

    /**
     * Run a repeating task at a specific location (region-based in Folia)
     */
    public static BukkitTask runTaskTimer(Plugin plugin, Location location, Runnable task, long delay, long period) {
        if (IS_FOLIA) {
            try {
                return (BukkitTask) regionScheduler_runAtFixedRate.invoke(regionScheduler, plugin, location, task, delay, period);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("Failed to run timer location-based task on Folia", e);
            }
        } else {
            return Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period);
        }
    }

    /**
     * Run a task for an entity (entity scheduler in Folia)
     */
    public static BukkitTask runTask(Plugin plugin, Entity entity, Runnable task) {
        if (IS_FOLIA) {
            try {
                Object entityScheduler = entity.getClass().getMethod("getScheduler").invoke(entity);
                return (BukkitTask) entityScheduler_run.invoke(entityScheduler, plugin, task, null);
            } catch (Exception e) {
                throw new RuntimeException("Failed to run entity task on Folia", e);
            }
        } else {
            return Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    /**
     * Run a delayed task for an entity (entity scheduler in Folia)
     */
    public static BukkitTask runTaskLater(Plugin plugin, Entity entity, Runnable task, long delay) {
        if (IS_FOLIA) {
            try {
                Object entityScheduler = entity.getClass().getMethod("getScheduler").invoke(entity);
                return (BukkitTask) entityScheduler_runDelayed.invoke(entityScheduler, plugin, task, null, delay);
            } catch (Exception e) {
                throw new RuntimeException("Failed to run delayed entity task on Folia", e);
            }
        } else {
            return Bukkit.getScheduler().runTaskLater(plugin, task, delay);
        }
    }

    /**
     * Run a repeating task for an entity (entity scheduler in Folia)
     */
    public static BukkitTask runTaskTimer(Plugin plugin, Entity entity, Runnable task, long delay, long period) {
        if (IS_FOLIA) {
            try {
                Object entityScheduler = entity.getClass().getMethod("getScheduler").invoke(entity);
                return (BukkitTask) entityScheduler_runAtFixedRate.invoke(entityScheduler, plugin, task, null, delay, period);
            } catch (Exception e) {
                throw new RuntimeException("Failed to run timer entity task on Folia", e);
            }
        } else {
            return Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period);
        }
    }

    /**
     * Run an async task
     */
    public static BukkitTask runTaskAsynchronously(Plugin plugin, Runnable task) {
        if (IS_FOLIA) {
            try {
                return (BukkitTask) asyncScheduler_runNow.invoke(asyncScheduler, plugin, task);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("Failed to run async task on Folia", e);
            }
        } else {
            return Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        }
    }

    /**
     * Run an async task with delay
     */
    public static BukkitTask runTaskLaterAsynchronously(Plugin plugin, Runnable task, long delay) {
        if (IS_FOLIA) {
            try {
                return (BukkitTask) asyncScheduler_runDelayed.invoke(asyncScheduler, plugin, task, delay * 50L, TimeUnit.MILLISECONDS);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("Failed to run delayed async task on Folia", e);
            }
        } else {
            return Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delay);
        }
    }

    /**
     * Run a repeating async task
     */
    public static BukkitTask runTaskTimerAsynchronously(Plugin plugin, Runnable task, long delay, long period) {
        if (IS_FOLIA) {
            try {
                return (BukkitTask) asyncScheduler_runAtFixedRate.invoke(asyncScheduler, plugin, task, delay * 50L, period * 50L, TimeUnit.MILLISECONDS);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("Failed to run timer async task on Folia", e);
            }
        } else {
            return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delay, period);
        }
    }

    /**
     * Convenience methods using OraxenPlugin instance
     */
    public static BukkitTask runTask(Runnable task) {
        return runTask(OraxenPlugin.get(), task);
    }

    public static BukkitTask runTaskLater(Runnable task, long delay) {
        return runTaskLater(OraxenPlugin.get(), task, delay);
    }

    public static BukkitTask runTaskTimer(Runnable task, long delay, long period) {
        return runTaskTimer(OraxenPlugin.get(), task, delay, period);
    }

    public static BukkitTask runTask(Location location, Runnable task) {
        return runTask(OraxenPlugin.get(), location, task);
    }

    public static BukkitTask runTaskLater(Location location, Runnable task, long delay) {
        return runTaskLater(OraxenPlugin.get(), location, task, delay);
    }

    public static BukkitTask runTaskTimer(Location location, Runnable task, long delay, long period) {
        return runTaskTimer(OraxenPlugin.get(), location, task, delay, period);
    }

    public static BukkitTask runTask(Entity entity, Runnable task) {
        return runTask(OraxenPlugin.get(), entity, task);
    }

    public static BukkitTask runTaskLater(Entity entity, Runnable task, long delay) {
        return runTaskLater(OraxenPlugin.get(), entity, task, delay);
    }

    public static BukkitTask runTaskTimer(Entity entity, Runnable task, long delay, long period) {
        return runTaskTimer(OraxenPlugin.get(), entity, task, delay, period);
    }

    public static BukkitTask runTaskAsynchronously(Runnable task) {
        return runTaskAsynchronously(OraxenPlugin.get(), task);
    }

    public static BukkitTask runTaskLaterAsynchronously(Runnable task, long delay) {
        return runTaskLaterAsynchronously(OraxenPlugin.get(), task, delay);
    }

    public static BukkitTask runTaskTimerAsynchronously(Runnable task, long delay, long period) {
        return runTaskTimerAsynchronously(OraxenPlugin.get(), task, delay, period);
    }
}