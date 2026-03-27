# docx2MD

A single HTML file that converts and edits Markdown documents in your browser.  
No install. No server. No upload. Open the file locally in Edge or Chrome.

---

## What it does

- Converts `.docx` files to Markdown using [mammoth.js](https://github.com/mwilliamson/mammoth.js)
- Opens and edits `.md`, `.txt`, `.html`, and `.csv` files directly
- Exports to `.md` or standalone `.svg` (with embedded Dasu/IFC metadata)
- Everything runs client-side — files never leave your machine

---

## Usage

1. Download `docx2MD.html`
2. Open it in Chrome or Edge as a local file (`File → Open`, or drag onto the browser tab)
3. Drop or browse to a file to open it, or click **New File** to start from scratch

No web server, no dependencies to install, no account required.

---

## Features

| Feature | Notes |
|---|---|
| File open | `.docx` `.md` `.txt` `.html` `.htm` `.csv` — drag-drop or file picker |
| New file | Prompts for filename, opens in Edit mode |
| View modes | Preview · Split (live) · Edit |
| Syntax highlighting | Headings, bold, italic, code, links, blockquotes — colour-coded in editor |
| Line numbers | Synced gutter on raw editor |
| Format toolbar | H1–H3, B, I, strikethrough, code, blockquote, lists, HR, link |
| Keyboard shortcuts | Ctrl+B bold · Ctrl+I italic · Ctrl+H find/replace · F11 zen mode |
| Find & Replace | Match count, prev/next navigation, replace one or all |
| Zen mode | Hides sidebar, full-width editor/preview — F11 or Esc to toggle |
| Paper/dark pane | Warm cream tone for the content area, dark UI stays |
| Font scale | 75%–200% slider — persists via Preferences |
| Auto-save draft | Writes to `localStorage`, restore banner on next open |
| Print | Clean white full-width output, no chrome — Ctrl+P or Print button |
| SVG export | Native SVG text elements (no foreignObject), always white background |
| Dasu/IFC metadata | `<metadata>` block with `dasu:` and `ifc:` namespaces for Bonsai BIM integration |
| Semantic SVG IDs | Every block tagged `data-dasu-type`, `data-dasu-token` for bridge queries |
| `{{token}}` detection | Placeholders flagged in SVG for IFC substitution |
| Preferences | Page margins, paper size, orientation, print header/footer, default view, font scale, tab size, auto-save, SVG export defaults |

---

## Supported file types

| Format | What happens |
|---|---|
| `.docx` | Converted to Markdown via mammoth.js — opens in Preview |
| `.md` / `.markdown` | Loaded as-is — opens in Edit |
| `.txt` | Loaded as-is — opens in Edit |
| `.html` / `.htm` | Stripped of scripts/styles, converted to Markdown — opens in Split |
| `.csv` | Parsed to GFM table — opens in Split |

---

## SVG export and Bonsai BIM

The SVG exporter writes native SVG primitives (no `<foreignObject>`), so the output opens correctly in Inkscape, browsers, and any standards-compliant viewer.

Every semantic block is wrapped in a `<g>` with attributes:

```xml
<g id="h1-0" data-dasu-type="heading" data-dasu-level="1">
  <text ...>Project Specification</text>
</g>

<g id="p-2" data-dasu-type="paragraph" data-dasu-tokens="ProjectName">
  <text data-dasu-token="ProjectName" ...>{{ProjectName}}</text>
</g>
```

The `<metadata>` block carries Dasu and IFC namespaces:

```xml
<metadata>
  <dasu:meta xmlns:dasu="https://dasu.app/ns/1.0"
             xmlns:ifc="https://www.buildingsmart.org/ifc/4.3">
    <dasu:project>My Project</dasu:project>
    <dasu:sheet>SP-001</dasu:sheet>
    <dasu:revision>A</dasu:revision>
    <ifc:class>IfcDocumentInformation</ifc:class>
    <ifc:globalId>...</ifc:globalId>
  </dasu:meta>
</metadata>
```

Reading tokens in Python (Dasu bridge):

```python
import xml.etree.ElementTree as ET

tree = ET.parse('spec.svg')
ns = {'dasu': 'https://dasu.app/ns/1.0'}

meta  = tree.find('.//dasu:meta', ns)
sheet = meta.find('dasu:sheet', ns).text

# Find and substitute {{token}} placeholders
for el in tree.findall('.//*[@data-dasu-token]'):
    key = el.get('data-dasu-token')
    el.text = ifc_data.get(key, el.text)
```

---

## Preferences

Open via the **Prefs** button in the header bar. Settings are saved to `localStorage`.

- **Print & Page** — margins (mm, per-side), paper size (A5/A4/A3/Letter), orientation, header/footer on/off
- **Editor** — default view mode, font scale, tab size (2 or 4), default pane (dark/paper), auto-save on/off
- **SVG Export** — default author, revision, IFC class

---

## Dependencies

| Library | Licence | Purpose |
|---|---|---|
| [mammoth.js](https://github.com/mwilliamson/mammoth.js) v1.6.0 | BSD | `.docx` → HTML conversion |
| Markdown renderer | Inlined (MIT) | No external dependency |

mammoth.js loads from `unpkg.com` with a `cdnjs.cloudflare.com` fallback. Everything else is self-contained in the HTML file.

---

## Browser support

Chrome and Edge (Chromium). Firefox works for most features but `measureText` accuracy for SVG layout may vary slightly. Safari untested.

---

## Relationship to Dasu.print

docx2MD is a companion tool to [Dasu.print](https://github.com/4nigel/dasu) — an open-source drawing sheet layout tool for Bonsai BIM. SVG exports from docx2MD are designed to be placed directly on Dasu drawing sheets, with metadata readable by the Dasu Python bridge.

---

## Licence

MIT
