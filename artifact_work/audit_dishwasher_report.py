from pathlib import Path
from zipfile import ZipFile

from lxml import etree


DOCX = Path(r"D:\develop\food-swift\第一次平时作业-现代洗碗机的历史与科伦设计演进分析.docx")
W = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
NS = {"w": W}


def qn(name: str) -> str:
    prefix, local = name.split(":", 1)
    return f"{{{W}}}{local}"


with ZipFile(DOCX) as zf:
    part_names = [
        name
        for name in zf.namelist()
        if name == "word/document.xml" or name.startswith("word/footer")
    ]
    roots = [(name, etree.fromstring(zf.read(name))) for name in part_names]
    text_runs = []
    for part_name, root in roots:
        for run in root.xpath(".//w:r[w:t]", namespaces=NS):
            text = "".join(run.xpath("./w:t/text()", namespaces=NS))
            rpr = run.find(qn("w:rPr"))
            fonts = rpr.find(qn("w:rFonts")) if rpr is not None else None
            color = rpr.find(qn("w:color")) if rpr is not None else None
            east = fonts.get(qn("w:eastAsia")) if fonts is not None else None
            ascii_font = fonts.get(qn("w:ascii")) if fonts is not None else None
            color_val = color.get(qn("w:val")) if color is not None else None
            text_runs.append((part_name, text, east, ascii_font, color_val))

    font_failures = [item for item in text_runs if item[2] != "楷体" or item[3] != "楷体"]
    color_failures = [item for item in text_runs if item[4] != "000000"]

    document_root = dict(roots)["word/document.xml"]
    paragraphs = []
    for p in document_root.xpath(".//w:body/w:p", namespaces=NS):
        paragraphs.append("".join(p.xpath(".//w:t/text()", namespaces=NS)))
    first_nonempty = next(text for text in paragraphs if text.strip())

    sect_pr = document_root.xpath(".//w:sectPr", namespaces=NS)
    tables = document_root.xpath(".//w:tbl", namespaces=NS)
    rows = document_root.xpath(".//w:tbl[1]/w:tr", namespaces=NS)
    unsplittable = [bool(row.xpath("./w:trPr/w:cantSplit", namespaces=NS)) for row in rows]
    repeated_header = bool(rows and rows[0].xpath("./w:trPr/w:tblHeader", namespaces=NS))
    media = [name for name in zf.namelist() if name.startswith("word/media/")]

print(f"first_nonempty={first_nonempty}")
print(f"text_run_count={len(text_runs)}")
print(f"font_failures={len(font_failures)}")
print(f"color_failures={len(color_failures)}")
print(f"sections={len(sect_pr)}")
print(f"tables={len(tables)} rows={len(rows)} repeated_header={repeated_header}")
print(f"all_rows_cant_split={all(unsplittable)}")
print(f"media={media}")
print(f"has_title={first_nonempty.startswith('第一次平时作业 - ')}")
print(f"has_all_citations={all(f'[{i}]' in ''.join(paragraphs) for i in range(1, 10))}")

if font_failures:
    print("FONT_FAILURE_SAMPLE", font_failures[:5])
if color_failures:
    print("COLOR_FAILURE_SAMPLE", color_failures[:5])

assert first_nonempty == "第一次平时作业 - 现代洗碗机的历史与科伦设计演进分析"
assert not font_failures
assert not color_failures
assert len(sect_pr) == 3
assert len(tables) == 1 and len(rows) == 6
assert repeated_header and all(unsplittable)
assert len(media) == 1
