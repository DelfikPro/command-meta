package delfikpro.commandmeta;

import lombok.AllArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;

@AllArgsConstructor
public class CommandMeta_1_13 implements Listener {

	private final CommandMetaPlugin plugin;

	@EventHandler
	public void handle(PlayerCommandSendEvent event) {
		if (event.getPlayer().isOp()) return;
		event.getCommands().clear();

		for (CommandMetaPlugin.Group group : plugin.getGroups()) {
			if (event.getPlayer().hasPermission(group.getPermission())) {
				for (String command : group.getCommands()) {
					event.getCommands().add(command);
				}
			}
		}
	}

}
