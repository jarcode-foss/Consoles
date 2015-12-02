package ca.jarcode.ascript;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used in classes that are being mapped as a Lua library to explicitly
 * state the name of the corresponding Lua function.
 *
 * @see LibraryCreator
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface ScriptName {
	/**
	 * Returns the name of the resulting Lua function for this method
	 *
	 * @return the name of the lua function
	 */
	String name() default "?";
}
