package ca.jarcode.consoles.computers.tests;

import ca.jarcode.consoles.computer.interpreter.luanative.LuaNEngine;
import org.junit.Test;

public class InfiniteLoopTest {

	@Test
	public void test() throws Throwable {

		NativeLayerTask task = new NativeLayerTask();
		task.init(true);

		LuaNEngine.ENGINE_INTERFACE.setmaxtime(1500);

		task.loadAndCallChunk("sandbox_test");
		try {
			task.loadAndCallTests(false);
		}
		// we're going to get an error after letting the script die.
		catch (Throwable ignored) {}
		task.cleanup();

		LuaNEngine.ENGINE_INTERFACE.setmaxtime(7000);
	}
}
