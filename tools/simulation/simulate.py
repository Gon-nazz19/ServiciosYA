"""
Simulated user test for the TP2 (Measure step).

Drives the real app on an Android emulator: every simulated user starts from a clean install
(new Firebase/Analytics instance), registers, and behaves according to a persona. All data in
Firestore and every Analytics event is produced by the app itself; the script only taps.

Usage:
    python simulate.py --users 20 --seed 42
    python simulate.py --users 1 --verbose        # quick smoke run

Output: runs/<run-id>/actions.jsonl (what each user did), runs/<run-id>/analytics.log
(events emitted by the app, captured from logcat) and runs/<run-id>/summary.json.
"""
import argparse
import json
import os
import random
import re
import subprocess
import sys
import threading
import time
from datetime import datetime
from pathlib import Path

from device import ADB, Device, DeviceError

ROOT = Path(__file__).resolve().parent
SEED_DATA = ROOT.parent / "seed" / "seed-data.json"
PASSWORD = "Simulacion123"

FIRST_NAMES = [
    "Lucia", "Martin", "Sofia", "Juan", "Valentina", "Mateo", "Camila", "Santiago", "Julieta", "Tomas",
    "Agustina", "Nicolas", "Florencia", "Facundo", "Micaela", "Joaquin", "Carolina", "Franco", "Paula",
    "Ignacio", "Rocio", "Lautaro", "Milagros", "Gonzalo", "Antonella",
]
LAST_NAMES = ["Gomez", "Fernandez", "Rodriguez", "Lopez", "Martinez", "Perez", "Sosa", "Diaz", "Romero", "Alvarez"]

# How often each need appears (a household in a small city).
NEED_WEIGHTS = {
    "plomeros": 30, "electricistas": 28, "aire-acondicionado": 18, "tecnicos-pc": 16, "jardineros": 8,
}

# What people type when they use the search box.
SEARCH_TERMS = {
    "plomeros": ["plom", "plomero", "perdida"],
    "electricistas": ["electric", "luz", "electricista"],
    "aire-acondicionado": ["aire", "split", "aire acondicionado"],
    "tecnicos-pc": ["pc", "computadora", "tecnico"],
    "jardineros": ["jardin", "pasto", "poda"],
}
# Services that are NOT in the catalog (to measure searches without results).
MISSING_SERVICES = ["cerrajero", "gasista", "pintor", "albanil", "herrero", "fletes"]

MESSAGES = {
    "plomeros": ["Hola, tengo una perdida de agua en la cocina", "Necesito destapar el bano, es urgente",
                 "Quiero cambiar la canilla del lavadero"],
    "electricistas": ["Se me corto la luz en media casa", "Necesito instalar dos enchufes nuevos",
                      "Salta la termica cuando prendo el horno"],
    "aire-acondicionado": ["El aire no enfria, creo que le falta gas", "Quiero instalar un split en el dormitorio"],
    "tecnicos-pc": ["La notebook no prende", "Necesito formatear la PC", "La compu anda muy lenta"],
    "jardineros": ["Necesito cortar el pasto del patio", "Quiero podar dos arboles"],
}

# Persona: (weight, description). Behaviour is implemented in run_persona().
PERSONAS = {
    "decidido": (30, "Entra a la categoria que necesita y pide contacto al primer prestador"),
    "comparador": (22, "Compara 2 o 3 prestadores y pide contacto al de mejor rating"),
    "buscador": (20, "Usa el buscador; a veces busca servicios que no existen"),
    "explorador": (15, "Mira categorias y prestadores pero no pide contacto"),
    "rebote": (13, "Se registra, mira la pantalla principal y se va"),
}


def load_catalog():
    data = json.loads(SEED_DATA.read_text(encoding="utf-8"))
    categories = {c["id"]: c["name"] for c in data["categories"]}
    providers = {}
    for p in data["providers"]:
        providers.setdefault(p["categoryId"], []).append(p)
    # Same order as the app: highest rating first.
    for items in providers.values():
        items.sort(key=lambda p: p["rating"] or 0, reverse=True)
    return categories, providers


class AnalyticsCapture:
    """Captures the Analytics events the app logs (FA verbose logging) while the simulation runs."""

    def __init__(self, path: Path):
        self.path = path
        self.process = None

    def start(self):
        for tag in ("FA", "FA-SVC"):
            subprocess.run([ADB, "shell", "setprop", f"log.tag.{tag}", "VERBOSE"], capture_output=True)
        subprocess.run([ADB, "shell", "setprop", "debug.firebase.analytics.app", "com.example.serviciosya"],
                       capture_output=True)
        subprocess.run([ADB, "logcat", "-c"], capture_output=True)
        self.file = open(self.path, "w", encoding="utf-8")
        self.process = subprocess.Popen(
            [ADB, "logcat", "-v", "time", "FA:V", "FA-SVC:V", "*:S"],
            stdout=self.file, stderr=subprocess.DEVNULL,
        )

    def stop(self):
        if self.process:
            time.sleep(3)  # let the last events flush
            self.process.terminate()
            self.process.wait(timeout=10)
            self.file.close()


class Simulation:
    def __init__(self, args):
        self.args = args
        self.rng = random.Random(args.seed)
        self.device = Device(verbose=args.verbose)
        self.categories, self.providers = load_catalog()
        self.run_id = datetime.now().strftime("%Y%m%d-%H%M%S")
        self.out_dir = ROOT / "runs" / self.run_id
        self.out_dir.mkdir(parents=True, exist_ok=True)
        self.actions_file = open(self.out_dir / "actions.jsonl", "w", encoding="utf-8")
        self.user = None

    # --- logging -----------------------------------------------------------
    def log(self, action, **details):
        entry = {"time": datetime.now().isoformat(timespec="seconds"), "user": self.user["id"], "action": action,
                 **details}
        self.actions_file.write(json.dumps(entry, ensure_ascii=False) + "\n")
        self.actions_file.flush()
        print(f"  [{self.user['id']}] {action} {json.dumps(details, ensure_ascii=False) if details else ''}")

    def pause(self, low=0.6, high=1.8):
        """Human-like thinking time between actions."""
        time.sleep(self.rng.uniform(low, high) * self.args.speed)

    # --- building blocks ---------------------------------------------------
    def register(self):
        d = self.device
        d.reset_app()
        d.launch()
        d.wait_for("¿No tenés cuenta? Crear cuenta", timeout=40)
        self.pause()
        d.tap("¿No tenés cuenta? Crear cuenta")
        d.wait_for("Creá tu cuenta")
        d.fill("Nombre", self.user["name"])
        d.fill("Email", self.user["email"])
        d.fill("Contraseña", PASSWORD)
        d.hide_keyboard("Creá tu cuenta")
        self.pause(0.3, 0.8)
        d.tap("Crear cuenta")
        d.wait_for("Hola, ", exact=False, timeout=40)
        # Categories load right after sign-in.
        d.wait_for(self.categories["plomeros"], timeout=30)
        self.log("registro", email=self.user["email"])

    def open_category(self, category_id):
        name = self.categories[category_id]
        self.device.tap(name)
        first = self.providers[category_id][0]["name"]
        self.device.wait_for(first, timeout=30)
        self.log("ver_categoria", categoria=name)
        self.pause()

    def open_provider(self, provider):
        self.device.tap(provider["name"])
        self.device.wait_for("Sobre mí", timeout=30)
        self.log("ver_prestador", prestador=provider["name"], rating=provider["rating"])
        self.pause(1.0, 2.5)  # reads the profile

    def request_contact(self, provider, category_id):
        d = self.device
        message = ""
        if self.rng.random() < 0.6:
            message = self.rng.choice(MESSAGES[category_id])
            d.fill("Mensaje (opcional)", message)
            d.hide_keyboard("Sobre mí")
        if not d.is_present("Solicitar contacto"):
            d.swipe_up()
        d.tap("Solicitar contacto")
        d.wait_for("Solicitud enviada", "Solicitud enviada correctamente.", timeout=30)
        self.log("solicitar_contacto", prestador=provider["name"], con_mensaje=bool(message))
        self.pause()

    def back(self):
        self.device.back()
        self.pause(0.3, 0.8)

    def check_requests(self):
        self.device.tap("Solicitudes")
        self.device.wait_for("Mis solicitudes")
        self.device.wait_for("Pendiente", timeout=20)
        self.log("ver_mis_solicitudes")
        self.pause()

    def go_home(self):
        # Back to Home from any depth.
        for _ in range(4):
            if self.device.is_present("Buscar servicio o prestador"):
                return
            self.back()
        self.device.tap("Inicio")

    def search(self, term):
        d = self.device
        d.fill("Buscar servicio o prestador", term)
        d.hide_keyboard("Hola, ", exact=False)
        time.sleep(1.5)  # results + analytics debounce
        found = not d.is_present("No encontramos resultados para esa búsqueda.")
        self.log("buscar", termino=term, con_resultados=found)
        self.pause()
        return found

    def pick_need(self):
        ids = list(NEED_WEIGHTS)
        return self.rng.choices(ids, weights=[NEED_WEIGHTS[i] for i in ids])[0]

    # --- personas ----------------------------------------------------------
    def run_persona(self, persona):
        rng = self.rng
        need = self.pick_need()
        providers = self.providers[need]

        if persona == "rebote":
            self.pause(2, 5)
            self.log("abandona", donde="inicio")
            return

        if persona == "decidido":
            self.open_category(need)
            provider = providers[0]
            self.open_provider(provider)
            self.request_contact(provider, need)

        elif persona == "comparador":
            self.open_category(need)
            seen = providers[: rng.choice([2, 3])]
            for provider in seen:
                self.open_provider(provider)
                self.back()
            best = max(seen, key=lambda p: p["rating"] or 0)
            self.open_provider(best)
            self.request_contact(best, need)

        elif persona == "buscador":
            if rng.random() < 0.35:
                missing = rng.choice(MISSING_SERVICES)
                self.search(missing)
                if rng.random() < 0.5:
                    self.log("abandona", donde="busqueda_sin_resultados")
                    return
                self.device.tap("Limpiar búsqueda")
                self.pause()
            term = rng.choice(SEARCH_TERMS[need])
            if not self.search(term):
                self.log("abandona", donde="busqueda_sin_resultados")
                return
            provider = providers[0]
            if self.device.is_present(provider["name"]) and rng.random() < 0.7:
                self.open_provider(provider)
                if rng.random() < 0.75:
                    self.request_contact(provider, need)
                else:
                    self.log("abandona", donde="perfil_prestador")
                    return
            else:
                self.device.tap(self.categories[need])
                self.device.wait_for(providers[0]["name"], timeout=30)
                self.log("ver_categoria", categoria=self.categories[need], desde="busqueda")
                self.open_provider(providers[0])
                self.request_contact(providers[0], need)

        elif persona == "explorador":
            for category in rng.sample(list(NEED_WEIGHTS), rng.choice([1, 2])):
                self.open_category(category)
                if rng.random() < 0.7:
                    self.open_provider(rng.choice(self.providers[category]))
                    self.back()
                self.go_home()
            self.log("abandona", donde="sin_decidirse")
            return

        # Users who sent a request often check it afterwards.
        if rng.random() < 0.6:
            self.go_home()
            self.check_requests()

    # --- main loop ---------------------------------------------------------
    def run(self):
        capture = AnalyticsCapture(self.out_dir / "analytics.log")
        capture.start()
        personas = list(PERSONAS)
        summary = {"run_id": self.run_id, "seed": self.args.seed, "started": datetime.now().isoformat(),
                   "users": []}
        try:
            for i in range(1, self.args.users + 1):
                persona = self.rng.choices(personas, weights=[PERSONAS[p][0] for p in personas])[0]
                name = f"{self.rng.choice(FIRST_NAMES)} {self.rng.choice(LAST_NAMES)}"
                self.user = {"id": f"U{i:02d}", "persona": persona, "name": name,
                             "email": f"sim-{self.run_id}-{i:02d}@serviciosya.test"}
                print(f"\n=== {self.user['id']} · {persona} · {name}")
                result = {**self.user, "ok": True}
                started = time.time()
                try:
                    self.register()
                    self.run_persona(persona)
                except (DeviceError, subprocess.TimeoutExpired) as error:
                    result.update(ok=False, error=str(error))
                    self.log("error", detalle=str(error))
                    self.device.shell("screencap", "-p", f"/sdcard/sim_error_{i:02d}.png", check=False)
                result["seconds"] = round(time.time() - started, 1)
                summary["users"].append(result)
                self.device.stop()
        finally:
            capture.stop()
            summary["finished"] = datetime.now().isoformat()
            (self.out_dir / "summary.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2),
                                                       encoding="utf-8")
            self.actions_file.close()
        failed = [u for u in summary["users"] if not u["ok"]]
        print(f"\nListo: {len(summary['users'])} usuarios, {len(failed)} con error. Salida en {self.out_dir}")
        return 1 if failed else 0


def main():
    parser = argparse.ArgumentParser(description="Simula usuarios manejando la app en el emulador.")
    parser.add_argument("--users", type=int, default=20)
    parser.add_argument("--seed", type=int, default=42, help="Semilla para que la simulación sea reproducible")
    parser.add_argument("--speed", type=float, default=1.0, help="Multiplicador de las pausas (0.5 = más rápido)")
    parser.add_argument("--verbose", action="store_true")
    args = parser.parse_args()
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
    sys.exit(Simulation(args).run())


if __name__ == "__main__":
    main()
