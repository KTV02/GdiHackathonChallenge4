/*-
 * ---license-start
 * Suchdienst
 * ---
 * Copyright (C) 2026 Dataport AÖR
 * ---
 * SPDX-License-Identifier: EUPL-1.2
 * Der Suchdienst ist Teil des Projekts Portalverbund Onlinegateway und macht Verwaltungsleistungen für externe Konsumenten anwendergerecht durchsuchbar und auffindbar.
 * ---license-end
 */
package de.dataport.suchdienst.jzufi.model.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ObjectFactory;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.SubclassExhaustiveStrategy;
import org.mapstruct.SubclassMapping;
import org.mapstruct.factory.Mappers;

@Mapper(
    unmappedTargetPolicy = ReportingPolicy.ERROR,
    subclassExhaustiveStrategy = SubclassExhaustiveStrategy.RUNTIME_EXCEPTION
)
public interface JzufiModelMapper {
    JzufiModelMapper INSTANCE = Mappers.getMapper(JzufiModelMapper.class);

    de.dataport.suchdienst.jzufi.model.v23.Leistung map23(de.xoev.xzufi.v2_3_1.Leistung source);

    de.dataport.suchdienst.jzufi.model.v23.OrganisationseinheitErweitert map23(
        de.xoev.xzufi.v2_3_1.OrganisationseinheitErweitert source
    );

    de.dataport.suchdienst.jzufi.model.v23.OnlinedienstErweitert map23(
        de.xoev.xzufi.v2_3_1.OnlinedienstErweitert source
    );

    @SubclassMapping(
        source = de.xoev.xzufi.v2_3_1.LeistungsstrukturObjektMitVerrichtungUndDetail.class,
        target = de.dataport.suchdienst.jzufi.model.v23.LeistungsstrukturObjektMitVerrichtungUndDetail.class
    )
    @SubclassMapping(
        source = de.xoev.xzufi.v2_3_1.LeistungsstrukturObjektMitVerrichtung.class,
        target = de.dataport.suchdienst.jzufi.model.v23.LeistungsstrukturObjektMitVerrichtung.class
    )
    @SubclassMapping(
        source = de.xoev.xzufi.v2_3_1.LeistungsstrukturObjekt.class,
        target = de.dataport.suchdienst.jzufi.model.v23.LeistungsstrukturObjekt.class
    )
    de.dataport.suchdienst.jzufi.model.v23.Leistungsstruktur map23(
        de.xoev.xzufi.v2_3_1.Leistungsstruktur source
    );

    @SubclassMapping(
        source = de.xoev.xzufi.v2_3_1.FormulardateiLink.class,
        target = de.dataport.suchdienst.jzufi.model.v23.FormulardateiLink.class
    )
    @SubclassMapping(
        source = de.xoev.xzufi.v2_3_1.FormulardateiXML.class,
        target = de.dataport.suchdienst.jzufi.model.v23.FormulardateiXML.class
    )
    @SubclassMapping(
        source = de.xoev.xzufi.v2_3_1.FormulardateiBinaer.class,
        target = de.dataport.suchdienst.jzufi.model.v23.FormulardateiBinaer.class
    )
    de.dataport.suchdienst.jzufi.model.v23.Formulardatei map23(
        de.xoev.xzufi.v2_3_1.Formulardatei source
    );

    @SubclassMapping(
        source = de.xoev.xzufi.v2_3_1.AnschriftOrganisationseinheit.class,
        target = de.dataport.suchdienst.jzufi.model.v23.AnschriftOrganisationseinheit.class
    )
    de.dataport.suchdienst.jzufi.model.v23.Anschrift map23(de.xoev.xzufi.v2_3_1.Anschrift source);

    @SubclassMapping(
        source = de.xoev.xzufi.v2_3_1.KontaktpersonErweitert.class,
        target = de.dataport.suchdienst.jzufi.model.v23.KontaktpersonErweitert.class
    )
    de.dataport.suchdienst.jzufi.model.v23.Kontaktperson map23(
        de.xoev.xzufi.v2_3_1.Kontaktperson source
    );

    @SubclassMapping(
        source = de.xoev.xzufi.v2_3_1.OnlinedienstErweitert.class,
        target = de.dataport.suchdienst.jzufi.model.v23.OnlinedienstErweitert.class
    )
    @SubclassMapping(
        source = de.xoev.xzufi.v2_3_1.OnlinedienstVorlage.class,
        target = de.dataport.suchdienst.jzufi.model.v23.OnlinedienstVorlage.class
    )
    de.dataport.suchdienst.jzufi.model.v23.Onlinedienst map23(
        de.xoev.xzufi.v2_3_1.Onlinedienst source
    );

    @SubclassMapping(
        source = de.xoev.xzufi.v2_3_1.Sprachversion.class,
        target = de.dataport.suchdienst.jzufi.model.v23.Sprachversion.class
    )
    de.dataport.suchdienst.jzufi.model.v23.Sprache map23(de.xoev.xzufi.v2_3_1.Sprache source);

    @SubclassMapping(
        source = de.xoev.xzufi.v2_3_1.Gebietsklasse.class,
        target = de.dataport.suchdienst.jzufi.model.v23.Gebietsklasse.class
    )
    de.dataport.suchdienst.jzufi.model.v23.Kategorieklasse map23(
        de.xoev.xzufi.v2_3_1.Kategorieklasse source
    );

    @SubclassMapping(
        source = de.xoev.xzufi.v2_3_1.CodeWithAuthorityType.class,
        target = de.dataport.suchdienst.jzufi.model.v23.CodeWithAuthorityType.class
    )
    de.dataport.suchdienst.jzufi.model.v23.CodeType map23(de.xoev.xzufi.v2_3_1.CodeType source);

    @ObjectFactory
    default de.dataport.suchdienst.jzufi.model.v23.AbstractMetaDataType metadata23() {
        return new de.dataport.suchdienst.jzufi.model.v23.AbstractMetaDataType() {};
    }

    de.dataport.suchdienst.jzufi.model.v23.AbstractMetaDataType map23(
        de.xoev.xzufi.v2_3_1.AbstractMetaDataType source
    );

    de.dataport.suchdienst.jzufi.model.v23.ZustaendigkeitTransferObjekt map23(
        de.xoev.xzufi.v2_3_1.ZustaendigkeitTransferObjekt source
    );

    @SubclassMapping(
        source = de.xoev.xzufi.v2_3_1.ZustaendigkeitOrganisationseinheit.class,
        target = de.dataport.suchdienst.jzufi.model.v23.ZustaendigkeitOrganisationseinheit.class
    )
    @SubclassMapping(
        source = de.xoev.xzufi.v2_3_1.ZustaendigkeitOnlinedienst.class,
        target = de.dataport.suchdienst.jzufi.model.v23.ZustaendigkeitOnlinedienst.class
    )
    @SubclassMapping(
        source = de.xoev.xzufi.v2_3_1.ZustaendigkeitPerson.class,
        target = de.dataport.suchdienst.jzufi.model.v23.ZustaendigkeitPerson.class
    )
    de.dataport.suchdienst.jzufi.model.v23.Zustaendigkeit map23(
        de.xoev.xzufi.v2_3_1.Zustaendigkeit source
    );
}
