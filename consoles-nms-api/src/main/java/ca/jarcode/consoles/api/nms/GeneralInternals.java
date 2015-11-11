package ca.jarcode.consoles.api.nms;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public interface GeneralInternals {

	interface InitResult {
		ItemFrame getEntity();
		int getEntityId();
	}

	boolean forceAddFrame(ItemFrame entity, World world);
	void forceRemoveFrame(ItemFrame entity, World world);
	boolean commandBlocksEnabled();
	void setCommandBlocksEnabled(boolean enabled);
	InitResult initFrame(World world, int x, int y, int z, short globalId, BlockFace face);

	void forceDeleteDirectory(File file) throws IOException;

	void fakeEnchantItem(ItemStack stack);
	void setItemNBTString(ItemStack stack, String key, String value);
	void setItemNBTBoolean(ItemStack stack, String key, boolean value);
	String getItemNBTString(ItemStack stack, String key);
	boolean getItemNBTBoolean(ItemStack stack, String key);
	boolean hasItemNBTTag(ItemStack stack);
	void modPlayerHead(ItemStack head, UUID owner, String texValue);
	void initHandle(ItemStack stack);

	ItemStack itemStackBuild(Material m, int i, short data, ItemMeta meta);
}
