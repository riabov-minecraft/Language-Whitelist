package com.riabov.langwhitelist;

import net.fabricmc.api.ClientModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class LangWhitelistMod implements ClientModInitializer {
    public static final String MOD_ID = "langwhitelist";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LangWhitelistConfig.load();
    }
}
