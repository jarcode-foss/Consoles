package jarcode.consoles.computer.bin;

import jarcode.consoles.computer.Computer;
import jarcode.consoles.computer.filesystem.FSBlock;
import jarcode.consoles.computer.filesystem.FSProvidedProgram;

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
