#!/usr/bin/env python3
"""
Génère les données de la démo interactive du portfolio.

Pour chaque programme de démonstration (.canAda), exécute le compilateur dans
ses différents modes et capture les 4 phases :
  1. Lexer        (-t)        -> liste de tokens typés
  2. Parser       (-json)     -> AST en JSON
  3. Sémantique   (défaut)    -> tables des symboles
  4. Génération   (défaut)    -> assembleur (out.s)

Produit un fichier JSON par démo + un manifest dans
portfolio/public/compiler-demo/.

Prérequis : avoir lancé `./gradlew shadowJar` au préalable (le script le fait
si l'artefact est absent).

Usage : python3 scripts/generate-demo-data.py
"""
import json
import os
import re
import subprocess
import sys
import tempfile

# --- Chemins -----------------------------------------------------------------
REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
JAVA_HOME = os.environ.get(
    "JAVA_HOME",
    "/Library/Java/JavaVirtualMachines/amazon-corretto-17.jdk/Contents/Home",
)
JAVA = os.path.join(JAVA_HOME, "bin", "java")
RES = os.path.join(REPO, "build", "resources", "main")
JAR = os.path.join(REPO, "build", "libs", "compilerminiada-1.0-SNAPSHOT-all.jar")
DEMO_DIR = os.path.join(RES, "demo")
# Sortie dans le portfolio voisin
OUT_DIR = os.path.join(
    os.path.dirname(REPO), "portfolio", "public", "compiler-demo"
)

# --- Catalogue des démos (ordre + libellés fr/en) ----------------------------
DEMOS = [
    {
        "name": "factorial",
        "title": {"fr": "Factorielle récursive", "en": "Recursive factorial"},
        "description": {
            "fr": "Calcul récursif de 5! — la récursivité se propage du source jusqu'à la pile ASM.",
            "en": "Recursive computation of 5! — recursion flows from source down to the ASM stack.",
        },
    },
    {
        "name": "sumFirstIntegers",
        "title": {"fr": "Somme des N premiers entiers", "en": "Sum of first N integers"},
        "description": {
            "fr": "Boucle accumulant la somme des N premiers entiers.",
            "en": "Loop accumulating the sum of the first N integers.",
        },
    },
    {
        "name": "multiplicationTable",
        "title": {"fr": "Table de multiplication", "en": "Multiplication table"},
        "description": {
            "fr": "Boucles imbriquées affichant la table de multiplication.",
            "en": "Nested loops printing the multiplication table.",
        },
    },
    {
        "name": "tictactoe",
        "title": {"fr": "Morpion", "en": "Tic-tac-toe"},
        "description": {
            "fr": "Jeu de morpion — records, conditions et procédures.",
            "en": "Tic-tac-toe game — records, conditions and procedures.",
        },
    },
    {
        "name": "combatSimulator",
        "title": {"fr": "Simulateur de combat", "en": "Combat simulator"},
        "description": {
            "fr": "Combat RPG entre deux personnages modélisés par des records.",
            "en": "RPG combat between two characters modelled with records.",
        },
    },
]

ANSI = re.compile(r"\x1b\[[0-9;]*m")
# Les valeurs d'enum (mode, opérateurs...) sortent sans guillemets -> JSON invalide.
# On les requote (tout en MAJUSCULES en position de valeur).
BARE_ENUM = re.compile(r": ([A-Z][A-Z_]*)(,?)[ \t]*$", re.MULTILINE)
TOKEN_RE = re.compile(r"<([^,]+), (\d+), (.*)>\s*$")


def run(args, cwd=None):
    """Lance le compilateur et renvoie stdout (décodé, ANSI retiré)."""
    proc = subprocess.run(
        [JAVA, "-cp", f"{RES}:{JAR}", "org.trad.pcl.Main", *args],
        cwd=cwd,
        capture_output=True,
        text=True,
        timeout=120,
    )
    return ANSI.sub("", proc.stdout + proc.stderr)


def parse_tokens(name):
    out = run([f"/demo/{name}.canAda", "-t"])
    tokens = []
    for line in out.splitlines():
        m = TOKEN_RE.match(line.strip())
        if m:
            tokens.append(
                {"type": m.group(1).strip(), "line": int(m.group(2)), "value": m.group(3)}
            )
    return tokens


def parse_ast(name):
    out = run([f"/demo/{name}.canAda", "-json"])
    idx = out.find("@@AST_JSON@@")
    if idx == -1:
        raise RuntimeError(f"Pas de JSON AST pour {name}")
    raw = out[idx + len("@@AST_JSON@@"):]
    raw = BARE_ENUM.sub(r': "\1"\2', raw)
    return json.loads(raw)


def parse_symbols(stdout):
    """Extrait les tables des symboles du stdout du mode complet."""
    scopes = []
    current = None
    entry_re = re.compile(r"^\s+(\S+) -> (\w+) \{ (.*) \}\s*$")
    scope_re = re.compile(r"SymbolTable for (\S+)")
    for line in stdout.splitlines():
        ms = scope_re.search(line)
        if "Entering new scope" in line and ms:
            current = {"scope": ms.group(1), "entries": []}
            scopes.append(current)
            continue
        me = entry_re.match(line)
        if me and current is not None:
            current["entries"].append(
                {"name": me.group(1), "kind": me.group(2), "detail": me.group(3)}
            )
    return scopes


def full_run(name):
    """Mode complet depuis un tmpdir (sans pcl.jar -> pas de VM) : asm + symboles."""
    with tempfile.TemporaryDirectory() as work:
        out = run([f"/demo/{name}.canAda"], cwd=work)
        asm_path = os.path.join(work, "out.s")
        asm = ""
        if os.path.exists(asm_path):
            with open(asm_path) as f:
                asm = f.read().rstrip("\n")
    return asm, parse_symbols(out)


def main():
    if not os.path.exists(JAR):
        print("⚠️  Fat jar absent — lance d'abord : ./gradlew shadowJar")
        sys.exit(1)
    os.makedirs(OUT_DIR, exist_ok=True)
    manifest = []
    for demo in DEMOS:
        name = demo["name"]
        print(f"▶ {name} ...", end=" ", flush=True)
        with open(os.path.join(DEMO_DIR, f"{name}.canAda")) as f:
            source = f.read().rstrip("\n")
        tokens = parse_tokens(name)
        ast = parse_ast(name)
        asm, symbols = full_run(name)
        data = {
            "name": name,
            "title": demo["title"],
            "description": demo["description"],
            "source": source,
            "tokens": tokens,
            "ast": ast,
            "symbols": symbols,
            "asm": asm,
        }
        with open(os.path.join(OUT_DIR, f"{name}.json"), "w") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
        manifest.append(
            {"name": name, "title": demo["title"], "description": demo["description"]}
        )
        print(
            f"{len(tokens)} tokens, {len(asm.splitlines())} lignes asm, "
            f"{len(symbols)} scopes ✓"
        )
    with open(os.path.join(OUT_DIR, "manifest.json"), "w") as f:
        json.dump(manifest, f, ensure_ascii=False, indent=2)
    print(f"\n✅ {len(manifest)} démos écrites dans {OUT_DIR}")


if __name__ == "__main__":
    main()
