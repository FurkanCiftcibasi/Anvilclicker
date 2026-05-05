package com.anvilclicker.gui;

import com.anvilclicker.config.AnvilClickerConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class AnvilClickerConfigScreen extends Screen {

    private static final int PANEL_W = 220;
    private static final int PANEL_H = 160;

    private final Screen parent;
    private TextFieldWidget delayField;
    private ButtonWidget toggleButton;
    private String feedbackMsg = "";

    public AnvilClickerConfigScreen(Screen parent) {
        super(Text.literal("AnvilClicker Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int top = (this.height - PANEL_H) / 2;

        AnvilClickerConfig cfg = AnvilClickerConfig.getInstance();

        delayField = new TextFieldWidget(
                this.textRenderer,
                cx - 50, top + 38, 100, 20,
                Text.literal("Delay ms")
        );
        delayField.setMaxLength(10);
        delayField.setText(String.valueOf(cfg.clickDelayMs));
        // Allow digits, one dot, and decimals e.g. "0.05"
        delayField.setTextPredicate(s -> s.isEmpty() || s.matches("\\d*\\.?\\d*"));
        this.addDrawableChild(delayField);

        toggleButton = ButtonWidget.builder(
                toggleLabel(cfg.enabled),
                btn -> {
                    AnvilClickerConfig c = AnvilClickerConfig.getInstance();
                    c.enabled = !c.enabled;
                    btn.setMessage(toggleLabel(c.enabled));
                }
        ).dimensions(cx - 55, top + 72, 110, 20).build();
        this.addDrawableChild(toggleButton);

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Save & Close"),
                btn -> saveAndClose()
        ).dimensions(cx - 55, top + 106, 110, 20).build());
    }

    private Text toggleLabel(boolean enabled) {
        return Text.literal("Auto-Clicker: " + (enabled ? "§aON" : "§cOFF"));
    }

    private void saveAndClose() {
        AnvilClickerConfig cfg = AnvilClickerConfig.getInstance();
        String raw = delayField.getText().trim();
        if (raw.isEmpty() || raw.equals(".")) { feedbackMsg = "§cEnter a valid number!"; return; }
        double val;
        try {
            val = Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            feedbackMsg = "§cInvalid number!";
            return;
        }
        if (val <= 0) { feedbackMsg = "§cDelay must be > 0!"; return; }
        if (val > 60000) { feedbackMsg = "§cMaximum delay is 60000ms!"; return; }
        cfg.clickDelayMs = val;
        cfg.save();
        this.client.setScreen(parent);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int cx = this.width / 2;
        int top = (this.height - PANEL_H) / 2;

        ctx.fill(cx - PANEL_W / 2, top, cx + PANEL_W / 2, top + PANEL_H, 0xCC000000);
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§lAnvilClicker Settings"), cx, top + 10, 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Click Delay (ms, e.g. 0.05 = 20/sec)"), cx, top + 26, 0xAAAAAA);

        if (!feedbackMsg.isEmpty()) {
            ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal(feedbackMsg), cx, top + PANEL_H - 14, 0xFFFFFF);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
