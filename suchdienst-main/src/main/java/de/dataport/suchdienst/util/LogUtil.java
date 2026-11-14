/*-
 * ---license-start
 * Suchdienst
 * ---
 * Copyright (C) 2025 Dataport AÖR
 * ---
 * SPDX-License-Identifier: EUPL-1.2
 * Der Suchdienst ist Teil des Projekts Portalverbund Onlinegateway und macht Verwaltungsleistungen für externe Konsumenten anwendergerecht durchsuchbar und auffindbar.
 * ---license-end
 */
package de.dataport.suchdienst.util;

import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;

public final class LogUtil {

    private LogUtil() {}

    public static String millisToSeconds(long ms) {
        return String.format(
            "%s.%s",
            StringUtils.leftPad("" + TimeUnit.MILLISECONDS.toSeconds(ms), 2, '0'),
            StringUtils.leftPad(
                "" + (ms - TimeUnit.SECONDS.toMillis(TimeUnit.MILLISECONDS.toSeconds(ms))),
                3,
                '0'
            )
        );
    }
}
