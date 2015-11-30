package ca.jarcode.consoles.computers.tests;

import ca.jarcode.consoles.computer.interpreter.luanative.LuaNEngine;
import org.junit.Test;

public class LuaNTest {
	@Test
	public void test() throws Throwable {
		
		NativeLayerTask task = new NativeLayerTask();
		task.init(false, true);
		task.loadAndCallChunk("tests");
		task.loadAndCallTests(true);
		task.cleanup();
		task.dumpValues();

		task = new NativeLayerTask();
		task.init(true, false);

		LuaNEngine.ENGINE_INTERFACE.setmaxtime(1500);

		task.loadAndCallChunk("sandbox_test");
		try {
			task.loadAndCallTests(false);
		}
		// we're going to get an error after letting the script die.
		catch (Throwable ignored) {}
		task.cleanup();

		LuaNEngine.ENGINE_INTERFACE.setmaxtime(7000);

		task.cleanupThreadContext();
	}
}
