"""Minimal Android UI driver over adb + uiautomator (standard library only)."""
import os
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from dataclasses import dataclass

PACKAGE = "com.example.serviciosya"
ACTIVITY = f"{PACKAGE}/.MainActivity"


def _adb_path() -> str:
    sdk = os.environ.get("ANDROID_HOME") or os.path.join(os.environ.get("LOCALAPPDATA", ""), "Android", "Sdk")
    exe = os.path.join(sdk, "platform-tools", "adb.exe" if os.name == "nt" else "adb")
    return exe if os.path.exists(exe) else "adb"


ADB = _adb_path()


@dataclass
class Node:
    text: str
    desc: str
    cls: str
    clickable: bool
    focused: bool
    bounds: tuple  # (left, top, right, bottom)

    @property
    def center(self):
        left, top, right, bottom = self.bounds
        return (left + right) // 2, (top + bottom) // 2

    @property
    def label(self):
        return self.text or self.desc


class DeviceError(RuntimeError):
    pass


class Device:
    def __init__(self, serial=None, verbose=False):
        self.base = [ADB] + (["-s", serial] if serial else [])
        self.verbose = verbose

    # --- adb ---------------------------------------------------------------
    def adb(self, *args, check=True, timeout=60) -> str:
        result = subprocess.run(
            self.base + list(args), capture_output=True, text=True, encoding="utf-8",
            errors="replace", timeout=timeout,
        )
        if check and result.returncode != 0:
            raise DeviceError(f"adb {' '.join(args)} failed: {result.stderr.strip()}")
        return result.stdout

    def shell(self, *args, **kwargs) -> str:
        return self.adb("shell", *args, **kwargs)

    # --- app lifecycle -----------------------------------------------------
    def reset_app(self):
        """Clears app data: Firebase then sees a brand-new installation (a new Analytics user)."""
        self.shell("am", "force-stop", PACKAGE)
        self.shell("pm", "clear", PACKAGE)

    def launch(self):
        self.shell("am", "start", "-n", ACTIVITY)

    def stop(self):
        self.shell("am", "force-stop", PACKAGE)

    # --- screen ------------------------------------------------------------
    def nodes(self) -> list:
        for _ in range(3):
            self.shell("uiautomator", "dump", "/sdcard/sim_ui.xml", check=False)
            raw = subprocess.run(self.base + ["exec-out", "cat", "/sdcard/sim_ui.xml"],
                                 capture_output=True, timeout=60).stdout
            xml = raw.decode("utf-8", errors="replace")
            if xml.strip().startswith("<?xml"):
                break
            time.sleep(0.5)
        else:
            raise DeviceError("No se pudo leer la pantalla (uiautomator dump).")
        root = ET.fromstring(xml[xml.index("<?xml"):])
        result = []
        for n in root.iter("node"):
            numbers = [int(v) for v in re.findall(r"-?\d+", n.get("bounds", "[0,0][0,0]"))]
            result.append(Node(
                text=n.get("text", ""),
                desc=n.get("content-desc", ""),
                cls=n.get("class", ""),
                clickable=n.get("clickable") == "true",
                focused=n.get("focused") == "true",
                bounds=tuple(numbers),
            ))
        return result

    def find(self, label, exact=True, nodes=None):
        nodes = nodes if nodes is not None else self.nodes()
        for node in nodes:
            value = node.label
            if (value == label) if exact else (label.lower() in value.lower()):
                return node
        return None

    def wait_for(self, *labels, timeout=20, exact=True):
        """Waits until any of the labels is on screen; returns (label, node)."""
        deadline = time.time() + timeout
        while time.time() < deadline:
            nodes = self.nodes()
            for label in labels:
                node = self.find(label, exact=exact, nodes=nodes)
                if node:
                    return label, node
            time.sleep(0.4)
        raise DeviceError(f"Timeout esperando {labels}")

    def is_present(self, label, exact=True):
        return self.find(label, exact=exact) is not None

    # --- input -------------------------------------------------------------
    def tap(self, node_or_label, exact=True, timeout=20):
        node = node_or_label if isinstance(node_or_label, Node) else self.wait_for(
            node_or_label, timeout=timeout, exact=exact)[1]
        x, y = node.center
        if self.verbose:
            print(f"    tap {node.label!r} @ {x},{y}")
        self.shell("input", "tap", str(x), str(y))
        time.sleep(0.3)

    def type_text(self, text: str):
        """Types ASCII text into the focused field (adb input cannot type accents)."""
        safe = text.encode("ascii", "ignore").decode()
        for chunk in re.findall(r".{1,40}", safe):
            escaped = chunk.replace(" ", "%s")
            for ch in "()<>|;&*\\~\"'`$?#!":
                escaped = escaped.replace(ch, "\\" + ch)
            self.shell("input", "text", escaped)
        time.sleep(0.2)

    def fill(self, field_label, text):
        self.tap(field_label)
        self.type_text(text)

    def keyboard_shown(self) -> bool:
        out = self.shell("dumpsys", "input_method", check=False)
        return "mInputShown=true" in out or "isInputViewShown=true" in out

    def hide_keyboard(self, anchor, exact=True):
        """Closes the keyboard by tapping a non-interactive text (the app clears focus on outside taps).

        adb's injected BACK would also navigate back on Android 15, so it is never used for this.
        """
        if not self.keyboard_shown():
            return
        self.tap(anchor, exact=exact)
        time.sleep(0.4)
        if self.keyboard_shown():
            raise DeviceError(f"No se pudo cerrar el teclado tocando {anchor!r}")

    def press_enter(self):
        self.shell("input", "keyevent", "KEYCODE_ENTER")

    def back(self):
        if self.keyboard_shown():
            raise DeviceError("Back con el teclado abierto: cerralo primero con hide_keyboard(anchor)")
        self.shell("input", "keyevent", "KEYCODE_BACK")
        time.sleep(0.5)

    def swipe_up(self):
        self.shell("input", "swipe", "540", "1700", "540", "700", "300")
        time.sleep(0.5)
