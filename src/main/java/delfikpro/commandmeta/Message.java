package delfikpro.commandmeta;

import lombok.Data;
import org.bukkit.entity.Player;

import java.util.List;

@Data
public class Message {

	private final List<String> lines;

	public void send(Player player, Object... placeholders) {
		for (String line : lines) {
			for (int i = 0; i < placeholders.length / 2; i++) {
				line = line.replace("<" + placeholders[i * 2] + ">", placeholders[i * 2 + 1] + "");
			}
			player.sendMessage(line.replace('&', '§'));
		}
	}

}
