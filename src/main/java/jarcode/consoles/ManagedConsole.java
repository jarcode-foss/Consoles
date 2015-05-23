package jarcode.consoles;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.map.MinecraftFont;

import java.lang.reflect.Field;

/*

Almost all consoles should be a managed console. This class allows for console
registration/removal (although, the ConsoleRenderer class is also dependant on
the management from ConsoleHandler - this is an organization oddity)

 */
public class ManagedConsole extends ConsoleRenderer {

	private static final String SUPPORTED_CHARACTERS = " !\"#$%&\'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
			"[\\]^_\'abcdefghijklmnopqrstuvwxyz{|}~\u007fÇüéâäàåçêëèïîìÄÅÉæÆôöòûùÿÖÜø£Ø×ƑáíóúñÑªº¿®¬½¼¡«»";

	public static String removeUnsupportedCharacters(String str) {
		char[] arr = str.toCharArray();
		for (int t = 0; t < str.length(); t++) {
			if (SUPPORTED_CHARACTERS.indexOf(arr[t]) == -1 && arr[t] != '\u00A7')
				arr[t] = '?';
		}
		return new String(arr);
	}

	protected String consoleType() {
		return name;
	}

	private short index;
	private String identifier = null;

	public ManagedConsole(int w, int h) {
		this(w, h, true);
	}
	public ManagedConsole(int w, int h, boolean cacheBackground) {
		super(w, h, cacheBackground);
		index = ConsoleHandler.getInstance().allocate(w * h);
		ConsoleHandler.getInstance().consoles.add(this);
	}
	public void create(BlockFace face, Location location) {
		super.create(index, face, location);
	}
	@Override
	public void remove() {
		remove(true);
	}
	void remove(boolean rm) {
		super.remove();
		if (rm)
			ConsoleHandler.getInstance().handleRemove(this);
	}
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}
	public String getIdentifier() {
		return identifier;
	}
	public ConsoleMeta createMeta() {
		return new ConsoleMeta(this.pos, this.face, this.getFrameWidth(), this.getFrameHeight());
	}
}
