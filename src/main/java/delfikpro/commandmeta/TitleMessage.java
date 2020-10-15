package delfikpro.commandmeta;

import org.bukkit.entity.Player;

import java.util.List;

public class TitleMessage extends Message {

	public TitleMessage(List<String> lines) {
		super(lines);
	}

	@Override
	public void send(Player player, Object... placeholders) {
		String title = getLines().get(0).replace('&', '§');
		String subtitle = getLines().size() > 1 ? getLines().get(1).replace('&', '§') : "";
		player.sendTitle(title, subtitle, 10, 70, 20);
	}

}
