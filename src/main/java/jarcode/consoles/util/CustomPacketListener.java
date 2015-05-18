package jarcode.consoles.util;

import net.minecraft.server.v1_8_R3.*;

import java.util.function.Predicate;

@SuppressWarnings("unchecked")
public class CustomPacketListener<T extends Packet> implements PacketListenerPlayIn {

	private Class<T> type;
	private Predicate<T> listener;
	private PacketListenerPlayIn oldIn;
	private PacketListener old;

	public CustomPacketListener(Class<T> type, Predicate<T> listener, PacketListener old) {
		this.type = type;
		this.listener = listener;

		if (old instanceof PacketListenerPlayIn)
			oldIn = (PacketListenerPlayIn) old;
		this.old = old;
	}
	@Override
	public void a(PacketPlayInArmAnimation packet) {
		if (type.isAssignableFrom(packet.getClass()))
			if (!listener.test((T) packet))
				return;
		oldIn.a(packet);
	}

	@Override
	public void a(PacketPlayInChat packet) {
		if (type.isAssignableFrom(packet.getClass()))
			if (!listener.test((T) packet))
				return;
		oldIn.a(packet);
	}

	@Override
	public void a(PacketPlayInTabComplete packet) {
		if (type.isAssignableFrom(packet.getClass()))
			if (!listener.test((T) packet))
				return;
		oldIn.a(packet);
	}

	@Override
	public void a(PacketPlayInClientCommand packet) {
		if (type.isAssignableFrom(packet.getClass()))
			if (!listener.test((T) packet))
				return;
		oldIn.a(packet);
	}

	@Override
	public void a(PacketPlayInSettings packet) {
		if (type.isAssignableFrom(packet.getClass()))
			if (!listener.test((T) packet))
				return;
		oldIn.a(packet);
	}

	@Override
	public void a(PacketPlayInTransaction packet) {
		if (type.isAssignableFrom(packet.getClass()))
			if (!listener.test((T) packet))
				return;
		oldIn.a(packet);
	}

	@Override
	public void a(PacketPlayInEnchantItem packet) {
		if (type.isAssignableFrom(packet.getClass()))
			if (!listener.test((T) packet))
				return;
		oldIn.a(packet);
	}

	@Override
	public void a(PacketPlayInWindowClick packet) {
		if (type.isAssignableFrom(packet.getClass()))
			if (!listener.test((T) packet))
				return;
		oldIn.a(packet);
	}

	@Override
	public void a(PacketPlayInCloseWindow packet) {
		if (type.isAssignableFrom(packet.getClass()))
			if (!listener.test((T) packet))
				return;
		oldIn.a(packet);
	}

	@Override
	public void a(PacketPlayInCustomPayload packet) {
		if (type.isAssignableFrom(packet.getClass()))
			if (!listener.test((T) packet))
				return;
		oldIn.a(packet);
	}

	@Override
	public void a(PacketPlayInUseEntity packet) {
		if (type.isAssignableFrom(packet.getClass()))
			if (!listener.test((T) packet))
				return;
		oldIn.a(packet);
	}

	@Override
	public void a(PacketPlayInKeepAlive packet) {
		if (type.isAssignableFrom(packet.getClass()))
			if (!listener.test((T) packet))
				return;
		oldIn.a(packet);
	}

	@Override
	public void a(PacketPlayInFlying packet) {
		if (type.isAssignableFrom(packet.getClass()))
			if (!listener.test((T) packet))
				return;
		oldIn.a(packet);
	}

	@Override
	public void a(PacketPlayInAbilities packet) {
		if (type.isAssignableFrom(packet.getClass()))
			if (!listener.test((T) packet))
				return;
		oldIn.a(packet);
	}

	@Override
	public void a(PacketPlayInBlockDig packet) {
		if (type.isAssignableFrom(packet.getClass()))
			if (!listener.test((T) packet))
				return;
		oldIn.a(packet);
	}

	@Override
	public void a(PacketPlayInEntityAction packet) {
		if (type.isAssignableFrom(packet.getClass()))
			if (!listener.test((T) packet))
				return;
		oldIn.a(packet);
	}

	@Override
	public void a(PacketPlayInSteerVehicle packet) {
		if (type.isAssignableFrom(packet.getClass()))
			if (!listener.test((T) packet))
				return;
		oldIn.a(packet);
	}

	@Override
	public void a(PacketPlayInHeldItemSlot packet) {
		if (type.isAssignableFrom(packet.getClass()))
			if (!listener.test((T) packet))
				return;
		oldIn.a(packet);
	}

	@Override
	public void a(PacketPlayInSetCreativeSlot packet) {
		if (type.isAssignableFrom(packet.getClass()))
			if (!listener.test((T) packet))
				return;
		oldIn.a(packet);
	}

	@Override
	public void a(PacketPlayInUpdateSign packet) {
		if (type.isAssignableFrom(packet.getClass()))
			if (!listener.test((T) packet))
				return;
		oldIn.a(packet);
	}

	@Override
	public void a(PacketPlayInBlockPlace packet) {
		if (type.isAssignableFrom(packet.getClass()))
			if (!listener.test((T) packet))
				return;
		oldIn.a(packet);
	}

	@Override
	public void a(PacketPlayInSpectate packet) {
		if (type.isAssignableFrom(packet.getClass()))
			if (!listener.test((T) packet))
				return;
		oldIn.a(packet);
	}

	@Override
	public void a(PacketPlayInResourcePackStatus packet) {
		if (type.isAssignableFrom(packet.getClass()))
			if (!listener.test((T) packet))
				return;
		oldIn.a(packet);
	}

	@Override
	public void a(IChatBaseComponent packet) {
		old.a(packet);
	}
}
