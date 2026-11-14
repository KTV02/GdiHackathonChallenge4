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
package de.dataport.suchdienst.suchen.controller;

import de.dataport.suchdienst.config.apiaudience.ApiAudience;
import de.dataport.suchdienst.config.apiaudience.ApiAudienceMarker;
import de.dataport.suchdienst.error.model.ErrorCodeEnum;
import de.dataport.suchdienst.error.model.ErrorModel;
import de.dataport.suchdienst.suchen.exception.SuchenException;
import de.dataport.suchdienst.suchen.model.out.statistic.FederalStateServices;
import de.dataport.suchdienst.suchen.service.StatisticService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@Validated
@ApiAudienceMarker(ApiAudience.PUBLIC)
@Tag(
    name = "Statistik",
    description = "Dieser Endpunkt liefert Statistiken über den Datenbestand aus."
)
public class StatisticController {

    private final StatisticService service;

    public StatisticController(StatisticService service) {
        this.service = service;
    }

    @GetMapping(value = "/v2/client-stats", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = "200",
                description = "Liefert die Statistik aus.",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = FederalStateServices.class),
                    examples = @ExampleObject(
                        name = "Statistik",
                        description = "erzeugt mit produktionsnahen Daten am 2022-07-20",
                        value = """
                        {
                            "timestamp": "2022-07-20T10:15:32.595314627Z",
                            "stateServices": {},
                            "stateStatistics": [
                                {
                                    "ars": "00",
                                    "lbs": {
                                        "count": 748,
                                        "lastUpdate": "2022-07-19T06:53:24.241073052Z"
                                    },
                                    "ods": {
                                        "count": 130,
                                        "lastUpdate": "2022-07-19T06:53:24.240965870Z"
                                    },
                                    "oes": {
                                        "count": 326,
                                        "lastUpdate": "2022-07-19T06:53:24.240986605Z"
                                    },
                                    "sps": {
                                        "count": 0
                                    }
                                },
                                {
                                    "ars": "01",
                                    "lbs": {
                                        "count": 792,
                                        "lastUpdate": "2022-07-19T07:20:17.278932448Z"
                                    },
                                    "ods": {
                                        "count": 271,
                                        "lastUpdate": "2022-07-19T07:20:16.796392653Z"
                                    },
                                    "oes": {
                                        "count": 3462,
                                        "lastUpdate": "2022-07-19T07:20:17.278885268Z"
                                    },
                                    "sps": {
                                        "count": 7075,
                                        "lastUpdate": "2022-07-19T07:20:16.547389372Z"
                                    }
                                },
                                {
                                    "ars": "02",
                                    "lbs": {
                                        "count": 782,
                                        "lastUpdate": "2022-07-19T07:32:27.427464354Z"
                                    },
                                    "ods": {
                                        "count": 80,
                                        "lastUpdate": "2022-07-19T07:32:26.774724853Z"
                                    },
                                    "oes": {
                                        "count": 7028,
                                        "lastUpdate": "2022-07-19T07:32:27.427423827Z"
                                    },
                                    "sps": {
                                        "count": 0
                                    }
                                },
                                {
                                    "ars": "03",
                                    "lbs": {
                                        "count": 1204,
                                        "lastUpdate": "2022-07-19T07:32:44.184895074Z"
                                    },
                                    "ods": {
                                        "count": 1378,
                                        "lastUpdate": "2022-07-19T07:32:44.086878429Z"
                                    },
                                    "oes": {
                                        "count": 5715,
                                        "lastUpdate": "2022-07-19T07:32:44.184855294Z"
                                    },
                                    "sps": {
                                        "count": 16769,
                                        "lastUpdate": "2022-07-19T07:32:43.381343560Z"
                                    }
                                },
                                {
                                    "ars": "04",
                                    "lbs": {
                                        "count": 725,
                                        "lastUpdate": "2022-07-19T06:52:55.753268143Z"
                                    },
                                    "ods": {
                                        "count": 418,
                                        "lastUpdate": "2022-07-19T06:52:55.574287893Z"
                                    },
                                    "oes": {
                                        "count": 212,
                                        "lastUpdate": "2022-07-19T06:52:55.753231130Z"
                                    },
                                    "sps": {
                                        "count": 0
                                    }
                                },
                                {
                                    "ars": "05",
                                    "lbs": {
                                        "count": 2398,
                                        "lastUpdate": "2022-07-19T07:45:04.930550542Z"
                                    },
                                    "ods": {
                                        "count": 1224,
                                        "lastUpdate": "2022-07-19T07:45:04.851279111Z"
                                    },
                                    "oes": {
                                        "count": 8579,
                                        "lastUpdate": "2022-07-19T07:45:04.930131315Z"
                                    },
                                    "sps": {
                                        "count": 122212,
                                        "lastUpdate": "2022-07-19T07:45:04.930263106Z"
                                    }
                                },
                                {
                                    "ars": "06",
                                    "lbs": {
                                        "count": 1101,
                                        "lastUpdate": "2022-07-19T07:07:13.172307543Z"
                                    },
                                    "ods": {
                                        "count": 1160,
                                        "lastUpdate": "2022-07-19T07:07:12.914191030Z"
                                    },
                                    "oes": {
                                        "count": 6129,
                                        "lastUpdate": "2022-07-19T07:07:13.171898551Z"
                                    },
                                    "sps": {
                                        "count": 8270,
                                        "lastUpdate": "2022-07-19T07:07:13.172190289Z"
                                    }
                                },
                                {
                                    "ars": "07",
                                    "lbs": {
                                        "count": 1464,
                                        "lastUpdate": "2022-07-19T07:20:24.126810804Z"
                                    },
                                    "ods": {
                                        "count": 1471,
                                        "lastUpdate": "2022-07-19T07:20:10.801643050Z"
                                    },
                                    "oes": {
                                        "count": 4305,
                                        "lastUpdate": "2022-07-19T07:20:24.126398398Z"
                                    },
                                    "sps": {
                                        "count": 5456,
                                        "lastUpdate": "2022-07-19T07:20:24.126434331Z"
                                    }
                                },
                                {
                                    "ars": "09",
                                    "lbs": {
                                        "count": 1758,
                                        "lastUpdate": "2022-07-19T08:34:53.494492467Z"
                                    },
                                    "ods": {
                                        "count": 34089,
                                        "lastUpdate": "2022-07-19T08:34:53.493357002Z"
                                    },
                                    "oes": {
                                        "count": 13627,
                                        "lastUpdate": "2022-07-19T08:34:53.493400214Z"
                                    },
                                    "sps": {
                                        "count": 232738,
                                        "lastUpdate": "2022-07-19T08:34:32.198085504Z"
                                    }
                                },
                                {
                                    "ars": "11",
                                    "lbs": {
                                        "count": 855,
                                        "lastUpdate": "2022-07-19T07:25:31.050583286Z"
                                    },
                                    "ods": {
                                        "count": 0
                                    },
                                    "oes": {
                                        "count": 685,
                                        "lastUpdate": "2022-07-19T07:25:31.050546529Z"
                                    },
                                    "sps": {
                                        "count": 0
                                    }
                                },
                                {
                                    "ars": "12",
                                    "lbs": {
                                        "count": 971,
                                        "lastUpdate": "2022-07-19T07:23:32.864106752Z"
                                    },
                                    "ods": {
                                        "count": 90,
                                        "lastUpdate": "2022-07-19T07:23:06.220174727Z"
                                    },
                                    "oes": {
                                        "count": 681,
                                        "lastUpdate": "2022-07-19T07:23:32.863749176Z"
                                    },
                                    "sps": {
                                        "count": 1168,
                                        "lastUpdate": "2022-07-19T07:23:32.863795896Z"
                                    }
                                },
                                {
                                    "ars": "13",
                                    "lbs": {
                                        "count": 915,
                                        "lastUpdate": "2022-07-19T07:18:11.005474044Z"
                                    },
                                    "ods": {
                                        "count": 707,
                                        "lastUpdate": "2022-07-19T07:18:11.005412169Z"
                                    },
                                    "oes": {
                                        "count": 1337,
                                        "lastUpdate": "2022-07-19T07:18:11.005422761Z"
                                    },
                                    "sps": {
                                        "count": 1704,
                                        "lastUpdate": "2022-07-19T07:18:09.311520836Z"
                                    }
                                },
                                {
                                    "ars": "15",
                                    "lbs": {
                                        "count": 995,
                                        "lastUpdate": "2022-07-19T07:00:48.226773965Z"
                                    },
                                    "ods": {
                                        "count": 67,
                                        "lastUpdate": "2022-07-19T07:00:11.015474793Z"
                                    },
                                    "oes": {
                                        "count": 3855,
                                        "lastUpdate": "2022-07-19T07:00:48.226459685Z"
                                    },
                                    "sps": {
                                        "count": 1264,
                                        "lastUpdate": "2022-07-19T07:00:48.226539239Z"
                                    }
                                },
                                {
                                    "ars": "16",
                                    "lbs": {
                                        "count": 769,
                                        "lastUpdate": "2022-07-19T07:01:12.617807491Z"
                                    },
                                    "ods": {
                                        "count": 847,
                                        "lastUpdate": "2022-07-19T07:01:12.550006762Z"
                                    },
                                    "oes": {
                                        "count": 2637,
                                        "lastUpdate": "2022-07-19T07:01:12.617595682Z"
                                    },
                                    "sps": {
                                        "count": 1952,
                                        "lastUpdate": "2022-07-19T07:01:12.617638564Z"
                                    }
                                }
                            ]
                        }"""
                    )
                )
            ),
            @ApiResponse(
                responseCode = "500",
                description = "Es ist ein interner Fehler aufgetreten",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorModel.class)
                )
            ),
        }
    )
    @Operation(
        summary = "Statistiken über Leistungen, Organisationseinheiten, Onlinedienste und Spezialisierungen",
        description = "Gibt Statistiken über vorhandene Leistungen, Organisationseinheiten, Onlinedienste und Spezialisierungen zurück. Es werden pro Bundesland und Gesamt Deutschland die Anzahl der Objekte und der Zeitpunkt der letzten Änderung angegeben."
    )
    public ResponseEntity<FederalStateServices> getClientStatsV2() {
        try {
            Map<String, Integer> clientBackwardCompatibility = new HashMap<>();
            return ResponseEntity.ok(
                new FederalStateServices(clientBackwardCompatibility, service.getStatistics())
            );
        } catch (Exception e) {
            // PV-8946 catch-log-rethrow
            throw new SuchenException(
                ErrorCodeEnum.ELSE_IO_ERROR,
                "Fehler beim Abrufen der Client-Stats",
                e
            );
        }
    }
}
