package uz.fovrecorder.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import uz.fovrecorder.Recorder;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void fovrecorder$afterRender(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        Recorder.onFrameRendered();
    }
}
