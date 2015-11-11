package ca.jarcode.consoles.computers.tests;

import org.junit.Test;

public class C_LuaTest {
	@Test
	public void test() throws Throwable {
		NativeLayerTask task = new NativeLayerTask();
		task.init();
		task.loadAndCallChunk();
		task.loadAndCallTests();
		task.cleanup();
	}
}
