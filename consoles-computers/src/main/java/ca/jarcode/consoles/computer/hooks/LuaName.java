package ca.jarcode.consoles.computer.hooks;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used in classes that are being mapped as a Lua library to explicitly
 * state the name of the corresponding Lua function.
 *
 * @see ca.jarcode.consoles.computer.hooks.LibraryCreator
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface LuaName {
	/**
	 * Returns the name of the resulting Lua function for this method
	 *
	 * @return the name of the lua function
	 */
	String name() default "?";
}
