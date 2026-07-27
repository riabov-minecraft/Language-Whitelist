package com.riabov.langwhitelist;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(LangWhitelistMod.MOD_ID)
public final class LangWhitelistMod {
    public static final String MOD_ID = "langwhitelist";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public LangWhitelistMod() {
        ModLoadingContext.get().registerExtensionPoint(
                IExtensionPoint.DisplayTest.class,
                () -> new IExtensionPoint.DisplayTest(
                        () -> NetworkConstants.IGNORESERVERONLY,
                        (remoteVersion, isServer) -> true
                )
        );

        if (FMLEnvironment.dist == Dist.CLIENT) {
            LangWhitelistConfig.load();
        }
    }
}
