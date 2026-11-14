import json
import re
from pathlib import Path
from urllib.parse import urlparse

from .errors import MatcherError

ROOT = Path(__file__).resolve().parent.parent
DEFAULT_CONFIG = ROOT / "config" / "authority-matcher.json"


def read_json(path):
    try:
        return json.loads(Path(path).read_text(encoding="utf-8"))
    except (OSError, ValueError):
        raise MatcherError("invalid_config", "JSON-Datei nicht lesbar oder ungültig.") from None


def load_config(path=None):
    path = Path(path or DEFAULT_CONFIG).resolve()
    data = read_json(path)
    if not isinstance(data, dict):
        raise MatcherError("invalid_config", "Konfiguration muss ein JSON-Objekt sein.")
    url = data.get("pvog_url", "")
    parsed = urlparse(url) if isinstance(url, str) else None
    if not parsed or parsed.scheme not in {"http", "https"} or not parsed.netloc or parsed.username or parsed.query or parsed.fragment:
        raise MatcherError("invalid_config", "pvog_url muss eine HTTP(S)-Basis-URL ohne Zugangsdaten sein.")
    ars = data.get("reference_ars")
    if not isinstance(ars, str) or not re.fullmatch(r"[0-9]{12}", ars):
        raise MatcherError("invalid_config", "reference_ars muss ein zwölfstelliger Gemeinde-ARS sein.")
    for key in ("categories_file", "snapshot_file"):
        value = data.get(key)
        if not isinstance(value, str) or not value:
            raise MatcherError("invalid_config", f"{key} fehlt.")
        data[key] = str((path.parent / value).resolve())
    for key, low, high, default in [("timeout_seconds", 1, 60, 15), ("max_pages", 1, 10, 3),
                                    ("page_size", 5, 100, 50), ("snapshot_max_age_days", 1, 365, 30)]:
        value = data.get(key, default)
        if type(value) is not int or not low <= value <= high:
            raise MatcherError("invalid_config", f"{key} muss zwischen {low} und {high} liegen.")
        data[key] = value
    if type(data.get("allow_snapshot_fallback", False)) is not bool:
        raise MatcherError("invalid_config", "allow_snapshot_fallback muss true oder false sein.")
    if type(data.get("suggest_related_on_no_match", True)) is not bool:
        raise MatcherError("invalid_config", "suggest_related_on_no_match muss true oder false sein.")
    if not isinstance(data.get("reference_region_name", ""), str):
        raise MatcherError("invalid_config", "reference_region_name muss Text sein.")
    return data
