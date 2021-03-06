package com.markus1002.extraboats.entity.item.boat;

import javax.annotation.Nullable;

import com.markus1002.extraboats.compatibility.Atmospheric;
import com.markus1002.extraboats.compatibility.Autumnity;
import com.markus1002.extraboats.compatibility.BambooBlocks;
import com.markus1002.extraboats.compatibility.BiomesOPlenty;
import com.markus1002.extraboats.compatibility.Bloomful;
import com.markus1002.extraboats.compatibility.BuzzierBees;
import com.markus1002.extraboats.compatibility.EndergeticExpansion;
import com.markus1002.extraboats.compatibility.SwampExpansion;
import com.markus1002.extraboats.compatibility.UpgradeAquatic;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.item.BoatEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Hand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootParameterSets;
import net.minecraft.world.storage.loot.LootParameters;
import net.minecraft.world.storage.loot.LootTable;

public abstract class ContainerBoatEntity extends ModBoatEntity implements IInventory, INamedContainerProvider
{
	private NonNullList<ItemStack> boatContainerItems = NonNullList.withSize(36, ItemStack.EMPTY);
	private boolean dropContentsWhenDead = true;
	@Nullable
	private ResourceLocation lootTable;
	private long lootTableSeed;

	public ContainerBoatEntity(EntityType<? extends BoatEntity> entityType, World worldIn)
	{
		super(entityType, worldIn);
	}

	protected void dropBreakItems()
	{
		super.dropBreakItems();
		if (!this.world.isRemote && this.dropContentsWhenDead && this.world.getGameRules().getBoolean(GameRules.DO_ENTITY_DROPS))
		{
			InventoryHelper.dropInventoryItems(this.world, this, this);
		}
	}
	
	public void killBoat()
	{
		super.killBoat();
		if (!this.world.isRemote && this.dropContentsWhenDead && this.world.getGameRules().getBoolean(GameRules.DO_ENTITY_DROPS))
		{
			InventoryHelper.dropInventoryItems(this.world, this, this);
		}
	}

	public boolean isEmpty()
	{
		for(ItemStack itemstack : this.boatContainerItems)
		{
			if (!itemstack.isEmpty())
			{
				return false;
			}
		}
		return true;
	}

	public ItemStack getStackInSlot(int index)
	{
		this.addLoot((PlayerEntity)null);
		return this.boatContainerItems.get(index);
	}

	public ItemStack decrStackSize(int index, int count)
	{
		this.addLoot((PlayerEntity)null);
		return ItemStackHelper.getAndSplit(this.boatContainerItems, index, count);
	}

	public ItemStack removeStackFromSlot(int index)
	{
		this.addLoot((PlayerEntity)null);
		ItemStack itemstack = this.boatContainerItems.get(index);
		if (itemstack.isEmpty())
		{
			return ItemStack.EMPTY;
		}
		else
		{
			this.boatContainerItems.set(index, ItemStack.EMPTY);
			return itemstack;
		}
	}

	public void setInventorySlotContents(int index, ItemStack stack)
	{
		this.addLoot((PlayerEntity)null);
		this.boatContainerItems.set(index, stack);
		if (!stack.isEmpty() && stack.getCount() > this.getInventoryStackLimit())
		{
			stack.setCount(this.getInventoryStackLimit());
		}
	}

	public boolean replaceItemInInventory(int inventorySlot, ItemStack itemStackIn)
	{
		if (inventorySlot >= 0 && inventorySlot < this.getSizeInventory())
		{
			this.setInventorySlotContents(inventorySlot, itemStackIn);
			return true;
		}
		else
		{
			return false;
		}
	}

	public void markDirty()
	{
	}

	public boolean isUsableByPlayer(PlayerEntity player)
	{
		if (this.removed)
		{
			return false;
		}
		else
		{
			return !(player.getDistanceSq(this) > 64.0D);
		}
	}

	@Nullable
	public Entity changeDimension(DimensionType destination)
	{
		this.dropContentsWhenDead = false;
		return super.changeDimension(destination);
	}

	@Override
	public void remove(boolean keepData)
	{
		super.remove(keepData);
		if (!keepData) itemHandler.invalidate();
	}

	protected void writeAdditional(CompoundNBT compound)
	{
		super.writeAdditional(compound);
		if (this.lootTable != null)
		{
			compound.putString("LootTable", this.lootTable.toString());
			if (this.lootTableSeed != 0L)
			{
				compound.putLong("LootTableSeed", this.lootTableSeed);
			}
		}
		else
		{
			ItemStackHelper.saveAllItems(compound, this.boatContainerItems);
		}
	}

	protected void readAdditional(CompoundNBT compound)
	{
		super.readAdditional(compound);
		this.boatContainerItems = NonNullList.withSize(this.getSizeInventory(), ItemStack.EMPTY);
		if (compound.contains("LootTable", 8))
		{
			this.lootTable = new ResourceLocation(compound.getString("LootTable"));
			this.lootTableSeed = compound.getLong("LootTableSeed");
		}
		else
		{
			ItemStackHelper.loadAllItems(compound, this.boatContainerItems);
		}
	}

	public boolean processInitialInteract(PlayerEntity player, Hand hand)
	{
		if (player.isShiftKeyDown())
		{
			player.openContainer(this);
			return true;
		}
		else
		{
			return super.processInitialInteract(player, hand);
		}
	}

	public void addLoot(@Nullable PlayerEntity player)
	{
		if (this.lootTable != null && this.world.getServer() != null)
		{
			LootTable loottable = this.world.getServer().getLootTableManager().getLootTableFromLocation(this.lootTable);
			this.lootTable = null;
			LootContext.Builder lootcontext$builder = (new LootContext.Builder((ServerWorld)this.world)).withParameter(LootParameters.POSITION, new BlockPos(this)).withSeed(this.lootTableSeed);
			lootcontext$builder.withParameter(LootParameters.KILLER_ENTITY, this);
			if (player != null)
			{
				lootcontext$builder.withLuck(player.getLuck()).withParameter(LootParameters.THIS_ENTITY, player);
			}

			loottable.fillInventory(this, lootcontext$builder.build(LootParameterSets.CHEST));
		}
	}

	public void clear()
	{
		this.addLoot((PlayerEntity)null);
		this.boatContainerItems.clear();
	}

	public void setLootTable(ResourceLocation lootTableIn, long lootTableSeedIn)
	{
		this.lootTable = lootTableIn;
		this.lootTableSeed = lootTableSeedIn;
	}

	@Nullable
	public Container createMenu(int p_createMenu_1_, PlayerInventory p_createMenu_2_, PlayerEntity p_createMenu_3_)
	{
		if (this.lootTable != null && p_createMenu_3_.isSpectator())
		{
			return null;
		}
		else
		{
			this.addLoot(p_createMenu_2_.player);
			return this.func_213968_a(p_createMenu_1_, p_createMenu_2_);
		}
	}

	protected abstract Container func_213968_a(int p_213968_1_, PlayerInventory p_213968_2_);

	private net.minecraftforge.common.util.LazyOptional<?> itemHandler = net.minecraftforge.common.util.LazyOptional.of(() -> new net.minecraftforge.items.wrapper.InvWrapper(this));

	@Override
	public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable net.minecraft.util.Direction facing)
	{
		if (this.isAlive() && capability == net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
			return itemHandler.cast();
		return super.getCapability(capability, facing);
	}

	public void dropContentsWhenDead(boolean value)
	{
		this.dropContentsWhenDead = value;
	}

	public Item getItemDropBoat()
	{
		switch(this.getModBoatType())
		{
		case OAK:
		default:
			return Items.OAK_BOAT;
		case SPRUCE:
			return Items.SPRUCE_BOAT;
		case BIRCH:
			return Items.BIRCH_BOAT;
		case JUNGLE:
			return Items.JUNGLE_BOAT;
		case ACACIA:
			return Items.ACACIA_BOAT;
		case DARK_OAK:
			return Items.DARK_OAK_BOAT;
			
		case CHERRY:
			return BiomesOPlenty.CHERRY_BOAT;
		case DEAD:
			return BiomesOPlenty.DEAD_BOAT;
		case ETHEREAL:
			return BiomesOPlenty.ETHEREAL_BOAT;
		case FIR:
			return BiomesOPlenty.FIR_BOAT;
		case HELLBARK:
			return BiomesOPlenty.HELLBARK_BOAT;
		case JACARANDA:
			return BiomesOPlenty.JACARANDA_BOAT;
		case MAGIC:
			return BiomesOPlenty.MAGIC_BOAT;
		case MAHOGANY:
			return BiomesOPlenty.MAHOGANY_BOAT;
		case PALM:
			return BiomesOPlenty.PALM_BOAT;
		case REDWOOD:
			return BiomesOPlenty.REDWOOD_BOAT;
		case UMBRAN:
			return BiomesOPlenty.UMBRAN_BOAT;
		case WILLOW:
			return BiomesOPlenty.WILLOW_BOAT;
			
		case DRIFTWOOD:
			return UpgradeAquatic.DRIFTWOOD_BOAT;
		case RIVER:
			return UpgradeAquatic.RIVER_BOAT;
			
		case BAMBOO:
			return BambooBlocks.BAMBOO_BOAT;
			
		case POISE:
			return EndergeticExpansion.POISE_BOAT;
			
		case WISTERIA:
			return Bloomful.WISTERIA_BOAT;
			
		case SE_WILLOW:
			return SwampExpansion.WILLOW_BOAT;
			
		case ROSEWOOD:
			return Atmospheric.ROSEWOOD_BOAT;
		case ASPEN:
			return Atmospheric.ASPEN_BOAT;
		case KOUSA:
			return Atmospheric.KOUSA_BOAT;
		case YUCCA:
			return Atmospheric.YUCCA_BOAT;
			
		case MAPLE:
			return Autumnity.MAPLE_BOAT;
			
		case HIVE:
			return BuzzierBees.HIVE_BOAT;
		}
	}

	public void updatePassenger(Entity passenger)
	{
		if (this.isPassenger(passenger))
		{
			float f1 = (float)((this.removed ? (double)0.01F : this.getMountedYOffset()) + passenger.getYOffset());

			Vec3d vec3d = (new Vec3d((double)0.2F, 0.0D, 0.0D)).rotateYaw(-this.rotationYaw * ((float)Math.PI / 180F) - ((float)Math.PI / 2F));
			passenger.setPosition(this.getPosX() + vec3d.x, this.getPosY() + (double)f1, this.getPosZ() + vec3d.z);
			passenger.rotationYaw += this.deltaRotation;
			passenger.setRotationYawHead(passenger.getRotationYawHead() + this.deltaRotation);
			this.applyYawToEntity(passenger);
			if (passenger instanceof AnimalEntity)
			{
				int j = passenger.getEntityId() % 2 == 0 ? 90 : 270;
				passenger.setRenderYawOffset(((AnimalEntity)passenger).renderYawOffset + (float)j);
				passenger.setRotationYawHead(passenger.getRotationYawHead() + (float)j);
			}
		}
	}

	protected boolean canFitPassenger(Entity passenger)
	{
		return !this.isBeingRidden() && !this.areEyesInFluid(FluidTags.WATER);
	}
}