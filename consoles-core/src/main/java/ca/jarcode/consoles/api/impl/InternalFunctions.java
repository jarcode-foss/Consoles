package ca.jarcode.consoles.api.impl;

import ca.jarcode.consoles.api.*;
import ca.jarcode.consoles.internal.*;
import net.minecraft.server.v1_8_R3.PacketPlayOutMap;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

public class InternalFunctions {

	public static void assign() {
		CanvasComponentBuilder.INTERNAL_BUILDER = InternalFunctions::buildComponent;
		CanvasContainerBuilder.INTERNAL_BUILDER = InternalFunctions::buildContainer;
		Console.INTERNAL_CONSOLE_BUILDER = InternalFunctions::buildConsole;
		CanvasFeed.INTERNAL_BUILDER = CanvasFeedImpl::new;
		CanvasTextArea.INTERNAL_BUILDER = CanvasTextAreaImpl::new;
		CanvasDialog.INTERNAL_BUILDER = CanvasDialogImpl::new;
		InternalHooks.INTERNAL_ALLOC = () -> ConsoleHandler.getInstance().allocate(1);
		InternalHooks.INTERNAL_FREE = global -> ConsoleHandler.getInstance().free(global, 1);
		InternalHooks.INTERNAL_TRANSLATE = (player, global) ->
				ConsoleHandler.getInstance().translateIndex(player.getName(), global);
		InternalHooks.INTERNAL_SEND_PACKET = (data, player, id) -> {
			PacketPlayOutMap packet = ConsoleMapRenderer.createUpdatePacket(data, id);
			((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
		};

	}

	private static CanvasComponent buildComponent(CanvasComponentBuilder builder) {
		return new ConsoleComponent(builder.getWidth(), builder.getHeight(), (ConsoleRenderer) builder.getCanvas()) {
			{
				if (builder.getBackground() != null)
					setBackground(builder.getBackground());
				setEnabled(builder.isEnabled());
				builder.getConstructors().stream().forEach(listeners -> listeners.accept(this));
			}
			@Override
			public void paint(CanvasGraphics g, String context) {
				builder.getPainters().stream().forEach(painter -> painter.paint(g, context));
			}
			@Override
			public void handleClick(int x, int y, Player player) {
				builder.getListeners().stream().forEach(listener -> listener.handle(x, y, player));
			}

			@Override
			public boolean enabled() {
				return builder.getEnabledSupplier() != null ? builder.getEnabledSupplier().getAsBoolean() : super.enabled();
			}

			@Override
			public void setEnabled(boolean enabled) {
				if (builder.getEnabledConsumer() != null)
					builder.getEnabledConsumer().accept(enabled);
				else super.setEnabled(enabled);
			}
		};
	}

	private static CanvasContainer buildContainer(CanvasContainerBuilder builder) {
		return new ConsoleContainer(builder.getWidth(), builder.getHeight(), (ConsoleRenderer) builder.getCanvas()) {
			{
				builder.getToAdd().stream().forEach(this::add);
			}
			@Override
			public void paint(CanvasGraphics g, String context) {
				builder.getPainters().stream().forEach(painter -> painter.paint(g, context));
			}

			@Override
			public Position2D getUnderlingComponentCoordinates(CanvasComponent component) {
				return builder.getMapper() == null ? null : builder.getMapper().apply(component);
			}

			@Override
			public void onClick(int x, int y, Player player) {
				builder.getListeners().stream().forEach(listener -> listener.handle(x, y, player));
			}

			@Override
			public void onAdd(CanvasComponent component) {
				builder.getAdders().stream().forEach(adder -> adder.accept(component));
			}

			@Override
			public boolean enabled() {
				return builder.getEnabledSupplier() != null ? builder.getEnabledSupplier().getAsBoolean() : super.enabled();
			}

			@Override
			public void setEnabled(boolean enabled) {
				if (builder.getEnabledConsumer() != null)
					builder.getEnabledConsumer().accept(enabled);
				else super.setEnabled(enabled);
			}
		};
	}

	private static ManagedConsole buildConsole(int width, int height, BlockFace face, Location location)
			throws ConsoleCreateException {
		ManagedConsole console = new ManagedConsole(width, height, false);
		console.setType("Custom");
		console.create(face, location);
		return console;
	}
}
