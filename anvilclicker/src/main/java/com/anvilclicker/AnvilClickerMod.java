package com.anvilclicker;

import com.anvilclicker.config.AnvilClickerConfig;
import com.anvilclicker.gui.AnvilClickerConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWKeyCallback;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

public class AnvilClickerMod implements ClientModInitializer {

    public static final int TARGET_SLOT = 49;

    private static KeyBinding keyToggle;
    private static KeyBinding keyOpenGui;
    private static long lastClickNano = 0L;

    // Set to true when the GLFW callback already handled the toggle,
    // so the tick handler skips it that same tick.
    private static volatile boolean handledByGlfw = false;

    public static KeyBinding getKeyToggle() { return keyToggle; }

    @Override
    public void onInitializeClient() {
        AnvilClickerConfig.getInstance();

        Object category = getOrCreateCategoryOnce("category.anvilclicker");
        keyToggle = KeyBindingHelper.registerKeyBinding(createKeyBinding(
                "key.anvilclicker.toggle", GLFW.GLFW_KEY_J, category));
        keyOpenGui = KeyBindingHelper.registerKeyBinding(createKeyBinding(
                "key.anvilclicker.opengui", GLFW.GLFW_KEY_K, category));

        // Outside menus: wasPressed() fires exactly once per press
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (keyToggle.wasPressed()) {
                if (!handledByGlfw) {
                    // Only toggle if GLFW didn't already handle it this press
                    if (client.currentScreen == null) {
                        toggleBot(client);
                    }
                }
                handledByGlfw = false;
                // Drain any extra queued presses
                while (keyToggle.wasPressed()) {}
            }

            while (keyOpenGui.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new AnvilClickerConfigScreen(null));
                }
            }
        });

        // Inside menus: wrap GLFW callback
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            long window = client.getWindow().getHandle();

            GLFWKeyCallback existing = GLFW.glfwSetKeyCallback(window, null);
            if (existing != null) GLFW.glfwSetKeyCallback(window, existing);

            GLFW.glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
                // Always forward everything to Minecraft first
                if (existing != null) existing.invoke(win, key, scancode, action, mods);

                int toggleKeyCode = keyToggle.getDefaultKey().getCode();
                if (key == toggleKeyCode && action == GLFW.GLFW_PRESS) {
                    handledByGlfw = true;
                    client.execute(() -> {
                        toggleBot(client);
                        if (!AnvilClickerConfig.getInstance().enabled && client.currentScreen != null) {
                            client.setScreen(null);
                        }
                    });
                }
            });
        });
    }

    private static void toggleBot(MinecraftClient client) {
        AnvilClickerConfig cfg = AnvilClickerConfig.getInstance();
        cfg.enabled = !cfg.enabled;
        if (client.player != null) {
            client.player.sendMessage(
                    Text.literal("AnvilClicker " + (cfg.enabled ? "§aENABLED" : "§cDISABLED")),
                    true
            );
        }
    }

    private static KeyBinding createKeyBinding(String id, int glfwKey, Object category) {
        try {
            for (Constructor<?> c : KeyBinding.class.getDeclaredConstructors()) {
                c.setAccessible(true);
                Class<?>[] p = c.getParameterTypes();
                if (p.length == 4 && p[0] == String.class && p[2] == int.class && !p[3].equals(String.class))
                    return (KeyBinding) c.newInstance(id, getKeysymValue(p[1]), glfwKey, category);
                if (p.length == 3 && p[0] == String.class && p[1] == int.class && !p[2].equals(String.class))
                    return (KeyBinding) c.newInstance(id, glfwKey, category);
                if (p.length == 4 && p[0] == String.class && p[2] == int.class && p[3].equals(String.class))
                    return (KeyBinding) c.newInstance(id, getKeysymValue(p[1]), glfwKey, "category.anvilclicker");
                if (p.length == 3 && p[0] == String.class && p[1] == int.class && p[2].equals(String.class))
                    return (KeyBinding) c.newInstance(id, glfwKey, "category.anvilclicker");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create KeyBinding: " + e, e);
        }
        throw new RuntimeException("No matching KeyBinding constructor");
    }

    private static Object getOrCreateCategoryOnce(String translationKey) {
        try {
            Class<?> categoryClass = null;
            for (Class<?> inner : KeyBinding.class.getDeclaredClasses()) { categoryClass = inner; break; }
            if (categoryClass == null) return translationKey;
            for (Field f : categoryClass.getDeclaredFields()) {
                f.setAccessible(true);
                if (Map.class.isAssignableFrom(f.getType()) && java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                    Map<?, ?> map = (Map<?, ?>) f.get(null);
                    for (Map.Entry<?, ?> entry : map.entrySet()) {
                        String k = entry.getKey().toString();
                        if (k.contains(translationKey) || k.endsWith(translationKey)) return entry.getValue();
                    }
                }
            }
            for (Method m : categoryClass.getDeclaredMethods()) {
                m.setAccessible(true);
                if (!java.lang.reflect.Modifier.isStatic(m.getModifiers())) continue;
                if (!categoryClass.isAssignableFrom(m.getReturnType())) continue;
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 1 && p[0] == String.class) return m.invoke(null, translationKey);
                if (p.length == 2 && p[0] == String.class) return m.invoke(null, translationKey, 10);
            }
            for (Constructor<?> c : categoryClass.getDeclaredConstructors()) {
                c.setAccessible(true);
                Class<?>[] p = c.getParameterTypes();
                if (p.length == 1 && p[0] == String.class) return c.newInstance(translationKey);
                if (p.length == 2 && p[0] == String.class) return c.newInstance(translationKey, 10);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to get/create Category: " + e, e);
        }
        return translationKey;
    }

    private static Object getKeysymValue(Class<?> typeClass) throws Exception {
        if (typeClass.isEnum()) {
            for (Object o : typeClass.getEnumConstants()) {
                String name = ((Enum<?>) o).name();
                if (name.contains("KEY") || name.contains("SYM")) return o;
            }
            Object[] c = typeClass.getEnumConstants();
            if (c.length > 0) return c[0];
        }
        for (Field f : typeClass.getDeclaredFields()) {
            f.setAccessible(true);
            if (typeClass.isAssignableFrom(f.getType())) {
                if (f.getName().contains("KEY") || f.getName().contains("SYM")) return f.get(null);
            }
        }
        for (Field f : typeClass.getDeclaredFields()) {
            f.setAccessible(true);
            if (typeClass.isAssignableFrom(f.getType())) return f.get(null);
        }
        throw new RuntimeException("Could not find KEYSYM in " + typeClass);
    }

    public static void onScreenTick(HandledScreen<?> screen) {
        AnvilClickerConfig cfg = AnvilClickerConfig.getInstance();
        long nowNano = System.nanoTime();
        long delayNano = (long) (cfg.clickDelayMs * 1_000_000.0);
        if (nowNano - lastClickNano < delayNano) return;
        lastClickNano = nowNano;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.interactionManager == null || client.player == null) return;
        if (screen.getScreenHandler().slots.size() <= TARGET_SLOT) return;

        client.interactionManager.clickSlot(
                screen.getScreenHandler().syncId,
                TARGET_SLOT,
                0,
                SlotActionType.PICKUP,
                client.player
        );
    }
}
