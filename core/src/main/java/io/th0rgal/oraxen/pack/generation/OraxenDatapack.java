package io.th0rgal.oraxen.pack.generation;

import io.th0rgal.oraxen.utils.VirtualFile;
import io.th0rgal.oraxen.utils.VersionUtil;

import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.packs.DataPack;

import com.google.gson.JsonObject;

import net.kyori.adventure.key.Key;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public abstract class OraxenDatapack {
    
    private static World getDefaultWorld() {
        List<World> worlds = Bukkit.getWorlds();
        if (worlds.isEmpty()) {
            throw new IllegalStateException("No worlds available - server may not be fully initialized");
        }
        return worlds.get(0);
    }
    
    protected static World getDefaultWorldSafe() {
        int maxRetries = 20; // 增加重試次數
        int retryDelay = 50; // 減少初始延遲
        
        for (int i = 0; i < maxRetries; i++) {
            try {
                return getDefaultWorld();
            } catch (IllegalStateException e) {
                if (i == maxRetries - 1) {
                    // 最後一次嘗試失敗，拋出異常
                    throw new RuntimeException("Failed to get world after " + maxRetries + " attempts. Server may not be fully initialized.", e);
                }
                
                // 等待後重試
                try {
                    Thread.sleep(retryDelay);
                    retryDelay = Math.min(retryDelay * 2, 1000); // 指數退避，最大1秒
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while waiting for world initialization", ie);
                }
            }
        }
        
        throw new RuntimeException("Unexpected error in getDefaultWorldSafe");
    }
    
    protected File datapackFolder;
    protected final JsonObject datapackMeta = new JsonObject();
    protected final boolean isFirstInstall;
    protected final boolean datapackEnabled;
    protected final String name;

    protected OraxenDatapack(String name, String description, int packFormat) {
        // 延遲初始化，避免在 Folia 上過早訪問世界
        this.datapackFolder = null;

        JsonObject data = new JsonObject();
        data.addProperty("description", description);
        data.addProperty("pack_format", packFormat);
        datapackMeta.add("pack", data);

        this.name = name;
        this.isFirstInstall = isFirstInstall();
        this.datapackEnabled = isDatapackEnabled();
    }
    
    protected void initializeDatapackFolder() {
        if (datapackFolder == null) {
            World world = getDefaultWorldSafe();
            datapackFolder = world.getWorldFolder().toPath()
                    .resolve("datapacks/" + name).toFile();
        }
    }

    protected void writeMCMeta() {
        initializeDatapackFolder();
        try {
            File packMeta = datapackFolder.toPath().resolve("pack.mcmeta").toFile();
            packMeta.createNewFile();
            FileUtils.writeStringToFile(packMeta, datapackMeta.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void clearOldDataPack() {
        initializeDatapackFolder();
        try {
            FileUtils.deleteDirectory(datapackFolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected abstract Key getDatapackKey();

    public abstract void generateAssets(List<VirtualFile> output);

    protected boolean isFirstInstall() {
        return Bukkit.getDataPackManager().getDataPacks().stream()
                .filter(d -> d.getKey() != null)
                .noneMatch(d -> getDatapackKey().equals(Key.key(d.getKey().toString())));
    }

    protected boolean isDatapackEnabled() {
        World world = getDefaultWorldSafe();
        for (DataPack dataPack : Bukkit.getDataPackManager().getEnabledDataPacks(world)) {
            if (dataPack.getKey() == null)
                continue;
            if (dataPack.getKey().equals(getDatapackKey()))
                return true;
        }
        for (DataPack dataPack : Bukkit.getDataPackManager().getDisabledDataPacks(world)) {
            if (dataPack.getKey() == null)
                continue;
            if (dataPack.getKey().equals(getDatapackKey()))
                return true;
        }
        return false;
    }

    protected void enableDatapack(boolean enabled) {
        if (VersionUtil.isPaperServer()) {
            Bukkit.getDatapackManager().getPacks().stream()
                    .filter(d -> d.getName() == this.name)
                    .findFirst()
                    .ifPresent(d -> d.setEnabled(enabled));
        }
    }

    public boolean isEnabled() {
        return datapackEnabled;
    }

    public boolean isFirstTime() {
        return isFirstInstall;
    }
}
