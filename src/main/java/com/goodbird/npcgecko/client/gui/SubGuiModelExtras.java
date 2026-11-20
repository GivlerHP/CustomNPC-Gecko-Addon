package com.goodbird.npcgecko.client.gui;

import com.goodbird.npcgecko.data.CustomModelData;
import com.goodbird.npcgecko.mixin.IDataDisplay;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.ResourceLocation;

import noppes.npcs.client.gui.util.*;
import noppes.npcs.entity.EntityNPCInterface;

import software.bernie.geckolib3.geo.render.built.GeoBone;
import software.bernie.geckolib3.geo.render.built.GeoModel;
import software.bernie.geckolib3.resource.GeckoLibCache;

import java.util.*;

public class SubGuiModelExtras extends SubGuiInterface implements ICustomScrollListener {
    private final CustomModelData model;
    private final String originalHead;
    private String head, filter = "";
    private int transition;
    private GuiCustomScroll bones;

    public SubGuiModelExtras(EntityNPCInterface npc) {
        this.npc = npc;
        model = ((IDataDisplay) npc.display).getCustomModelData();
        head = originalHead = model.getHeadBoneName();
        transition = model.getTransitionLengthTicks();
        setMenuBackground();
        closeOnEsc = true;
    }

    @Override
    public void initGui() {
        xSize = Math.min(340, width - 12);
        ySize = Math.min(264, height - 12);
        super.initGui();
        int left = guiLeft + 8, available = xSize - 16;
        addLabel(new GuiNpcLabel(0, "gecko.headBone", left, guiTop + 8, 0x292929));
        addTextField(
                new GuiNpcTextField(
                        1, this, fontRendererObj, left, guiTop + 20, available, 20, head));
        addTextField(
                new GuiNpcTextField(
                        3, this, fontRendererObj, left, guiTop + 44, available - 24, 18, filter));
        getTextField(3).setHoverText("editor.searchBones");
        getTextField(3).placeholder("editor.searchBones");
        addButton(new GuiNpcButton(3, guiLeft + xSize - 28, guiTop + 44, 20, 18, "X"));
        bones = new GuiCustomScroll(this, 0);
        bones.guiLeft = left;
        bones.guiTop = guiTop + 66;
        bones.setSize(available, Math.max(28, ySize - 156));
        addScroll(bones);
        addLabel(new GuiNpcLabel(2, "gecko.transition", left, guiTop + ySize - 85, 0x292929));
        GuiNpcTextField field =
                new GuiNpcTextField(
                        2,
                        this,
                        fontRendererObj,
                        left,
                        guiTop + ySize - 73,
                        available,
                        20,
                        Integer.toString(transition));
        field.setIntegersOnly();
        field.setMinMaxDefault(0, Integer.MAX_VALUE, transition);
        addTextField(field);
        addButton(
                new GuiNpcButton(
                        1, guiLeft + xSize - 152, guiTop + ySize - 27, 70, 20, "gui.cancel"));
        addButton(
                new GuiNpcButton(2, guiLeft + xSize - 78, guiTop + ySize - 27, 70, 20, "gui.done"));
        rebuild();
    }

    private void collect(List<GeoBone> source, List<String> result) {
        for (GeoBone bone : source) {
            result.add(bone.name);
            collect(bone.childBones, result);
        }
    }

    private void rebuild() {
        GeoModel geometry =
                GeckoLibCache.getInstance()
                        .getGeoModels()
                        .get(new ResourceLocation(model.getModel()));
        List<String> names = new ArrayList<>();
        if (geometry != null) collect(geometry.topLevelBones, names);
        names.removeIf(
                name ->
                        !name.toLowerCase(Locale.ROOT)
                                .contains(filter.toLowerCase(Locale.ROOT).trim()));
        bones.setList(names);
        bones.setSelected(head);
        bones.revealSelection();
        boolean valid = geometry != null && geometry.getBone(head).isPresent();
        getButton(2).enabled = valid || head.equals(originalHead);
        addLabel(
                new GuiNpcLabel(
                        4,
                        valid ? "" : "editor.boneMissing",
                        guiLeft + 8,
                        guiTop + ySize - 45,
                        0x8C3527));
    }

    @Override
    public void keyTyped(char c, int key) {
        super.keyTyped(c, key);
        head = getTextField(1).getText();
        filter = getTextField(3).getText();
        transition = getTextField(2).getInteger();
        rebuild();
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 1) close();
        if (button.id == 2 && button.enabled) {
            model.setHeadBoneName(getTextField(1).getText());
            model.setTransitionLengthTicks(getTextField(2).getInteger());
            close();
        }
        if (button.id == 3) {
            filter = "";
            getTextField(3).setText("");
            rebuild();
        }
    }

    @Override
    public void customScrollClicked(int x, int y, int button, GuiCustomScroll scroll) {
        if (scroll.getSelected() != null) {
            head = scroll.getSelected();
            getTextField(1).setText(head);
            rebuild();
        }
    }

    @Override
    public void customScrollDoubleClicked(String name, GuiCustomScroll scroll) {
        customScrollClicked(0, 0, 0, scroll);
    }
}
