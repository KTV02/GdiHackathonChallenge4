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
package de.dataport.suchdienst.config;

public final class AuthorityRoles {

    private AuthorityRoles() {}

    public static final class Backoffice {

        private Backoffice() {}

        public static final String ES_HEALTH = "hasRole('o_sud_read_elastic_search_state')";
        public static final String ES_INDICES = "hasRole('o_sud_read_elastic_search_indices')";
        public static final String GEOCODING = "hasRole('o_sud_test_geocoding_access')";
        public static final String READ_TASKS = "hasRole('o_sud_read_tasks')";
        public static final String READ_DATA_OVERVIEW = "hasRole('o_sud_read_dataoverview')";
    }

    public static final class XzufiImport {

        private XzufiImport() {}

        public static final String FROM_DIR = "hasRole('o_sud_start_xzufi_import_from_directory')";
        public static final String FROM_FILE = "hasRole('o_sud_start_xzufi_import_from_file')";
        public static final String FROM_PUSH = "hasRole('o_sud_push_xml')";
        public static final String READ_IMPORT_QUEUE = "hasRole('o_sud_read_import_queue')";
        public static final String DELETE_IMPORT_REQUEST = "hasRole('o_sud_delete_import_request')";
        public static final String UPDATE_IMPORT_REQUEST_STATUS =
            "hasRole('o_sud_update_import_request_status')";
        public static final String READ_IMPORT_REQUEST_DETAILS =
            "hasRole('o_sud_read_import_request_details')";
    }

    public static final class Ars {

        private Ars() {}

        public static final String UPLOAD = "hasRole('o_sud_upload_regional_keys')";
        public static final String UPLOAD_ZIP = "hasRole('o_sud_upload_zip')";
    }

    public static final class Testing {

        private Testing() {}

        public static final String SD_IDS = "hasRole('o_sud_read_testing_services_ids')";
        public static final String OD_IDS = "hasRole('o_sud_read_testing_online_services_ids')";
        public static final String OU_IDS = "hasRole('o_sud_read_testing_organisation_unit_ids')";
        public static final String FO_IDS = "hasRole('o_sud_read_testing_formular_ids')";
        public static final String SP_IDS = "hasRole('o_sud_read_testing_specialisation_ids')";
        public static final String IMPORT_ACTIVE = "hasRole('o_sud_read_testing_import_active')";
    }

    public static final class Codelist {

        private Codelist() {}

        public static final String READ = "hasRole('o_sud_read_codelists')";
        public static final String READ_ENTRIES = "hasRole('o_sud_read_codelist_entries')";
        public static final String READ_ID = "hasRole('o_sud_read_codelist_entry_by_id')";
        public static final String READ_URI = "hasRole('o_sud_update_codelist_by_uri_and_version')";
        public static final String UPLOAD = "hasRole('o_sud_upload_codelist')";
        public static final String STATS = "hasRole('o_sud_read_codelist_info')";
    }

    public static final class Verwaltungsleistung {

        private Verwaltungsleistung() {}

        public static final String CALCULATE = "hasRole('o_sud_calculate_verwaltungsleistung')";
        public static final String CALCULATE_ALL =
            "hasRole('o_sud_calculate_all_verwaltungsleistung')";
        public static final String READ_UPTODATE =
            "hasRole('o_sud_read_uptodate_verwaltungsleistung')";
    }

    public static final class OnlineServiceRelations {

        private OnlineServiceRelations() {}

        public static final String CALCULATE = "hasRole('o_sud_calculate_od_rel')";
        public static final String CALCULATE_ALL = "hasRole('o_sud_calculate_all_od_rel')";
    }
}
