package com.riabov.langwhitelist.mixin;

import com.riabov.langwhitelist.LangWhitelistConfig;
import com.riabov.langwhitelist.LangWhitelistMod;
import net.minecraft.client.resources.Language;
import net.minecraft.client.resources.LanguageManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(LanguageManager.class)
public abstract class LanguageManagerMixin {
    @Unique
    private static final AtomicBoolean langwhitelist$warnedNoMatches = new AtomicBoolean(false);

    @Inject(
            method = "getLanguages()Ljava/util/SortedSet;",
            at = @At("RETURN"),
            cancellable = true
    )
    private void langwhitelist$filterLanguages(
            CallbackInfoReturnable<SortedSet<Language>> callback
    ) {
        Set<String> allowed = LangWhitelistConfig.getAllowedOrNull();
        if (allowed == null) {
            return;
        }

        SortedSet<Language> original = callback.getReturnValue();
        if (original == null || original.isEmpty()) {
            return;
        }

        Comparator<? super Language> comparator = original.comparator();
        if (comparator == null) {
            comparator = Comparator.comparing(language -> {
                String code = language == null || language.getCode() == null
                        ? ""
                        : language.getCode();
                return code.toLowerCase(Locale.ROOT);
            });
        }

        TreeSet<Language> filtered = new TreeSet<>(comparator);

        for (Language language : original) {
            if (language == null) {
                continue;
            }

            String code = language.getCode();
            if (code == null) {
                continue;
            }

            if (allowed.contains(code.toLowerCase(Locale.ROOT))) {
                filtered.add(language);
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

        callback.setReturnValue(Collections.unmodifiableSortedSet(filtered));
    }

    @Unique
    private static String langwhitelist$sampleCodes(
            SortedSet<Language> languages,
            int maximum
    ) {
        StringBuilder result = new StringBuilder();
        int count = 0;

        for (Language language : languages) {
            if (language == null) {
                continue;
            }

            String code = language.getCode();
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
