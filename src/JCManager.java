package com.jedk1.jedcore;

import org.bukkit.Bukkit;

 import com.jedk1.jedcore.ability.firebending.LightningBurst;
 import com.jedk1.jedcore.ability.waterbending.HealingWaters;
 import com.jedk1.jedcore.ability.waterbending.IcePassive;
 import com.jedk1.jedcore.util.RegenTempBlock;

public class JCManager implements Runnable {

	public JedCore plugin;
	
	public JCManager(JedCore plugin) {
		this.plugin = plugin;
	}
	
	public void run() {
		LightningBurst.progressAll();
		
		HealingWaters.heal(Bukkit.getServer());
		IcePassive.handleSkating();
//		IceWall.progressAll();
		
		RegenTempBlock.manage();
	}
}