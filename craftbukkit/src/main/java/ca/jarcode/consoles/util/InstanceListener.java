package ca.jarcode.consoles.util;

import ca.jarcode.consoles.Consoles;
import org.bukkit.Bukkit;
import org.bukkit.event.*;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;

import java.util.function.Consumer;

/**
 * This is a nice utility class for registering listeners on the fly, lambda compatible!
 *
 * @author Jarcode
 */
public class InstanceListener {

	private Listener identifier = new Listener() {};

	/**
	 * Destroys all listeners registered under this object
	 */
	public void destroy() {
		HandlerList.unregisterAll(identifier);
	}

	/**
	 * Registers an event handler
	 *
	 * @param event the event class to listen for
	 * @param handler the handler to use
	 * @param <T> the type of the event
	 */
	public <T extends Event> void register(Class<T> event, Consumer<T> handler) {
		register(event, handler, EventPriority.NORMAL);
	}

	/**
	 * Registers an event handler
	 *
	 * @param event the event class to listen for
	 * @param handler the handler to use
	 * @param priority the priority of the event
	 * @param <T> the type of the event
	 */
	@SuppressWarnings("unchecked")
	public <T extends Event> void register(Class<T> event, Consumer<? super T> handler, EventPriority priority) {
		Bukkit.getServer().getPluginManager().registerEvent(event, identifier, priority,
				(listener, e) -> handler.accept((T) e), Consoles.getInstance());
	}

	/**
	 * Creates a {@link ca.jarcode.consoles.util.InstanceListener.ConsumerChain}, which can
	 * be used to chain multiple event classes to a single event handler.
	 *
	 * @param consumer the event handler to use for the chain
	 * @param <S> the base type of the events
	 * @return a new {@link ca.jarcode.consoles.util.InstanceListener.ConsumerChain}
	 */
	public <S extends Event> ConsumerChain<S> chain(Consumer<S> consumer) {
		return new ConsumerChain<>(consumer);
	}

	/**
	 * Associates this listener with a plugin, causing it to be destroyed when
	 * the plugin is unloaded
	 *
	 * @param plugin the plugin to associate with
	 */
	public void associate(Plugin plugin) {
		register(PluginDisableEvent.class, (e) -> {
			if (e.getPlugin() == plugin)
				destroy();
		});
	}

	/**
	 * This class is used to chain multiple event classes to a single handler
	 *
	 * @param <S> the base type of the events to chain
	 */
	public class ConsumerChain<S extends Event> {
		private Consumer<S> consumer;

		private ConsumerChain(Consumer<S> consumer) {
			this.consumer = consumer;
		}

		/**
		 * Registers an event class to the underlying handler
		 *
		 * @param type the type of event to listen for
		 * @param <T> the type of the event
		 * @return the instance of this object, for chaining method calls
		 */
		public <T extends S> ConsumerChain register(Class<T> type) {
			InstanceListener.this.register(type, consumer::accept);
			return this;
		}
	}
}
