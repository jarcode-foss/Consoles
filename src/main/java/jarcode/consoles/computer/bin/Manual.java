package jarcode.consoles.computer.bin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Manual {
	public String author() default "?";
	public String version() default "1.0";
	public String contents() default "";
}
