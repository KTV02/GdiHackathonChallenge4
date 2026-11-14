"""Report category coverage of a known official extract without inventing mappings."""

from .catalog import SnapshotCatalog, records
from .categories import CategoryRegistry
from .config import load_config

ROLE_URI = "urn:xoev-de:fim:codeliste:xzufi.zustaendigkeitsrolle"


def coverage_report(config=None):
    config = config or load_config()
    catalog = SnapshotCatalog(config)
    registry = CategoryRegistry(config["categories_file"])
    missing = {}
    complete, unique, conditional, no_org = 0, 0, 0, 0
    for entry in catalog.items:
        detail = entry["detail"]
        orgs = [o for o in records(detail.get("organisations"), "organisations")
                if o.get("isPublicDisplayAllowed") is not False and isinstance(o.get("role"), dict)
                and o["role"].get("uri") == ROLE_URI and o["role"].get("code") in {"01", "02"}]
        if not orgs:
            no_org += 1
            continue
        all_mapped, codes, has_criteria = True, set(), False
        for org in orgs:
            has_criteria |= bool(records(org.get("criteria"), "organisation.criteria"))
            mapped = registry.for_organisation(org.get("id"), catalog.ars)
            codes.update(c["code"] for c in mapped)
            if not mapped:
                all_mapped = False
                item = missing.setdefault(org["id"], {"id": org["id"], "name": org.get("title", ""),
                                                       "evidence_url": entry["evidence"]["url"], "service_ids": []})
                if detail["id"] not in item["service_ids"]:
                    item["service_ids"].append(detail["id"])
        complete += all_mapped
        conditional += has_criteria
        unique += all_mapped and len(codes) == 1 and not has_criteria
    return {"reference_ars": catalog.ars, "snapshot_retrieved_at": catalog.retrieved_at,
            "scope": "bounded_snapshot_only", "configured_categories": len(registry.categories),
            "service_count": len(catalog.items), "services_with_all_organisations_categorised": complete,
            "services_with_unique_category_and_no_criteria": unique,
            "services_with_additional_criteria": conditional, "services_without_competent_organisation": no_org,
            "unmapped_organisation_count": len(missing),
            "unmapped_organisations": sorted(missing.values(), key=lambda item: item["id"])}
