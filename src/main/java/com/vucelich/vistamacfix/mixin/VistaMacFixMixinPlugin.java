package com.vucelich.vistamacfix.mixin;

import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import com.vucelich.vistamacfix.LiveFeedSkip;
import com.vucelich.vistamacfix.MixinGates;

public final class VistaMacFixMixinPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        try {
            String descriptor = descriptorFor(mixinClassName);
            if (descriptor == null) {
                return false;
            }
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            if (loader == null) {
                loader = VistaMacFixMixinPlugin.class.getClassLoader();
            }
            return MixinGates.shouldApply(loader, targetClassName, descriptor);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static String descriptorFor(String mixinClassName) {
        if (mixinClassName.endsWith("VistaLiveFeedMixin44")) {
            return MixinGates.RENDER_44;
        }
        if (mixinClassName.endsWith("VistaLiveFeedMixin55")) {
            return MixinGates.RENDER_55;
        }
        return null;
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
        String descriptor = descriptorFor(mixinClassName);
        if (descriptor == null || targetClass == null || targetClass.methods == null) {
            return;
        }
        for (MethodNode method : targetClass.methods) {
            if ("render".equals(method.name) && descriptor.equals(method.desc)) {
                LiveFeedSkip.insert(method);
            }
        }
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
