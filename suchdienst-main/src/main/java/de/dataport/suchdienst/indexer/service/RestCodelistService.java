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
package de.dataport.suchdienst.indexer.service;

import static de.dataport.suchdienst.config.CacheConfiguration.CACHE_NAME_CODELIST_DOWNLOAD;

import de.dataport.suchdienst.config.CacheConfiguration;
import de.dataport.suchdienst.config.SuchdienstProperties;
import de.dataport.suchdienst.error.ApiException;
import de.dataport.suchdienst.error.ApiWarnException;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.error.model.ExceptionType;
import de.dataport.suchdienst.indexer.exception.ImportException;
import de.dataport.suchdienst.util.Exceptions;
import de.dataport.suchdienst.util.MdcTemplate;
import de.dataport.suchdienst.util.RequestUtils;
import de.dataport.suchdienst.util.model.ProtocolType;
import de.xrepository.genericodeservices.CodeListDocument;
import de.xrepository.genericodeservices.ObjectFactory;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import java.io.IOException;
import java.io.StringReader;
import java.net.ConnectException;
import java.net.URI;
import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Service
@Slf4j
public class RestCodelistService {

    public static final String CURRENT_VERSION_NAME_ERROR_CODE =
        "rest_codelist_service.exception_current_version_name_error";
    /**
     * Sperrt gleichzeitige Verbindungschecks
     */
    private final Lock lock = new ReentrantLock();

    private final MessageSource messageSource;
    private final SuchdienstProperties properties;
    private final ExecutorService executor = Executors.newFixedThreadPool(1);
    private final JsonMapper jsonMapper;
    private long lastOnlineCheck;
    private boolean online;
    private final RestTemplate restTemplate;
    private final AntivirusScannerService antivirusService;
    private final JAXBContext jaxbContext;

    public RestCodelistService(
        SuchdienstProperties properties,
        AntivirusScannerService antivirusService,
        MessageSource messageSource,
        @Qualifier("codelistTemplate") RestTemplate restTemplate,
        JsonMapper jsonMapper
    ) throws JAXBException {
        this.properties = properties;
        this.messageSource = messageSource;
        this.restTemplate = restTemplate;
        this.antivirusService = antivirusService;
        this.jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
        this.jsonMapper = jsonMapper;
    }

    public boolean canConnectToXRepository() {
        // wir cachen das pruefergebnis einige zeit, um nicht jedesmal pruefen zu
        // muessen.
        long millisSinceLastCheck = System.currentTimeMillis() - lastOnlineCheck;
        if (millisSinceLastCheck > properties.getCodelistCheckExpire()) {
            this.lock.lock();
            try {
                if (millisSinceLastCheck > properties.getCodelistCheckExpire()) {
                    try (var _ = MdcTemplate.withProtokolltyp(ProtocolType.EXTERN)) {
                        online = checkXRepositoryReachable();
                    }
                    lastOnlineCheck = System.currentTimeMillis();
                }
            } finally {
                lock.unlock();
            }
        }
        return online;
    }

    /**
     * Damit wir nicht ggf. lange auf das Fehlschlagen warten, führen wir den Check im Hintergrund aus
     * und warten nur eingeschränkt auf das Ergebnis.
     */
    private boolean checkXRepositoryReachable() {
        var mdcContext = MdcTemplate.currentContext();
        try {
            boolean reachable = executor
                .submit(() ->
                    MdcTemplate.callWithContext(mdcContext, this::requestXRepositoryStatus)
                )
                .get(5L, TimeUnit.SECONDS);
            if (reachable) {
                log.info("XRepository ist erreichbar.");
            } else {
                log.warn("XRepository ist nicht erreichbar.");
            }
            return reachable;
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
            // Das vorherige Prüfergebnis beibehalten, der Check konnte nicht abgeschlossen werden.
            return online;
        } catch (
            IllegalStateException
            | ExecutionException
            | RestClientException
            | TimeoutException e
        ) {
            //PV-8946 catch-log-return-error
            String msg = String.format(
                "XRepository konnte nicht erreicht werden %s",
                e.getMessage()
            );
            log.warn(msg);
            return false;
        }
    }

    private boolean requestXRepositoryStatus() {
        URI requestUri = UriComponentsBuilder.fromUriString(properties.getCodelist().getBaseUrl())
            .pathSegment("codeliste", "urn:de:xzufi:codeliste:leistungstextmodulleika")
            .build()
            .toUri();
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<@NonNull MultiValueMap<@NonNull String, String>> entity = new HttpEntity<>(
            headers
        );
        ResponseEntity<@NonNull String> personEntity = restTemplate.exchange(
            requestUri,
            HttpMethod.GET,
            entity,
            String.class
        );
        return personEntity.getStatusCode() == HttpStatus.OK;
    }

    /**
     * Versucht, die angegebene Codeliste vom XRepo zu laden. Das Ergebnis kann gecached werden. Die
     * Cache-Konfiguration muss sicherstellen, dass jeder Eintrag nach einer Wartezeit wieder entfernt
     * wird und es damit zu Retries kommt, da auch fehlerhafte Versuche gecached werden (sollen).
     *
     * @param uri                   die URI der Codeliste
     * @param version               die Version
     * @param ignoreDownloadFailure falls {@literal true} werden sämtliche Fehler ignoriert
     * @return das runtergeladene CodeListDocument oder {@literal null} falls
     * {@literal ignoreDownloadFailure==true} und ein Fehler aufgetreten ist
     */
    @Cacheable(
        cacheNames = CACHE_NAME_CODELIST_DOWNLOAD,
        cacheManager = CacheConfiguration.IN_MEM_MGR,
        key = "{#uri, #version}"
    )
    public CodeListDocument downloadCodeMap(
        String uri,
        String version,
        boolean ignoreDownloadFailure
    ) {
        try {
            return downloadCodeMap(uri, version);
        } catch (RuntimeException e) {
            Exceptions.findCause(e, InterruptedException.class).ifPresent(_ ->
                Thread.currentThread().interrupt()
            );
            if (ignoreDownloadFailure) {
                // PV-8946 catch-log-skip
                log.warn("Fehler beim Codelist-Download: {}", e.getMessage());
                return null;
            } else {
                //PV-8946 catch-rethrow
                throw e;
            }
        }
    }

    protected CodeListDocument downloadCodeMap(String uri, String version) {
        CodeListDocument response = null;
        try {
            log.info("EXTERN: Lade Codeliste herunter. URI {}, Version {}", uri, version);
            ResponseEntity<@NonNull String> responseString = restTemplate.getForEntity(
                parseUri(uri, version),
                String.class
            );
            boolean fileOk = antivirusService.scanFileContent(responseString.getBody());
            if (fileOk) {
                if (
                    responseString.getStatusCode() == HttpStatus.OK &&
                    responseString.getBody() != null
                ) {
                    StringReader reader = new StringReader(responseString.getBody());
                    @SuppressWarnings("unchecked")
                    JAXBElement<CodeListDocument> root = (JAXBElement<CodeListDocument>) jaxbContext
                        .createUnmarshaller()
                        .unmarshal(reader);
                    response = root.getValue();
                }
            } else {
                throw new ImportException(
                    ErrorCodeEnum.CODELIST_DOWNLOAD,
                    messageSource.getMessage(
                        "rest_codelist_service.exception_download_codelist_virus_detected_error",
                        new String[] { uri, RequestUtils.sanitizeUserInput(version) },
                        Locale.GERMAN
                    ),
                    ExceptionType.TECHNISCH
                );
            }
            log.info(
                "EXTERN: Codeliste erfolgreich heruntergeladen. URI: {}, Version: {}",
                uri,
                version
            );
        } catch (JAXBException e) {
            //PV-8946 catch-rethrow
            throw new ImportException(
                ErrorCodeEnum.CODELIST_DOWNLOAD,
                messageSource.getMessage(
                    "rest_codelist_service.exception_download_codelist_parse_error",
                    new String[] { parseUri(uri, version).toString() },
                    Locale.GERMAN
                ),
                e
            );
        } catch (ResourceAccessException e) {
            //PV-8946 catch-rethrow
            if (e.getCause() instanceof ConnectException) {
                throw new ApiWarnException(
                    ErrorCodeEnum.CODELIST_DOWNLOAD,
                    messageSource.getMessage(
                        "rest_codelist_service.exception_download_codelist_xrepo_not_reachable_error",
                        null,
                        Locale.GERMAN
                    ),
                    ExceptionType.TECHNISCH
                );
            }
            throw new ImportException(
                ErrorCodeEnum.CODELIST_DOWNLOAD,
                messageSource.getMessage(
                    "rest_codelist_service.exception_download_codelist_other_error",
                    new String[] { parseUri(uri, version).toString() },
                    Locale.GERMAN
                ),
                ExceptionType.TECHNISCH
            );
        } catch (RestClientException e) {
            //PV-8946 catch-rethrow
            if (
                e instanceof HttpClientErrorException httpClientErrorException &&
                httpClientErrorException.getStatusCode().value() == HttpStatus.NOT_FOUND.value()
            ) {
                throw new ApiException(
                    ErrorCodeEnum.NOT_FOUND,
                    messageSource.getMessage(
                        "rest_codelist_service.exception_download_codelist_not_found_error",
                        new String[] { parseUri(uri, version).toString() },
                        Locale.GERMAN
                    ),
                    e
                );
            }
            throw new ImportException(
                ErrorCodeEnum.CODELIST_DOWNLOAD,
                messageSource.getMessage(
                    "rest_codelist_service.exception_download_codelist_other_error",
                    new String[] { parseUri(uri, version).toString() },
                    Locale.GERMAN
                ),
                ExceptionType.TECHNISCH,
                e
            );
        } catch (IOException e) {
            //PV-8946 catch-rethrow
            throw new ImportException(
                ErrorCodeEnum.CODELIST_DOWNLOAD,
                messageSource.getMessage(
                    "rest_codelist_service.exception_download_codelist_virus_scan_error",
                    null,
                    Locale.GERMAN
                ),
                ExceptionType.TECHNISCH,
                e
            );
        }
        return response;
    }

    protected String getCurrentVersionNameFromCodelist(String uri) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<@NonNull String> entity = new HttpEntity<>("parameters", headers);
        String version = null;

        try {
            ResponseEntity<@NonNull String> responseString = restTemplate.exchange(
                parseUri(uri, null),
                HttpMethod.GET,
                entity,
                String.class
            );

            String body = responseString.getBody();
            if (body == null) {
                throw new ImportException(
                    ErrorCodeEnum.BAD_REQUEST,
                    messageSource.getMessage(CURRENT_VERSION_NAME_ERROR_CODE, null, Locale.GERMAN),
                    ExceptionType.TECHNISCH
                );
            }

            if (!antivirusService.scanFileContent(body)) {
                throw new ImportException(
                    ErrorCodeEnum.CODELIST_DOWNLOAD,
                    messageSource.getMessage(
                        "rest_codelist_service.exception_search_codelist_virus_detected_error",
                        new String[] { uri },
                        Locale.GERMAN
                    ),
                    ExceptionType.TECHNISCH
                );
            }

            if (responseString.getStatusCode() == HttpStatus.OK) {
                JsonNode node = jsonMapper.readTree(body);
                if (node.hasNonNull("version")) {
                    version = node.get("version").asString();
                } else {
                    throw new ImportException(
                        ErrorCodeEnum.BAD_REQUEST,
                        messageSource.getMessage(
                            CURRENT_VERSION_NAME_ERROR_CODE,
                            null,
                            Locale.GERMAN
                        ),
                        ExceptionType.TECHNISCH
                    );
                }
            }

            log.info(
                "Aktuelle Codelistenversion heruntergeladen. Uri: {}, Version: {}",
                uri,
                version
            );
            return version;
        } catch (JacksonException | RestClientException | IOException e) {
            throw new ImportException(
                ErrorCodeEnum.BAD_REQUEST,
                messageSource.getMessage(CURRENT_VERSION_NAME_ERROR_CODE, null, Locale.GERMAN),
                e
            );
        }
    }

    private URI parseUri(@NonNull String uriCodelist, String versionCodelist) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(
            properties.getCodelist().getBaseUrl()
        );
        if (versionCodelist != null) {
            return builder
                .pathSegment("xrepository", "{uriCodelist}_{versionCodelist}", "genericode")
                .build(uriCodelist, versionCodelist);
        } else {
            return builder
                .pathSegment("codeliste", "{uriCodelist}", "gueltigeVersion")
                .build(uriCodelist);
        }
    }
}
