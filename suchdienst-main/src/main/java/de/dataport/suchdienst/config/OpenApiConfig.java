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

import static io.swagger.v3.oas.models.security.SecurityScheme.Type.OAUTH2;

import de.dataport.suchdienst.config.apiaudience.ApiAudience;
import de.dataport.suchdienst.config.apiaudience.ApiAudienceResolver;
import de.dataport.suchdienst.config.conditions.OnMasterCondition;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.Arrays;
import java.util.List;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

@Configuration
public class OpenApiConfig {

    private static final String ACCEPT_LANGUAGE_PARAMETER = "AcceptLanguage";
    private static final String CONTENT_LANGUAGE_HEADER = "ContentLanguage";
    private static final String LANGUAGE_DESCRIPTION = """
    Sprachabhängige Inhalte werden anhand des optionalen HTTP-Headers `Accept-Language` ausgewählt. \
    Unterstützt werden Deutsch (`de`) und Englisch (`en`); regionale Varianten wie `en-US` werden der jeweiligen Sprache zugeordnet. \
    Wenn der Header fehlt, ungültig ist oder keine unterstützte Sprache enthält, wird Deutsch verwendet. \
    Der Antwortheader `Content-Language` enthält die tatsächlich verwendete Sprache. \
    Mehrsprachige Onlinedienstparameter können nur für Sprachen ausgegeben werden, die im Feld `sprachversion` des Onlinedienstes enthalten sind.
    """;

    @Value("${info.app.version:-}")
    private String version;

    private final String authServer;
    private final String realm;
    private final ApiAudienceResolver apiAudienceResolver;

    @Autowired
    public OpenApiConfig(
        @Value("${keycloak.auth-server-url}") String authServer,
        @Value("${keycloak.realm}") String realm,
        ApiAudienceResolver apiAudienceResolver
    ) {
        this.authServer = authServer;
        this.realm = realm;
        this.apiAudienceResolver = apiAudienceResolver;
    }

    @Bean
    public OpenAPI openApi(Environment env) {
        OpenAPI openApi = new OpenAPI().info(
            new Info()
                .title("Portalverbund - Suchdienst REST-API")
                .contact(new Contact().name("FITKO").url("https://www.fitko.de/"))
                .license(
                    new License()
                        .name("Creative Commons Attribution Share Alike 4.0 (CC BY-SA 4.0)")
                        .url("https://creativecommons.org/licenses/by-sa/4.0/")
                )
                .version(version)
                .description(
                    "Das Portalverbund-Onlinegateway (PVOG) stellt zentral die Verwaltungsleistungen von Bund und Ländern bereit. " +
                        "Die Aufgabe des Suchdienstes ist es, die bereitgestellten Verwaltungsleistungen anwendergerecht und öffentlich durchsuchbar und auffindbar zu machen. " +
                        "Die vom Suchdienst bereitgestellte REST-API bietet verschiedene Endpunkte zum Durchsuchen des Datenbestands des PVOG." +
                        "<br/><br/>" +
                        LANGUAGE_DESCRIPTION.replace("\n", "<br/>") +
                        "<br/><br/>" +
                        "Hinweis: Die APIs der Produktivumgebung unter pvog.fitko.de werden am 01.08.2026 abgeschaltet. " +
                        "Bitte stellen Sie rechtzeitig auf pvog.fitko.net um. Für die Umgebungen Demo und PreProd sind keine Änderungen notwendig. " +
                        "<br/><br/>" +
                        "Weitere Informationen zur Versionierung der Schnittstellen finden Sie in unserem " +
                        "<a target=\"_blank\" href=\"https://produktportal.pvog.fitko.de/docs/suchdienst/doku/#versionierung-der-schnittstelle\">Produktportal</a>."
                )
        );
        Components components = new Components()
            .addParameters(
                ACCEPT_LANGUAGE_PARAMETER,
                new Parameter()
                    .in("header")
                    .name("Accept-Language")
                    .description(LANGUAGE_DESCRIPTION)
                    .required(false)
                    .schema(new StringSchema().example("de"))
            )
            .addHeaders(
                CONTENT_LANGUAGE_HEADER,
                new Header()
                    .description("Die für die Antwort tatsächlich verwendete Sprache.")
                    .schema(new StringSchema()._enum(List.of("de", "en")))
            );

        List<String> activeProfiles = Arrays.asList(env.getActiveProfiles());
        if (
            activeProfiles.contains(Profiles.PRODUCTION) &&
            activeProfiles.contains(Profiles.OPENAPI_SHOW_INTERNAL)
        ) {
            components.addSecuritySchemes("pvog_auth", internApiAuthentication());
        }

        openApi.components(components);
        return openApi;
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
            .group("suchen")
            .displayName("Suchen")
            .addOperationCustomizer(languageDocumentationCustomizer())
            .addOpenApiMethodFilter(method ->
                apiAudienceResolver.hasAudience(method, ApiAudience.PUBLIC)
            )
            .build();
    }

    @Bean
    @Profile(Profiles.BETA_API_ACTIVE)
    public GroupedOpenApi betaApi() {
        return GroupedOpenApi.builder()
            .group("beta")
            .displayName("Beta")
            .addOperationCustomizer(languageDocumentationCustomizer())
            .addOpenApiMethodFilter(method ->
                apiAudienceResolver.hasAudience(method, ApiAudience.BETA)
            )
            .build();
    }

    @Profile(Profiles.OPENAPI_SHOW_INTERNAL)
    @Bean
    @Conditional(OnMasterCondition.class)
    public GroupedOpenApi internApi() {
        return GroupedOpenApi.builder()
            .group("admin")
            .displayName("Admin")
            .addOpenApiMethodFilter(method ->
                apiAudienceResolver.hasAudience(method, ApiAudience.INTERNAL)
            )
            .build();
    }

    private OperationCustomizer languageDocumentationCustomizer() {
        return (operation, _) -> {
            operation.addParametersItem(
                new Parameter().$ref("#/components/parameters/" + ACCEPT_LANGUAGE_PARAMETER)
            );
            operation
                .getResponses()
                .values()
                .forEach(response ->
                    response.addHeaderObject(
                        "Content-Language",
                        new Header().$ref("#/components/headers/" + CONTENT_LANGUAGE_HEADER)
                    )
                );
            return operation;
        };
    }

    private SecurityScheme internApiAuthentication() {
        var authUrl = String.format(
            "%s/realms/%s/protocol/openid-connect/auth",
            this.authServer,
            this.realm
        );
        var tokenUrl = String.format(
            "%s/realms/%s/protocol/openid-connect/token",
            this.authServer,
            this.realm
        );

        var oAuthAuthorizationCodeFlow = new OAuthFlow();
        var oAuthClientCredentialsFlow = new OAuthFlow();

        oAuthAuthorizationCodeFlow.authorizationUrl(authUrl);
        oAuthAuthorizationCodeFlow.tokenUrl(tokenUrl);
        oAuthAuthorizationCodeFlow.setScopes(new Scopes());
        oAuthClientCredentialsFlow.tokenUrl(tokenUrl);
        oAuthClientCredentialsFlow.setScopes(new Scopes());

        return new SecurityScheme()
            .type(OAUTH2)
            .flows(
                new OAuthFlows()
                    .authorizationCode(oAuthAuthorizationCodeFlow)
                    .clientCredentials(oAuthClientCredentialsFlow)
            );
    }
}
