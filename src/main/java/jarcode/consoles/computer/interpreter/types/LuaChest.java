package jarcode.consoles.computer.interpreter.types;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.inventory.*;
import org.luaj.vm2.LuaValue;

import java.util.*;

public class LuaChest {

	private Chest bukkit;
	private Inventory inv;
	public LuaChest(Chest bukkit) {
		this.bukkit = bukkit;
		this.inv = bukkit.getBlockInventory();
	}
	public int size() {
		return inv.getSize();
	}
	public int amountAt(int slot) {
		ItemStack stack = inv.getItem(slot);
		return stack == null ? 0 : stack.getAmount();
	}
	public String typeAt(int slot) {
		ItemStack stack = inv.getItem(slot);
		return stack == null ? "AIR" : stack.getType().name();
	}
	public int move(int slot, LuaValue table) {
		return move(slot, table, 64);
	}
	public int move(int slot, LuaValue table, int amt) {
		if (amt == 0 || table == LuaValue.NIL || !table.isuserdata() || slot < 0) return -2;
		LuaChest chest = (LuaChest) table.checkuserdata();
		ItemStack source = inv.getItem(slot);
		if (source == null || source.getType() == Material.AIR) return -1;
		ItemStack clone = source.clone();
		if (source.getAmount() > amt) {
			source.setAmount(source.getAmount() - amt);
		}
		else {
			amt = source.getAmount();
			inv.setItem(slot, new ItemStack(Material.AIR));
		}
		clone.setAmount(amt);
		chest.inv.addItem(clone);
		return amt;
	}
	public int craft(String material) {
		return craft(material, 0);
	}
	@SuppressWarnings("Convert2streamapi")
	public int craft(String material, int durability) {
		String fm = material.toUpperCase();
		Material target = Arrays.asList(Material.values()).stream()
				.filter((m) -> m.name().equals(fm))
				.findFirst()
				.orElseGet(() -> null);
		if (target == null) return -1; // invalid target name
		Iterator<Recipe> it = Bukkit.getServer().recipeIterator();
		Recipe r = null;
		while (it.hasNext()) {
			Recipe recipe = it.next();
			if (recipe.getResult().getType() == target && recipe.getResult().getDurability() == durability) {
				r = recipe;
				break;
			}
		}
		if (r == null)
			return -1; // no recipe for target
		List<ItemStack> args = null;
		if (r instanceof ShapedRecipe) {
			args = new ArrayList<>();
			ShapedRecipe shaped = (ShapedRecipe) r;
			List<Character> list = new ArrayList<>();
			Map<Character, Integer> counts = new HashMap<>();
			for (String str : shaped.getShape()) {
				for (char c : str.toCharArray()) {
					if (!counts.containsKey(c))
						counts.put(c, 0);
					int current = counts.get(c);
					counts.put(c, current + 1);
				}
			}
			for (Map.Entry<Character, ItemStack> entry : shaped.getIngredientMap().entrySet()) {
				if (!list.contains(entry.getKey())) {
					for (int t = 0; t < counts.get(entry.getKey()); t++)
						args.add(entry.getValue());
					list.add(entry.getKey());
				}
			}
		}
		else if (r instanceof ShapelessRecipe) {
			ShapelessRecipe shapeless = (ShapelessRecipe) r;
			args = shapeless.getIngredientList();
		}
		if (args == null)
			return -2; // invalid recipe
		ItemStack[] arr = inv.getContents();
		for (int t = 0; t < arr.length; t++)
			arr[t] = arr[t].clone();
		for (ItemStack ingredient : args) {
			boolean found = false;
			for (int t = 0; t < arr.length; t++) {
				if (arr[t] != null
						&& arr[t].getDurability() == ingredient.getDurability()
						&& arr[t].getType() == ingredient.getType()) {
					arr[t] = null;
					found = true;
					break;
				}
			}
			if (!found) return 0; // not enough ingredients!
		}
		inv.setContents(arr);
		inv.addItem(r.getResult().clone());
		return 1; // success!
	}
}
