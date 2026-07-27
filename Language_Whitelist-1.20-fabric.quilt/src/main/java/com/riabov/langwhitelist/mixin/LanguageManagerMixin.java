package com.riabov.langwhitelist.mixin;

import com.riabov.langwhitelist.LangWhitelistConfig;
import com.riabov.langwhitelist.LangWhitelistMod;
import net.minecraft.client.resources.language.LanguageInfo;
import net.minecraft.client.resources.language.LanguageManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(LanguageManager.class)
public abstract class LanguageManagerMixin {
    @Unique
    private static final AtomicBoolean langwhitelist$warnedNoMatches = new AtomicBoolean(false);

    @Inject(
            method = "getLanguages()Ljava/util/SortedMap;",
            at = @At("RETURN"),
            cancellable = true
    )
    private void langwhitelist$filterLanguages(
            CallbackInfoReturnable<SortedMap<String, LanguageInfo>> callback
    ) {
        Set<String> allowed = LangWhitelistConfig.getAllowedOrNull();
        if (allowed == null) {
            return;
        }

        SortedMap<String, LanguageInfo> original = callback.getReturnValue();
        if (original == null || original.isEmpty()) {
            return;
        }

        Comparator<? super String> comparator = original.comparator();
        TreeMap<String, LanguageInfo> filtered = new TreeMap<>(comparator);

        for (Map.Entry<String, LanguageInfo> entry : original.entrySet()) {
            String code = entry.getKey();
            if (code == null) {
                continue;
            }

            if (allowed.contains(code.toLowerCase(Locale.ROOT))) {
                filtered.put(code, entry.getValue());
            }
        }

        if (filtered.isEmpty()) {
            if (langwhitelist$warnedNoMatches.compareAndSet(false, true)) {
                LangWhitelistMod.LOGGER.warn(
                        "[Language Whitelist] No languages matched your config. "
                                + "allowed_languages={} | available(first 10 of {}): {}",
                        allowed,
                        original.size(),
                        langwhitelist$sampleCodes(original, 10)
                );
            }

            return;
        }

        callback.setReturnValue(Collections.unmodifiableSortedMap(filtered));
    }

    @Unique
    private static String langwhitelist$sampleCodes(
            SortedMap<String, LanguageInfo> languages,
            int maximum
    ) {
        StringBuilder result = new StringBuilder();
        int count = 0;

        for (String code : languages.keySet()) {
            if (code == null) {
                continue;
            }

            if (count > 0) {
                result.append(", ");
            }

            result.append(code);
            count++;

            if (count >= maximum) {
                break;
            }
        }

        return result.toString();
    }
}
