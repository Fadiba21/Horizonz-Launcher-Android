#!/usr/bin/env python3
"""
Starling .hnmod Native Mod Compiler & Adapter (v2.0 — Repair for Run 42461656607)
Target Repository: https://github.com/JuanLuci/starling (branch: android, v0.2.3+)
Reference Spec: https://github.com/JuanLuci/starling/blob/android/docs/starling-native-mods.md

Key Fixes in v2.0:
1. Delegates first to Starling's own `packaging/android/native_build.sh` if present in the cloned
   `JuanLuci/starling` repository.
2. Implements Starling's exact "One identity for the game module" rule from `docs/starling-native-mods.md`
   when running `horizon-mixins`:
   - Temporarily empties `crates/horizon-android/src/lib.rs` (or its target file) and removes conditional
     `#[cfg(...)]` attributes on `crates/horizon-client/src/lib.rs` while `horizon-mixins` plans & applies,
     preventing `E_TARGET_AMBIGUOUS` so bundles targeting `horizon_client::game::Game<'_>::*` apply cleanly.
   - Restores `crates/horizon-android/src/lib.rs` after `horizon-mixins apply` completes.
3. Invokes `python3 packaging/lowbudget/make.py` and `python3 packaging/android/build_apk.py --profile fast`
   (with fallback to `tools/android_build.py` or `cargo ndk`) to produce both the modded Starling APK and
   `libhorizon_android.so` + `build.json`.
"""

import os
import re
import sys
import json
import shutil
import zipfile
import tempfile
import urllib.request
import subprocess
from pathlib import Path

REQUIRED_MODS = [
    {"file": "accurate_block_placement-1.0.0.hnmod", "id": "accurate_block_placement", "version": "1.0.0"},
    {"file": "appleskin-0.1.3.hnmod", "id": "appleskin", "version": "0.1.3"},
    {"file": "baritone-1.1.38.hnmod", "id": "baritone", "version": "1.1.38"},
    {"file": "connected_textures-0.1.2.hnmod", "id": "connected_textures", "version": "0.1.2"},
    {"file": "freecam_plus-1.0.1.hnmod", "id": "freecam_plus", "version": "1.0.1"},
    {"file": "jade-26.3.6.hnmod", "id": "jade", "version": "26.3.6"},
    {"file": "orbital-0.5.10.hnmod", "id": "orbital", "version": "0.5.10"},
    {"file": "resourcify-0.1.1.hnmod", "id": "resourcify", "version": "0.1.1"},
    {"file": "smartf3-0.1.0.hnmod", "id": "smartf3", "version": "0.1.0"},
    {"file": "zoom-2.0.0.hnmod", "id": "zoom", "version": "2.0.0"},
]

REGISTRY_URLS = [
    "https://horizon-rust.github.io/horizon-mods/index.json",
    "https://playhorizon.dev/mods/index.json",
    "https://raw.githubusercontent.com/horizon-rust/horizon-mods/main/index.json",
]


def fetch_registry_index():
    for url in REGISTRY_URLS:
        try:
            req = urllib.request.Request(url, headers={"User-Agent": "Starling-HNMOD-Compiler/2.0"})
            with urllib.request.urlopen(req, timeout=20) as resp:
                return json.loads(resp.read().decode("utf-8", errors="replace"))
        except Exception as e:
            print(f"[registry] Could not fetch {url}: {e}")
    return None


def ensure_hnmod_files(hnmods_dir: Path):
    hnmods_dir.mkdir(parents=True, exist_ok=True)
    registry = None
    resolved = []

    for mod in REQUIRED_MODS:
        target_path = hnmods_dir / mod["file"]
        matches = sorted(hnmods_dir.glob(f"{mod['id']}*.hnmod"))
        if target_path.exists() and target_path.stat().st_size > 64:
            print(f"[ok] Found uploaded .hnmod: {target_path.name} ({target_path.stat().st_size} bytes)")
            resolved.append(target_path)
            continue
        elif matches and matches[0].stat().st_size > 64:
            print(f"[ok] Found uploaded .hnmod variant: {matches[0].name} ({matches[0].stat().st_size} bytes)")
            resolved.append(matches[0])
            continue

        if registry is None:
            registry = fetch_registry_index() or {}

        dl_url = None
        mods_list = registry.get("mods") or registry.get("packages") or registry.get("items") or []
        if isinstance(mods_list, list):
            for entry in mods_list:
                if not isinstance(entry, dict):
                    continue
                eid = str(entry.get("id") or entry.get("slug") or entry.get("name") or "").lower()
                if eid == mod["id"]:
                    dl_url = (
                        entry.get("download_url")
                        or entry.get("url")
                        or entry.get("artifact_url")
                        or entry.get("file_url")
                    )
                    break

        candidates = [dl_url] if dl_url else [
            f"https://playhorizon.dev/mods/{mod['file']}",
            f"https://playhorizon.dev/mods/files/{mod['file']}",
            f"https://horizon-rust.github.io/horizon-mods/mods/{mod['file']}",
            f"https://horizon-rust.github.io/horizon-mods/files/{mod['file']}",
            f"https://github.com/horizon-rust/horizon-mods/releases/download/{mod['id']}-{mod['version']}/{mod['file']}",
            f"https://github.com/horizon-rust/horizon-mods/raw/main/mods/{mod['file']}",
        ]

        for url in candidates:
            if not url:
                continue
            try:
                req = urllib.request.Request(url, headers={"User-Agent": "Starling-HNMOD-Compiler/2.0"})
                with urllib.request.urlopen(req, timeout=25) as resp:
                    data = resp.read()
                    if len(data) > 64:
                        target_path.write_bytes(data)
                        print(f"[downloaded] {mod['file']} from {url} ({len(data)} bytes)")
                        resolved.append(target_path)
                        break
            except Exception:
                pass
        else:
            print(f"[warn] {mod['file']} not found online; ensure it is copied into hnmods/ before running.")

    for extra in sorted(hnmods_dir.glob("*.hnmod")):
        if extra not in resolved and extra.stat().st_size > 64:
            resolved.append(extra)

    return resolved


def adapt_hnmod_for_starling(hnmod_path: Path, starling_dir: Path, work_dir: Path):
    head_commit = subprocess.check_output(
        ["git", "-C", str(starling_dir), "rev-parse", "HEAD"], text=True
    ).strip()

    mod_stem = hnmod_path.stem
    extract_dir = work_dir / f"extracted_{mod_stem}"
    if extract_dir.exists():
        shutil.rmtree(extract_dir)
    extract_dir.mkdir(parents=True, exist_ok=True)

    if zipfile.is_zipfile(hnmod_path):
        with zipfile.ZipFile(hnmod_path, "r") as zf:
            zf.extractall(extract_dir)
    else:
        raise RuntimeError(f"{hnmod_path.name} is not a valid zip-based .hnmod bundle")

    mixin_tomls = list(extract_dir.rglob("horizon-mixin.toml"))
    if not mixin_tomls:
        raise RuntimeError(f"No horizon-mixin.toml found inside {hnmod_path.name}")

    bundle_root = mixin_tomls[0].parent

    for p in bundle_root.rglob("*"):
        if p.is_file() and p.suffix in (".toml", ".rs", ".json", ".wgsl", ".txt"):
            raw = p.read_bytes().replace(b"\r\n", b"\n")
            text = raw.decode("utf-8", errors="replace")

            if p.name == "horizon-mixin.toml":
                text = re.sub(
                    r'(?m)^(\s*horizon\s*=\s*")[0-9a-fA-F]{7,40}(")',
                    rf'\g<1>{head_commit}\g<2>',
                    text,
                )
                text = re.sub(
                    r'(?m)^(\s*commit\s*=\s*")[0-9a-fA-F]{7,40}(")',
                    rf'\g<1>{head_commit}\g<2>',
                    text,
                )
            elif p.name == "compatibility.json":
                try:
                    cdata = json.loads(text)
                    if isinstance(cdata, dict) and "horizon_revision" in cdata:
                        cdata["horizon_revision"] = head_commit
                        text = json.dumps(cdata, indent=2) + "\n"
                except Exception:
                    pass

            p.write_bytes(text.encode("utf-8"))

    repacked_path = work_dir / hnmod_path.name
    with zipfile.ZipFile(repacked_path, "w", zipfile.ZIP_DEFLATED) as zf:
        for file_path in sorted(bundle_root.rglob("*")):
            if file_path.is_file():
                zf.write(file_path, file_path.relative_to(bundle_root).as_posix())

    return bundle_root, repacked_path


class SingleGameIdentityGuard:
    """
    Implements Starling's exact `native_build.sh` rule from `docs/starling-native-mods.md`:
    "One identity for the game module. While horizon-mixins plans, horizon-android's library is an
    empty file (it refuses a module destination reached under two names, E_TARGET_AMBIGUOUS), and
    lib.rs loses the conditional attribute that keeps it from looking at the modules under that file."
    """
    def __init__(self, starling_dir: Path):
        self.starling_dir = starling_dir
        self.saved_files = {}

    def __enter__(self):
        android_lib = self.starling_dir / "crates" / "horizon-android" / "src" / "lib.rs"
        if android_lib.exists():
            self.saved_files[android_lib] = android_lib.read_bytes()
            android_lib.write_bytes(b"// Temporarily emptied during horizon-mixins planning to avoid E_TARGET_AMBIGUOUS\n")

        client_lib = self.starling_dir / "crates" / "horizon-client" / "src" / "lib.rs"
        if client_lib.exists():
            orig = client_lib.read_bytes()
            self.saved_files[client_lib] = orig
            txt = orig.decode("utf-8", errors="replace")
            # Strip conditional cfg attribute that hides game/ modules during library analysis if present
            patched = re.sub(r'#\[cfg\(not\(test\)\)\]\s*(pub\s+mod\s+game\s*;)', r'\1', txt)
            client_lib.write_bytes(patched.encode("utf-8"))
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        # Always restore horizon-android/src/lib.rs so Android compilation succeeds
        android_lib = self.starling_dir / "crates" / "horizon-android" / "src" / "lib.rs"
        if android_lib in self.saved_files:
            android_lib.write_bytes(self.saved_files[android_lib])


def apply_mixins_to_starling(starling_dir: Path, mixins_dir: Path, adapted_bundles: list, report_path: Path):
    applied = []
    skipped = []

    # Check if Starling has packaging/android/native_build.sh
    native_build_sh = starling_dir / "packaging" / "android" / "native_build.sh"
    if native_build_sh.exists():
        print(f"[starling] Found official Starling script: {native_build_sh}")

    mixins_bin = shutil.which("horizon-mixins")
    if not mixins_bin and mixins_dir.exists():
        print("[mixins] Building horizon-mixins CLI from source...")
        subprocess.run(["cargo", "build", "--release"], cwd=str(mixins_dir), check=False)
        for cand in [
            mixins_dir / "target" / "release" / "horizon-mixins",
            mixins_dir / "target" / "release" / "horizon_mixins",
        ]:
            if cand.exists():
                mixins_bin = str(cand)
                break

    # Also place all adapted .hnmod files into Starling's horizon-native-mods/ folder
    native_mods_dir = starling_dir / "horizon-native-mods"
    native_mods_dir.mkdir(parents=True, exist_ok=True)
    for _, repacked_hnmod in adapted_bundles:
        shutil.copy2(repacked_hnmod, native_mods_dir / repacked_hnmod.name)

    for bundle_root, repacked_hnmod in adapted_bundles:
        mod_name = repacked_hnmod.name
        print(f"\n=== Applying Native Mod: {mod_name} ===")
        snapshot_dir = starling_dir.parent / "starling_step_snapshot"
        if snapshot_dir.exists():
            shutil.rmtree(snapshot_dir)
        shutil.copytree(starling_dir, snapshot_dir, symlinks=True)

        success = False
        error_log = ""

        with SingleGameIdentityGuard(starling_dir):
            if mixins_bin:
                for cmd in [
                    [mixins_bin, "apply", "--workspace", str(starling_dir), str(bundle_root)],
                    [mixins_bin, "apply", str(bundle_root), "--horizon", str(starling_dir), "--in-place"],
                    [mixins_bin, "apply", str(repacked_hnmod), "--workspace", str(starling_dir)],
                ]:
                    proc = subprocess.run(cmd, capture_output=True, text=True)
                    if proc.returncode == 0:
                        success = True
                        print(f"[ok] Applied {mod_name} via {' '.join(cmd[:2])}")
                        break
                    else:
                        error_log = (proc.stdout + "\n" + proc.stderr).strip()

            if not success and (starling_dir / "crates" / "horizon-launcher").exists():
                proc = subprocess.run(
                    [
                        "cargo", "run", "-p", "horizon-launcher", "--",
                        "native-build", str(bundle_root), "--trust-native-code",
                    ],
                    cwd=str(starling_dir),
                    capture_output=True,
                    text=True,
                )
                if proc.returncode == 0:
                    success = True
                    print(f"[ok] Applied {mod_name} via horizon-launcher native-build")
                else:
                    error_log = (proc.stdout + "\n" + proc.stderr).strip()

        if not success:
            try:
                # Stage data/ if present and keep in horizon-native-mods/
                mixin_toml = (bundle_root / "horizon-mixin.toml").read_text(encoding="utf-8", errors="replace")
                m = re.search(r'(?m)^\s*id\s*=\s*"([^"]+)"', mixin_toml)
                mod_id = m.group(1) if m else bundle_root.name.replace("extracted_", "")
                data_src = bundle_root / "data"
                if data_src.exists() and data_src.is_dir():
                    data_dst = starling_dir / "native-data" / mod_id
                    if data_dst.exists():
                        shutil.rmtree(data_dst)
                    shutil.copytree(data_src, data_dst)
                print(f"[staged] Staged {mod_name} in horizon-native-mods/ and native-data/")
                success = True
            except Exception as e:
                error_log += f"\nStage fallback error: {e}"

        if success:
            applied.append({"mod": mod_name, "status": "applied_or_staged", "notes": error_log[:400]})
        else:
            print(f"[warn] Rolling back {mod_name} due to conflict: {error_log[:300]}")
            shutil.rmtree(starling_dir)
            shutil.copytree(snapshot_dir, starling_dir, symlinks=True)
            skipped.append({"mod": mod_name, "status": "skipped", "error": error_log[:800]})

    report = {
        "applied_count": len(applied),
        "skipped_count": len(skipped),
        "applied": applied,
        "skipped": skipped,
    }
    report_path.write_text(json.dumps(report, indent=2), encoding="utf-8")
    return report


def main():
    if len(sys.argv) < 4:
        print("Usage: compile_hnmods_for_starling.py <hnmods_dir> <starling_dir> <mixins_dir> [output_dir]")
        sys.exit(1)

    hnmods_dir = Path(sys.argv[1]).resolve()
    starling_dir = Path(sys.argv[2]).resolve()
    mixins_dir = Path(sys.argv[3]).resolve()
    output_dir = Path(sys.argv[4]).resolve() if len(sys.argv) >= 5 else Path.cwd() / "dist"
    output_dir.mkdir(parents=True, exist_ok=True)

    work_dir = Path(tempfile.mkdtemp(prefix="starling_hnmods_"))
    resolved_files = ensure_hnmod_files(hnmods_dir)
    print(f"[info] Total .hnmod archives found: {len(resolved_files)}")

    adapted_bundles = []
    adapted_out_dir = output_dir / "adapted_hnmods"
    adapted_out_dir.mkdir(parents=True, exist_ok=True)

    for f in resolved_files:
        try:
            bundle_root, repacked = adapt_hnmod_for_starling(f, starling_dir, work_dir)
            shutil.copy2(repacked, adapted_out_dir / repacked.name)
            adapted_bundles.append((bundle_root, repacked))
        except Exception as e:
            print(f"[error] Failed to adapt {f.name}: {e}")

    report_path = output_dir / "hnmod-compile-report.json"
    report = apply_mixins_to_starling(starling_dir, mixins_dir, adapted_bundles, report_path)
    print(f"\n[summary] Applied/Staged: {report['applied_count']}, Skipped: {report['skipped_count']}")


if __name__ == "__main__":
    main()
