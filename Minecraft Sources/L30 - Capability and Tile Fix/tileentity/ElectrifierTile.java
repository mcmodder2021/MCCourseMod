package com.kaupenjoe.coppermod.tileentity;

import com.kaupenjoe.coppermod.item.ModItems;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.apache.logging.log4j.LogManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ElectrifierTile extends TileEntity implements ITickableTileEntity
{
    private final ItemStackHandler itemHandler = createHandler();
    private final CustomEnergyStorage energyStorage = createEnergyStorage();

    private final LazyOptional<IItemHandler> handler = LazyOptional.of(() -> itemHandler);
    private final LazyOptional<IEnergyStorage> energyHandler = LazyOptional.of(() -> energyStorage);

    private int tick = 0;

    public ElectrifierTile(TileEntityType<?> tileEntityTypeIn)
    {
        super(tileEntityTypeIn);
    }

    public ElectrifierTile()
    {
        this(ModTileEntities.ELECTRIFIER_TILE_ENTITY.get());
    }

    private CustomEnergyStorage createEnergyStorage()
    {
        return new CustomEnergyStorage(100, 0) {
            @Override
            protected void onEnergyChanged()
            {
                markDirty();
            }
        };
    }

    @Override
    public void remove() {
        super.remove();
        handler.invalidate();
        energyHandler.invalidate();
    }

    @Override
    public void tick()
    {
        if(world.isRemote) {
            return;
        }

        tick++;
        if(tick > 10) // should be configurable
        {
            if(this.itemHandler.getStackInSlot(0).getItem() == Items.DIAMOND && energyStorage.getEnergyStored() < 64)
            {
                itemHandler.extractItem(0, 1, false); // "deletes" the item
                energyStorage.generatePower(1);
            }

            if(this.itemHandler.getStackInSlot(1).getItem() == ModItems.COPPER_WIRE.get()
                && energyStorage.getEnergyStored() > 0 && this.itemHandler.getStackInSlot(2).getCount() < 64)
            {
                itemHandler.extractItem(1, 1, false);
                itemHandler.insertItem(2, new ItemStack(Items.EMERALD, 1), false);
                energyStorage.consumePower(1);
            }

            tick = 0;
            markDirty();
        }
    }

    @Override
    public void read(BlockState state, CompoundNBT tag)
    {
        itemHandler.deserializeNBT(tag.getCompound("inv"));
        energyStorage.deserializeNBT(tag.getCompound("energy"));

        tick = tag.getInt("counter");
        super.read(state, tag);
    }

    @Override
    public CompoundNBT write(CompoundNBT tag)
    {
        tag.put("inv", itemHandler.serializeNBT());
        tag.put("energy", energyStorage.serializeNBT());

        tag.putInt("counter", tick);
        return super.write(tag);
    }

    private ItemStackHandler createHandler()
    {
        return new ItemStackHandler(3)
        {
            @Override
            protected void onContentsChanged(int slot)
            {
                markDirty();
            }

            @Override
            public boolean isItemValid(int slot, @Nonnull ItemStack stack)
            {
                switch (slot)
                {
                    case 0: return stack.getItem() == Items.DIAMOND;
                    case 1: return stack.getItem() == ModItems.COPPER_WIRE.get();
                    case 2: return stack.getItem() == Items.EMERALD;
                    default: return false;
                }
            }

            @Nonnull
            @Override
            public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate)
            {
                if(!isItemValid(slot, stack))
                {
                    return stack;
                }

                return super.insertItem(slot, stack, simulate);
            }
        };
    }

    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction side)
    {
        if(capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
        {
            return handler.cast();
        }
        else if(capability == CapabilityEnergy.ENERGY)
        {
            return energyHandler.cast();
        }

        return super.getCapability(capability, side);
    }
}
