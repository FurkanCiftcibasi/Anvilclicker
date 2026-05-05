package com.anvilclicker.mixin;

import com.anvilclicker.AnvilClickerMod;
import com.anvilclicker.config.AnvilClickerConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public class HandledScreenMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        AnvilClickerConfig cfg = AnvilClickerConfig.getInstance();
        if (!cfg.enabled) return;

        HandledScreen<?> self = (HandledScreen<?>) (Object) this;
        if (self.getScreenHandler().slots.size() <= 49) return;

        AnvilClickerMod.onScreenTick(self);
    }
}
