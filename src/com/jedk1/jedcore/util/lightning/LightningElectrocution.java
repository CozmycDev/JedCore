package com.jedk1.jedcore.util.lightning;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.util.MovementHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public final class LightningElectrocution {

	private LightningElectrocution() {
	}

	public static void apply(LivingEntity target, CoreAbility ability, double durationMillis) {
		if (target == null || target.isDead() || ability == null) {
			return;
		}

		Player caster = ability.getPlayer();
		if (caster != null && caster.equals(target)) {
			return;
		}

		if (target instanceof Player) {
			BendingPlayer bPlayer = BendingPlayer.getBendingPlayer((Player) target);
			if (bPlayer != null && bPlayer.isAvatarState()) {
				return;
			}
		}

		if (MovementHandler.isStopped(target)) {
			return;
		}

		MovementHandler handler = new MovementHandler(target, ability);
		handler.stopWithDuration(toTicks(durationMillis), Element.LIGHTNING.getColor() + "* Electrocuted *");
	}

	public static long toTicks(double durationMillis) {
		return Math.max(1L, Math.round(durationMillis / 50.0));
	}
}
