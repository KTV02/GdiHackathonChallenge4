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
package de.dataport.suchdienst.util.model;

/**
 * Definiert die gültigen Schlüssel des MDC (Mapped Diagnostic Context). Idealerweise werden in den
 * anderen Diensten dieselben Schlüssel verwendet. Gesetzt werden sie ausschließlich über
 * {@code MdcTemplate}, alle Schlüssel landen unverändert in der Log-Ausgabe.
 *
 * <p>Die Schlüssel sind nach Gültigkeitsbereich gruppiert:
 * <ul>
 *   <li><b>Request-Kontext</b> — gilt für einen kompletten HTTP-Request, gesetzt im
 *       {@code RequestParameterFilter}.</li>
 *   <li><b>Verarbeitungs-Kontext</b> — gilt für einen Ausschnitt der Verarbeitung (Datei, Leistung,
 *       Geschäftsobjekt) und wird vom jeweiligen Service gesetzt.</li>
 *   <li><b>Messwerte</b> — gelten nur für das eine Log-Statement, das sie ausgibt.</li>
 *   <li><b>API-Zugriff</b> — beschreiben eine eingehende Anfrage an eine öffentliche Schnittstelle,
 *       gesetzt am Zugriffs-Log des {@code RequestParameterFilter}.</li>
 *   <li><b>Steuerung und Fehler</b> — steuern die Auswertung im Log-Backend.</li>
 * </ul>
 */
public final class MdcKeys {

    // ---------------------------------------------------------------------------------------------
    // Request-Kontext: gesetzt im RequestParameterFilter, gilt für den gesamten Request
    // ---------------------------------------------------------------------------------------------

    /**
     * Pro HTTP-Request neu generierte ID. Wird auch in der Fehler-Response als {@code requestId}
     * zurückgegeben, damit ein Aufrufer sein Problem im Log wiederfindet.
     * Beispiel: {@code 8f1c1a1e-0e4e-4d5f-9a44-3b5b0d0f5e11}
     */
    public static final String REQUEST_ID = "request_id";

    /**
     * Benutzer können durch einen optionalen API-Key identifiziert werden (PV-13746).
     */
    public static final String APICLIENTID = "api_client_id";

    /**
     * Transaktionsid die vom Sammlerdienst mit einer Nachricht geliefert wird. Gesetzt für Requests
     * auf den Push-Import-Endpunkt und beim Abarbeiten des zugehörigen Queue-Eintrags.
     */
    public static final String TRANSAKTION_ID = "transaktion_id";

    /**
     * Nachrichten-UUID die vom Sammlerdienst mit einer Nachricht geliefert wird, siehe
     * {@link #TRANSAKTION_ID}.
     */
    public static final String NACHRICHT_UUID = "nachricht_uuid";

    // ---------------------------------------------------------------------------------------------
    // Verarbeitungs-Kontext: gesetzt vom jeweiligen Service für die Dauer eines Verarbeitungsschritts
    // ---------------------------------------------------------------------------------------------

    /**
     * Die ID einer LB, die gerade bearbeitet wird
     */
    public static final String LBID = "lbid";

    /**
     * Die ID einer SP, die gerade bearbeitet wird
     */
    public static final String SPID = "spid";

    /**
     * Die ID einer OE, die gerade bearbeitet wird
     */
    public static final String OEID = "oeid";

    /**
     * Die ID eines OD, der gerade bearbeitet wird
     */
    public static final String ODID = "odid";

    /**
     * Die ID eines FO, das gerade bearbeitet wird
     */
    public static final String FOID = "foid";

    /**
     * Die ID einer ZS, das gerade bearbeitet wird
     */
    public static final String ZSID = "zsid";

    /**
     * Name des Bundeslandes, welches gerade verarbeitet wird
     */
    public static final String BUNDESLAND = "bundesland";

    /**
     * Dateiname einer XZUFI-Datei, die gerade eingelesen wird (kommt nur bei manuellen Uploads ueber
     * das Backoffice vor)
     */
    public static final String XZUFIFILE = "xzufifile";

    // ---------------------------------------------------------------------------------------------
    // Messwerte: gelten nur für das Log-Statement, das sie ausgibt
    // ---------------------------------------------------------------------------------------------

    /**
     * Zeit in Millisekunden, die der Import einer Leistung benötigt hat
     */
    public static final String IMPORT_DURATION = "import_duration";

    /**
     * Anzahl an Objekte, welche beim Import geschrieben wurden
     */
    public static final String OBJECT_COUNT = "object_count";

    // ---------------------------------------------------------------------------------------------
    // API-Zugriff: gesetzt am Zugriffs-Log für eine eingehende Anfrage an eine öffentliche Schnittstelle
    // ---------------------------------------------------------------------------------------------

    /**
     * Der aufgerufene Endpunkt (Pfad) einer eingehenden Anfrage, z. B. für das API-Zugriffs-Logging
     */
    public static final String ENDPOINT = "endpoint";

    /**
     * Die IP-Adresse des anfragenden Systems einer eingehenden Anfrage
     */
    public static final String CLIENT_IP = "client_ip";

    /**
     * Der User-Agent-Header einer eingehenden Anfrage, sofern vom Aufrufer mitgesendet
     */
    public static final String USER_AGENT = "user_agent";

    /**
     * Zeitpunkt, zu dem eine eingehende Anfrage registriert wurde
     */
    public static final String REQUEST_TIMESTAMP = "request_timestamp";

    /**
     * HTTP-Statuscode, mit dem eine eingehende Anfrage beantwortet wurde
     */
    public static final String RESPONSE_STATUS = "response_status";

    // ---------------------------------------------------------------------------------------------
    // Steuerung und Fehler
    // ---------------------------------------------------------------------------------------------

    /**
     * Ordnet eine Log-Ausgabe einem Protokoll zu, siehe {@link ProtocolType}. Für Requests im
     * {@code RequestParameterFilter} gesetzt, innerhalb der Verarbeitung überschreiben ihn einzelne
     * Blöcke (z.B. {@link ProtocolType#EXTERN} um einen Aufruf an einen Fremddienst).
     */
    public static final String PROTOKOLLTYP = "protokolltyp";

    /**
     * Fehlercode, siehe {@link de.dataport.suchdienst.error.model.ErrorCodeEnum}. Nur an der
     * Log-Ausgabe gesetzt, die den Fehler meldet.
     */
    public static final String ERROR_CODE = "error_code";

    /**
     * Benennt den Fremddienst bei {@link ProtocolType#EXTERN}, verfügbare Werte siehe
     * {@link de.dataport.suchdienst.util.model.ExternerBereich}
     */
    public static final String EXTERNER_BEREICH = "externer_bereich";

    /**
     * HTTP-Statuscode einer externen Antwort
     */
    public static final String HTTP_STATUS = "http_status";

    private MdcKeys() {
        // verstecke Konstruktor
    }
}
