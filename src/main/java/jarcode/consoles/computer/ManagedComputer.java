package jarcode.consoles.computer;

import java.util.UUID;

public class ManagedComputer extends Computer {

	private static final int WIDTH = 3, HEIGHT = 2;

	public ManagedComputer(String hostname, UUID owner) {
		super(hostname, owner, WIDTH, HEIGHT);
		ComputerHandler.getInstance().register(this);
	}

	@Override
	public void destroy() {
		super.destroy();
		ComputerHandler.getInstance().unregister(this);
	}
}
