/*
 * Copyright © 2020 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.config;

import com.wireguard.config.BadConfigException.Location;
import com.wireguard.config.BadConfigException.Reason;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class BadConfigExceptionTest {

    private static final String[] CONFIG_NAMES = {
            "invalid-key",
            "invalid-number",
            "invalid-value",
            "missing-attribute",
            "missing-section",
            "missing-value",
            "syntax-error",
            "unknown-attribute",
            "unknown-section"
    };
    private static final Map<String, InputStream> CONFIG_MAP = new HashMap<>();

    @BeforeClass
    public static void readConfigs() {
        for (final String config: CONFIG_NAMES) {
            CONFIG_MAP.put(config, BadConfigExceptionTest.class.getClassLoader().getResourceAsStream(config + ".conf"));
        }
    }

    @AfterClass
    public static void closeStreams() {
        for (final InputStream inputStream : CONFIG_MAP.values()) {
            try {
                inputStream.close();
            } catch (final IOException ignored) {
            }
        }
    }

    @Test
    public void throws_correctly_with_INVALID_KEY_reason() {
        try {
            Config.parse(CONFIG_MAP.get("invalid-key"));
        } catch (final BadConfigException e) {
            assertEquals(e.getReason(), Reason.INVALID_KEY);
            assertEquals(e.getLocation(), Location.PUBLIC_KEY);
        } catch (final IOException e) {
            e.printStackTrace();
            fail("IOException thrown during test");
        }
    }

}
