package jarcode.consoles.computer;

import java.util.UUID;

public class ManagedComputer extends Computer {

	public static final int WIDTH = 3, HEIGHT = 2;

	public ManagedComputer(String hostname, UUID owner) {
		super(hostname.toLowerCase(), owner, WIDTH, HEIGHT);
		ComputerHandler.getInstance().register(this);
	}

	@Override
	public void destroy() {
		super.destroy();
		ComputerHandler.getInstance().unregister(this);
	}
}
