package com.goodbird.npcgecko.client;

import cpw.mods.fml.client.GuiModList;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.util.StatCollector;
import net.minecraftforge.client.event.GuiOpenEvent;

/** Updates this mod's Forge description when the player's language changes. */
public final class LocalizedModMetadata {
    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (!(event.gui instanceof GuiModList)) return;
        ModContainer mod = Loader.instance().getIndexedModList().get("npcgecko");
        if (mod != null && mod.getMetadata() != null && StatCollector.canTranslate("npcgecko.mod.description")) {
            mod.getMetadata().description = StatCollector.translateToLocal("npcgecko.mod.description");
        }
    }
}
