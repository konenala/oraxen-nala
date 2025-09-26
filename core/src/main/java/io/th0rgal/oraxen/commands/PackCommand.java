package io.th0rgal.oraxen.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenPack;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.config.ResourcesManager;
import io.th0rgal.oraxen.pack.dispatch.PackSender;
import io.th0rgal.oraxen.pack.upload.UploadManager;
import io.th0rgal.oraxen.utils.AdventureUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class PackCommand {

    @SuppressWarnings("unchecked")
    CommandAPICommand getPackCommand() {
        return new CommandAPICommand("pack")
                .withPermission("oraxen.command.pack")
                .withSubcommand(sendPackCommand())
                .withSubcommand(sendPackMessage())
                .withSubcommand(extractDefaultPackContent());

    }

    private CommandAPICommand sendPackCommand() {
        return new CommandAPICommand("send")
                .withPermission("oraxen.command.pack.send")
                .withOptionalArguments(new EntitySelectorArgument.ManyPlayers("targets"))
                .executes((sender, args) -> {
                    final Collection<Player> targets = resolveTargets(sender, args.getOptional("targets").orElse(null));
                    final var packSender = extractPackSender(sender);
                    if (packSender == null) {
                        return;
                    }
                    for (final Player target : targets)
                        packSender.sendPack(target);
                });
    }

    private CommandAPICommand sendPackMessage() {
        return new CommandAPICommand("msg")
                .withOptionalArguments(new EntitySelectorArgument.ManyPlayers("targets"))
                .executes((sender, args) -> {
                    final Collection<Player> targets = resolveTargets(sender, args.getOptional("targets").orElse(null));
                    final String packUrl = extractPackUrl(); // 資源包連結，可能尚未生成
                    if (packUrl == null || packUrl.isBlank()) {
                        Message.PACK_NOT_UPLOADED.send(sender);
                        return;
                    }
                    final var packResolver = AdventureUtils.tagResolver("pack_url", packUrl);
                    for (final Player target : targets)
                        Message.COMMAND_JOIN_MESSAGE.send(target, packResolver);
                });
    }

    /**
     * 將指令參數組合成合法的玩家集合。
     * @param sender 指令來源
     * @param rawTargets 可能為多名玩家的集合，或缺省
     * @return 可安全迭代的玩家集合（若無目標則為空集合）
     */
    @SuppressWarnings("unchecked")
    private Collection<Player> resolveTargets(Object sender, Object rawTargets) {
        if (rawTargets instanceof Collection<?> collection) {
            return (Collection<Player>) collection;
        }
        if (sender instanceof Player player) {
            return Collections.singletonList(player);
        }
        return Collections.emptyList();
    }

    /**
     * 取得目前設定好的資源包 URL。
     * @return 若尚未上傳則回傳 null
     */
    private String extractPackUrl() {
        if (OraxenPlugin.get().getUploadManager() == null
            || OraxenPlugin.get().getUploadManager().getHostingProvider() == null) {
            return null;
        }
        return OraxenPlugin.get().getUploadManager().getHostingProvider().getPackURL();
    }

    /**
     * 取得可用的 PackSender，若尚未初始化則嘗試觸發上傳並提示。
     * @param sender 指令來源
     * @return PackSender 或 null（已顯示提示訊息）
     */
    private PackSender extractPackSender(Object sender) {
        CommandSender commandSender = sender instanceof CommandSender ? (CommandSender) sender : null;
        UploadManager uploadManager = OraxenPlugin.get().getUploadManager();
        if (uploadManager == null) {
            OraxenPack.uploadPack();
            if (commandSender != null) {
                Message.PACK_UPLOADING.send(commandSender);
            }
            return null;
        }
        PackSender packSender = uploadManager.getSender();
        if (packSender == null) {
            if (commandSender != null) {
                Message.PACK_NOT_UPLOADED.send(commandSender);
            }
            uploadManager.uploadAsyncAndSendToPlayers(OraxenPlugin.get().getResourcePack(), false, false);
            return null;
        }
        return packSender;
    }

    private CommandAPICommand extractDefaultPackContent() {
        return new CommandAPICommand("extract_default")
                .withOptionalArguments(new TextArgument("folder").replaceSuggestions(ArgumentSuggestions.strings("all", "textures", "models", "sounds")))
                .withOptionalArguments(new BooleanArgument("override"))
                .executes((sender, args) -> {
                    final String type = (String) args.getOptional("folder").orElse("all");
                    final ZipInputStream zip = ResourcesManager.browse();
                    try {
                        ZipEntry entry = zip.getNextEntry();
                        while (entry != null) {
                            extract(entry, type, OraxenPlugin.get().getResourceManager(), (Boolean) args.getOptional("override").orElse(false));
                            entry = zip.getNextEntry();
                        }
                        zip.closeEntry();
                        zip.close();
                    } catch (final IOException ex) {
                        ex.printStackTrace();
                    }

                });
    }

    private void extract(ZipEntry entry, String type, ResourcesManager resourcesManager, boolean override) {
        if (!entry.getName().startsWith("pack/" + (type.equals("all") ? "" : type))) return;
        resourcesManager.extractFileIfTrue(entry, !OraxenPlugin.get().getDataFolder().toPath().resolve(entry.getName()).toFile().exists() || override);
    }
}
