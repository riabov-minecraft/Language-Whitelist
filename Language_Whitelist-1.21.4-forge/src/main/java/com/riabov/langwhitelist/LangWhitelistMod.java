package com.riabov.langwhitelist;

import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(LangWhitelistMod.MOD_ID)
public final class LangWhitelistMod {
    public static final String MOD_ID = "langwhitelist";
    public static final Logger LOGGER = LogUtils.getLogger();

    public LangWhitelistMod() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            LangWhitelistConfig.load();
        }
    }
}
