package jarcode.consoles.util;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.GenericFutureListener;
import jarcode.controller.loader.UnsafeTools;
import net.minecraft.server.v1_8_R1.*;

import javax.crypto.SecretKey;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;

// This is a wrapper for a client's network manager
// This is a much safer alternative than using instrumentation to
// get information about packet destinations (ProtocolLib)
//
// However, I don't know if this will screw ProtocolLib up. If it
// does, good. It's terrible.
//
// We can also effectively listen to any method in this class
public class NetworkManagerWrapper extends NetworkManager {

	private static final Method CHANNEL_READ_0;
	private static final Method HANDLE_LISTENER;
	private static final Field PROTOCOL_DIRECTION;
	private static final Field CHANNEL;

	static {
		try {
			CHANNEL_READ_0 = NetworkManager.class
					.getDeclaredMethod("channelRead0", ChannelHandlerContext.class, Object.class);
			CHANNEL_READ_0.setAccessible(true);
			HANDLE_LISTENER = NetworkManager.class.getDeclaredMethod("a", ChannelHandlerContext.class, Packet.class);
			HANDLE_LISTENER.setAccessible(true);
			PROTOCOL_DIRECTION = NetworkManager.class.getDeclaredField("g");
			PROTOCOL_DIRECTION.setAccessible(true);
			CHANNEL = NetworkManager.class.getDeclaredField("i");
			CHANNEL.setAccessible(true);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * <p>Wraps a network manager in an instance of this class. The wrapper
	 * contains uninitialized fields, and overloads every method to
	 * pass to the underlying manager.</p>
	 *
	 * <p>You can wrap a wrapper, just in case you were wondering.</p>
	 *
	 * @param manager the manager to wrap
	 * @return a wrapped manager
	 */
	public static NetworkManagerWrapper wrap(NetworkManager manager) {

		// check protocol state, this wrapper is only meant for 'play'
		Object protocol;
		try {
			protocol = ((Channel) CHANNEL.get(manager)).attr(c).get();
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Could not obtain protocol", e);
		}
		if (protocol != EnumProtocol.PLAY) {
			throw new RuntimeException("Wrong protocol: " + protocol);
		}

		NetworkManagerWrapper wrapper;
		try {
			// create wrapper, and avoid calling <init> code and constructors
			// this avoids creating pointless netty channels in the network manager
			// that the wrapper extends
			wrapper = UnsafeTools.allocateInstance(NetworkManagerWrapper.class);
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		}
		wrapper.init(manager);
		return wrapper;
	}

	private NetworkManager underlying;
	private HashMap<Class<? extends Packet>, List<Predicate<? extends Packet>>> listeners;

	private void init(NetworkManager underlying) {
		// initialize this here, otherwise we get problems with dead references
		listeners = new HashMap<>();
		// underlying, 'real' network manager
		this.underlying = underlying;
		// copy references to public fields in original object
		this.j = underlying.j;
		this.spoofedUUID = underlying.spoofedUUID;
		this.spoofedProfile = underlying.spoofedProfile;
		this.preparing = underlying.preparing;
		try {
			// copy the channel instance, because there's a static method to obtain it
			CHANNEL.set(this, CHANNEL.get(underlying));
			// copy protocol direction
			PROTOCOL_DIRECTION.set(this, PROTOCOL_DIRECTION.get(underlying));
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	// this is never called, because we allocate an instance of this wrapper directly.
	@SuppressWarnings("unused")
	private NetworkManagerWrapper(EnumProtocolDirection enumprotocoldirection) {
		super(enumprotocoldirection);
	}

	/*
	 * We overload EVERY method here so that this acts exactly like the underlying wrapper.
	 * There's only a few instances where we need to flip the 'preparing' flag in addition
	 * to passing the method call to the underlying manager. This way the 'preparing' flag
	 * corresponds with the underlying flag.
	 */

	@Override
	public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
		underlying.channelRegistered(ctx);
	}

	@Override
	public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
		underlying.channelUnregistered(ctx);
	}

	@Override
	public void channelActive(ChannelHandlerContext channelhandlercontext) throws Exception {
		underlying.channelActive(channelhandlercontext);
		// flip the preparing flag to correspond with the underlying object
		this.preparing = false;
	}

	@Override
	public void a(EnumProtocol enumprotocol) {
		underlying.a(enumprotocol);
	}

	@Override
	public void channelInactive(ChannelHandlerContext channelhandlercontext) {
		underlying.channelInactive(channelhandlercontext);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext channelhandlercontext, Throwable throwable) {
		underlying.exceptionCaught(channelhandlercontext, throwable);
	}

	@Override
	protected void a(ChannelHandlerContext channelhandlercontext, Packet packet) {
		try {
			HANDLE_LISTENER.invoke(underlying, channelhandlercontext, packet);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void a(PacketListener packetlistener) {
		underlying.a(packetlistener);
	}
	public <T extends Packet> void registerOutgoingListener(Class<T> type, Predicate<T> function) {
		if (!listeners.containsKey(type))
			listeners.put(type, new ArrayList<>());
		List<Predicate<? extends Packet>> list = listeners.get(type);
		list.add(function);
	}
	@SuppressWarnings("unchecked")
	private <T extends Packet> boolean handle(Predicate<T> listener, Packet packet) {
		return listener.test((T) packet);
	}
	@SuppressWarnings("SuspiciousMethodCalls")
	private List<Predicate<? extends Packet>> listenersFor(Class<? extends Packet> type) {
		ArrayList<Predicate<? extends Packet>> list = new ArrayList<>();
		Class<?> at = type;
		while (at != Packet.class && at != null) {
			List<Predicate<? extends Packet>> from = listeners.get(at);
			if (from != null)
				list.addAll(from);
			at = at.getSuperclass();
		}
		return list;
	}
	@Override
	public void handle(Packet packet) {
		List<Predicate<? extends Packet>> list = listenersFor(packet.getClass());
		for (Predicate<? extends Packet> listener : list) {
			try {
				if (!handle(listener, packet))
					return;
			}
			catch (Throwable e) {
				e.printStackTrace();
			}
		}
		underlying.handle(packet);
	}

	@Override
	public void a(Packet packet, GenericFutureListener genericfuturelistener, GenericFutureListener... listeners) {
		underlying.a(packet, genericfuturelistener, listeners);
	}

	@Override
	public void a() {
		underlying.a();
	}

	@Override
	public SocketAddress getSocketAddress() {
		return underlying.getSocketAddress();
	}

	@Override
	public void close(IChatBaseComponent ichatbasecomponent) {
		// flip the preparing flag to correspond with the underlying object
		preparing = false;
		underlying.close(ichatbasecomponent);
	}

	@Override
	public boolean c() {
		return underlying.c();
	}

	@Override
	public void a(SecretKey secretkey) {
		underlying.a(secretkey);
	}

	@Override
	public boolean g() {
		return underlying.g();
	}

	@Override
	public boolean h() {
		return underlying.h();
	}

	@Override
	public PacketListener getPacketListener() {
		return underlying.getPacketListener();
	}

	@Override
	public IChatBaseComponent j() {
		return underlying.j();
	}

	@Override
	public void k() {
		underlying.k();
	}

	@Override
	public void a(int i) {
		underlying.a(i);
	}

	@Override
	public void l() {
		underlying.l();
	}

	@Override
	public boolean acceptInboundMessage(Object msg) throws Exception {
		return underlying.acceptInboundMessage(msg);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		underlying.channelRead(ctx, msg);
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		underlying.channelReadComplete(ctx);
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		underlying.userEventTriggered(ctx, evt);
	}

	@Override
	public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
		underlying.channelWritabilityChanged(ctx);
	}

	@Override
	protected void channelRead0(ChannelHandlerContext channelhandlercontext, Object object) {
		// we can't access protected methods, use reflection
		try {
			CHANNEL_READ_0.invoke(underlying, channelhandlercontext, object);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public SocketAddress getRawAddress() {
		return underlying.getRawAddress();
	}

	@Override
	public boolean isSharable() {
		return underlying.isSharable();
	}

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		underlying.handlerAdded(ctx);
	}

	@Override
	public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
		underlying.handlerRemoved(ctx);
	}
	@Override
	public int hashCode() {
		return underlying.hashCode();
	}

	@SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
	@Override
	public boolean equals(Object obj) {
		return underlying.equals(obj);
	}

	@Override
	public String toString() {
		return underlying.toString();
	}
}
