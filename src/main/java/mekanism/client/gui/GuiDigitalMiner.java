package mekanism.client.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import mekanism.api.Coord4D;
import mekanism.api.EnumColor;
import mekanism.api.TileNetworkList;
import mekanism.client.gui.button.GuiButtonDisableableImage;
import mekanism.client.gui.element.*;
import mekanism.client.gui.element.GuiSlot.SlotOverlay;
import mekanism.client.gui.element.GuiSlot.SlotType;
import mekanism.client.gui.element.tab.GuiSecurityTab;
import mekanism.client.gui.element.tab.GuiUpgradeTab;
import mekanism.client.gui.element.tab.GuiVisualsTab;
import mekanism.client.sound.SoundHandler;
import mekanism.common.Mekanism;
import mekanism.common.MekanismSounds;
import mekanism.common.content.miner.ThreadMinerSearch.State;
import mekanism.common.inventory.container.ContainerDigitalMiner;
import mekanism.common.network.PacketDigitalMinerGui.DigitalMinerGuiMessage;
import mekanism.common.network.PacketDigitalMinerGui.MinerGuiPacket;
import mekanism.common.network.PacketTileEntity.TileEntityMessage;
import mekanism.common.tile.TileEntityDigitalMiner;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiDigitalMiner extends GuiMekanismTile<TileEntityDigitalMiner> {

    public static final int START_BUTTON_ID = 0;
    public static final int STOP_BUTTON_ID = 1;
    public static final int CONFIG_BUTTON_ID = 2;
    public static final int RESET_BUTTON_ID = 3;
    private GuiButton startButton;
    private GuiButton stopButton;
    private GuiButton configButton;
    private GuiButton resetButton;

    public GuiDigitalMiner(InventoryPlayer inventory, TileEntityDigitalMiner tile) {
        super(tile, new ContainerDigitalMiner(inventory, tile));
        ResourceLocation resource = getGuiLocation();
        addGuiElement(new GuiRedstoneControl(this, tileEntity, resource, 0, 0));
        addGuiElement(new GuiSecurityTab(this, tileEntity, resource, 0, 0));
        addGuiElement(new GuiUpgradeTab(this, tileEntity, resource, 0, 0));
        addGuiElement(new GuiPowerBarshort(this, tileEntity, resource, 157, 38));
        addGuiElement(new GuiVisualsTab(this, tileEntity, resource));
        addGuiElement(new GuiEnergyInfo(() -> {
            double perTick = tileEntity.getPerTick();
            String multiplier = MekanismUtils.getEnergyDisplay(perTick);
            ArrayList<String> ret = new ArrayList<>(4);
            ret.add(LangUtils.localize("mekanism.gui.digitalMiner.capacity") + ": " + MekanismUtils.getEnergyDisplay(tileEntity.getMaxEnergy()));
            ret.add(LangUtils.localize("gui.needed") + ": " + multiplier + "/t");
            if (perTick > tileEntity.getMaxEnergy()) {
                ret.add(TextFormatting.RED + LangUtils.localize("mekanism.gui.insufficientbuffer"));
            }
            ret.add(LangUtils.localize("mekanism.gui.bufferfree") + ": " + MekanismUtils.getEnergyDisplay(tileEntity.getMaxEnergy() - tileEntity.getEnergy()));
            return ret;
        }, this, resource));
        addGuiElement(new GuiSlot(SlotType.POWER, this, resource, 151, 19).with(SlotOverlay.POWER));
        addGuiElement(new GuiBlack(this,resource,8,20,76,67));
        ySize += 76;
    }

    @Override
    public void initGui() {
        super.initGui();
        buttonList.clear();
        int buttonStart = 19;
        buttonList.add(this.startButton = new GuiButtonDisableableImage(START_BUTTON_ID, guiLeft + 87, guiTop + buttonStart, 61, 18, 180,18,-18,18,getGuiLocation()));
        buttonList.add(this.stopButton = new GuiButtonDisableableImage(STOP_BUTTON_ID, guiLeft + 87, guiTop + buttonStart + 17, 61, 18, 180,18,-18,18,getGuiLocation()));
        buttonList.add(this.configButton = new GuiButtonDisableableImage(CONFIG_BUTTON_ID, guiLeft + 87, guiTop + buttonStart + 34, 61, 18, 180,18,-18,18,getGuiLocation()));
        buttonList.add(this.resetButton = new GuiButtonDisableableImage(RESET_BUTTON_ID, guiLeft + 87, guiTop + buttonStart + 51, 61, 18, 180,18,-18,18,getGuiLocation()));
        updateEnabledButtons();
    }

    @Override
    protected void actionPerformed(GuiButton guibutton) throws IOException {
        super.actionPerformed(guibutton);
        switch (guibutton.id) {
            case START_BUTTON_ID:
                Mekanism.packetHandler.sendToServer(new TileEntityMessage(tileEntity, TileNetworkList.withContents(3)));
                break;
            case STOP_BUTTON_ID:
                Mekanism.packetHandler.sendToServer(new TileEntityMessage(tileEntity, TileNetworkList.withContents(4)));
                break;
            case CONFIG_BUTTON_ID:
                Mekanism.packetHandler.sendToServer(new DigitalMinerGuiMessage(MinerGuiPacket.SERVER, Coord4D.get(tileEntity), 0, 0, 0));
                break;
            case RESET_BUTTON_ID:
                Mekanism.packetHandler.sendToServer(new TileEntityMessage(tileEntity, TileNetworkList.withContents(5)));
                break;
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        updateEnabledButtons();
    }

    private void updateEnabledButtons() {
        startButton.enabled = tileEntity.searcher.state == State.IDLE || !tileEntity.running;
        stopButton.enabled = tileEntity.searcher.state != State.IDLE && tileEntity.running;
        configButton.enabled = tileEntity.searcher.state == State.IDLE;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString(tileEntity.getName(), (xSize / 2) - (fontRenderer.getStringWidth(tileEntity.getName()) / 2), 4, 0x404040);
        fontRenderer.drawString(LangUtils.localize("container.inventory"), 8, (ySize - 96) + 2, 0x404040);
        String runningType;
        if (tileEntity.getPerTick() > tileEntity.getMaxEnergy()) {
            runningType = LangUtils.localize("mekanism.gui.digitalMiner.lowPower");
        } else if (tileEntity.running) {
            runningType = LangUtils.localize("gui.digitalMiner.running");
        } else {
            runningType = LangUtils.localize("gui.idle");
        }
        fontRenderer.drawString(runningType, 9, 21, 0x00CD00);
        fontRenderer.drawString(tileEntity.searcher.state.localize(), 9, 31, 0x00CD00);
        fontRenderer.drawString(LangUtils.localize("gui.digitalMiner.toMine") + ":" + " " + tileEntity.clientToMine, 9, 41, 0x00CD00);
        if (!tileEntity.missingStack.isEmpty()) {
            drawColorIcon(64, 21, EnumColor.DARK_RED, 0.8F);
            renderItem(tileEntity.missingStack, 64, 21);
        } else {
            mc.getTextureManager().bindTexture(MekanismUtils.getResource(ResourceType.GUI_ELEMENT, "GuiSlot.png"));
            drawTexturedModalRect(64, 21, SlotOverlay.CHECK.textureX, SlotOverlay.CHECK.textureY, 18, 18);
        }
        fontRenderer.drawString(LangUtils.localize("gui.start"),87 + 61 / 2 - (fontRenderer.getStringWidth(LangUtils.localize("gui.start")) / 2), 19 + (18 - 8) / 2, startButton.enabled ? 0xffffff : 0x9e9e9e);
        fontRenderer.drawString(LangUtils.localize("gui.stop"),87 + 61 / 2  - (fontRenderer.getStringWidth(LangUtils.localize("gui.stop")) / 2), 19 +  17 + (18 - 8) / 2, stopButton.enabled ? 0xffffff : 0x9e9e9e);
        fontRenderer.drawString(LangUtils.localize("gui.config"),87 + 61 / 2  - (fontRenderer.getStringWidth(LangUtils.localize("gui.config")) / 2), 19 +  34 + (18  - 8) / 2, configButton.enabled ? 0xffffff : 0x9e9e9e);
        fontRenderer.drawString(LangUtils.localize("gui.digitalMiner.reset"),87  + 61 / 2  - (fontRenderer.getStringWidth(LangUtils.localize("gui.digitalMiner.reset")) / 2), 19 + 51 + (18 - 8) / 2, resetButton.enabled ? 0xffffff : 0x9e9e9e);

        //TODO:Patch the font on the button
        /*
        renderText(LangUtils.localize("gui.on"),19 + 15 / 2 ,  56,1F,false);
        renderText(LangUtils.localize("gui.on"), 38 + 15 / 2,  56, 1F,false);
        renderText(LangUtils.localize("gui.on"),  57 + 15 / 2, 56, 1F,false);
        renderText(LangUtils.localize("gui.off"),+ 19 + 15 / 2, 65,1F,false);
        renderText(LangUtils.localize("gui.off"), 38 + 15 / 2, 65 , 1F,false);
        renderText(LangUtils.localize("gui.off"), 57 + 15 / 2,  65, 1F,false);
         */

        int xAxis = mouseX - guiLeft;
        int yAxis = mouseY - guiTop;
        if (autoEjectButtoninBounds(xAxis,yAxis)) {
            displayTooltip(LangUtils.localize("gui.autoEject") + ":" + LangUtils.transOnOff(tileEntity.doEject), xAxis, yAxis);
        } else if (autoPullButtoninBounds(xAxis,yAxis)) {
            displayTooltip(LangUtils.localize("gui.digitalMiner.autoPull")+ ":" + LangUtils.transOnOff(tileEntity.doPull), xAxis, yAxis);
        } else if (silkTouchButtoninBounds(xAxis,yAxis)) {
            displayTooltip(LangUtils.localize("gui.digitalMiner.silkTouch")+ ":" + LangUtils.transOnOff(tileEntity.silkTouch), xAxis, yAxis);
        } else if (xAxis >= 64 && xAxis <= 80 && yAxis >= 21 && yAxis <= 37) {
            if (!tileEntity.missingStack.isEmpty()) {
                displayTooltip(LangUtils.localize("gui.digitalMiner.missingBlock"), xAxis, yAxis);
            } else {
                displayTooltip(LangUtils.localize("gui.well"), xAxis, yAxis);
            }
        }else if (xAxis >= -21 && xAxis <= -3 && yAxis >= 116 && yAxis <= 134) {
            List<String> info = new ArrayList<>();
            boolean energy = tileEntity.getEnergy() < tileEntity.energyUsage || tileEntity.getEnergy() == 0;
            if (energy) {
                info.add(LangUtils.localize("gui.no_energy"));
            }
            if (energy) {
                displayTooltips(info, xAxis, yAxis);
            }
        }
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(int xAxis, int yAxis) {
        super.drawGuiContainerBackgroundLayer(xAxis, yAxis);
        ResourceLocation resource = getGuiLocation();
        mc.renderEngine.bindTexture(resource);
        drawTexturedModalRect(guiLeft + 19, guiTop + 56, tileEntity.doEject ? 180 : 196,  55 , 15, 17);
        drawTexturedModalRect(guiLeft + 38, guiTop + 56, tileEntity.doPull ? 180 : 196, 55, 15, 17);
        drawTexturedModalRect(guiLeft + 57, guiTop + 56, tileEntity.silkTouch ? 180 : 196, 55, 15, 17);
        drawTexturedModalRect(guiLeft + 24, guiTop + 77, 180,  73, 5, 5);
        drawTexturedModalRect(guiLeft + 43, guiTop + 77, 186,  73, 5, 5);
        drawTexturedModalRect(guiLeft + 62, guiTop + 77, 192,  73, 5, 5);
        boolean energy = tileEntity.getEnergy() < tileEntity.energyUsage || tileEntity.getEnergy() == 0;
        for (int slotY = 0; slotY < 3; slotY++) {
            for (int slotX = 0; slotX < 9; slotX++) {
            mc.getTextureManager().bindTexture(MekanismUtils.getResource(ResourceType.GUI_ELEMENT, "GuiSlot.png"));
            if (tileEntity.inventory.get(slotX + slotY * 9).getCount() == tileEntity.inventory.get(slotX + slotY * 9).getMaxStackSize()){
                    drawTexturedModalRect(guiLeft + 7 + slotX * 18,guiTop + 91 + slotY * 18,158,0,18,18);
                }
            }
        }
        if (energy){
            mc.getTextureManager().bindTexture(MekanismUtils.getResource(MekanismUtils.ResourceType.GUI_ELEMENT, "GuiWarningInfo.png"));
            drawTexturedModalRect(guiLeft - 26, guiTop + 112,0,0,26,26);
            addGuiElement(new GuiWarningInfo(this,getGuiLocation(),false));
        }
    }

    @Override
    protected ResourceLocation getGuiLocation() {
        return MekanismUtils.getResource(ResourceType.GUI, "GuiDigitalMiner.png");
    }

    protected boolean silkTouchButtoninBounds(int xAxis, int yAxis) {
        return xAxis > 57 && xAxis < 57 + 15 && yAxis > 52 && yAxis < 52 + 28;
    }

    protected boolean autoEjectButtoninBounds(int xAxis, int yAxis) {
        return xAxis > 19 && xAxis < 19 + 15 && yAxis > 52 && yAxis < 52 + 28;
    }

    protected boolean autoPullButtoninBounds(int xAxis, int yAxis) {
        return xAxis > 38 && xAxis < 38 + 15 && yAxis > 52 && yAxis < 52 + 28;
    }
    @Override
    public void mouseClicked(int x, int y, int button)throws IOException {
        super.mouseClicked(x, y, button);
        int xAxis = x - guiLeft;
        int yAxis = y - guiTop;
        if (silkTouchButtoninBounds(xAxis, yAxis)) {
            TileNetworkList data = TileNetworkList.withContents(9);
            Mekanism.packetHandler.sendToServer(new TileEntityMessage(tileEntity, data));
            SoundHandler.playSound(MekanismSounds.BEEP2);
        }else if (autoEjectButtoninBounds(xAxis, yAxis)) {
            TileNetworkList data = TileNetworkList.withContents(0);
            Mekanism.packetHandler.sendToServer(new TileEntityMessage(tileEntity, data));
            SoundHandler.playSound(MekanismSounds.BEEP2);
        }else if (autoPullButtoninBounds(xAxis, yAxis)) {
            TileNetworkList data = TileNetworkList.withContents(1);
            Mekanism.packetHandler.sendToServer(new TileEntityMessage(tileEntity, data));
            SoundHandler.playSound(MekanismSounds.BEEP2);
        }
    }

    private void renderText(String text, int x, int y, float size, boolean scale) {
        GlStateManager.pushMatrix();
        GlStateManager.scale(size, size, size);
        fontRenderer.drawString(text, scale ? (int) ((1F / size) * x) : x, scale ? (int) ((1F / size) * y) : y, 0x404040);
        GlStateManager.popMatrix();
    }

}