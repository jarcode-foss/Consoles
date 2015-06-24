package ca.jarcode.consoles.internal;

/*

A component that can be written to, thus allowing the creation of listeners for it.

 */
public interface WritableComponent {
	public ConsoleMessageListener createListener();
}
