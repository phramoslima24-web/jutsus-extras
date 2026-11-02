package com.shinobiextras.client;

import com.shinobiextras.jutsu.ClientState;
import com.shinobiextras.jutsu.ExtraJutsu;
import com.shinobiextras.network.ActionPacket;
import com.shinobiextras.network.ModNetwork;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** Tela dos jutsus extras: clique para aprender (paga XP) ou, se ja sabe, para selecionar. */
public class ExtraJutsuScreen extends Screen {
    @Nullable
    private final Screen parent;
    private final List<Button> buttons = new ArrayList<>();

    public ExtraJutsuScreen(@Nullable Screen parent) {
        super(Component.translatable("gui.shinobiextras.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        buttons.clear();
        ExtraJutsu[] all = ExtraJutsu.values();
        int colW = 170;
        int gap = 8;
        int left = this.width / 2 - colW - gap / 2;
        int top = 40;
        for (int i = 0; i < all.length; i++) {
            ExtraJutsu j = all[i];
            int idx = i;
            int x = left + (i % 2) * (colW + gap);
            int y = top + (i / 2) * 24;
            Button b = Button.builder(label(j), btn -> ModNetwork.CHANNEL.sendToServer(
                            new ActionPacket(ActionPacket.LEARN_OR_SELECT, idx)))
                    .bounds(x, y, colW, 20)
                    .tooltip(Tooltip.create(Component.translatable(j.langKey() + ".desc")))
                    .build();
            buttons.add(b);
            this.addRenderableWidget(b);
        }
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, b -> this.onClose())
                .bounds(this.width / 2 - 50, top + ((all.length + 1) / 2) * 24 + 24, 100, 20).build());
    }

    private static Component label(ExtraJutsu j) {
        Component name = Component.translatable(j.langKey());
        if (ClientState.isLearned(j)) {
            boolean sel = ClientState.selected() == j.ordinal();
            return Component.literal(sel ? "> " : "").append(name).append(sel ? " <" : "");
        }
        return name.copy().append(" (" + j.xpCost + " XP)");
    }

    @Override
    public void tick() {
        ExtraJutsu[] all = ExtraJutsu.values();
        for (int i = 0; i < buttons.size() && i < all.length; i++) {
            buttons.get(i).setMessage(label(all[i]));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);
        graphics.drawCenteredString(this.font, Component.translatable("gui.shinobiextras.hint"), this.width / 2, 25, 0xAAAAAA);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
