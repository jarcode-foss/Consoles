package jarcode.consoles.computer.filesystem;

import java.util.Map;
import java.util.UUID;

public abstract class FSBlock {

	public String owner = "root";
	// own  all
	// !rwx rwx?
	// 0110 1000
	public byte permissions = 0x78;

	// this is for serialization of filesystems. There might be duplicate objects in the filesystem tree,
	// so this is what we use to identify them when recreating the tree
	public final UUID uuid;

	// instead of serializing these with classes, we assign ids in the block implementations to refer to types
	public final byte id;

	public FSBlock(byte id) {
		this.uuid = UUID.randomUUID();
		this.id = id;
	}
	public FSBlock(byte id, UUID uuid) {
		this.uuid = uuid;
		this.id = id;
	}
	// this is just some vague idea of the memory contained in this block, related to how much memory it actually uses
	public int size() {
		int size = 0;
		if (this instanceof FSFolder) {
			size += 16; // map header
			for (Map.Entry<String, FSBlock> entry : ((FSFolder) this).contents.entrySet()) {
				size += 48 + (entry.getKey().length() * 4);
				size += entry.getValue().size();
			}
		}
		else if (this instanceof FSFile) {
			size += ((FSFile) this).getSize() + 12;
		}
		return size;
	}
	public static boolean allowedBlockName(String str) {
		for (char c : str.toCharArray()) {
			if (!((0x30 <= c && 0x39 >= c) || (0x41 <= c && 0x5A >= c)
					|| (0x61 <= c && 0x7A >= c) || c == '.' || c == '$' || c == '_' || c == '(' || c == ')')) {
				return false;
			}
		}
		return true;
	}
	// mask(0x7E) is a fast way to grant all permissions
	// mask(0x68) is the default (read, write for owner, read for user)
	// mask(0x78) is the default for root (read, write, and execute for root, read for user)
	public void mask(byte v) {
		permissions |= v;
	}
	// checks the bit at a certain index of the permissions byte
	public boolean check(int i) {
		return (permissions & (0x01 << i)) != 0;
	}
	// checks for a mask on the permissions byte;
	public boolean check(byte mask) {
		return (permissions & mask) == mask;
	}
	// noob-friendly permission check
	public boolean check(FSGroup group, char perm) {
		switch (perm) {
			case 'r': return group == FSGroup.OWNER ? check(6) : check(3);
			case 'w': return group == FSGroup.OWNER ? check(5): check(2);
			case 'x': return group == FSGroup.OWNER ? check(4) : check(1);
			default: return false;
		}
	}
	public void setReadable(FSGroup group, boolean r) {
		if (group == FSGroup.OWNER)
			if (r) permissions |= 0x40; else permissions &= ~0x40;
		if (group == FSGroup.ALL)
			if (r) permissions |= 0x08; else permissions &= ~0x08;
	}
	public void setWritable(FSGroup group, boolean w) {
		if (group == FSGroup.OWNER)
			if (w) permissions |= 0x20; else permissions &= ~0x20;
		if (group == FSGroup.ALL)
			if (w) permissions |= 0x04; else permissions &= ~0x04;
	}
	public void setExecutable(FSGroup group, boolean x) {
		if (group == FSGroup.OWNER)
			if (x) permissions |= 0x10; else permissions &= ~0x10;
		if (group == FSGroup.ALL)
			if (x) permissions |= 0x02; else permissions &= ~0x02;
	}
}