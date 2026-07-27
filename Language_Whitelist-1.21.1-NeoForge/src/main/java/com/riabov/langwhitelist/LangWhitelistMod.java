package com.riabov.langwhitelist;

import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(value = LangWhitelistMod.MOD_ID, dist = Dist.CLIENT)
public final class LangWhitelistMod {
    public static final String MOD_ID = "langwhitelist";
    public static final Logger LOGGER = LogUtils.getLogger();

    public LangWhitelistMod() {
        LangWhitelistConfig.load();
    }
}
