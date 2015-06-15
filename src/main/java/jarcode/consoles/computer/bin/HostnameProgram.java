package jarcode.consoles.computer.bin;

import jarcode.consoles.computer.Computer;
import jarcode.consoles.computer.filesystem.FSBlock;
import jarcode.consoles.computer.filesystem.FSProvidedProgram;
import jarcode.consoles.computer.manual.ProvidedManual;

import static jarcode.consoles.computer.ProgramUtils.*;

@ProvidedManual(
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
				if (computer.setHostname(str.toLowerCase())) {
					return str.toLowerCase();
				}
				else return null;
			}, this::terminated);
			if (hostname != null)
				print("hostname changed to: '" + hostname + '\'');
			else
				print("invalid or taken hostname: '" + str + '\'');
		}
		else {
			print("illegal hostname: '" + str + '\'');
		}
	}
}
