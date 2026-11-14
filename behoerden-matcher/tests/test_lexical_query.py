"""Lexikalische Anfrage: Tippfehlertoleranz darf Wörter wie Schenkung/Scheidung nicht gleichsetzen."""
from authority_matcher.index import FUZZY_BOOST, lexical_body


def levenshtein(a, b):
    prev = list(range(len(b) + 1))
    for i, ca in enumerate(a, 1):
        cur = [i]
        for j, cb in enumerate(b, 1):
            cur.append(min(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + (ca != cb)))
        prev = cur
    return prev[-1]


def test_exact_clause_has_no_fuzziness_and_fuzzy_clause_is_weakened():
    body = lexical_body("Schenkung melden", 40)
    exact, fuzzy = [c["multi_match"] for c in body["query"]["bool"]["should"]]
    assert "fuzziness" not in exact and "boost" not in exact
    assert fuzzy["boost"] == FUZZY_BOOST < 1
    assert fuzzy["prefix_length"] >= 2
    assert body["size"] == 40 and body["query"]["bool"]["minimum_should_match"] == 1


def test_fuzziness_allows_at_most_one_edit():
    fuzzy = lexical_body("x", 1)["query"]["bool"]["should"][1]["multi_match"]
    low, high = (int(v) for v in fuzzy["fuzziness"].removeprefix("AUTO:").split(","))
    assert fuzzy["fuzziness"].startswith("AUTO:")
    assert high > 50                                  # nie 2 Zeichen Abstand
    # Das Problemwort: 2 Zeichen Abstand -> darf nicht mehr als Tippfehler gelten
    assert levenshtein("schenkung", "scheidung") == 2
    max_edits = 0 if len("schenkung") < low else 1 if len("schenkung") < high else 2
    assert max_edits < levenshtein("schenkung", "scheidung")


def test_semantic_hit_outranks_lone_lexical_hit(monkeypatch):
    """'Scheidung': Wortsuche liefert 'Gewalt gegen Frauen' (nur 'Frau' passt) auf Rang 1, die Vektorsuche 'Scheidungsantrag'."""
    from authority_matcher import index

    def hit(i, title):
        return {"_id": i, "_source": {"id": i, "titel": title}}

    class FakeClient:
        def search(self, index, body=None, knn=None, size=None):
            if knn is None:
                return {"hits": {"hits": [hit("a", "Gewalt gegen Frauen"), hit("c", "Altersrente für Frauen")]}}
            return {"hits": {"hits": [hit("b", "Scheidungsantrag Entscheidung")]}}

    class FakeEmbedder:
        def encode_query(self, text):
            return [0.0]

    monkeypatch.setattr(index, "es_client", lambda url: FakeClient())
    monkeypatch.setattr(index, "cached_embedder", lambda name: FakeEmbedder())
    rows = index.es_search("scheiden Frau", url="x", index_name="i", semantic=True, model_name="m")
    assert [d["id"] for d, _ in rows][:2] == ["b", "a"]
    assert rows[1][1] >= 0.013 > rows[2][1]            # Rang 1 nur lexikalisch bleibt knapp über der Schwelle, Rang 2 nicht
    lexical_only = index.es_search("scheiden Frau", url="x", index_name="i", semantic=False, model_name="m")
    assert lexical_only[0][1] == 1 / 61                 # ohne Vektorsuche unverändert
