package ca.jarcode.consoles.computers.tests;

import ca.jarcode.ascript.luanative.LuaNEngine;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.*;
import java.util.*;

import ca.jarcode.ascript.luanative.*;

public class LuaNTest {
	@Test
	public void test() throws Throwable {
		
		NativeLayerTask task = new NativeLayerTask();
		task.init(false, true);
		task.loadAndCallChunk("tests");
		task.loadAndCallTests(true);
		if (LuaNScriptValue.TRACK_INSTANCES) {
			String list = Arrays.asList(LuaNScriptValue.remainingContextValues(0)).stream()
				.map((value) -> NativeLayerTask.valueString(value))
				.collect(Collectors.joining(", "));
			System.out.println("unmanaged values:\n " + list);
			list = Arrays.asList(LuaNScriptValue.remainingContextValues(task.getEngineAddress())).stream()
				.map((value) -> NativeLayerTask.valueString(value))
				.collect(Collectors.joining(", "));
			System.out.printf("instance values (%x):\n " + list + "\n", task.getEngineAddress());
		}
		task.cleanup();

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

		AtomicReference<Throwable> ref = new AtomicReference<>();
		Thread thread = new Thread(() -> {
			try {
				NativeLayerTask threadTask = new NativeLayerTask();
				threadTask.init(false, false);
				threadTask.loadAndCallChunk("tests");
				threadTask.loadAndCallTests(true);
				threadTask.cleanup();
				threadTask.cleanupThreadContext();
			}
			catch (Throwable e) {
				ref.set(e);
			}
		});

		thread.start();
		thread.join();

		Throwable e = ref.get();
		if (e != null) throw e;

		task.cleanupThreadContext();

		boolean err = false;

		// doing this twice should throw an exception
		try {
			task.cleanupThreadContext();
		}
		catch (Throwable ex) {
			NativeLayerTask.stdout.println(
					"Caught exception: " + ex.getClass().getSimpleName() + ", " + ex.getMessage()
			);
			err = true;
		}
		if (!err)
			throw new RuntimeException("cleanupThreadContext() did not error on duplicate call");
	}
}
