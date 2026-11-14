"""Lokal durchsuchbare Basis + optionaler echter Elasticsearch-Index."""
from __future__ import annotations
import re
import json
import os
from pathlib import Path
from typing import Iterable
from functools import lru_cache

from .catalog import AuthorityTaxonomy, normalize_record


def load_catalog(path: Path, taxonomy: AuthorityTaxonomy, *, demo: bool = False) -> list[dict]:
    records: dict[str, dict] = {}
    with path.open(encoding="utf-8") as f:
        for number, line in enumerate(f, 1):
            if not line.strip():
                continue
            raw = json.loads(line)
            if "mapping_status" in raw and "titel" in raw and "behoerdenkategorien" in raw:
                record = raw
            else:
                record = normalize_record(raw, taxonomy, origin="demo" if demo or raw.get("origin") == "demo" else "fim")
            if record:
                # Stability: merge duplicate IDs, do not silently make inconsistent
                # assignments appear as a single certain authority.
                if record["id"] in records:
                    prior = records[record["id"]]
                    for cat in record["behoerdenkategorien"]:
                        if cat not in prior["behoerdenkategorien"]:
                            prior["behoerdenkategorien"].append(cat)
                    prior["synonyme"] = sorted(set(prior["synonyme"] + record["synonyme"]))
                    if len(prior["behoerdenkategorien"]) > 1 and prior["quelle"] != "demo":
                        prior["mapping_status"] = "mehrdeutig"
                else:
                    records[record["id"]] = record
    # Der FIM-Leistungssteckbrief enthaelt oft keinen zustaendigen Behördentyp.
    # Nur wenn ein anderer *amtlicher FIM-Stammtext mit identischem LeiKa-Schlüssel*
    # eine explizite Stelle nennt, übernehmen wir die belegten Alternativen.
    by_key: dict[str, set[str]] = {}
    original_fields: dict[str, set[str]] = {}
    for doc in records.values():
        key = doc.get("leistungsschluessel")
        if key and doc.get("quelle", "").startswith("fim:") and doc["behoerdenkategorien"]:
            by_key.setdefault(key, set()).update(doc["behoerdenkategorien"])
            original_fields.setdefault(key, set()).update(doc.get("zustaendige_stelle_original", []))
    for doc in records.values():
        key = doc.get("leistungsschluessel")
        if key and not doc["behoerdenkategorien"] and doc.get("quelle", "").startswith("fim:") and by_key.get(key):
            doc["behoerdenkategorien"] = sorted(by_key[key])
            doc["zustaendige_stelle_original"] = sorted(original_fields.get(key, set()))
            doc["mapping_status"] = ("aus_anderem_fim_stammtext" if len(by_key[key]) == 1
                                      else "mehrdeutig_aus_fim_stammtexten")
    return list(records.values())


def document_text(doc: dict) -> str:
    return "\n".join([doc["titel"]] * 3 + doc.get("synonyme", []) * 2 + [doc.get("beschreibung", "")])


class Embedder:
    """Multilingual E5 (oder ein anderes sentence-transformers Modell)."""
    def __init__(self, model_name: str):
        try:
            from sentence_transformers import SentenceTransformer
        except ImportError as exc:
            raise RuntimeError("Bitte pip install -e '.[semantic]' ausführen") from exc
        self.model_name = model_name
        self.model = SentenceTransformer(model_name)

    def encode_docs(self, texts: list[str]):
        prefix = "passage: " if "e5" in self.model_name.lower() else ""
        return self.model.encode([prefix + t for t in texts], normalize_embeddings=True,
                                 batch_size=16, show_progress_bar=False).tolist()

    def encode_query(self, text: str):
        prefix = "query: " if "e5" in self.model_name.lower() else ""
        return self.model.encode([prefix + text], normalize_embeddings=True).tolist()[0]


@lru_cache(maxsize=2)
def cached_embedder(model_name: str) -> Embedder:
    return Embedder(model_name)

def es_client(url: str):
    try:
        from elasticsearch import Elasticsearch
    except ImportError as exc:
        raise RuntimeError("Bitte pip install -e '.[elastic]' ausführen") from exc
    key = os.environ.get("ELASTICSEARCH_API_KEY")
    return Elasticsearch(url, api_key=key, request_timeout=60) if key else Elasticsearch(url, request_timeout=60)


def index_elastic(docs: list[dict], *, url: str, index_name: str, embeddings: bool,
                  model_name: str = "intfloat/multilingual-e5-small") -> dict:
    client = es_client(url)
    model = cached_embedder(model_name) if embeddings else None
    source_docs = docs
    if not source_docs:
        raise ValueError("Keine FIM-Leistungen vorhanden; Import prüfen")
    dimensions = len(model.encode_query('Beispiel')) if model else 0
    mapping = {
        'properties': {
            'titel': {'type':'text','analyzer':'german'},
            'beschreibung': {'type':'text','analyzer':'german'},
            'synonyme': {'type':'text','analyzer':'german'},
            'id': {'type':'keyword'},
            'leistungsschluessel': {'type':'keyword'},
            'behoerdenkategorien': {'type':'keyword'},
            'mapping_status': {'type':'keyword'},
            'quelle': {'type':'keyword'},
            'zustaendige_stelle_original': {'type':'keyword','ignore_above':1024},
            'zuordnung_belege': {'type':'object','enabled':False},
        }
    }
    if model:
        mapping['properties']['embedding'] = {
            'type':'dense_vector','dims':dimensions,'index':True,'similarity':'cosine'
        }
    if client.indices.exists(index=index_name):
        client.indices.delete(index=index_name)
    client.indices.create(index=index_name, mappings=mapping)
    from elasticsearch.helpers import streaming_bulk

    def actions():
        # Encode in chunks: large FIM catalogs cannot fit all embedding vectors
        # and full texts in memory twice.
        for start in range(0,len(source_docs),64):
            batch=source_docs[start:start+64]
            vectors = model.encode_docs([document_text(d) for d in batch]) if model else None
            for i, doc in enumerate(batch):
                obj=dict(doc)
                if vectors is not None:
                    obj['embedding']=vectors[i]
                yield {'_index':index_name,'_id':obj['id'],'_source':obj}

    failures=[]
    imported=0
    for ok, result in streaming_bulk(client, actions(), raise_on_error=False,
                                     chunk_size=64, max_retries=2):
        if ok:
            imported += 1
        elif len(failures)<5:
            failures.append(result)
    client.indices.refresh(index=index_name)
    if failures:
        raise RuntimeError(f'Elasticsearch: Importfehler, Beispiele: {str(failures)[:800]}')
    if imported != len(source_docs):
        raise RuntimeError(f'Elasticsearch unvollständig: {imported} von {len(source_docs)}')
    return {'indexed':imported,
            'with_category':sum(bool(d.get('behoerdenkategorien')) for d in source_docs),
            'embedding':model_name if model else None}


LEXICAL_FIELDS = ["titel^4", "synonyme^3", "beschreibung"]
FUZZY_BOOST = 0.3
FUZZINESS = "AUTO:5,100"   # Tippfehler-Toleranz der Wortsuche (Länge ab der 1 bzw. 2 Zeichen Abstand gelten)
RRF_K = 60                  # Glättung der Rangfusion: 1/(RRF_K + Rang)
# Gewicht der BM25-Treffer in der Rangfusion (RRF), wenn die semantische Suche mitläuft. Die Wortsuche kennt keine
# Wortfamilien (scheiden/Scheidung, geschenkt/Schenkung) und bleibt bei Fließtext an Allerweltswörtern hängen
# (melden, Frauen); die Vektorsuche trifft dort. Ein reiner BM25-Treffer auf Rang 1 liegt mit 0,8 knapp über der
# Mindestschwelle des Matchers (0.013), Rang 2 und tiefer fallen ohne Bestätigung durch die Vektorsuche heraus.
LEXICAL_WEIGHT = 0.8


def lexical_body(query: str, size: int) -> dict:
    """BM25-Anfrage: exakte Treffer zählen voll, Tippfehler-Treffer nur abgeschwächt.

    Früher lief ``fuzziness: AUTO`` über alle Felder (bis zu 2 Zeichen Abstand ab 6 Zeichen
    Wortlänge). Damit traf z. B. "Schenkung" auch "Scheidung" (2 Zeichen Abstand) mit vollem
    Gewicht. Jetzt: höchstens 1 Zeichen Abstand, erst ab 5 Zeichen Wortlänge, die ersten
    2 Zeichen müssen stimmen, und der Treffer wird mit FUZZY_BOOST abgeschwächt.
    """
    should = [{"multi_match": {"query": query, "fields": LEXICAL_FIELDS, "type": "best_fields"}}]
    if FUZZY_BOOST > 0 and FUZZINESS:
        should.append({"multi_match": {"query": query, "fields": LEXICAL_FIELDS, "type": "best_fields",
                                       "fuzziness": FUZZINESS, "prefix_length": 2, "boost": FUZZY_BOOST}})
    return {"size": size, "query": {"bool": {"should": should, "minimum_should_match": 1}}}


# Füllwörter einer Bürgeranfrage, die in Leistungstiteln nicht vorkommen und die Wortsuche nur verwässern
# ("Ich möchte ... wo kann ich ..."). Nur für die Wortsuche; die Vektorsuche bekommt den Originaltext.
QUERY_CLEAN = False
_FILLER = frozenset("""ich mich mir mein meine meinen meinem meiner wir uns unser unsere du dich dir möchte möchten mochte
will wollen würde würden muss müssen kann können darf dürfen soll sollen brauche brauchen bitte gerne hallo hi wo wie
was wann wer wen wem welche welcher welches warum wieso ob dass tun machen mache gemacht habe hast hat haben bin ist sind
war waren wird werden wurde wurden jemand etwas irgendwo irgendwie eigentlich gerade jetzt schon noch auch nur mal einfach
denn halt also bekommen bekomme kriege kriegen geht gehen""".split())


def clean_query(query: str) -> str:
    """Füllwörter für die Wortsuche entfernen; bleibt nichts übrig, den Originaltext verwenden."""
    if not QUERY_CLEAN:
        return query
    words = [w for w in re.findall(r"[\wÄÖÜäöüß\-]+", query) if w.lower() not in _FILLER]
    return " ".join(words) if words else query


def _lexical_hits(client, index_name: str, query: str, size: int) -> list[dict]:
    return client.search(index=index_name, body=lexical_body(clean_query(query), size))["hits"]["hits"]


def _knn_hits(client, index_name: str, query: str, size: int, model_name: str) -> list[dict]:
    embedding = cached_embedder(model_name).encode_query(query)
    return client.search(index=index_name, knn={"field": "embedding", "query_vector": embedding, "k": size,
                                                  "num_candidates": max(100, size * 3)}, size=size)["hits"]["hits"]


def fuse(lexical: list[dict], knn: list[dict] | None) -> list[tuple[dict, float]]:
    """Rangfusion (RRF) von Wort- und Vektorsuche; BM25-Ränge zählen mit LEXICAL_WEIGHT, wenn beide laufen."""
    ranks = {}
    for i, hit in enumerate(lexical):
        ranks[hit["_id"]] = [hit["_source"], (LEXICAL_WEIGHT if knn is not None else 1.0) / (RRF_K + i + 1)]
    for i, hit in enumerate(knn or []):
        if hit["_id"] not in ranks:
            ranks[hit["_id"]] = [hit["_source"], 0.0]
        ranks[hit["_id"]][1] += 1.0 / (RRF_K + i + 1)
    return sorted(((row[0], row[1]) for row in ranks.values()), key=lambda x: x[1], reverse=True)


def es_search(query: str, *, url: str, index_name: str, semantic: bool, model_name: str,
              size: int = 12) -> list[tuple[dict, float]]:
    client = es_client(url)
    lexical = _lexical_hits(client, index_name, query, size)
    knn = _knn_hits(client, index_name, query, size, model_name) if semantic else None
    return fuse(lexical, knn)
