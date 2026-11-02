package com.shinobiextras.client;

import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class ClientKeys {
    private ClientKeys() {}

    public static final String CATEGORY = "key.categories.shinobiextras";
    public static final KeyMapping USE = new KeyMapping("key.shinobiextras.use", GLFW.GLFW_KEY_Z, CATEGORY);
    public static final KeyMapping CYCLE = new KeyMapping("key.shinobiextras.cycle", GLFW.GLFW_KEY_X, CATEGORY);
    public static final KeyMapping MENU = new KeyMapping("key.shinobiextras.menu", GLFW.GLFW_KEY_K, CATEGORY);
}
