package jarcode.consoles.computer.interpreter.types;

import jarcode.consoles.computer.manual.Arg;
import jarcode.consoles.computer.manual.FunctionManual;
import jarcode.consoles.computer.manual.TypeManual;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.inventory.*;
import org.luaj.vm2.LuaError;

import java.util.*;
import java.util.function.BooleanSupplier;

import static jarcode.consoles.computer.ProgramUtils.*;

@TypeManual("Represents a chest that this computer has access to.")
@SuppressWarnings("unused")
public class LuaChest {

	private Inventory inv;
	private BooleanSupplier terminated;

	public LuaChest(Chest bukkit, BooleanSupplier terminated) {
		this.inv = bukkit.getBlockInventory();
		this.terminated = terminated;
	}

	@FunctionManual("Returns the amount of slots in this chest")
	public int size() {
		return inv.getSize();
	}

	@FunctionManual("Returns the amount of items at the given slot")
	public int amountAt(
			@Arg(name = "slot", info = "the id of the slot to check") int slot) {
		ItemStack stack = inv.getItem(slot);
		return stack == null ? 0 : stack.getAmount();
	}

	@FunctionManual("Returns the type of the item at the given slot")
	public String typeAt(
			@Arg(name = "slot", info = "the id of the slot to check") int slot) {
		ItemStack stack = inv.getItem(slot);
		return stack == null ? "AIR" : stack.getType().name();
	}

	public int move(int slot, LuaChest chest) {
		return move(slot, chest, 64);
	}

	@FunctionManual("Moves items from a slot in this chest to another chest. Slot values start at 0 and range to " +
			"the chest size, minus one. This function has various return values:\n\n" +
			"\t\t&e-2&f - invalid arguments\n" +
			"\t\t&e-1&f - slot is empty\n" +
			"\t\totherwise, this function will return the amount of items moved")
	public int move(
			@Arg(name = "slot", info = "the id of the slot to move items from") int slot,
			@Arg(name = "chest", info = "the LuaChest to move the items to") LuaChest chest,
			@Arg(name = "amount", info = "&7(optional)&f amount of items to move") int amount) {
		return schedule(() -> {
			int amt = amount;
			if (amt == 0 || chest == null || slot < 0) return -2;
			ItemStack source = inv.getItem(slot);
			if (source == null || source.getType() == Material.AIR) return -1;
			ItemStack clone = source.clone();
			if (source.getAmount() > amt) {
				source.setAmount(source.getAmount() - amt);
			} else {
				amt = source.getAmount();
				inv.setItem(slot, new ItemStack(Material.AIR));
			}
			clone.setAmount(amt);
			chest.inv.addItem(clone);
			return amt;
		}, terminated);
	}

	public int craft(String material) {
		return craft(material, 0);
	}

	@FunctionManual("Attempts to craft a specific material with the items inside of this chest. An (optional) " +
			"durability argument can be provided to further specify the crafting recipe to use. This " +
			"function has various return values:\n\n" +
			"\t\t&e1&f - success\n" +
			"\t\t&e0&f - not enough ingredients\n" +
			"\t\t&e-1&f - invalid material name\n" +
			"\t\t&e-2&f - no recipe for given material\n" +
			"\t\t&e-3&f - invalid recipe type (occurs on rare plugin conflicts)")
	@SuppressWarnings("Convert2streamapi")
	public int craft(
			@Arg(name = "material", info = "material to craft, as a qualified Bukkit material name") String material,
			@Arg(name = "durability", info = "&7(optional)&f durability of the material to craft") int durability) {
		return schedule(() -> {
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
				return -2; // no recipe for target
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
				return -3; // invalid recipe
			ItemStack[] arr = inv.getContents();
			for (int t = 0; t < arr.length; t++)
				arr[t] = arr[t] != null ? arr[t].clone() : null;
			for (ItemStack ingredient : args) {
				boolean found = false;
				for (int t = 0; t < arr.length; t++) {
					if (arr[t] != null
							&& (arr[t].getDurability() == ingredient.getDurability() || ingredient.getDurability() == 32767)
							&& arr[t].getType() == ingredient.getType()) {
						if (arr[t].getAmount() <= 1)
							arr[t] = null;
						else arr[t].setAmount(arr[t].getAmount() - 1);
						found = true;
						break;
					}
				}
				if (!found) return 0; // not enough ingredients!
			}
			inv.setContents(arr);
			inv.addItem(r.getResult().clone());
			return 1; // success!
		}, terminated);
	}
}
