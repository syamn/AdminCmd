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
package be.Balor.Manager.Commands.Player;

import org.bukkit.command.CommandSender;

import be.Balor.Manager.Commands.CommandArgs;
import be.Balor.Manager.Commands.CoreCommand;
import be.Balor.Player.BannedPlayer;
import be.Balor.Tools.Utils;
import be.Balor.bukkit.AdminCmd.ACHelper;

/**
 * @author Balor (aka Antoine Aflalo)
 * 
 */
public class UnBan extends CoreCommand {

	/**
	 * 
	 */
	public UnBan() {
		permNode = "admincmd.player.ban";
		cmdName = "bal_unban";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * be.Balor.Manager.ACCommands#execute(org.bukkit.command.CommandSender,
	 * java.lang.String[])
	 */
	@Override
	public void execute(CommandSender sender, CommandArgs args) {
		String unban = args.getString(0);
		BannedPlayer player = ACHelper.getInstance().isBanned(unban);
		if (player != null) {
			if (!Utils.checkImmunity(sender, plugin.getServer().getPlayer(player.getPlayer()))) {
				Utils.sI18n(sender, "insufficientLvl");
				return;
			}
			ACHelper.getInstance().unBanPlayer(unban);
			String unbanMsg = Utils.I18n("unban", "player", unban);
			if (unbanMsg != null)
				Utils.broadcastMessage(unbanMsg);
		} else
			Utils.sI18n(sender, "playerNotFound", "player", unban);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see be.Balor.Manager.ACCommands#argsCheck(java.lang.String[])
	 */
	@Override
	public boolean argsCheck(String... args) {
		return args != null && args.length >= 1;
	}

}
