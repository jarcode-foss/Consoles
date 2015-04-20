package jarcode.consoles.computer.bin;

import jarcode.consoles.computer.Computer;
import jarcode.consoles.computer.filesystem.FSBlock;
import jarcode.consoles.computer.filesystem.FSProvidedProgram;

@Manual(
		author = "Jarcode",
		version = "1.2",
		contents = "A script that is capable of either printing or changing the hostname of a computer."
)
public class HostnameProgram extends FSProvidedProgram {
	@Override
	public void run(String str, Computer computer) throws Exception {
		if (str.trim().isEmpty()) {
			print(computer.getHostname());
		}
		else if (FSBlock.allowedBlockName(str)) {
			String hostname = schedule(() -> {
				if (computer.hostname(str.toLowerCase())) {
					return str.toLowerCase();
				}
				else return null;
			});
			if (hostname != null)
				print("hostname changed to: '" + hostname + '\'');
			else
				print("Invalid or taken hostname: '" + str + '\'');
		}
		else {
			print("illegal hostname: '" + str + '\'');
		}
	}
}
