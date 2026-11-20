package com.goodbird.npcgecko.client.gui;

import kamkeel.npcs.addon.client.GeckoAddonClient;
import kamkeel.npcs.addon.client.GeckoAddonClient.GestureAvailability;
import kamkeel.npcs.addon.client.GeckoAddonClient.ModelResources;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import noppes.npcs.client.gui.util.GuiCustomScroll;
import noppes.npcs.client.gui.util.GuiModelPreview;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.GuiNpcLabel;
import noppes.npcs.client.gui.util.GuiNpcTextField;
import noppes.npcs.client.gui.util.ICustomScrollListener;
import noppes.npcs.client.gui.util.SubGuiInterface;
import noppes.npcs.controllers.data.DialogAnimation;
import noppes.npcs.entity.EntityNPCInterface;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import software.bernie.geckolib3.resource.GeckoLibCache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

final class GuiStringSelection extends SubGuiInterface implements ICustomScrollListener {
    private static final int APPLY = 1, CANCEL = 2, PLAY = 3, CLEAR = 4, SCOPE = 5, RESET_SEARCH = 6, STOP = 7;
    private boolean allResources, playing;
    private final String heading, file, original;
    private final boolean pickingFile, loop;
    private final Consumer<String> apply;
    private final GuiModelPreview preview = new GuiModelPreview();
    private ModelResources resources;
    private Object revision;
    private List<String> options = Collections.emptyList();
    private GuiCustomScroll choices;
    private GuiNpcTextField search;
    private String selected, query = "", note = "";
    private int previewRight, playY, focus = -1;

    GuiStringSelection(EntityNPCInterface npc, String heading, String file, String selected, boolean pickingFile,
                       boolean loop, Consumer<String> apply) {
        this.npc = npc;
        this.heading = heading;
        this.file = file;
        this.original = this.selected = selected;
        this.pickingFile = pickingFile;
        this.loop = loop;
        this.apply = apply;
        setMenuBackground();
    }

    @Override
    public void initGui() {
        xSize = Math.min(360, width - 14);
        ySize = Math.min(244, height - 12);
        super.initGui();
        int left = guiLeft + 8;
        int right = guiLeft + xSize - 8;
        int bottom = guiTop + ySize;
        int top = guiTop + 23;
        addLabel(new GuiNpcLabel(0, heading, left, guiTop + 8, 0x292929));
        previewRight = left + Math.min(110, (xSize - 16) / 3);
        playY = bottom - 68;
        preview.bounds(left, top, previewRight, pickingFile ? playY + 19 : playY - 3);
        int listLeft = previewRight + 7;
        search = new GuiNpcTextField(0, this, fontRendererObj, listLeft, top, right - listLeft, 17, query)
            .placeholder("editor.search");
        search.setMaxStringLength(256);
        search.setCursorPositionEnd();
        search.setFocused(focus < 0);
        addTextField(search);

        choices = new GuiCustomScroll(this, 0, 0) {
            @Override protected String displayName(String entry) { return label(entry); }
        };
        choices.guiLeft = listLeft;
        choices.guiTop = top + 43;
        choices.setSize(right - listLeft, Math.max(16, bottom - 45 - choices.guiTop));
        addButton(new GuiNpcButton(SCOPE, listLeft, top + 21, right - listLeft - 24, 18,
            allResources ? "editor.allResources" : "editor.compatible"));
        addButton(new GuiNpcButton(RESET_SEARCH, right - 20, top + 21, 20, 18, "X"));
        addScroll(choices);

        int buttonWidth = Math.min(74, (xSize - 26) / 3);
        addButton(new GuiNpcButton(CANCEL, right - 2 * buttonWidth - 4, bottom - 27, buttonWidth, 20, "gui.cancel"));
        addButton(new GuiNpcButton(APPLY, right - buttonWidth, bottom - 27, buttonWidth, 20, "gui.choose"));
        if (!pickingFile) {
            addButton(new GuiNpcButton(PLAY, left, playY, previewRight - left, 20, "gui.animationPlay"));
            getButton(PLAY).setHoverText(loop ? "gecko.previewLoop" : "gecko.previewOnce");
            if (!original.isEmpty()) addButton(new GuiNpcButton(CLEAR, left, bottom - 27, buttonWidth, 20, "gui.clear"));
        }
        refreshCatalog();
        filter();
        selectionChanged();
    }

    private String label(String entry) {
        return pickingFile ? GeckoAddonClient.resourceName(entry) : entry;
    }

    private boolean refreshCatalog() {
        ModelResources current = GeckoAddonClient.modelResources();
        Object currentRevision = current == null ? null : current.resourceRevision();
        if (resources == current && revision == currentRevision) return false;
        resources = current;
        revision = currentRevision;
        options = resources == null ? Collections.emptyList() : pickingFile ? resources.files() : resources.clips(file);
        return true;
    }

    private boolean compatible(String option) {
        if (resources == null) return false;
        String geometry = resources.geometry(npc);
        if (!pickingFile) return resources.clipAvailability(geometry, DialogAnimation.of(file, option)).canPlay();
        return resources.clips(option).stream().anyMatch(clip -> resources.clipAvailability(geometry, DialogAnimation.of(option, clip)).canPlay());
    }

    private void filter() {
        String needle = query.trim().toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String option : options) {
            if (!allResources && !compatible(option)) continue;
            if (option.toLowerCase(Locale.ROOT).contains(needle) || label(option).toLowerCase(Locale.ROOT).contains(needle)) {
                matches.add(option);
            }
        }
        choices.setUnsortedList(matches);
        choices.setSelected(selected);
        choices.revealSelection();
        getButton(APPLY).enabled = choices.getSelected() != null && (pickingFile || compatible(selected));
    }

    private void selectionChanged() {
        preview.close();
        playing = false;
        if (getButton(PLAY) != null) getButton(PLAY).displayString = tr("gui.animationPlay");
        note = "";
        if (resources == null) {
            note = tr("gecko.resourceEmpty");
            if (!pickingFile) getButton(PLAY).enabled = false;
            return;
        }
        preview.show(resources.previewModel(npc, null, false, DialogAnimation.NONE, false), false);
        if (pickingFile && !selected.isEmpty() && options.contains(selected)) {
            GeckoLibCache.ResourceOrigin origin = GeckoLibCache.getInstance().getResourceOrigin(new ResourceLocation(selected), false);
            long count = resources.clips(selected).stream().filter(clip -> resources.clipAvailability(resources.geometry(npc), DialogAnimation.of(selected, clip)).canPlay()).count();
            note = tr("gecko.origin." + origin.name().toLowerCase(Locale.ROOT)) + " · " + count + "/" + resources.clips(selected).size() + " " + tr("editor.compatible");
        } else if (!pickingFile) {
            String geometry = resources.geometry(npc);
            DialogAnimation reference = DialogAnimation.of(file, selected);
            GestureAvailability availability = resources.clipAvailability(geometry, reference);
            getButton(PLAY).enabled = reference.isPresent() && availability.canPlay();
            if (!selected.isEmpty()) {
                note = GuiModelPreview.clipStatus(resources, geometry, reference, availability);
                double duration = resources.durationTicks(reference);
                if (note.isEmpty() && duration > 0) note = durationLabel(duration / 20, loop);
            }
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (refreshCatalog()) {
            filter();
            selectionChanged();
        }
        preview.tick();
    }

    @Override
    public void drawScreen(int x, int y, float partialTicks) {
        super.drawScreen(x, y, partialTicks);
        preview.draw(x, 0xFF211F24, 0xFFD9B78E);
        preview.scroll(x, y, Mouse.getDWheel());
        if (choices.list.isEmpty()) fontRendererObj.drawString(tr(options.isEmpty() ? "gecko.resourceEmpty" : "gecko.searchEmpty"),
            choices.guiLeft + 4, choices.guiTop + 5, 0xBBBBBB);
        String message = preview.failed() ? tr("gui.modelPreviewUnavailable") : note;
        String detail = selected.isEmpty() ? message : message.isEmpty() ? selected : selected + " · " + message;
        drawDetail(detail, x, y);
        if (focus >= 0 && focus < buttonList.size()) {
            GuiButton button = (GuiButton) buttonList.get(focus);
            if (button.enabled) drawRect(button.xPosition + 2, button.yPosition + 1,
                button.xPosition + button.getButtonWidth() - 2, button.yPosition + 2, 0xFFE3D2B5);
        }
    }

    private void drawDetail(String detail, int x, int y) {
        if (detail.isEmpty()) return;
        int available = xSize - 16;
        int top = guiTop + ySize - 40;
        boolean clipped = fontRendererObj.getStringWidth(detail) > available;
        fontRendererObj.drawString(clipped ? fontRendererObj.trimStringToWidth(detail, available - 9) + "..." : detail,
            guiLeft + 8, top, 0x51483F);
        if (clipped && x >= guiLeft + 8 && x < guiLeft + xSize - 8 && y >= top && y < top + 10) {
            drawHoveringText(fontRendererObj.listFormattedStringToWidth(detail, width - 24), x, y, fontRendererObj);
        }
    }

    @Override
    public void mouseClicked(int x, int y, int button) {
        focus = -1;
        if (!preview.mouseClicked(x, y, button)) super.mouseClicked(x, y, button);
    }

    @Override
    public void keyTyped(char character, int key) {
        if (!hasSubGui() && preview.keyTyped(character, key)) return;
        if (key == Keyboard.KEY_ESCAPE) {
            close();
        } else if (key == Keyboard.KEY_TAB) {
            do {
                focus = Math.floorMod(focus + 1 + (isShiftKeyDown() ? -1 : 1), buttonList.size() + 1) - 1;
            } while (focus >= 0 && !((GuiButton) buttonList.get(focus)).enabled);
            search.setFocused(focus < 0);
        } else if (key == Keyboard.KEY_UP || key == Keyboard.KEY_DOWN) {
            if (!choices.list.isEmpty()) {
                choices.selected = Math.max(0, Math.min(choices.list.size() - 1,
                    choices.selected + (key == Keyboard.KEY_UP ? -1 : 1)));
                choices.revealSelection();
                customScrollClicked(0, 0, 0, choices);
            }
        } else if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
            if (focus < 0) choose();
            else if (((GuiButton) buttonList.get(focus)).enabled) actionPerformed((GuiButton) buttonList.get(focus));
        } else if (search.textboxKeyTyped(character, key) && !query.equals(search.getText())) {
            query = search.getText();
            filter();
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case APPLY:
                choose();
                break;
            case CANCEL:
                close();
                break;
            case SCOPE:
                allResources = !allResources;
                initGui();
                break;
            case RESET_SEARCH:
                query = "";
                search.setText("");
                filter();
                break;
            case PLAY:
                if (resources == null) return;
                playing = !playing;
                preview.show(resources.previewModel(npc, null, false, playing ? DialogAnimation.of(file, selected) : DialogAnimation.NONE, loop), loop);
                getButton(PLAY).displayString = tr(playing ? "editor.stop" : "gui.animationPlay");
                break;
            case CLEAR:
                apply.accept("");
                close();
                break;
            default:
                break;
        }
    }

    @Override
    public void customScrollClicked(int x, int y, int button, GuiCustomScroll scroll) {
        String value = scroll.getSelected();
        if (value == null) return;
        getButton(APPLY).enabled = pickingFile || compatible(value);
        if (value.equals(selected)) return;
        selected = value;
        selectionChanged();
    }

    @Override
    public void customScrollDoubleClicked(String selection, GuiCustomScroll scroll) {
        choose();
    }

    private void choose() {
        if (refreshCatalog()) filter();
        String value = choices.getSelected();
        if (value == null || !pickingFile && !compatible(value)) return;
        apply.accept(value);
        close();
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

    static String durationLabel(double seconds, boolean loop) {
        return GuiModelPreview.durationLabel(seconds)
            + " · " + tr(loop ? "gecko.previewLoop" : "gecko.previewOnce");
    }

    private static String tr(String key) {
        return StatCollector.translateToLocal(key);
    }
}
