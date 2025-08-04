package com.jedk1.jedcore.command;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.jedk1.jedcore.JedCore;
import com.projectkorra.projectkorra.command.PKCommand;

public class JedCoreCommand extends PKCommand {

	private static final String DOWNLOAD_URL = "https://github.com/Hihelloy-main/JedCore";

	private static final Set<UUID> DEVELOPERS = Set.of(
			UUID.fromString("4eb6315e-9dd1-49f7-b582-c1170e497ab0"), // jedk1
			UUID.fromString("d57565a5-e6b0-44e3-a026-979d5de10c4d"), // s3xi
			UUID.fromString("e98a2f7d-d571-4900-a625-483cbe6774fe"), // Aztl
			UUID.fromString("b1318b21-5956-445c-a328-bad3175c1c7a")  // Hihelloy
	);

	public JedCoreCommand() {
		super("jedcore", "/bending jedcore", "This command shows the JedCore version and debug info.", new String[] { "jedcore", "jc" });
	}

	@Override
	public void execute(CommandSender sender, List<String> args) {
		if (!correctLength(sender, args.size(), 0, 1) || (!hasPermission(sender) && !isSenderJedCoreDev(sender))) {
			return;
		}

		if (args.isEmpty()) {
			sendBuildInfo(sender);
			return;
		}

		if (args.size() == 1 && (hasPermission(sender, "debug") || isSenderJedCoreDev(sender))) {
			if (args.get(0).equalsIgnoreCase("refresh")) {
				sender.sendMessage(ChatColor.AQUA + "JedCore refreshed.");
				return;
			}
		}

		help(sender, false);
	}

	public static void sendBuildInfo(CommandSender sender) {
		sender.sendMessage(ChatColor.GRAY + "Running JedCore Build: " + ChatColor.RED + JedCore.plugin.getDescription().getVersion());
		sender.sendMessage(ChatColor.GRAY + "Developed by: " + ChatColor.RED + String.join(", ", JedCore.plugin.getDescription().getAuthors()));
		sender.sendMessage(ChatColor.GRAY + "Modified by: " + ChatColor.RED + "plushmonkey");
		sender.sendMessage(ChatColor.GRAY + "Maintained by: " + ChatColor.RED + "Cozmyc");
		sender.sendMessage(ChatColor.GRAY + "Updated by: " + ChatColor.RED + "Hihelloy");
		sender.sendMessage(ChatColor.GRAY + "URL: " + ChatColor.RED + ChatColor.ITALIC + DOWNLOAD_URL);
	}

	private boolean isSenderJedCoreDev(CommandSender sender) {
		if (sender instanceof Player player) {
			return DEVELOPERS.contains(player.getUniqueId());
		}
		return false;
	}
}
