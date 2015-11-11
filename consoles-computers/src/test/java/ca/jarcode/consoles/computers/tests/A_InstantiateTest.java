package ca.jarcode.consoles.computers.tests;

import org.junit.Test;

public class A_InstantiateTest {
	@Test
	public void attemptInstantiate() throws Throwable {
		NativeLayerTask task = new NativeLayerTask();
		task.init();
		task.cleanup();
	}
}
