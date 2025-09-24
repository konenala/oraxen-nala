package io.th0rgal.oraxen.utils.scheduler;

import io.th0rgal.oraxen.OraxenPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.TimeUnit;

/**
 * Oraxen Scheduler utility for Folia compatibility
 * Automatically detects if running on Folia or regular Bukkit/Spigot
 * and uses the appropriate scheduling methods.
 */
public class OraxenScheduler {

    private static final boolean IS_FOLIA;

    static {
        boolean isFolia = false;
        try {
            Class.forName("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler");
            isFolia = true;
        } catch (ClassNotFoundException e) {
            // Not Folia
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
            // Folia returns ScheduledTask, we need to wrap it
            return new FoliaTaskWrapper(Bukkit.getGlobalRegionScheduler().run(plugin, (scheduledTask) -> task.run()));
        } else {
            try {
                return Bukkit.getScheduler().runTask(plugin, task);
            } catch (UnsupportedOperationException e) {
                // Lophine/Folia with disabled Bukkit scheduler - run directly
                io.th0rgal.oraxen.utils.logs.Logs.logWarning("Bukkit scheduler disabled, running task directly: " + e.getMessage());
                task.run();
                return null;
            }
        }
    }

    /**
     * Run a task with delay on the main thread (or global region in Folia)
     */
    public static BukkitTask runTaskLater(Plugin plugin, Runnable task, long delay) {
        if (IS_FOLIA) {
            return new FoliaTaskWrapper(Bukkit.getGlobalRegionScheduler().runDelayed(plugin, (scheduledTask) -> task.run(), delay));
        } else {
            try {
                return Bukkit.getScheduler().runTaskLater(plugin, task, delay);
            } catch (UnsupportedOperationException e) {
                // Lophine/Folia with disabled Bukkit scheduler - run directly
                io.th0rgal.oraxen.utils.logs.Logs.logWarning("Bukkit scheduler disabled, running delayed task directly: " + e.getMessage());
                task.run();
                return null;
            }
        }
    }

    /**
     * Run a repeating task on the main thread (or global region in Folia)
     */
    public static BukkitTask runTaskTimer(Plugin plugin, Runnable task, long delay, long period) {
        if (IS_FOLIA) {
            return new FoliaTaskWrapper(Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, (scheduledTask) -> task.run(), delay, period));
        } else {
            try {
                return Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period);
            } catch (UnsupportedOperationException e) {
                // Lophine/Folia with disabled Bukkit scheduler - run directly
                io.th0rgal.oraxen.utils.logs.Logs.logWarning("Bukkit scheduler disabled, running timer task directly: " + e.getMessage());
                task.run();
                return null;
            }
        }
    }

    /**
     * Run a task at a specific location (region-based in Folia)
     */
    public static BukkitTask runTask(Plugin plugin, Location location, Runnable task) {
        if (IS_FOLIA) {
            return new FoliaTaskWrapper(Bukkit.getRegionScheduler().run(plugin, location, (scheduledTask) -> task.run()));
        } else {
            return Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    /**
     * Run a delayed task at a specific location (region-based in Folia)
     */
    public static BukkitTask runTaskLater(Plugin plugin, Location location, Runnable task, long delay) {
        if (IS_FOLIA) {
            return new FoliaTaskWrapper(Bukkit.getRegionScheduler().runDelayed(plugin, location, (scheduledTask) -> task.run(), delay));
        } else {
            return Bukkit.getScheduler().runTaskLater(plugin, task, delay);
        }
    }

    /**
     * Run a repeating task at a specific location (region-based in Folia)
     */
    public static BukkitTask runTaskTimer(Plugin plugin, Location location, Runnable task, long delay, long period) {
        if (IS_FOLIA) {
            return new FoliaTaskWrapper(Bukkit.getRegionScheduler().runAtFixedRate(plugin, location, (scheduledTask) -> task.run(), delay, period));
        } else {
            return Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period);
        }
    }

    /**
     * Run a task for an entity (entity scheduler in Folia)
     */
    public static BukkitTask runTask(Plugin plugin, Entity entity, Runnable task) {
        if (IS_FOLIA) {
            return new FoliaTaskWrapper(entity.getScheduler().run(plugin, (scheduledTask) -> task.run(), null));
        } else {
            return Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    /**
     * Run a delayed task for an entity (entity scheduler in Folia)
     */
    public static BukkitTask runTaskLater(Plugin plugin, Entity entity, Runnable task, long delay) {
        if (IS_FOLIA) {
            return new FoliaTaskWrapper(entity.getScheduler().runDelayed(plugin, (scheduledTask) -> task.run(), null, delay));
        } else {
            return Bukkit.getScheduler().runTaskLater(plugin, task, delay);
        }
    }

    /**
     * Run a repeating task for an entity (entity scheduler in Folia)
     */
    public static BukkitTask runTaskTimer(Plugin plugin, Entity entity, Runnable task, long delay, long period) {
        if (IS_FOLIA) {
            return new FoliaTaskWrapper(entity.getScheduler().runAtFixedRate(plugin, (scheduledTask) -> task.run(), null, delay, period));
        } else {
            return Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period);
        }
    }

    /**
     * Run a task asynchronously
     */
    public static BukkitTask runTaskAsynchronously(Plugin plugin, Runnable task) {
        if (IS_FOLIA) {
            return new FoliaTaskWrapper(Bukkit.getAsyncScheduler().runNow(plugin, (scheduledTask) -> task.run()));
        } else {
            try {
                return Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
            } catch (UnsupportedOperationException e) {
                // Lophine/Folia with disabled Bukkit scheduler - run directly
                io.th0rgal.oraxen.utils.logs.Logs.logWarning("Bukkit scheduler disabled, running async task directly: " + e.getMessage());
                task.run();
                return null;
            }
        }
    }

    /**
     * Run a delayed task asynchronously
     */
    public static BukkitTask runTaskLaterAsynchronously(Plugin plugin, Runnable task, long delay) {
        if (IS_FOLIA) {
            return new FoliaTaskWrapper(Bukkit.getAsyncScheduler().runDelayed(plugin, (scheduledTask) -> task.run(), delay, TimeUnit.MILLISECONDS));
        } else {
            try {
                return Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delay);
            } catch (UnsupportedOperationException e) {
                // Lophine/Folia with disabled Bukkit scheduler - run directly
                io.th0rgal.oraxen.utils.logs.Logs.logWarning("Bukkit scheduler disabled, running delayed async task directly: " + e.getMessage());
                task.run();
                return null;
            }
        }
    }

    /**
     * Run a repeating task asynchronously
     */
    public static BukkitTask runTaskTimerAsynchronously(Plugin plugin, Runnable task, long delay, long period) {
        if (IS_FOLIA) {
            return new FoliaTaskWrapper(Bukkit.getAsyncScheduler().runAtFixedRate(plugin, (scheduledTask) -> task.run(), delay, period, TimeUnit.MILLISECONDS));
        } else {
            try {
                return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delay, period);
            } catch (UnsupportedOperationException e) {
                // Lophine/Folia with disabled Bukkit scheduler - run directly
                io.th0rgal.oraxen.utils.logs.Logs.logWarning("Bukkit scheduler disabled, running timer async task directly: " + e.getMessage());
                task.run();
                return null;
            }
        }
    }

    /**
     * Wrapper class to convert Folia's ScheduledTask to BukkitTask
     */
    private static class FoliaTaskWrapper implements BukkitTask {
        private final Object scheduledTask;

        public FoliaTaskWrapper(Object scheduledTask) {
            this.scheduledTask = scheduledTask;
        }

        @Override
        public int getTaskId() {
            return scheduledTask.hashCode();
        }

        @Override
        public Plugin getOwner() {
            return OraxenPlugin.get();
        }

        @Override
        public boolean isSync() {
            return false; // Folia tasks are generally async
        }

        @Override
        public boolean isCancelled() {
            try {
                return (boolean) scheduledTask.getClass().getMethod("isCancelled").invoke(scheduledTask);
            } catch (Exception e) {
                return false;
            }
        }

        @Override
        public void cancel() {
            try {
                scheduledTask.getClass().getMethod("cancel").invoke(scheduledTask);
            } catch (Exception e) {
                // Ignore
            }
        }
    }
}