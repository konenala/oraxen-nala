package io.th0rgal.oraxen.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.config.ResourcesManager;
import io.th0rgal.oraxen.utils.AdventureUtils;
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
                    for (final Player target : targets)
                        OraxenPlugin.get().getUploadManager().getSender().sendPack(target);
                });
    }

    private CommandAPICommand sendPackMessage() {
        return new CommandAPICommand("msg")
                .withOptionalArguments(new EntitySelectorArgument.ManyPlayers("targets"))
                .executes((sender, args) -> {
                    final Collection<Player> targets = resolveTargets(sender, args.getOptional("targets").orElse(null));
                    for (final Player target : targets)
                        Message.COMMAND_JOIN_MESSAGE.send(target, AdventureUtils.tagResolver("pack_url",
                                (OraxenPlugin.get().getUploadManager().getHostingProvider().getPackURL())));
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
