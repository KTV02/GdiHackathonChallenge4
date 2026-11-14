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
package de.dataport.suchdienst.backoffice.controller;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Dieses Interface wird nie implementiert.
 * Einziger Sinn ist, die Annotationen bereitzustellen, die swagger zur Generierung der Schnittstellenbeschreibung nutzt.
 * vgl. <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/cat-health.html">Doku</a>.
 * <p/>
 * Im Unterschied dazu ist <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/cluster-health.html">Doku</a>
 * nicht die API, die wir tatsächlich benutzen, aber dort ist etwas mehr Info.
 */
@Schema(
    description = "You can use the cat health API to get the health status of a cluster. " +
        "Siehe <a href=\"https://www.elastic.co/guide/en/elasticsearch/reference/current/cat-health.html\">elasticsearch-Referenz</a>.<p/>" +
        "cat APIs are only intended for human consumption using the command line or Kibana console. " +
        "They are not intended for use by applications."
)
@SuppressWarnings("java:S100") // die Methodennamen wurden hier bewusst so vergeben
interface ElasticHealthResponse {
    @Schema(description = "(integer) Epoche in Sekunden", example = "\"1658315331\"")
    String getEpoch();

    @Schema(
        example = "11:42:30",
        format = "HH:MM:SS",
        description = "Zeitstempel im Format HH:MM:SS"
    )
    String getTimestamp();

    @Schema(example = "elasticsearch")
    String getCluster();

    @Schema(
        description = "(string) Health status of the cluster, based on the state of its primary and replica shards.<p/>" +
            "Statuses are:<ul>" +
            "<li>green: All shards are assigned.</li>" +
            "<li>yellow: All primary shards are assigned, but one or more replica shards are unassigned. " +
            "If a node in the cluster fails, some data could be unavailable until that node is repaired.</li>" +
            "<li>red: One or more primary shards are unassigned, so some data is unavailable. " +
            "This can occur briefly during cluster startup as primary shards are assigned.</li>" +
            "</ul>",
        allowableValues = { "green", "yellow", "red" } // see org.elasticsearch.cluster.health.ClusterHealthStatus
    )
    String getStatus();

    @Schema(name = "node.total", description = "(integer) The number of nodes within the cluster.")
    String getNode_Total();

    @Schema(
        name = "node.data",
        description = "(integer) The number of nodes that are dedicated data nodes."
    )
    String getNode_Data();

    @Schema(description = "(integer) The total number of active primary and replica shards. ")
    String getShards();

    @Schema(description = "(integer) The number of active primary shards.")
    String getPri();

    @Schema(description = "(integer) The number of shards that are under relocation.")
    String getRelo();

    @Schema(description = "(integer) The number of shards that are under initialization.")
    String getInit();

    @Schema(description = "(integer) The number of shards that are not allocated.")
    String getUnassign();

    @Schema(
        description = "(integer) The number of cluster-level changes that have not yet been executed."
    )
    String getPending_tasks();

    @Schema(
        example = "-",
        description = "\"-\" or the time expressed in milliseconds since the earliest initiated task is waiting for being performed."
    )
    // z.B. String "-", also keinen Integer
    String getMax_task_wait_time();

    @Schema(
        example = "54.6%",
        description = "The ratio of active shards in the cluster expressed as a percentage."
    )
    // z.B. String "54.6%", also keinen float
    String getActive_shards_percent();
}
