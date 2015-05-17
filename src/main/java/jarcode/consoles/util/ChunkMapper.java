package jarcode.consoles.util;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multisets;
import jarcode.consoles.Position2D;
import jarcode.consoles.api.CanvasGraphics;
import net.minecraft.server.v1_8_R2.*;

import java.util.ArrayList;
import java.util.List;

public class ChunkMapper {

	// most of this code is decompiled and extracted from the minecraft server
	// I have not yet bothered to try to understand it. Most of this is
	// difficult to read because of the decompiler used.
	@SuppressWarnings({"unchecked", "ConstantConditions"})
	public static boolean updateSection(PreparedMapSection section, World world, int centerX, int centerZ,
	                                                     int updateX, int updateZ, int scale) {
		System.out.println("updating map with center: " + centerX + ", " + centerZ);

		boolean updated = false;

		int i = 1 << scale;
		int l = (updateX - centerX) / i + 64;
		int i1 = (updateZ - centerZ) / i + 64;
		int j1 = 128 / i;
		if(world.worldProvider.o()) {
			j1 /= 2;
		}

		for(int k1 = l - j1 + 1; k1 < l + j1; ++k1) {
			double d0 = 0.0D;

			for(int l1 = i1 - j1 - 1; l1 < i1 + j1; ++l1) {
				if(k1 >= 0 && l1 >= -1 && k1 < 128 && l1 < 128) {
					int i2 = k1 - l;
					int j2 = l1 - i1;
					boolean flag1 = i2 * i2 + j2 * j2 > (j1 - 2) * (j1 - 2);
					int k2 = (centerX / i + k1 - 64) * i;
					int l2 = (centerZ / i + l1 - 64) * i;
					HashMultiset hashmultiset = HashMultiset.create();
					Chunk chunk = world.getChunkAtWorldCoords(new BlockPosition(k2, 0, l2));
					if(!chunk.isEmpty()) {
						int i3 = k2 & 15;
						int j3 = l2 & 15;
						int k3 = 0;
						double d1 = 0.0D;
						if(world.worldProvider.o()) {
							int blockPosition = k2 + l2 * 231871;
							blockPosition = blockPosition * blockPosition * 31287121 + blockPosition * 11;
							if((blockPosition >> 20 & 1) == 0) {
								hashmultiset.add(Blocks.DIRT.g(Blocks.DIRT.getBlockData().set(BlockDirt.VARIANT, BlockDirt.EnumDirtVariant.DIRT)), 10);
							} else {
								hashmultiset.add(Blocks.STONE.g(Blocks.STONE.getBlockData().set(BlockStone.VARIANT, BlockStone.EnumStoneVariant.STONE)), 100);
							}

							d1 = 100.0D;
						} else {
							BlockPosition.MutableBlockPosition var37 = new BlockPosition.MutableBlockPosition();

							for(int i4 = 0; i4 < i; ++i4) {
								for(int b0 = 0; b0 < i; ++b0) {
									int material = chunk.b(i4 + i3, b0 + j3) + 1;
									IBlockData b1 = Blocks.AIR.getBlockData();
									if(material > 1) {
										do {
											--material;
											b1 = chunk.getBlockData(var37.c(i4 + i3, material, b0 + j3));
										} while(b1.getBlock().g(b1) == MaterialMapColor.b && material > 0);

										if(material > 0 && b1.getBlock().getMaterial().isLiquid()) {
											int b2 = material - 1;

											Block block;
											do {
												block = chunk.getTypeAbs(i4 + i3, b2--, b0 + j3);
												++k3;
											} while(b2 > 0 && block.getMaterial().isLiquid());
										}
									}

									d1 += (double)material / (double)(i * i);
									hashmultiset.add(b1.getBlock().g(b1));
								}
							}
						}

						k3 /= i * i;
						double d2 = (d1 - d0) * 4.0D / (double)(i + 4) + ((double)(k1 + l1 & 1) - 0.5D) * 0.4D;
						byte var38 = 1;
						if(d2 > 0.6D) {
							var38 = 2;
						}

						if(d2 < -0.6D) {
							var38 = 0;
						}

						MaterialMapColor var39 = Iterables.getFirst(Multisets.copyHighestCountFirst(hashmultiset), MaterialMapColor.b);
						if(var39 == MaterialMapColor.n) {
							d2 = (double)k3 * 0.1D + (double)(k1 + l1 & 1) * 0.2D;
							var38 = 1;
							if(d2 < 0.5D) {
								var38 = 2;
							}

							if(d2 > 0.9D) {
								var38 = 0;
							}
						}

						d0 = d1;
						if(l1 >= 0 && i2 * i2 + j2 * j2 < j1 * j1 && (!flag1 || (k1 + l1 & 1) != 0)) {
							synchronized (section.LOCK) {
								byte var40 = section.colors[k1 + l1 * 128];
								assert var39 != null;
								byte var41 = (byte) (var39.M * 4 + var38);
								if (var40 != var41) {
									section.colors[k1 + l1 * 128] = var41;
									section.flag(k1, l1);
									updated = true;
									System.out.println("Switching bit: " + k1 + ", " + l1);
								}
							}
						}
					}
				}
			}
		}
		return updated;
	}

	public static Position2D align(int x, int y, int scale) {
		int j = 128 * (1 << scale);
		int k = MathHelper.floor((x + 64.0D) / (double)j);
		int l = MathHelper.floor((y + 64.0D) / (double)j);
		return new Position2D(k * j + j / 2 - 64, l * j + j / 2 - 64);
	}

	public static class PreparedMapSection {
		private byte[] colors = new byte[128 * 128];
		private List<Position2D> flags = new ArrayList<>();
		private final Object LOCK = new Object();
		private void flag(int x, int y) {
			flags.add(new Position2D(x, y));
		}
		public void render(CanvasGraphics g, int x, int y) {
			synchronized (LOCK) {
				flags.stream()
						.filter(flag -> g.getWidth() > x + flag.getX() && g.getHeight() > y + flag.getY())
						.forEach(flag -> g.draw(flag.getX() + x, flag.getY() + y, colors[flag.getX() + (flag.getY() * 128)]));
				flags.clear();
			}
		}
	}
}
