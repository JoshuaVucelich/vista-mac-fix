package com.vucelich.vistamacfix;

import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Unhooks vistafix while mixin configs are still being prepared, before
 * {@code TvBlockEntityRenderer} is transformed.
 */
public final class VistaMacFixMixinPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) {
        VistaFixNeutralizer.neutralize(false);
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        VistaFixNeutralizer.neutralize(false);
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        VistaFixNeutralizer.neutralize(false);
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        if (targetClassName != null && targetClassName.endsWith("TvBlockEntityRenderer")) {
            VistaFixNeutralizer.noteRenderedClass(targetClass);
        }
    }
}
