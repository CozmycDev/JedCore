package com.jedk1.jedcore;

import com.jedk1.jedcore.ability.earthbending.EarthPillar;
import com.jedk1.jedcore.ability.firebending.LightningBurst;
import com.jedk1.jedcore.ability.waterbending.HealingWaters;
import com.jedk1.jedcore.ability.waterbending.IceWall;
import com.jedk1.jedcore.ability.waterbending.passive.IcePassive;
import com.jedk1.jedcore.util.RegenTempBlock;
import com.jedk1.jedcore.util.ThrownEntityTracker;
import org.bukkit.Bukkit;

public class JCManager implements Runnable {

	public JedCore plugin;
	
	public JCManager(JedCore plugin) {
		this.plugin = plugin;
	}
	
	public void run() {
		LightningBurst.progressAll();
		HealingWaters.heal(Bukkit.getServer());
		IcePassive.handleSkating();
		IceWall.progressAll();
		EarthPillar.progressAll();
		ThrownEntityTracker.updateAll();
		RegenTempBlock.manage();
	}
}