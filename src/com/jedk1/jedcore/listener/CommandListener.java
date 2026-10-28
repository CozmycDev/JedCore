package com.jedk1.jedcore.listener;

import com.jedk1.jedcore.JedCore;
import com.jedk1.jedcore.command.JedCoreCommand;
import com.jedk1.jedcore.event.PKCommandEvent;
import com.jedk1.jedcore.event.PKCommandEvent.CommandType;
import com.projectkorra.projectkorra.command.PKCommand;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;

public class CommandListener implements Listener {

	JedCore plugin;
	String[] cmdaliases = {"/bending", "/bend", "/b", "/pk", "/projectkorra", "/korra", "/mtla", "/tla"};

	public CommandListener(JedCore plugin) {
		this.plugin = plugin;
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
		String cmd = event.getMessage().toLowerCase();
		String[] args = cmd.split("\\s+");
		if (Arrays.asList(cmdaliases).contains(args[0]) && args.length >= 2) {
			PKCommandEvent new_event = new PKCommandEvent(event.getPlayer(), args, null);
			for (PKCommand command : PKCommand.instances.values()) {
				if (Arrays.asList(command.getAliases()).contains(args[1].toLowerCase())) {
					new_event = new PKCommandEvent(event.getPlayer(), args, CommandType.getType(command.getName()));
				}
			}
			Bukkit.getServer().getPluginManager().callEvent(new_event);
		}
	}

	CommandType[] types = {CommandType.ADD, CommandType.BIND, CommandType.CHOOSE, CommandType.CLEAR, CommandType.PRESET, CommandType.REMOVE};

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPKCommand(final PKCommandEvent event) {
		new BukkitRunnable() {
			public void run() {
				if (event.getType() != null) {
					if (event.getType().equals(CommandType.VERSION) && event.getSender().hasPermission("bending.command.version")) {
						JedCoreCommand.sendBuildInfo(event.getSender());
					}
				}
			}
		}.runTaskLater(JedCore.plugin, 1);
	}
}
