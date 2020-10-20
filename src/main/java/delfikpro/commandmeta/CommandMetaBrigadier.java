package delfikpro.commandmeta;

import lombok.AllArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Brigadier hints
 * Available only on 1.13+
 */
@AllArgsConstructor
public class CommandMetaBrigadier implements Listener {

	private final CommandMetaPlugin plugin;

	@EventHandler
	public void handle(PlayerCommandSendEvent event) {
		if (event.getPlayer().isOp()) return;
		event.getCommands().clear();

		for (Group group : plugin.getGroups()) {
			if (event.getPlayer().hasPermission(group.getPermission())) {
				for (String command : group.getTabCompletions()) {
					event.getCommands().add(command);
				}
			}
		}
	}

	@EventHandler
	public void handle(PlayerJoinEvent e) {
		Bukkit.getScheduler().runTaskLater(plugin, () -> e.getPlayer().updateCommands(), 20);
	}

}
