package mekanism.common.inventory.container.tile.filter.list;

import javax.annotation.Nonnull;
import mekanism.common.inventory.container.MekanismContainerTypes;
import mekanism.common.inventory.container.tile.filter.FilterEmptyContainer;
import mekanism.common.tile.TileEntityLogisticalSorter;
import mekanism.common.util.text.TextComponentUtil;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.ITextComponent;

public class LSFilterListContainer extends FilterEmptyContainer<TileEntityLogisticalSorter> {

    public LSFilterListContainer(int id, PlayerInventory inv, TileEntityLogisticalSorter tile) {
        super(MekanismContainerTypes.LS_FILTER_LIST, id, inv, tile);
    }

    public LSFilterListContainer(int id, PlayerInventory inv, PacketBuffer buf) {
        this(id, inv, getTileFromBuf(buf, TileEntityLogisticalSorter.class));
    }

    @Nonnull
    @Override
    public ITextComponent getDisplayName() {
        return TextComponentUtil.translate("mekanism.container.logistical_sorter_filter_list");
    }
}