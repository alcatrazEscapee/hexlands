package com.alcatrazescapee.hexlands.platform;

import java.util.ServiceLoader;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public interface XPlatform
{
    XPlatform INSTANCE = find(XPlatform.class);
    Logger LOGGER = LogUtils.getLogger();

    static <T> T find(Class<T> clazz) {

        final T service = ServiceLoader.load(clazz)
            .findFirst()
            .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
        LOGGER.debug("Loaded {} for service {}", service, clazz);
        return service;
    }
}
