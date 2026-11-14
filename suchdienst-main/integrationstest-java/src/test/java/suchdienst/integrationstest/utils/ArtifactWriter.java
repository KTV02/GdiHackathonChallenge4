/*-
 * ---license-start
 * Suchdienst Integrationstests
 * ---
 * Copyright (C) 2026 Dataport AÖR
 * ---
 * SPDX-License-Identifier: EUPL-1.2
 * Der Suchdienst ist Teil des Projekts Portalverbund Onlinegateway und macht Verwaltungsleistungen für externe Konsumenten anwendergerecht durchsuchbar und auffindbar.
 * ---license-end
 */
package suchdienst.integrationstest.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;

public class ArtifactWriter {

    private static final String INPUT_FOLDERNAME = "input";
    private static final String OUTPUT_FOLDERNAME = "output";
    private static final String LOG_FOLDERNAME = "logs";

    private static final Path ROOT_PATH = Path.of("artifacts");
    private static final Path SERVER_LOG_PATH = ROOT_PATH.resolve("fullServerLog.txt");
    private Charset logCharset;

    private final Path testcaseLogFolder;
    private final Path testcaseInputFolder;
    private final Path testcaseOutputFolder;

    private int serverLogCounter = 0;

    private final List<String> currentServerLog = new ArrayList<>();
    private long nextServerLogByteOffset;

    public ArtifactWriter(String testcasename) {
        if (Files.notExists(SERVER_LOG_PATH, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalStateException("Kein Server-Log gefunden!");
        }
        setLogCharset();

        Path testcaseFolder = ROOT_PATH.resolve(testcasename);
        testcaseLogFolder = testcaseFolder.resolve(LOG_FOLDERNAME);
        testcaseInputFolder = testcaseFolder.resolve(INPUT_FOLDERNAME);
        testcaseOutputFolder = testcaseFolder.resolve(OUTPUT_FOLDERNAME);

        try {
            // Anlegen und Aufräumen von Ordnern
            Files.createDirectories(testcaseLogFolder);
            FileUtils.cleanDirectory(testcaseLogFolder.toFile());

            Files.createDirectories(testcaseInputFolder);
            FileUtils.cleanDirectory(testcaseInputFolder.toFile());

            Files.createDirectories(testcaseOutputFolder);
            FileUtils.cleanDirectory(testcaseOutputFolder.toFile());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        nextServerLogByteOffset = currentServerLogSize();
    }

    /**
     * Speichert alle Serverlogeinträge seit erzeugung des ArtefactWriters ab
     */
    public synchronized void writeServerLog() {
        serverLogCounter++;
        Path file = testcaseLogFolder.resolve("ServerLog" + serverLogCounter + ".txt");
        List<String> logs = getCurrentServerLog();
        writeInPath(logs, file);
    }

    private void writeInPath(List<String> lines, Path path) {
        try {
            Files.write(path, lines);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public synchronized List<String> getCurrentServerLog() {
        long currentLogSize = currentServerLogSize();
        if (currentLogSize < getNextServerLogByteOffset()) {
            currentServerLog.clear();
            nextServerLogByteOffset = 0;
        }
        if (currentLogSize == nextServerLogByteOffset) {
            return List.copyOf(currentServerLog);
        }

        try (InputStream inputStream = Files.newInputStream(SERVER_LOG_PATH)) {
            inputStream.skipNBytes(nextServerLogByteOffset);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, logCharset))) {
                reader.lines().forEach(currentServerLog::add);
            }
            nextServerLogByteOffset = currentLogSize;
            return List.copyOf(currentServerLog);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private long getNextServerLogByteOffset() {
        return nextServerLogByteOffset;
    }

    private long currentServerLogSize() {
        try {
            return Files.size(SERVER_LOG_PATH);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void setLogCharset() {
        if (SystemUtils.IS_OS_WINDOWS) {
            logCharset = StandardCharsets.ISO_8859_1;
        } else {
            logCharset = StandardCharsets.UTF_8;
        }
    }
}
