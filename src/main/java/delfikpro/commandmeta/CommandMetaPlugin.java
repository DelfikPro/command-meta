package delfikpro.commandmeta;

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
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.TabCompleteEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Collectors;

@Getter
public class CommandMetaPlugin extends JavaPlugin implements Listener, CommandExecutor {

	private final Map<String, Message> messages = new HashMap<>();
	private final Map<String, Message> overriddenCommands = new HashMap<>();
	private final List<Group> groups = new ArrayList<>();
	private int helpPerPage;

	@Override
	public void onEnable() {
		saveDefaultConfig();
		reloadFromConfig();

		Bukkit.getPluginManager().registerEvents(this, this);

		try {
			Class.forName("org.bukkit.event.player.PlayerCommandSendEvent");
			Bukkit.getPluginManager().registerEvents(new CommandMetaBrigadier(this), this);
		} catch (ClassNotFoundException ignored) {}

	}

	public void reloadFromConfig() {

		groups.clear();
		messages.clear();
		overriddenCommands.clear();

		FileConfiguration config = getConfig();

		helpPerPage = config.getInt("help per page", 8);

		ConfigurationSection groups = config.getConfigurationSection("groups");

		if (groups != null) {
			for (String key : groups.getKeys(false)) {
				ConfigurationSection groupConfig = groups.getConfigurationSection(key);
				if (groupConfig == null) continue;
				String permission = groupConfig.getString("permission");
				List<String> help = getStringListSmart(groupConfig, "help").stream().map(s -> s.replace('&', '§')).collect(Collectors.toList());
				List<String> commands = getStringListSmart(groupConfig, "commands").stream().map(String::toLowerCase).collect(Collectors.toList());
				this.groups.add(new Group(permission, help, commands));
			}

		}

		ConfigurationSection messagesConfig = config.getConfigurationSection("messages");

		if (messagesConfig != null) {
			for (String key : messagesConfig.getKeys(false)) {
				List<String> lines = new ArrayList<>(getStringListSmart(messagesConfig, key));
				boolean title = !lines.isEmpty() && lines.get(0).equals("title");
				if (title) lines.remove(0);
				Message message = title ? new TitleMessage(lines) : new Message(lines);
				if (key.startsWith("override /"))
					overriddenCommands.put(key.replace("override /", ""), message);
				else messages.put(key, message);
			}
		}

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
			if (!sender.hasPermission(group.getPermission())) continue;
			if (group.getCommands().contains(command)) return true;
		}
		return false;
	}

	@EventHandler
	public void onTab(TabCompleteEvent e) {

		if (e.getSender().isOp()) return;

		String start = e.getBuffer().toLowerCase();
		if (!start.startsWith("/")) return;
		start = start.substring(1);

		String[] args = start.split(" ", -1);
		if (args.length == 1) {
			List<String> variants = new ArrayList<>();
			for (Group group : groups) {
				if (e.getSender().hasPermission(group.getPermission())) {
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

		if (player.isOp() && command.equalsIgnoreCase("rlm")) {
			reloadConfig();
			reloadFromConfig();
			player.sendMessage("§aКонфигурация перезагружена");
			e.setCancelled(true);
			return;
		}

		if (command.equalsIgnoreCase("help")) {
			e.setCancelled(true);

			List<String> help = new ArrayList<>();
			for (Group group : groups) {
				if (player.hasPermission(group.getPermission())) help.addAll(group.getHelp());
			}

			String pageStr = args.length > 1 ? args[1] : "1";
			try {
				int page = Integer.parseInt(pageStr);
				int total = (help.size() - 1) / helpPerPage + 1;
				if (page <= 0 || page > total)
					throw new NumberFormatException();

				messages.get("help header").send(player,
						"page", page,
						"total", total,
						"next", page + 1);
				for (int i = (page - 1) * helpPerPage; i < page * helpPerPage && i < help.size(); i++) {
					messages.get("help entry").send(player, "help", help.get(i));
				}
				if (page < total) messages.get("help footer").send(player,
						"page", page,
						"total", total,
						"next", page + 1);
			} catch (NumberFormatException ex) {
				messages.get("no page").send(player, "page", args[1]);
			}
			return;
		}

		if (command.contains(":") && !player.isOp()) {
			e.setCancelled(true);
			return;
		}

		if (e.getPlayer().isOp()) return;

		Message customCommand = messages.get("custom " + message.toLowerCase());
		if (customCommand != null) {
			e.setCancelled(true);
			customCommand.send(player);
			return;
		}

		Message overridenCommand = overriddenCommands.get(command);
		if (overridenCommand != null) {
			e.setCancelled(true);
			overridenCommand.send(player);
			return;
		}

		if (!canSenderUse(player, command)) {
			messages.get("no command").send(player);
			e.setCancelled(true);
		}

	}


}
