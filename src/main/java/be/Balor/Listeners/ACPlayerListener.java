/************************************************************************
 * This file is part of AdminCmd.
 *
 * AdminCmd is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AdminCmd is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AdminCmd.  If not, see <http://www.gnu.org/licenses/>.
 ************************************************************************/
package be.Balor.Listeners;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;

import be.Balor.Manager.CommandManager;
import be.Balor.Manager.Permissions.PermissionManager;
import be.Balor.Player.ACPlayer;
import be.Balor.Player.BannedPlayer;
import be.Balor.Player.PlayerManager;
import be.Balor.Tools.ShootFireball;
import be.Balor.Tools.Type;
import be.Balor.Tools.UpdateInvisible;
import be.Balor.Tools.Utils;
import be.Balor.World.ACWorld;
import be.Balor.bukkit.AdminCmd.ACHelper;
import be.Balor.bukkit.AdminCmd.ACPluginManager;
import belgium.Balor.Workers.AFKWorker;
import belgium.Balor.Workers.InvisibleWorker;

/**
 * @author Balor (aka Antoine Aflalo)
 * 
 */
public class ACPlayerListener extends PlayerListener {
	@Override
	public void onPlayerLogin(PlayerLoginEvent event) {
		BannedPlayer player = ACHelper.getInstance().isBanned(event.getPlayer().getName());
		if (player != null) {
			event.disallow(Result.KICK_BANNED, player.getReason());
			return;
		}
		if (ACHelper.getInstance().isServerLocked()
				&& !PermissionManager.hasPerm(event.getPlayer(), "admincmd.server.lockdown", false)) {
			event.disallow(Result.KICK_OTHER, "Server is Locked.");
			return;
		}
		if (PermissionManager.hasPerm(event.getPlayer(), "admincmd.player.bypass", false))
			event.allow();
	}

	@Override
	public void onPlayerMove(PlayerMoveEvent event) {
		Player p = event.getPlayer();
		if (ACHelper.getInstance().getConfBoolean("autoAfk")) {
			AFKWorker.getInstance().updateTimeStamp(p);
			if (AFKWorker.getInstance().isAfk(p))
				AFKWorker.getInstance().setOnline(p);
		}
		ACPlayer player = ACPlayer.getPlayer(p.getName());
		if (player.hasPower(Type.FROZEN)) {
			// event.setCancelled(true);
			/**
			 * https://github.com/Bukkit/CraftBukkit/pull/434
			 * 
			 * @author Evenprime
			 */
			((CraftPlayer) p).getHandle().netServerHandler.teleport(event.getFrom());
			return;
		}
		Float power = player.getPower(Type.FLY).getFloat(0);
		if (power != 0)
			if (p.isSneaking())
				p.setVelocity(p.getLocation().getDirection().multiply(power));
			else if (ACHelper.getInstance().getConfBoolean("glideWhenFallingInFlyMode")) {
				Vector vel = p.getVelocity();
				vel.add(p.getLocation().getDirection()
						.multiply(ACHelper.getInstance().getConfFloat("gliding.multiplicator"))
						.setY(0));
				if (vel.getY() < ACHelper.getInstance().getConfFloat(
						"gliding.YvelocityCheckToGlide")) {
					vel.setY(ACHelper.getInstance().getConfFloat("gliding.newYvelocity"));
					p.setVelocity(vel);
				}
			}
	}

	@Override
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player p = event.getPlayer();
		PlayerManager.getInstance().setOnline(p.getName());
		if (playerRespawnOrJoin(event.getPlayer())) {
			event.setJoinMessage(null);
			Utils.sI18n(event.getPlayer(), "stillInv");
		}
		ACPlayer player = ACPlayer.getPlayer(p.getName());
		player.setInformation("immunityLvl", ACHelper.getInstance().getLimit(p, "immunityLvl"));
		if (player.hasPower(Type.FAKEQUIT))
			event.setJoinMessage(null);
		if (player.getInformation("firstTime").getBoolean(true)) {
			player.setInformation("firstTime", false);
			if (ACHelper.getInstance().getConfBoolean("firstConnectionToSpawnPoint"))
				ACHelper.getInstance().spawn(p);
			if (ACHelper.getInstance().getConfBoolean("DisplayRulesOnlyOnFirstJoin"))
				Utils.sParsedLocale(p, "Rules");
			if (ACHelper.getInstance().getConfBoolean("MessageOfTheDay"))
				Utils.sParsedLocale(p, "MOTDNewUser");
		} else if (ACHelper.getInstance().getConfBoolean("MessageOfTheDay"))
			Utils.sParsedLocale(p, "MOTD");
		player.setInformation("lastConnection", System.currentTimeMillis());

		if (ACHelper.getInstance().getConfBoolean("DisplayNewsOnJoin"))
			Utils.sParsedLocale(p, "NEWS");
		if (ACHelper.getInstance().getConfBoolean("DisplayRulesOnJoin")
				&& !ACHelper.getInstance().getConfBoolean("DisplayRulesOnlyOnFirstJoin"))
			Utils.sParsedLocale(p, "Rules");
		if (ACHelper.getInstance().getConfBoolean("tpRequestActivatedByDefault")
				&& !player.hasPower(Type.TP_REQUEST)
				&& PermissionManager.hasPerm(p, "admincmd.tp.toggle", false))
			player.setPower(Type.TP_REQUEST);
	}

	@Override
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player p = event.getPlayer();
		ACPlayer player = ACPlayer.getPlayer(p.getName());
		player.setInformation("immunityLvl", ACHelper.getInstance().getLimit(p, "immunityLvl"));
		PlayerManager.getInstance().setOffline(ACPlayer.getPlayer(p.getName()));
		if (player.hasPower(Type.FAKEQUIT))
			event.setQuitMessage(null);
		else if (InvisibleWorker.getInstance().hasInvisiblePowers(p.getName()))
			event.setQuitMessage(null);
			AFKWorker.getInstance().removePlayer(p);
	}

	@Override
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		Player player = event.getPlayer();
		playerRespawnOrJoin(player);
		if (ACHelper.getInstance().getConfBoolean("respawnAtSpawnPoint")) {
			Location loc = null;
			String worldName = player.getWorld().getName();
			loc = ACWorld.getWorld(worldName).getSpawn();
			if (loc == null)
				loc = player.getWorld().getSpawnLocation();
			event.setRespawnLocation(loc);
		}

	}

	@Override
	public void onPlayerTeleport(PlayerTeleportEvent event) {
		if (event.isCancelled())
			return;
		Location from = event.getFrom();
		Location to = event.getTo();
		String playername = event.getPlayer().getName();
		boolean otherWorld = !from.getWorld().equals(to.getWorld());
		ACPlayer player = ACPlayer.getPlayer(playername);
		if (otherWorld) {
			player.setLastLocation(from);
		}
		if (player.hasPower(Type.FROZEN)) {
			event.setCancelled(true);
			return;
		}
		if (ACHelper.getInstance().getConfBoolean("resetPowerWhenTpAnotherWorld") && !otherWorld
				&& !PermissionManager.hasPerm(event.getPlayer(), "admincmd.player.noreset", false)) {
			player.removeAllSuperPower();
			if (InvisibleWorker.getInstance().hasInvisiblePowers(playername)) {
				InvisibleWorker.getInstance().reappear(event.getPlayer());
				Utils.sI18n(event.getPlayer(), "changedWorld");
			}

		}
		playerRespawnOrJoin(event.getPlayer());
	}

	@Override
	public void onPlayerInteract(PlayerInteractEvent event) {
		Player p = event.getPlayer();
		if (ACHelper.getInstance().getConfBoolean("autoAfk")) {
			AFKWorker.getInstance().updateTimeStamp(p);
			if (AFKWorker.getInstance().isAfk(p))
				AFKWorker.getInstance().setOnline(p);
		}
		ACPlayer player = ACPlayer.getPlayer(p.getName());
		if (player.hasPower(Type.FROZEN)) {
			event.setCancelled(true);
			return;
		}
		if (((event.getAction() == Action.LEFT_CLICK_BLOCK) || (event.getAction() == Action.LEFT_CLICK_AIR))) {
			if (player.hasPower(Type.THOR))
				p.getWorld().strikeLightning(p.getTargetBlock(null, 600).getLocation());
			Float power = null;
			if ((power = player.getPower(Type.VULCAN).getFloat(0)) != 0)
				p.getWorld()
						.createExplosion(p.getTargetBlock(null, 600).getLocation(), power, true);
			power = null;
			if ((power = player.getPower(Type.FIREBALL).getFloat(0)) != 0)
				ShootFireball.shoot(p, power);
			tpAtSee(player);

		}
	}

	private boolean playerRespawnOrJoin(Player newPlayer) {
		ACPluginManager
				.getServer()
				.getScheduler()
				.scheduleSyncDelayedTask(ACHelper.getInstance().getCoreInstance(),
						new UpdateInvisibleOnJoin(newPlayer), 15);
		if (InvisibleWorker.getInstance().hasInvisiblePowers(newPlayer.getName())) {
			ACPluginManager
					.getServer()
					.getScheduler()
					.scheduleSyncDelayedTask(ACHelper.getInstance().getCoreInstance(),
							new UpdateInvisible(newPlayer), 15);
			return true;
		}
		return false;
	}

	@Override
	public void onPlayerChat(PlayerChatEvent event) {
		Player p = event.getPlayer();
		ACPlayer player = ACPlayer.getPlayer(p.getName());
		if (ACHelper.getInstance().getConfBoolean("autoAfk")) {
			AFKWorker.getInstance().updateTimeStamp(p);
			if (AFKWorker.getInstance().isAfk(p))
				AFKWorker.getInstance().setOnline(p);
		}
		if (player.hasPower(Type.MUTED)) {
			event.setCancelled(true);
			Utils.sI18n(p, "muteEnabled");
		}
	}

	@Override
	public void onPlayerPickupItem(PlayerPickupItemEvent event) {
		if (event.isCancelled())
			return;
		ACPlayer player = ACPlayer.getPlayer(event.getPlayer().getName());
		if (player.hasPower(Type.NO_PICKUP))
			event.setCancelled(true);
	}

	@Override
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
		if (CommandManager.getInstance()
				.processCommandString(event.getPlayer(), event.getMessage())) {
			event.setCancelled(true);
			event.setMessage("/AdminCmd took the control of that command.");
		}
	}

	/**
	 * Tp at see mode
	 * 
	 * @param p
	 */
	private void tpAtSee(ACPlayer player) {
		if (player.hasPower(Type.TP_AT_SEE))
			try {
				Player p = player.getHandler();
				Block toTp = p.getWorld().getBlockAt(
						p.getTargetBlock(null,
								ACHelper.getInstance().getConfInt("maxRangeForTpAtSee"))
								.getLocation().add(0, 1, 0));
				if (toTp.getTypeId() == 0) {
					Location loc = toTp.getLocation().clone();
					loc.setPitch(p.getLocation().getPitch());
					loc.setYaw(p.getLocation().getYaw());
					player.setLastLocation(p.getLocation());
					p.teleport(loc);
				}
			} catch (Exception e) {
			}
	}

	protected class UpdateInvisibleOnJoin implements Runnable {
		Player newPlayer;

		/**
		 *
		 */
		public UpdateInvisibleOnJoin(Player p) {
			newPlayer = p;
		}

		@Override
		public void run() {
			for (Player toVanish : InvisibleWorker.getInstance().getAllInvisiblePlayers())
				InvisibleWorker.getInstance().invisible(toVanish, newPlayer);
		}
	}

}
