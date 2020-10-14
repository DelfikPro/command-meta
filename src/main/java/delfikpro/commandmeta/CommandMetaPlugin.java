package delfikpro.commandmeta;

import lombok.Data;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.TabCompleteEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Collectors;

@Getter
public class CommandMetaPlugin extends JavaPlugin implements Listener, CommandExecutor {

	@Data
	public static class Group {

		private final String permission;
		private final List<String> help;
		private final List<String> commands;

	}

	private List<Group> groups;
	private String messageHelpHeader;
	private String messageHelpEntry;
	private String messageHelpFooter;
	private String messageForbiddenMessage;
	private String messageInvalidPageNumber;
	private int helpPerPage;
	private Map<String, String> customCommands;

	@Override
	public void onEnable() {
		groups = new ArrayList<>();
		customCommands = new HashMap<>();
		Bukkit.getPluginManager().registerEvents(this, this);
		saveDefaultConfig();
		FileConfiguration config = getConfig();
		ConfigurationSection groups = config.getConfigurationSection("groups");
		if (groups == null) {
			System.out.println();
		}
		for (String key : groups.getKeys(false)) {
			ConfigurationSection groupConfig = groups.getConfigurationSection(key);
			String permission = groupConfig.getString("permission");
			List<String> help = getStringListSmart(groupConfig, "help").stream().map(s -> s.replace('&', '§')).collect(Collectors.toList());
			List<String> commands = getStringListSmart(groupConfig, "commands").stream().map(String::toLowerCase).collect(Collectors.toList());
			this.groups.add(new Group(permission, help, commands));
		}

		messageHelpHeader = config.getString("format.help-header").replace('&', '§');
		messageHelpEntry = config.getString("format.help-entry").replace('&', '§');
		messageHelpFooter = config.getString("format.help-footer").replace('&', '§');
		messageForbiddenMessage = config.getString("format.forbidden-command").replace('&', '§');
		messageInvalidPageNumber = config.getString("format.invalid-page-number").replace('&', '§');

		helpPerPage = config.getInt("format.help-per-page", 8);

		ConfigurationSection customCommands = config.getConfigurationSection("format").getConfigurationSection("custom-commands");
		customCommands.getKeys(false).forEach(k -> this.customCommands.put(k, customCommands.getString(k).replace('&', '§')));

		try {
			Class.forName("org.bukkit.event.player.PlayerCommandSendEvent");
			Bukkit.getPluginManager().registerEvents(new CommandMeta_1_13(this), this);
		} catch (ClassNotFoundException ignored) {}

	}

	@Override
	public void onDisable() {
		super.onDisable();
	}

	public static List<String> getStringListSmart(ConfigurationSection section, String key) {
		Object object = section.get(key);
		if (object == null) return Collections.emptyList();
		if (object instanceof List) return ((List<?>) object).stream().map(String::valueOf).collect(Collectors.toList());
		return Arrays.asList(String.valueOf(object).split("\n"));
	}

	public boolean canSenderUse(CommandSender sender, String command) {
		command = command.toLowerCase();
		for (Group group : groups) {
			if (!sender.hasPermission(group.permission)) continue;
			if (group.commands.contains(command)) return true;
		}
		return false;
	}

	@EventHandler
	public void onTab(TabCompleteEvent e) {

		if (e.getSender().isOp()) return;

		String start = e.getBuffer().toLowerCase();
		if (!start.startsWith("/")) return;
		start = start.substring(1);
//		e.getSender().sendMessage("§eCompleting: '§f" + start + "§e'");

		String[] args = start.split(" ", -1);
		if (args.length == 1) {
			List<String> variants = new ArrayList<>();
			for (Group group : groups) {
				if (e.getSender().hasPermission(group.permission)) {
					for (String command : group.getCommands()) {
						if (command.startsWith(start))
							variants.add("/" + command);
					}
				}
			}
			e.setCompletions(variants);
		} else if (args.length > 1) {
			if (!canSenderUse(e.getSender(), args[0])) {
				e.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void onCommand(PlayerCommandPreprocessEvent e) {
		String message = e.getMessage();
		String[] args = message.split(" ");
		String command = args[0].substring(1).toLowerCase();
		Player player = e.getPlayer();

		if (player.isOp() && command.equalsIgnoreCase("rlcmeta")) {
			onDisable();
			onEnable();
			player.sendMessage("§aКонфигурация перезагружена");
			e.setCancelled(true);
			return;
		}
		if (command.equalsIgnoreCase("help")) {
			e.setCancelled(true);

			List<String> help = new ArrayList<>();
			for (Group group : groups) {
				if (player.hasPermission(group.permission)) help.addAll(group.help);
			}

			String pageStr = args.length > 1 ? args[1] : "1";
			try {
				int page = Integer.parseInt(pageStr);
				int total = (help.size() - 1) / helpPerPage + 1;
				if (page <= 0 || page > total)
					throw new NumberFormatException();

				player.sendMessage(messageHelpHeader
						.replace("<page>", page + "")
						.replace("<total>", total + "")
						.replace("<next>", (page + 1) + ""));
				for (int i = (page - 1) * helpPerPage; i < page * helpPerPage && i < help.size(); i++) {
					player.sendMessage(messageHelpEntry.replace("<help>", help.get(i)));
				}
				if (total > page) player.sendMessage(messageHelpFooter
						.replace("<page>", page + "")
						.replace("<total>", total + "")
						.replace("<next>", (page + 1) + ""));
			} catch (NumberFormatException ex) {
				player.sendMessage(messageInvalidPageNumber.replace("<page>", args[1]));
			}
			return;
		}

		if (command.contains(":") && !player.isOp()) {
			e.setCancelled(true);
			return;
		}



		if (e.getPlayer().isOp()) return;

		String customCommand = customCommands.get(command);
		if (customCommand != null) {
			e.setCancelled(true);
			player.sendMessage(customCommand);
			return;
		}


		if (!canSenderUse(player, command)) {
			player.sendMessage(messageForbiddenMessage);
			e.setCancelled(true);
		}

	}

	@EventHandler
	public void handle(PlayerJoinEvent e) {
		Bukkit.getScheduler().runTaskLater(this, () -> e.getPlayer().updateCommands(), 20);
	}


}
