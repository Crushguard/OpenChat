#!/usr/bin/env python3
"""Checks OpenChat's translations against the English source (app/src/main/res/values/strings*.xml).

Usage: python3 tools/i18n/check_translations.py [qualifier ...]   (default: every values-<q> folder listed
in LOCALES below). Exit code 1 when any error is found. Rules (the same ones TranslationCompletenessTest
enforces in CI):
  - every translatable <string>/<plurals> of the English file exists in the same-named file of the locale;
  - no extra keys, no key that English marks translatable="false";
  - no empty value; the same format placeholders (%d, %s, %1$s, …) as English;
  - <plurals> carry every CLDR category the language needs (and "other");
  - apostrophes escaped (\\'), no unescaped leading @ or ?; links (http/https URLs) kept verbatim;
  - warnings (not errors) for values identical to English that look untranslated.
"""
import glob
import os
import re
import sys
import xml.etree.ElementTree as ET

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
RES = os.path.join(ROOT, "app", "src", "main", "res")

# qualifier -> required plural categories (CLDR, as Android's ICU uses them)
LOCALES = {
    "in": {"other"},
    "pt-rBR": {"one", "many", "other"},
    "pt": {"one", "many", "other"},
    "ur": {"one", "other"},
    "hi": {"one", "other"},
    "tr": {"one", "other"},
    "es": {"one", "many", "other"},
    "ar": {"zero", "one", "two", "few", "many", "other"},
    "fa": {"one", "other"},
    "ps": {"one", "other"},
    "iw": {"one", "two", "other"},
    "fr": {"one", "many", "other"},
    "de": {"one", "other"},
    "it": {"one", "many", "other"},
    "ru": {"one", "few", "many", "other"},
    "zh": {"other"},
    "ha": {"one", "other"},
    "my": {"other"},
}
ALLOWED_EXTRA = {"iw": {"many"}}  # older CLDR had "many" for Hebrew; harmless
PLACEHOLDER = re.compile(r"%(?:(\d+)\$)?[-#+ 0,(]*\d*(?:\.\d+)?([sdfxXcb%])")
URL = re.compile(r"https?://[^\s<>\"']+")
BRANDS = {"OpenChat", "WhatsApp", "WhatsApp Business", "WhatsApp Web", "Telegram", "Google Play", "Android"}


def placeholders(text):
    found, index = [], 0
    for m in PLACEHOLDER.finditer(text):
        position, conversion = m.group(1), m.group(2)
        if conversion == "%":
            continue
        index += 1
        found.append((int(position) if position else index, conversion))
    return sorted(found)


def raw_values(path):
    """name -> raw inner XML of each <string>, and name -> {quantity: raw} for <plurals>."""
    text = open(path, encoding="utf-8").read()
    strings = {m.group(1): m.group(3) for m in re.finditer(r'<string\s+name="([^"]+)"([^>]*)>(.*?)</string>', text, re.S)}
    plurals = {}
    for m in re.finditer(r'<plurals\s+name="([^"]+)"[^>]*>(.*?)</plurals>', text, re.S):
        plurals[m.group(1)] = {q.group(1): q.group(2) for q in re.finditer(r'<item\s+quantity="([^"]+)"\s*>(.*?)</item>', m.group(2), re.S)}
    return strings, plurals


def parse(path):
    tree = ET.parse(path)  # raises on malformed XML
    strings, plurals, untranslatable = {}, {}, set()
    for el in tree.getroot():
        name = el.get("name")
        if el.get("translatable") == "false":
            untranslatable.add(name)
            continue
        if el.tag == "string":
            strings[name] = "".join(el.itertext())
        elif el.tag == "plurals":
            plurals[name] = {item.get("quantity"): "".join(item.itertext()) for item in el.findall("item")}
    return strings, plurals, untranslatable


def check_raw(label, raw, errors):
    unescaped = re.search(r"(?<!\\)'", raw)
    if unescaped and not (raw.startswith('"') and raw.endswith('"')):
        errors.append(f"{label}: unescaped apostrophe (use \\')")
    stripped = raw.strip()
    if stripped.startswith("@") or stripped.startswith("?"):
        errors.append(f"{label}: starts with @ or ? (escape it)")


def main(argv):
    qualifiers = argv or list(LOCALES)
    sources = sorted(glob.glob(os.path.join(RES, "values", "strings*.xml")))
    source = {}
    for path in sources:
        source[os.path.basename(path)] = parse(path)
    all_untranslatable = set().union(*(s[2] for s in source.values()))
    total_errors = 0
    for q in qualifiers:
        errors, warnings = [], []
        folder = os.path.join(RES, f"values-{q}")
        if not os.path.isdir(folder):
            print(f"[{q}] MISSING folder values-{q}")
            total_errors += 1
            continue
        required = LOCALES.get(q, {"other"})
        for name, (en_strings, en_plurals, _) in source.items():
            path = os.path.join(folder, name)
            if not en_strings and not en_plurals:
                continue
            if not os.path.exists(path):
                errors.append(f"{name}: file missing")
                continue
            try:
                strings, plurals, _ = parse(path)
            except ET.ParseError as e:
                errors.append(f"{name}: malformed XML: {e}")
                continue
            raw_strings, raw_plurals = raw_values(path)
            for key in sorted(set(strings) | set(plurals)):
                if key in all_untranslatable:
                    errors.append(f"{name}/{key}: English marks it translatable=\"false\" — remove it")
            for key, en in en_strings.items():
                if key not in strings:
                    errors.append(f"{name}/{key}: missing")
                    continue
                value = strings[key]
                if not value.strip():
                    errors.append(f"{name}/{key}: empty")
                if placeholders(value) != placeholders(en):
                    errors.append(f"{name}/{key}: placeholders {placeholders(value)} != English {placeholders(en)}")
                if sorted(URL.findall(value)) != sorted(URL.findall(en)):
                    errors.append(f"{name}/{key}: links {URL.findall(value)} != English {URL.findall(en)} (keep them verbatim)")
                check_raw(f"{name}/{key}", raw_strings.get(key, ""), errors)
                if value == en and len(en) > 12 and re.search(r"[A-Za-z]{4}", en) and en not in BRANDS:
                    warnings.append(f"{name}/{key}: identical to English: {en!r}")
            for key in strings:
                if key not in en_strings and key not in all_untranslatable:
                    errors.append(f"{name}/{key}: extra key (not in English)")
            for key, en_items in en_plurals.items():
                if key not in plurals:
                    errors.append(f"{name}/{key}: plural missing")
                    continue
                items = plurals[key]
                missing = required - set(items)
                if missing:
                    errors.append(f"{name}/{key}: plural categories missing {sorted(missing)} (need {sorted(required)})")
                extra = set(items) - required - ALLOWED_EXTRA.get(q, set())
                if extra:
                    warnings.append(f"{name}/{key}: categories not used by this language {sorted(extra)}")
                en_other = placeholders(en_items.get("other", ""))
                for quantity, value in items.items():
                    ph = placeholders(value)
                    if ph != en_other and not (quantity in ("zero", "one", "two") and set(ph) <= set(en_other)):
                        errors.append(f"{name}/{key}[{quantity}]: placeholders {ph} != English other {en_other}")
                    if not value.strip():
                        errors.append(f"{name}/{key}[{quantity}]: empty")
                    check_raw(f"{name}/{key}[{quantity}]", raw_plurals.get(key, {}).get(quantity, ""), errors)
            for key in plurals:
                if key not in en_plurals and key not in all_untranslatable:
                    errors.append(f"{name}/{key}: extra plural (not in English)")
        status = "OK" if not errors else f"{len(errors)} error(s)"
        print(f"[{q}] {status}, {len(warnings)} warning(s)")
        for e in errors:
            print(f"  ERROR {e}")
        for w in warnings:
            print(f"  warn  {w}")
        total_errors += len(errors)
    return 1 if total_errors else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
