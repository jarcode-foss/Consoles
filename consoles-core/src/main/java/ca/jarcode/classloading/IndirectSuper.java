package ca.jarcode.classloading;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IndirectSuper {

	private MethodHandle handle;

	public IndirectSuper(Class<?> type, String methodName, Class<?> ret) {
		try {
			MethodHandle handle = MethodHandles.lookup()
					.findSpecial(type.getSuperclass(), methodName, MethodType.methodType(ret), type);
		} catch (NoSuchMethodException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	public void call(Object context, Object... args) throws Throwable {
		List<Object> list = new ArrayList<>();
		list.add(context);
		list.addAll(Arrays.asList(args));
		handle.invoke(list.toArray(new Object[list.size()]));
	}
}
