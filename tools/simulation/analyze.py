"""
Analyzes a simulation run: behaviour funnel from the actions log, cross-checked with the
Analytics events the app actually emitted (captured from logcat).

Usage:
    python analyze.py                 # latest run
    python analyze.py runs/<run-id>   # a specific run
Prints Markdown ready to paste into docs/RESULTADOS.md.
"""
import json
import re
import sys
from collections import Counter, defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parent


def pct(part, total):
    return "—" if total == 0 else f"{part / total * 100:.0f} %"


def table(headers, rows):
    if not rows:
        return "_Sin datos._\n"
    line = lambda cells: "| " + " | ".join(str(c) for c in cells) + " |"
    return "\n".join([line(headers), line(["---"] * len(headers)), *map(line, rows)]) + "\n"


def load(run_dir: Path):
    actions = [json.loads(l) for l in (run_dir / "actions.jsonl").read_text(encoding="utf-8").splitlines() if l]
    summary = json.loads((run_dir / "summary.json").read_text(encoding="utf-8"))
    events = Counter()
    # Full logcat capture, or the events-only extract that is versioned in Git.
    analytics_log = next((run_dir / n for n in ("analytics.log", "analytics-events.log") if (run_dir / n).exists()),
                         None)
    if analytics_log:
        for match in re.finditer(r"Logging event: origin=app,name=([a-z_]+)",
                                 analytics_log.read_text(encoding="utf-8", errors="replace")):
            events[match.group(1)] += 1
    return actions, summary, events


def main():
    run_dir = Path(sys.argv[1]) if len(sys.argv) > 1 else max((ROOT / "runs").iterdir())
    actions, summary, events = load(run_dir)
    users = [u for u in summary["users"] if u["ok"]]
    failed = [u for u in summary["users"] if not u["ok"]]
    by_user = defaultdict(list)
    for a in actions:
        by_user[a["user"]].append(a)

    def did(user_id, action):
        return any(a["action"] == action for a in by_user[user_id])

    ids = [u["id"] for u in users]
    registered = len(ids)
    explored = [u for u in ids if did(u, "ver_categoria") or did(u, "buscar")]
    viewed_provider = [u for u in ids if did(u, "ver_prestador")]
    requested = [u for u in ids if did(u, "solicitar_contacto")]

    out = [f"# Resultados de la simulación `{run_dir.name}`\n"]
    out.append(f"- Usuarios simulados: **{len(summary['users'])}** ({len(failed)} con error de automatización, excluidos)")
    out.append(f"- Semilla: `{summary['seed']}` (la corrida es reproducible)")
    out.append(f"- Inicio: {summary['started'][:16].replace('T', ' ')} · Fin: {summary['finished'][:16].replace('T', ' ')}")
    durations = [u["seconds"] for u in users]
    if durations:
        out.append(f"- Duración promedio de una sesión: {sum(durations) / len(durations):.0f} s")
    out.append("")

    out.append("## Perfiles simulados\n")
    personas = Counter(u["persona"] for u in users)
    out.append(table(["Perfil", "Usuarios", "Pidieron contacto"], [
        [p, n, sum(1 for u in users if u["persona"] == p and u["id"] in requested)]
        for p, n in personas.most_common()
    ]))

    out.append("## Embudo (por usuario)\n")
    steps = [("Se registró", registered), ("Exploró (categoría o búsqueda)", len(explored)),
             ("Abrió un prestador", len(viewed_provider)), ("Pidió contacto", len(requested))]
    rows, previous = [], None
    for name, value in steps:
        rows.append([name, value, pct(value, registered), "—" if previous is None else pct(value, previous)])
        previous = value
    out.append(table(["Paso", "Usuarios", "% del total", "% del paso anterior"], rows))

    out.append("## Dónde abandonaron los que no pidieron contacto\n")
    exits = Counter(a["donde"] for a in actions if a["action"] == "abandona" and a["user"] in ids)
    labels = {"inicio": "En la pantalla de inicio, sin explorar", "busqueda_sin_resultados": "Tras una búsqueda sin resultados",
              "sin_decidirse": "Después de explorar, sin decidirse", "perfil_prestador": "En el perfil de un prestador"}
    out.append(table(["Momento", "Usuarios"], [[labels.get(k, k), v] for k, v in exits.most_common()]))

    out.append("## Búsquedas\n")
    searches = [a for a in actions if a["action"] == "buscar" and a["user"] in ids]
    empty = [s for s in searches if not s["con_resultados"]]
    out.append(f"- Búsquedas: **{len(searches)}**, sin resultados: **{len(empty)}** ({pct(len(empty), len(searches))})\n")
    out.append(table(["Término", "Veces", "¿Encontró algo?"], [
        [term, n, "sí" if any(s["con_resultados"] for s in searches if s["termino"] == term) else "**no**"]
        for term, n in Counter(s["termino"] for s in searches).most_common()
    ]))

    out.append("## Solicitudes\n")
    requests = [a for a in actions if a["action"] == "solicitar_contacto" and a["user"] in ids]
    with_msg = sum(1 for r in requests if r["con_mensaje"])
    checked = sum(1 for u in requested if did(u, "ver_mis_solicitudes"))
    out.append(f"- Solicitudes enviadas: **{len(requests)}** · con mensaje: {with_msg} ({pct(with_msg, len(requests))})")
    out.append(f"- Usuarios que después revisaron *Mis solicitudes*: {checked} de {len(requested)}\n")
    out.append(table(["Prestador", "Solicitudes"], Counter(r["prestador"] for r in requests).most_common()))

    out.append("## Control: eventos que emitió la app (Analytics, capturados del dispositivo)\n")
    expected = {
        "category_view": sum(1 for a in actions if a["action"] == "ver_categoria" and a["user"] in ids),
        "provider_view": sum(1 for a in actions if a["action"] == "ver_prestador" and a["user"] in ids),
        "contact_request": len(requests),
        "service_search": len(searches),
    }
    out.append(table(["Evento", "Acciones simuladas", "Eventos emitidos por la app", "¿Coincide?"], [
        [name, n, events.get(name, 0), "✅" if events.get(name, 0) == n else "⚠️"] for name, n in expected.items()
    ]))
    if failed:
        out.append("## Usuarios con error de automatización\n")
        out.append(table(["Usuario", "Error"], [[u["id"], u.get("error", "")] for u in failed]))

    print("\n".join(out))


if __name__ == "__main__":
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
    main()
