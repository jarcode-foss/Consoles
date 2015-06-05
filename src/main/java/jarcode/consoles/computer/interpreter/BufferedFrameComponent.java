package jarcode.consoles.computer.interpreter;

import jarcode.consoles.internal.ConsoleComponent;
import jarcode.consoles.internal.InputComponent;
import jarcode.consoles.api.CanvasGraphics;
import jarcode.consoles.computer.Computer;
import jarcode.consoles.computer.interpreter.types.LuaInteraction;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/*

Component used to render content/actions from Lua code.

 */
public class BufferedFrameComponent extends ConsoleComponent implements InputComponent {

	private final CopyOnWriteArrayList<LuaInteraction> interactions = new CopyOnWriteArrayList<>();
	private final Object STR_LOCK = new Object();
	private StringBuilder input = new StringBuilder();
	private final CopyOnWriteArrayList<Consumer<CanvasGraphics>> ops = new CopyOnWriteArrayList<>();

	public BufferedFrameComponent(Computer computer) {
		super(computer.getViewWidth(), computer.getViewHeight(), computer.getConsole());
		this.setEnabled(true);
	}

	public void setOperations(Collection<Consumer<CanvasGraphics>> ops) {
		this.ops.clear();
		this.ops.addAll(ops);
		repaint();
	}

	public LuaInteraction interaction() {
		if (interactions.size() == 0) return null;
		LuaInteraction[] arr = interactions.stream()
				.skip(1)
				.toArray(LuaInteraction[]::new);
		LuaInteraction at = interactions.get(0);
		interactions.clear();
		interactions.addAll(Arrays.asList(arr));
		return at;
	}

	public String input() {
		synchronized (STR_LOCK) {
			String str = input.toString();
			input = new StringBuilder();
			return str;
		}
	}

	@Override
	public void handleClick(int x, int y, Player player) {
		interactions.add(new LuaInteraction(x, y, player.getName()));
	}

	@Override
	public void paint(CanvasGraphics g, String context) {
		for (Consumer<CanvasGraphics> op : ops) {
			op.accept(g);
		}
	}

	@Override
	public void handleInput(String input, String player) {
		synchronized (STR_LOCK) {
			this.input.append(input);
			this.input.append('\n');
		}
	}
}
