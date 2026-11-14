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
package de.dataport.suchdienst.common.model;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Repräsentiert einen Leika-Schlüssel.
 * <p>
 * Ein Leika-Schlüssel ist eine 14-stellige Zahl, wobei die Stellen eine spezielle Bedeutung haben:
 * <ol>
 * <li>die ersten 2 Stellen kennzeichnen die Instanz, die den Schlüssel definiert hat. "99" sind
 * bundeseinheitlich vergebene Schlüsel, ansonsten ist es die Länderkennung nach AGS</li>
 * <li>die übrigen Stellen bestehen aus vier Gruppen mit jeweils 3 Zeichen, siehe
 * https://de.wikipedia.org/wiki/LeiKa</li>
 * <li>Insgesamt ist der Aufbau also:
 *
 * <pre>
 * Instanz | Leistungsgruppierung | Leistungskennung | Verrichtungskennung | Verrichtungsdetail
 * </pre>
 *
 * </li>
 * </ol>
 * <p>
 * Durch die vier Gruppen wird eine Hierarchie gebildet, die man sich als Baum vorstellen kann:
 *
 * <pre>
 * 99 008 000 000 000
 * ├───99 008 001 000 000
 * │   ├───99 008 001 001 000
 * │   │   ├───99 008 001 001 012
 * │   │   └───99 008 001 001 020
 * │   └───99 008 001 002 000
 * └───99 008 002 000 000
 * </pre>
 * </p>
 * <p>
 * Alle Knoten eines Teilbaums haben dementsprechend ein gemeinames Präfix, wenn man nur die ersten
 * X Stellen bis zur ersten 000er Gruppe der Wurzel betrachtet.
 * </p>
 */
@ToString
@EqualsAndHashCode
@Getter
public class LeikaKey {

    private final String key;
    private final boolean privateSpace;
    private static final int KEY_LENGTH = 14;
    private static final Pattern LEIKA_PATTERN = Pattern.compile("^\\d{14}$");

    public LeikaKey(String leika) {
        if (!isLeikaKey(leika)) {
            throw new IllegalArgumentException(
                "Ungültiger Leika-Schlüssel! Muss exakt 14 numerische Stellen haben"
            );
        }
        this.key = leika;
        this.privateSpace =
            this.key.charAt(2) == '9' ||
            this.key.charAt(5) == '9' ||
            this.key.charAt(8) == '9' ||
            this.key.charAt(11) == '9';
    }

    /**
     * Privater Konstruktor für die Methoden in dieser Klasse um die Hierarchie rauf oder runter zu
     * laufen (package private wg. Unit Tests).
     *
     * @param key          der Schlüssel, kann unvollständig (also nur ein Präfix eines vollständigen) sein
     * @param privateSpace ob der ursprüngliche Schlüssel, von dem die neue Instanz abgeleitet wird ein
     *                     länderspezifischer Leika war
     */
    LeikaKey(String key, boolean privateSpace) {
        this.key = key;
        this.privateSpace = privateSpace;
    }

    /**
     * Liefert true, wenn key den Syntaxkriterien eines Leika-Schlüssels genügt.
     */
    public static boolean isLeikaKey(String key) {
        return key != null && LEIKA_PATTERN.matcher(key).matches();
    }

    /**
     * Liefert den Teil des Leikas, mit dem die Suchen ausgeführt werden. Wenn es sich um einen
     * länderspezifischen Leika handelt, muss der Länderschlüssel ("Instanz") beim Suchen berücksichtigt
     * werden, ansonsten nicht.
     *
     * @return den den Schlüssel für die Suche, also entweder mit oder ohne Länderschlüssel
     */
    public String getSearchKey() {
        return key.substring(isPrivateSpace() ? 0 : 2);
    }

    /**
     * Liefert sich selbst und alle in der Hierarchie darüber liegenden Schlüssel (bzw. deren Präfixe).
     *
     * @return eine Menge von Schlüsseln inkl. {@literal this}, niemals {@literal NULL}
     */
    public Set<LeikaKey> getHierarchy() {
        var s = new HashSet<LeikaKey>();
        s.add(this);
        String longestPath = key;
        while (longestPath.length() > 2) {
            s.add(new LeikaKey(longestPath, this.isPrivateSpace()));
            longestPath = longestPath.substring(0, longestPath.length() - 3);
        }
        return s;
    }

    /**
     * Liefert den vollständigen übergeordneten LeikaKey zu dieser Instanz durch Ersetzung des
     * niedrigsten gefüllten Blocks durch 000.
     *
     * @return den übergeordneten LeikaKey zu diesem oder einen leeren Optional, wenn dieser Key schon
     * der obersten (für uns interessanten) Ebene entspricht.
     */
    public Optional<LeikaKey> getParentKey() {
        var abstractKey = key.toCharArray();
        for (int startOfBlock = 11; startOfBlock > 3; startOfBlock -= 3) {
            if (!key.startsWith("000", startOfBlock)) {
                Arrays.fill(abstractKey, startOfBlock, startOfBlock + 3, '0');
                return Optional.of(new LeikaKey(new String(abstractKey), this.privateSpace));
            }
        }
        return Optional.empty();
    }

    /**
     * Liefert das Präfix des Pfads zu diesem LeikaKey durch Abschneiden aller 000er Blöcke am Ende. Nur
     * für Schlüssel definiert, die nicht auf der untersten Ebene der Hierarchie liegen.
     *
     * @return den Pfad zu diesem LeikaKey oder ein leeren Optional, wenn es sich um ein Blatt handelt.
     */
    public Optional<LeikaKey> getPathPrefix() {
        if (key.endsWith("000")) {
            return Optional.of(new LeikaKey(key.substring(0, getDepth()), this.isPrivateSpace()));
        }
        return Optional.empty();
    }

    /**
     * Liefert den vollständigen, 14-stelligen Schlüssel zu dieser Instanz, indem rechts mit Nullen
     * aufgefüllt wird.
     * <p>
     * Instanzen können durch {@link #getPathPrefix()} oder {@link #getHierarchy()} verkürzt sein, da
     * dort die abschließenden 000er-Blöcke abgeschnitten werden. Das Auffüllen mit Nullen ist
     * verlustfrei und stellt exakt den ursprünglichen Schlüssel wieder her, z.B.
     * {@code 99008001001} -> {@code 99008001001000}.
     * </p>
     *
     * @return den auf 14 Stellen mit Nullen aufgefüllten Schlüssel
     */
    public String getFullKey() {
        return key.length() >= KEY_LENGTH ? key : key + "0".repeat(KEY_LENGTH - key.length());
    }

    /**
     * Liefert die Detailierungstiefe dieses Schlüssel, was der Länge des "gefüllten" Schlüssels
     * entspricht.
     *
     * @return die Länge des gefüllten Präfix
     */
    public int getDepth() {
        int depth = key.length();
        while (depth > 5 && key.regionMatches(depth - 3, "000", 0, 3)) {
            depth -= 3;
        }
        return depth;
    }
}
