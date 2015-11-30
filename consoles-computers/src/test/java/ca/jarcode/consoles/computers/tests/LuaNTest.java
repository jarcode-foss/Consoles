package ca.jarcode.consoles.computers.tests;

import org.junit.Test;

public class LuaNTest {
	@Test
	public void test() throws Throwable {
		
		NativeLayerTask task = new NativeLayerTask();
		task.init(false);
		task.loadAndCallChunk("tests");
		task.loadAndCallTests(true);
		task.cleanup();
		task.dumpValues();

	}
}
