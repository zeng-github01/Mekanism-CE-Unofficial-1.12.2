package mekanism.common.integration;

import mcjty.theoneprobe.api.*;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasTankInfo;
import mekanism.api.gas.IGasHandler;
import mekanism.common.Mekanism;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.config.MekanismConfig;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;

import java.util.function.Function;

@SuppressWarnings("unused")//IMC bound
public class TOPProvider implements Function<ITheOneProbe, Void>, IProbeInfoProvider {

    @Override
    public Void apply(ITheOneProbe iTheOneProbe) {
        iTheOneProbe.registerProvider(this);
        return null;
    }

    @Override
    public String getID() {
        return Mekanism.MODID;
    }

    @Override
    public void addProbeInfo(ProbeMode mode, IProbeInfo probeInfo, EntityPlayer player, World world, IBlockState blockState, IProbeHitData data) {
        if (!MekanismConfig.current().mekce.GasTOP.val()) {
            if (mode != ProbeMode.EXTENDED) {
                return;
            }
        }

        final TileEntity tile = world.getTileEntity(data.getPos());

        if (tile != null) {
            if (tile.hasCapability(Capabilities.GAS_HANDLER_CAPABILITY, null)) {
                IGasHandler handler = tile.getCapability(Capabilities.GAS_HANDLER_CAPABILITY, null);
                if (handler != null) {
                    GasTankInfo[] tanks = handler.getTankInfo();
                    for (GasTankInfo tank : tanks) {
                        IProgressStyle style = probeInfo.defaultProgressStyle().suffix("mB");
                        if (tank.getGas() != null) {
                            Gas gas = tank.getGas().getGas();
                            probeInfo.text(TextStyleClass.NAME +  I18n.translateToLocal("gui.gas:") + gas.getLocalizedName());
                            int tint = gas.getTint();
                            //TOP respects transparency so we need to filter out the transparent layer
                            // if the gas has one. (Currently they are all fully transparent)
                            if ((tint & 0xFF000000) == 0) {
                                tint = 0xFF000000 | tint;
                            }
                            if (tint != 0xFFFFFFFF) {
                                //TOP bugs out with full white background so just use default instead
                                // The default is a slightly off white color so is better for readability
                                style = style.filledColor(tint).alternateFilledColor(tint).borderColor(MekanismConfig.current().mekce.GasTopBarBorder.val()).numberFormat(NumberFormat.COMPACT);
                            }
                        }
                        probeInfo.progress(tank.getStored(), tank.getMaxGas(), style);
                    }
                }
            }
        }
    }
}
