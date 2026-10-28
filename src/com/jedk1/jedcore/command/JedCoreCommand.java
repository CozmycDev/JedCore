package com.jedk1.jedcore.command;

import com.jedk1.jedcore.JedCore;
import com.projectkorra.projectkorra.command.PKCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.List;

public class JedCoreCommand extends PKCommand {
	private static final String DOWNLOAD_URL = "https://github.com/CozmycDev/JedCore";

	public JedCoreCommand() {
		super("jedcore", "/bending jedcore", "This command will show the statistics and version of JedCore.", new String[] { "jedcore", "jc" });
	}

	@Override
	public void execute(CommandSender sender, List<String> args) {
		if (!correctLength(sender, args.size(), 0, 1) || !hasPermission(sender)) {
			return;
		}
		if (args.size() == 0) {
			sendBuildInfo(sender);
		} else if (args.size() == 1 && hasPermission(sender, "debug")) {
			//Dev commands for debugging etc.
			if (args.get(0).equalsIgnoreCase("refresh")) {
				sender.sendMessage(ChatColor.AQUA + "Jedcore refreshed.");
			}
		} else {
			help(sender, false);
		}
	}

	public static void sendBuildInfo(CommandSender sender) {
		sender.sendMessage(ChatColor.GRAY + "Running JedCore Build: " + ChatColor.RED + JedCore.plugin.getDescription().getVersion());
		sender.sendMessage(ChatColor.GRAY + "Developed by: " + ChatColor.RED + JedCore.plugin.getDescription().getAuthors().toString().replace("[", "").replace("]", ""));
		sender.sendMessage(ChatColor.GRAY + "Modified by: " + ChatColor.RED + "plushmonkey");
		sender.sendMessage(ChatColor.GRAY + "Maintained by: " + ChatColor.RED + "Cozmyc");
		sender.sendMessage(ChatColor.GRAY + "URL: " + ChatColor.RED + ChatColor.ITALIC + DOWNLOAD_URL);
	}
}
