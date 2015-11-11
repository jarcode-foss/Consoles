package ca.jarcode.consoles.computers.tests;

import org.junit.Test;

public class B_LoadChunkTest {
	@Test
	public void attemptLoad() throws Throwable {
		NativeLayerTask task = new NativeLayerTask();
		task.init();
		task.loadAndCallChunk();
		task.cleanup();
	}
}
