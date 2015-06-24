package ca.jarcode.consoles.drone;

import net.minecraft.server.v1_8_R3.*;

public class DroneEntity extends EntityChicken {
	public DroneEntity(World world) {
		super(world);
	}

	@Override
	protected void initAttributes() {
		super.initAttributes();
		this.setInvisible(true);
	}

	@Override
	public boolean isInLove() {
		return false;
	}

	@Override
	public boolean isInvulnerable(DamageSource source) {
		return true;
	}

	@Override
	public void m() {
		// do nothing
	}

	@Override
	public void e(float f, float f1) {
		// nothing is done here anyways
	}

	// sound
	@Override
	protected String z() {
		return super.z();
	}

	// sound
	@Override
	protected String bo() {
		return super.bo();
	}

	// sound
	@Override
	protected String bp() {
		return super.bp();
	}

	// sound
	@Override
	protected void a(BlockPosition blockposition, Block block) {
		super.a(blockposition, block);
	}

	// death loot
	@Override
	protected Item getLoot() {
		return null;
	}

	// death loot
	@Override
	protected void dropDeathLoot(boolean flag, int i) {}

	// make baby chicken
	@Override
	public EntityChicken b(EntityAgeable entityageable) {
		return null;
	}

	// attractive item type (for path finding)
	@Override
	public boolean d(ItemStack itemstack) {
		return false;
	}

	// load
	@Override
	public void a(NBTTagCompound nbttagcompound) {
		super.a(nbttagcompound);
	}

	// exp
	@Override
	protected int getExpValue(EntityHuman entityhuman) {
		return 0;
	}

	// save
	@Override
	public void b(NBTTagCompound nbttagcompound) {
		super.b(nbttagcompound);
	}

	@Override
	protected boolean isTypeNotPersistent() {
		return true;
	}

	@Override
	public void al() {}

	@Override
	public boolean isChickenJockey() {
		return false;
	}

	// set jockey
	@Override
	public void l(boolean flag) {}


	@Override
	public EntityAgeable createChild(EntityAgeable entityageable) {
		return super.createChild(entityageable);
	}
}
