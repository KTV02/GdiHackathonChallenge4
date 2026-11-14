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
package de.dataport.suchdienst.importer.model.warteschlange;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.dataport.suchdienst.importer.model.XZuFiVersion;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterOutputStream;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.util.StdConverter;

class BytesToBase64 extends StdConverter<byte[], String> {

    protected static final String DEFLATE_PREFIX = "deflate:";

    @Override
    public String convert(byte[] value) {
        return (
            DEFLATE_PREFIX +
            new String(BytesToBase64.encode(compress(value)), StandardCharsets.UTF_8)
        );
    }

    private static byte[] compress(byte[] xzufi) {
        if (xzufi == null) {
            return new byte[0];
        }
        try (var out = new ByteArrayOutputStream(); var defl = new DeflaterOutputStream(out)) {
            defl.write(xzufi);
            defl.flush();
            defl.finish();

            return out.toByteArray();
        } catch (IOException e) {
            //PV-8946 catch-rethrow
            throw new UncheckedIOException(e);
        }
    }

    private static byte[] encode(byte[] src) {
        if (src.length == 0) {
            return src;
        }
        return Base64.getEncoder().encode(src);
    }
}

class Base64ToBytes extends StdConverter<String, byte[]> {

    @Override
    public byte[] convert(String value) {
        if (value.startsWith(BytesToBase64.DEFLATE_PREFIX)) {
            value = value.substring(BytesToBase64.DEFLATE_PREFIX.length());
            return decompress(Base64ToBytes.decodeFromString(value));
        } else {
            return decodeFromString(value);
        }
    }

    private static byte[] decompress(byte[] compressed) {
        try (var out = new ByteArrayOutputStream(); var infl = new InflaterOutputStream(out)) {
            infl.write(compressed);
            infl.flush();
            infl.finish();

            return out.toByteArray();
        } catch (IOException e) {
            //PV-8946 catch-rethrow
            throw new UncheckedIOException(e);
        }
    }

    private static byte[] decodeFromString(String src) {
        if (src.isEmpty()) {
            return new byte[0];
        }
        return Base64.getDecoder().decode(src);
    }
}

@Data
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class ImportRequest implements ImportRequestInfo {

    private Instant requestProcessedAt;
    private final Instant requestAcceptedAt;
    private final TransactionMetadata metaData;

    @JsonSerialize(converter = BytesToBase64.class)
    @JsonDeserialize(converter = Base64ToBytes.class)
    private byte[] xzufi;

    private final XZuFiVersion xzufiVersion;

    private ImportRequestStatus status;

    @JsonProperty("xzufiVersion")
    public XZuFiVersion getXzufiVersion() {
        return xzufiVersion;
    }

    public static ImportRequest createOpen(
        TransactionMetadata txnMetaData,
        byte[] file,
        XZuFiVersion xZuFiVersion
    ) {
        return new ImportRequest(
            null,
            Instant.now(),
            txnMetaData,
            file,
            xZuFiVersion,
            ImportRequestStatus.OPEN
        );
    }

    public static ImportRequest createStatusUpdate(
        ImportRequestStatus status,
        Instant processingTime
    ) {
        return new ImportRequest(processingTime, null, null, null, null, status);
    }
}
