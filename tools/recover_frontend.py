#!/usr/bin/env python3
"""Recover a buildable Vue 2 project from the source maps shipped with FoodSwift.

The maps contain original TypeScript module bodies and compiled Vue render functions,
but not the original SFC templates or extracted SCSS.  Recovered SFCs therefore attach
the compiled render functions to the recovered class components.  The already-built
CSS and public assets are copied as compatibility assets.
"""

from __future__ import annotations

import argparse
import base64
import json
import re
import shutil
from collections import defaultdict
from pathlib import Path


SOURCE_PREFIX = "webpack:///./"


def load_sources(maps_dir: Path) -> list[tuple[str, str]]:
    records: list[tuple[str, str]] = []
    for map_path in sorted(maps_dir.glob("*.js.map")):
        if map_path.name.startswith("chunk-vendors"):
            continue
        data = json.loads(map_path.read_text(encoding="utf-8-sig"))
        for source, content in zip(data.get("sources", []), data.get("sourcesContent", [])):
            if not content or not source.startswith(SOURCE_PREFIX):
                continue
            relative = source[len(SOURCE_PREFIX) :]
            if relative.startswith("node_modules/") or relative.startswith("webpack/"):
                continue
            records.append((relative, content))
    return records


def safe_write(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    normalized = content.replace("\r\n", "\n").replace("\r", "\n")
    path.write_text(normalized, encoding="utf-8", newline="\n")


def recover_typescript(records: list[tuple[str, str]], output: Path) -> int:
    recovered: dict[str, str] = {}
    for source, content in records:
        if "?" in source:
            continue
        if source.startswith("src/") and source.endswith(".ts"):
            recovered[source] = content

    for source, content in recovered.items():
        safe_write(output / source, content)
    return len(recovered)


def choose_vue_parts(entries: list[tuple[str, str]]) -> tuple[str | None, str | None, str | None]:
    script = None
    render = None
    aggregator = None
    for source, content in entries:
        if "var render = function" in content and "staticRenderFns" in content:
            render = content
        elif "export default class" in content and "import mod from" not in content:
            script = content
        elif "import { render, staticRenderFns }" in content and "normalizer(" in content:
            aggregator = content
    return script, render, aggregator


def scope_id_from_aggregator(content: str | None) -> str | None:
    if not content:
        return None
    match = re.search(
        r"normalizer\(\s*script,\s*render,\s*staticRenderFns,\s*false,\s*null,\s*(?:\"([^\"]+)\"|null)",
        content,
        re.S,
    )
    return match.group(1) if match else None


def convert_component_script(script: str, render_import: str, scope_id: str | None) -> str:
    unnamed = re.compile(r"export\s+default\s+class\s+extends\s+")
    named = re.compile(r"export\s+default\s+class\s+([A-Za-z_$][\w$]*)\s+extends\s+")

    class_name = "RecoveredComponent"
    named_match = named.search(script)
    if named_match:
        class_name = named_match.group(1)
        script = named.sub(f"class {class_name} extends ", script, count=1)
    elif unnamed.search(script):
        script = unnamed.sub(f"class {class_name} extends ", script, count=1)
    else:
        raise ValueError("Unable to locate the default class component")

    attachment = [
        "",
        f"const __component = {class_name} as any",
        "__component.options.render = __recoveredRender",
        "__component.options.staticRenderFns = __recoveredStaticRenderFns",
    ]
    if scope_id:
        attachment.append(f"__component.options._scopeId = 'data-v-{scope_id}'")
    attachment.extend([f"export default {class_name}", ""])

    return (
        f"import {{ render as __recoveredRender, staticRenderFns as __recoveredStaticRenderFns }} "
        f"from './{render_import}'\n"
        + script.strip()
        + "\n"
        + "\n".join(attachment)
    )


def recover_vue_components(records: list[tuple[str, str]], output: Path) -> tuple[int, list[str]]:
    grouped: dict[str, list[tuple[str, str]]] = defaultdict(list)
    for source, content in records:
        if not source.startswith("src/") or ".vue" not in source:
            continue
        base = source.split("?", 1)[0]
        if base.endswith(".vue"):
            grouped[base].append((source, content))

    count = 0
    skipped: list[str] = []
    for base, entries in sorted(grouped.items()):
        script, render, aggregator = choose_vue_parts(entries)
        if not script or not render:
            skipped.append(base)
            continue

        component_path = output / base
        render_name = component_path.stem + ".render"
        render_path = component_path.with_name(render_name + ".ts")
        render_module = render.replace("var render =", "export const render =", 1)
        render_module = render_module.replace("var staticRenderFns =", "export const staticRenderFns =", 1)
        render_module = re.sub(r"\nexport \{ render, staticRenderFns \}\s*$", "\n", render_module)
        safe_write(render_path, "// Recovered compiled template render functions.\n// @ts-nocheck\n" + render_module)

        converted = convert_component_script(script, render_name, scope_id_from_aggregator(aggregator))
        sfc = "<!-- Recovered from production source maps; original <template> was not embedded. -->\n"
        sfc += '<script lang="ts">\n' + converted + "</script>\n"
        safe_write(component_path, sfc)
        count += 1
    return count, skipped


def recover_assets(records: list[tuple[str, str]], static_root: Path, output: Path) -> int:
    count = 0
    for source, content in records:
        if "?" in source or not source.startswith("src/assets/"):
            continue
        destination = output / source
        public_match = re.search(r'__webpack_public_path__ \+ "([^\"]+)"', content)
        data_match = re.search(r'base64,([^\"]+)', content, re.S)
        if public_match:
            candidate = static_root / public_match.group(1)
            if candidate.exists():
                destination.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(candidate, destination)
                count += 1
        elif data_match:
            destination.parent.mkdir(parents=True, exist_ok=True)
            destination.write_bytes(base64.b64decode(re.sub(r"\s+", "", data_match.group(1))))
            count += 1
    return count


def copy_compatibility_public(static_root: Path, output: Path) -> None:
    public = output / "public"
    public.mkdir(parents=True, exist_ok=True)
    for directory in ("css", "fonts", "img", "media"):
        source = static_root / directory
        if source.exists():
            shutil.copytree(source, public / directory, dirs_exist_ok=True)
    favicon = static_root / "favicon.ico"
    if favicon.exists():
        shutil.copy2(favicon, public / "favicon.ico")


def ensure_style_placeholders(output: Path) -> None:
    styles = output / "src/styles"
    styles.mkdir(parents=True, exist_ok=True)
    placeholders = (
        "element-variables.scss",
        "index.scss",
        "home.scss",
        "newRJWMsystem.scss",
        "_variables.scss",
    )
    for name in placeholders:
        path = styles / name
        if not path.exists():
            safe_write(
                path,
                "// Original SCSS was extracted from the production bundle and was not embedded in its source map.\n",
            )


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--static-root", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()

    static_root = args.static_root.resolve()
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=True)
    records = load_sources(static_root / "js")
    ts_count = recover_typescript(records, output)
    vue_count, skipped = recover_vue_components(records, output)
    asset_count = recover_assets(records, static_root, output)
    copy_compatibility_public(static_root, output)
    ensure_style_placeholders(output)

    print(f"Recovered {ts_count} TypeScript modules")
    print(f"Recovered {vue_count} Vue components")
    print(f"Recovered {asset_count} source assets")
    if skipped:
        print("Skipped Vue sources without both script and render content:")
        for item in skipped:
            print(f"  {item}")


if __name__ == "__main__":
    main()
