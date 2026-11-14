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
package de.dataport.suchdienst.testcontainers;

import java.time.Duration;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Basisklasse für Tests, die Elasticsearch Container verwenden, hier initialisieren wir den Container und sorgen
 * für ein gemeinsames Logging des Setups, um z.b. das Debugging zu vereinfachen.
 */
@Slf4j
public abstract class AbstractElasticsearchContainerTest {

    @Container
    @ServiceConnection
    protected static final ElasticsearchContainer elastic = createElasticsearchContainer();

    /**
     * Das Logging insbesondere der httpHostAddress ist nützlich, damit kann man den Test im Debugger anhalten und über die
     * httpHostAddress z.b. mit Kibana oder IDE Plugin in Elasticsearch hineinschauen.
     */
    @BeforeAll
    public static void logContainerProperties() {
        var httpHostAddress = elastic.getHttpHostAddress();
        var exposedPorts = elastic.getExposedPorts();
        log.info(
            "Container '{}' startet, httpHostAddress={}, exposedPorts={} ",
            elastic.getDockerImageName(),
            httpHostAddress,
            exposedPorts
        );
    }

    public static ElasticsearchContainer createElasticsearchContainer() {
        return new ElasticsearchContainer(
            DockerImageName.parse(
                "pvog-images.repo-ex.zcdi.dataport.de/elasticsearch:9.5.5"
            ).asCompatibleSubstituteFor("docker.elastic.co/elasticsearch/elasticsearch")
        )
            .withEnv(
                Map.of(
                    "discovery.type",
                    "single-node",
                    "xpack.security.enabled",
                    "false",
                    "ES_JAVA_OPTS",
                    "-Xms512m -Xmx512m"
                )
            )
            .withCopyFileToContainer(
                MountableFile.forClasspathResource("decompounder/word_list_de.txt"),
                "/usr/share/elasticsearch/config/decompounder/word_list_de.txt"
            )
            .withCopyFileToContainer(
                MountableFile.forClasspathResource("decompounder/hyphenation_patterns_de.xml"),
                "/usr/share/elasticsearch/config/decompounder/hyphenation_patterns_de.xml"
            )
            .withStartupTimeout(Duration.ofMinutes(2));
    }
}
