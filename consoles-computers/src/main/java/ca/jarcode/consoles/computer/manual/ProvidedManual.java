package ca.jarcode.consoles.computer.manual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ProvidedManual {
	String author() default "?";
	String version() default "1.0";
	String contents() default "";
}
