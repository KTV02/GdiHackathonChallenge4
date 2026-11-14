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
package de.dataport.suchdienst.suchen.model.out.xzuficopy;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.CollapsedStringAdapter;
import jakarta.xml.bind.annotation.adapters.NormalizedStringAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import lombok.Data;

/**
 * @deprecated Kopie der Klasse {@link de.xoev.xzufi.v2_2_0.Code}!
 * Wurde angelegt zur Entkopplung der Daten-Modelle und soll mit wachsenden API-Versionen entfernt werden!
 */
@Deprecated(forRemoval = true)
@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    name = "Code",
    namespace = "http://xoev.de/schemata/code/1_0",
    propOrder = { "code", "name" }
)
public class Code {

    /**
     * In diesem XML-Element wird der Code einer Codeliste übermittelt.
     */
    @XmlElement(required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "token")
    protected String code; // NOSONAR - Bezeichnung "code" ist vorgegeben und darf nicht geändert werden

    /**
     * Mit diesem optionalen XML-Element kann die Beschreibung des Codes, wie in der jeweiligen Beschreibungsspalte der Codeliste vorgegeben, übermittelt werden.
     */
    @XmlJavaTypeAdapter(NormalizedStringAdapter.class)
    @XmlSchemaType(name = "normalizedString")
    protected String name;

    /**
     * Mit diesem XML-Attribut wird die Kennung der Codeliste übermittelt, in deren Kontext der jeweilige Code zu interpretieren ist. Die Kennung identifiziert die Codeliste, nicht jedoch deren Version eindeutig.
     * Wird bereits im Rahmen des XÖV-Standards eine Kennung vorgegeben (es handelt sich in diesem Fall um einen Code-Typ 1, 2 oder 3) darf auf eine nochmalige Angabe der Kennung bei der Übermittlung eines Codes verzichtet werden. Aus diesem Grund ist das XML-Attribut listURI zunächst als optional deklariert.
     */
    @XmlAttribute(name = "listURI")
    @XmlSchemaType(name = "anyURI")
    protected String listURI;

    /**
     * Die konkrete Version der zu nutzenden Codeliste wird mit diesem XML-Attribut übertragen.
     * Analog zum listURI ist die Bestimmung der Version einer Codeliste bei der Übertragung eines Codes zwingend. Die Version kann jedoch ebenfalls bereits im XÖV-Standard festgelegt werden (es handelt sich in diesem Fall um einen Code-Typ 1 oder 2).
     */
    @XmlAttribute(name = "listVersionID")
    @XmlJavaTypeAdapter(NormalizedStringAdapter.class)
    @XmlSchemaType(name = "normalizedString")
    protected String listVersionID;
}
