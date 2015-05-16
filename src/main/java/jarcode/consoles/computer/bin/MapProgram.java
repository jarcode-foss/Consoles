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
		schedule(() -> {
			MapComponent component = new MapComponent(computer.getViewWidth(),
					computer.getViewHeight(), computer,
					computer.getConsole().getLocation().getBlockX(),
					computer.getConsole().getLocation().getBlockY());
			computer.setComponent(6, component);
			computer.switchView(7);
		});
	}
}
