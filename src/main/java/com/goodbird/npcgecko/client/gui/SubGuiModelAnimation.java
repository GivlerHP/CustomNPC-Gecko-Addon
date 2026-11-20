package com.goodbird.npcgecko.client.gui;

import com.goodbird.npcgecko.data.CustomModelData;
import com.goodbird.npcgecko.mixin.IDataDisplay;
import kamkeel.npcs.addon.client.GeckoAddonClient;
import kamkeel.npcs.addon.client.GeckoAddonClient.GestureAvailability;
import kamkeel.npcs.addon.client.GeckoAddonClient.ModelResources;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import noppes.npcs.client.gui.util.GuiModelPreview;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.GuiNpcLabel;
import noppes.npcs.client.gui.util.SubGuiInterface;
import noppes.npcs.controllers.data.DialogAnimation;
import noppes.npcs.entity.EntityNPCInterface;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.EnumMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

public final class SubGuiModelAnimation extends SubGuiInterface {
    private int playingSlot = -1;
    private static final int FILE = 1, CANCEL = 2, DONE = 3, SLOT = 10, PLAY = 20;

    private enum Slot {
        IDLE("gecko.idle", true, CustomModelData::getIdleAnim, CustomModelData::setIdleAnim),
        WALK("gecko.walk", true, CustomModelData::getWalkAnim, CustomModelData::setWalkAnim),
        MELEE("gecko.melee", false, CustomModelData::getMeleeAttackAnim, CustomModelData::setMeleeAttackAnim),
        RANGED("gecko.ranged", false, CustomModelData::getRangedAttackAnim, CustomModelData::setRangedAttackAnim),
        HURT("gecko.hurt", false, CustomModelData::getHurtAnim, CustomModelData::setHurtAnim);

        final String label;
        final boolean loop;
        final Function<CustomModelData, String> read;
        final BiConsumer<CustomModelData, String> write;

        Slot(String label, boolean loop, Function<CustomModelData, String> read, BiConsumer<CustomModelData, String> write) {
            this.label = label;
            this.loop = loop;
            this.read = read;
            this.write = write;
        }
    }

    private final GuiModelPreview preview = new GuiModelPreview();
    private final EnumMap<Slot, String> initial = new EnumMap<>(Slot.class);
    private final EnumMap<Slot, String> selections = new EnumMap<>(Slot.class);
    private final String originalFile;
    private String file, previewNote = "";
    private ModelResources resources;
    private Object revision;
    private int focus = -1, previewWidth;

    public SubGuiModelAnimation(EntityNPCInterface npc) {
        this.npc = npc;
        setMenuBackground();
        CustomModelData model = model();
        originalFile = model.getAnimFile();
        file = originalFile.isEmpty() ? "" : new ResourceLocation(originalFile).toString();
        for (Slot slot : Slot.values()) initial.put(slot, slot.read.apply(model));
        selections.putAll(initial);
    }

    @Override
    public void initGui() {
        xSize = Math.min(366, width - 14);
        ySize = Math.min(230, height - 12);
        super.initGui();
        resources = GeckoAddonClient.modelResources();
        revision = resources == null ? null : resources.resourceRevision();
        preview.close();
        previewNote = "";

        int left = guiLeft + 8;
        int right = guiLeft + xSize - 8;
        int top = guiTop + 23;
        int bottom = guiTop + ySize;
        int footer = bottom - 27;
        previewWidth = Math.min(106, (xSize - 16) / 3);
        preview.bounds(left, top, left + previewWidth, footer - 19);
        if (hasSubGui()) return;
        addLabel(new GuiNpcLabel(0, "gecko.modelAnimation", left, guiTop + 8, 0x292929));
        if (resources != null) preview.show(resources.previewModel(npc, null, false, DialogAnimation.NONE, false), false);
        int rowHeight = Math.max(20, Math.min(26, (footer - top - 16) / (Slot.values().length + 1)));
        int rowLeft = left + previewWidth + 8;
        int labelWidth = 0;
        for (Slot slot : Slot.values()) labelWidth = Math.max(labelWidth, fontRendererObj.getStringWidth(tr(slot.label)));
        labelWidth = Math.min(labelWidth + 6, (right - rowLeft) / 2);
        int buttonX = rowLeft + labelWidth;
        int valueWidth = right - buttonX - 23;
        boolean fileExists = resources != null && resources.files().contains(file);

        addRowLabel(FILE, "gecko.animationFile", rowLeft, top, labelWidth - 3);
        addValue(FILE, buttonX, top, right - buttonX, file.isEmpty() ? tr("gui.choose") : GeckoAddonClient.resourceName(file),
            fileExists ? file : file + " · " + tr("scenario.animationMissingFile"), resources != null && !resources.files().isEmpty());

        int y = top + rowHeight;
        String geometry = resources == null ? "" : resources.geometry(npc);
        for (Slot slot : Slot.values()) {
            int id = slot.ordinal();
            String value = selections.get(slot);
            DialogAnimation reference = DialogAnimation.of(file, value);
            GestureAvailability availability = resources == null ? GestureAvailability.NO_MODEL
                : resources.clipAvailability(geometry, reference);
            String detail = value.isEmpty() ? "" : status(geometry, reference, availability);
            addRowLabel(SLOT + id, slot.label, rowLeft, y, labelWidth - 3);
            addValue(SLOT + id, buttonX, y, valueWidth, value.isEmpty() ? tr("gui.none") : value,
                detail.isEmpty() ? value : value + " · " + detail, fileExists || !value.isEmpty());
            GuiNpcButton play = new GuiNpcButton(PLAY + id, right - 19, y, 19, 20, "▶");
            play.enabled = reference.isPresent() && availability.canPlay();
            play.setHoverText(detail.isEmpty() ? tr(slot.loop ? "gecko.previewLoop" : "gecko.previewOnce") : detail);
            addButton(play);
            y += rowHeight;
        }
        int footerWidth = Math.min(74, (xSize - 26) / 3);
        addButton(new GuiNpcButton(CANCEL, right - footerWidth * 2 - 4, footer, footerWidth, 20, "gui.cancel"));
        addButton(new GuiNpcButton(DONE, right - footerWidth, footer, footerWidth, 20, "gui.done"));
        if (focus >= buttonList.size()) focus = -1;
    }

    private void addRowLabel(int id, String key, int x, int y, int available) {
        String label = tr(key);
        GuiNpcLabel row = new GuiNpcLabel(id, fontRendererObj.trimStringToWidth(label, available), x, y + 6, 0x292929);
        addLabel(row);
    }

    private void addValue(int id, int x, int y, int width, String value, String description, boolean enabled) {
        GuiNpcButton button = new GuiNpcButton(id, x, y, width, 20,
            fontRendererObj.trimStringToWidth(value, width - 8));
        button.setHoverText(description);
        button.enabled = enabled;
        addButton(button);
    }

    private CustomModelData model() {
        return ((IDataDisplay) npc.display).getCustomModelData();
    }

    private String status(String geometry, DialogAnimation clip, GestureAvailability availability) {
        if (resources == null) return tr("gecko.resourceEmpty");
        if (!clip.isPresent()) return tr("scenario.animationMissingClip");
        return GuiModelPreview.clipStatus(resources, geometry, clip, availability);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (!button.enabled) return;
        if (button.id == CANCEL) {
            close();
        } else if (button.id == DONE) {
            CustomModelData model = model();
            String normalized = originalFile.isEmpty() ? "" : new ResourceLocation(originalFile).toString();
            if (!file.equals(normalized)) model.setAnimFile(file);
            for (Slot slot : Slot.values()) {
                String value = selections.get(slot);
                if (!value.equals(initial.get(slot))) slot.write.accept(model, value);
            }
            close();
        } else if (button.id == FILE) {
            setSubGui(new GuiStringSelection(npc, "gecko.animationFile", file, file, true, false, value -> { file = value; playingSlot = -1; previewNote = tr("editor.animationFileChanged"); }));
        } else if (button.id >= PLAY) {
            if (resources == null) return;
            Slot slot = Slot.values()[button.id - PLAY];
            String geometry = resources.geometry(npc);
            DialogAnimation clip = DialogAnimation.of(file, selections.get(slot));
            GestureAvailability availability = resources.clipAvailability(geometry, clip);
            if (!clip.isPresent() || !availability.canPlay()) return;
            boolean stop = playingSlot == slot.ordinal();
            playingSlot = stop ? -1 : slot.ordinal();
            preview.show(resources.previewModel(npc, null, false, stop ? DialogAnimation.NONE : clip, slot.loop), slot.loop);
            for (Slot entry : Slot.values()) getButton(PLAY + entry.ordinal()).displayString = entry.ordinal() == playingSlot ? "[]" : ">";
            String detail = status(geometry, clip, availability);
            previewNote = detail.isEmpty()
                ? GuiStringSelection.durationLabel(preview.durationSeconds(), slot.loop) : detail;
        } else if (button.id >= SLOT) {
            Slot slot = Slot.values()[button.id - SLOT];
            setSubGui(new GuiStringSelection(npc, slot.label, file, selections.get(slot), false, slot.loop,
                value -> selections.put(slot, value)));
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (hasSubGui()) return;
        ModelResources current = GeckoAddonClient.modelResources();
        if (current != resources || current != null && revision != current.resourceRevision()) initGui();
        preview.tick();
    }

    @Override
    public void drawScreen(int x, int y, float partialTicks) {
        super.drawScreen(x, y, partialTicks);
        if (hasSubGui()) return;
        preview.draw(x, 0xFF211F24, 0xFFD9B78E);
        preview.scroll(x, y, Mouse.getDWheel());
        int bottom = guiTop + ySize;
        String detail = preview.failed() ? tr("gui.modelPreviewUnavailable") : previewNote;
        if (!detail.isEmpty()) {
            int available = xSize - 16;
            boolean clipped = fontRendererObj.getStringWidth(detail) > available;
            fontRendererObj.drawString(clipped ? fontRendererObj.trimStringToWidth(detail, available - 9) + "..." : detail,
                guiLeft + 8, bottom - 40, 0x51483F);
            if (clipped && y >= bottom - 42 && y < bottom - 29 && x >= guiLeft + 8 && x < guiLeft + xSize - 8) {
                drawHoveringText(fontRendererObj.listFormattedStringToWidth(detail, width - 24), x, y, fontRendererObj);
            }
        }
        if (focus >= 0 && focus < buttonList.size()) {
            GuiButton button = (GuiButton) buttonList.get(focus);
            if (button.enabled) drawRect(button.xPosition + 2, button.yPosition + 1,
                button.xPosition + button.getButtonWidth() - 2, button.yPosition + 2, 0xFFE3D2B5);
        }
    }

    @Override
    public void mouseClicked(int x, int y, int button) {
        focus = -1;
        if (hasSubGui() || !preview.mouseClicked(x, y, button)) super.mouseClicked(x, y, button);
    }

    @Override
    public void keyTyped(char c, int key) {
        if (!hasSubGui() && preview.keyTyped(c, key)) return;
        if (hasSubGui()) {
            super.keyTyped(c, key);
        } else if (key == Keyboard.KEY_ESCAPE) {
            close();
        } else if (key == Keyboard.KEY_TAB) {
            do {
                focus = Math.floorMod(focus + (isShiftKeyDown() ? -1 : 1), buttonList.size());
            } while (!((GuiButton) buttonList.get(focus)).enabled);
        } else if ((key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) && focus >= 0) {
            actionPerformed((GuiButton) buttonList.get(focus));
        }
    }

    @Override
    public void close() {
        preview.close();
        super.close();
    }

    @Override
    public void onGuiClosed() {
        preview.close();
        super.onGuiClosed();
    }

    private static String tr(String key) {
        return StatCollector.translateToLocal(key);
    }
}
