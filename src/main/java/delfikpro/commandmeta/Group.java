package delfikpro.commandmeta;

import lombok.Data;

import java.util.List;

@Data
public class Group {

	private final String permission;
	private final List<String> help;
	private final List<String> commands;

}
