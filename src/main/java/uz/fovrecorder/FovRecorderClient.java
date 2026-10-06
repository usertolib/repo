package uz.fovrecorder;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class FovRecorderClient implements ClientModInitializer {
    private static KeyMapping toggleKey;
    private int tick;

    @Override
    public void onInitializeClient() {
        Config.load();
        KeyMapping.Category cat = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("fovrecorder", "main"));
        toggleKey = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.fovrecorder.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F9, cat));
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (toggleKey.consumeClick()) Recorder.toggle();
            if (Recorder.isRecording() && Config.showRec && mc.player != null && ++tick % 10 == 0) {
                mc.gui.setOverlayMessage(Component.literal("\u25CF REC").withStyle(ChatFormatting.RED), false);
            }
        });
    }
}
