package jarcode.consoles.computer.bin;

import jarcode.consoles.computer.Computer;
import jarcode.consoles.computer.MapComponent;
import jarcode.consoles.computer.filesystem.FSProvidedProgram;

@Manual(
		author = "Jarcode",
		version = "1.8",
		contents = "A program that maps out the area adjacent to this computer"
)
public class MapProgram extends FSProvidedProgram {
	@Override
	public void run(String str, Computer computer) throws Exception {

		int x = computer.getConsole().getLocation().getBlockX();
		int y = computer.getConsole().getLocation().getBlockZ();

		if (!str.trim().isEmpty()) {
			String[] split = str.split(" ");
			x = Integer.parseInt(split[0]);
			y = Integer.parseInt(split[1]);
		}

		print("Using coords: " + x + ", " + y);

		final int finalX = x;
		final int finalY = y;
		schedule(() -> {

			MapComponent component = new MapComponent(computer.getViewWidth(),
					computer.getViewHeight(), computer,
					finalX, finalY);
			computer.setComponent(6, component);
			computer.switchView(7);
		});
	}
}
