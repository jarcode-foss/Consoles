package ca.jarcode.consoles.internal;

import ca.jarcode.consoles.api.nms.CommandExecutor;

/*

A component that can be written to, thus allowing the creation of listeners for it.

 */
public interface WritableComponent {
	CommandExecutor createListener();
}
