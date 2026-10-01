# Offline MapLibre basemap pack

Bundled under `assets/map/` and copied to `filesDir/map/` on Map open when
`PACK_VERSION` changes (atomic replace). After copy, `style.json` token
`__PACK_ROOT__` is rewritten to the absolute pack directory so `mbtiles://`
URLs resolve on-device.

## Locked tile envelope (Colony_Locations_sept_2025.csv)

- **Lat:** [-78.08, -74.53]
- **Lon:** [163.07, 168.13]
- **Includes:** Cape Washington, Markham Is, Erebus Bay Inside/Outside sites
- **Excludes from pack:** `Other`, `Baxter Meadows` (Bozeman Local — drawn as overlay only)

## Required files (APK / assets)

| File | ~Size | Purpose |
|------|-------|---------|
| `PACK_VERSION` | — | Stamp; bump whenever any pack file below changes |
| `style.json` | ~15 KB | MapLibre style with **no HTTP** glyph/sprite/tile URLs |
| `region.mbtiles` | ~51 MB | Antarctica vectors (tippecanoe) |
| `hillshade.mbtiles` | ~60 MB | Land-clipped RAMP2 hillshade (EPSG:3857) |
| `bathymetry.mbtiles` | ~86 MB | Ocean depth colors (ETOPO1 elevation → RGBA) |
| `bozeman.mbtiles` | ~11 MB | Gallatin Valley / Bozeman OSM (local testing) |
| `glyphs/Open Sans Regular/` | ~230 KB | Label glyphs (folder name uses a real space) |
| `icons/` | ~8 KB | COMNAP `research-station` PNG (+ `@2x`) |

**APK impact:** this pack is ~**209 MB** uncompressed in assets (dominates install size).
Largest levers: bathymetry → hillshade → region → bozeman. Colony rectangles are
**not** in this pack (Room / CSV import). Runtime overlays (colonies, GPS) are added
in `MapLibreMapView.kt`, not `style.json`.

Build / clip steps: [`map_pack_build/README.md`](../../../../../map_pack_build/README.md).

## Sources (`style.json` → MBTiles)

| Style `source` id | Type | File | `source-layer`s |
|-------------------|------|------|-----------------|
| `bathymetry` | raster | `bathymetry.mbtiles` | — |
| `hillshade` | raster | `hillshade.mbtiles` | — |
| `basemap` | vector | `region.mbtiles` | `coastline_high_poly`, `coastline_high_line`, `moraines_med`, `lakes_high`, `contours_med`, `graticule`, `comnap` |
| `bozeman` | vector | `bozeman.mbtiles` | `parks`, `waterways`, `roads`, `places` |

Glyphs: `file://__PACK_ROOT__/glyphs/{fontstack}/{range}.pbf` → **Open Sans Regular** only.

---

## Style layer map (edit guide)

Layers are listed **bottom → top** (paint order). Change colors / opacity / zoom in
`style.json`, then bump `PACK_VERSION`.

### Antarctica (primary)

| QGIS Basemap panel | Tippecanoe / raster | Style layer `id` | Type | Zoom | Key paint / layout (what to tweak) |
|--------------------|---------------------|------------------|------|------|--------------------------------------|
| *(none — ocean base)* | — | `background` | background | all | `background-color` `#b8d1e4` |
| ETOPO1 Elevation → bathymetry | `bathymetry` source | `bathymetry` | raster | ≤14 | Mute ocean; fades out by z13 (`raster-opacity` → 0) so tile edges don’t fight the coastline. |
| RAMP2 Hillshade | `hillshade` source | `hillshade` | raster | ≤14 | Strong at overview; fades by z13. Land-clip is coarse — don’t leave it opaque under thin fills at close zoom. |
| Coastlines (polygon) | `coastline_high_poly` | `coastline-fill-med` | fill | ≤10 | Colors: land `#d4d2cc`, ice shelf `#f7fbff`, ice tongue `#c9d3e2`. Opacity ~`0.82`–`0.88` masks jagged hillshade/bathymetry clip edges. |
| Coastlines (polygon) | `coastline_high_poly` | `coastline-fill-high` | fill | ≥9 | Same colors/opacity as med (crossfade band z9–10). |
| Coastlines (line) | `coastline_high_line` | `coastline-shore-line` | line | ≥8 | `SURFACE`: grounding line `#3d5a73`, ice shelf / default `#547eb6`. Width z8→14: `0.5→1.1`. |
| Moraines | `moraines_med` | `moraines-fill` | fill | all | `#e6e1dc` fill + outline, opacity `1`. |
| Lakes | `lakes_high` | `lakes-fill` | fill | all | Fill `#377eb8`, outline `#265980`. |
| Contours | `contours_med` | `contours-med` | line | ≥6 | Attribute **`HEIGHT`**. Index every 400 m → `#7a5f48` (thicker); others `#8f7057`. Width scales with zoom. |
| Contours (labels) | `contours_med` | `contour-labels-major` | symbol | 7–10 | Labels where `HEIGHT % 800 == 0`. Line placement; color `#5c4635` + light halo. |
| Contours (labels) | `contours_med` | `contour-labels` | symbol | ≥10 | Labels where `HEIGHT % 400 == 0`. Spacing/size ease in with zoom. |
| 15-min lat + 30-min lon | `graticule` | `graticule` | line | ≥5 | Black dashed (`line-dasharray` `[2,2]`), opacity `0.4`. Props: `kind`, `label`, `value`. |
| 15-min lat + 30-min lon | `graticule` | `graticule-labels` | symbol | ≥7 | `text-field` ← `label` (e.g. `76°30'S`). |
| COMNAP listed facilities | `comnap` | `comnap-facilities` | symbol | all | Icon `research-station` from `icons/` (loaded in Kotlin). Size scales z7→14. |
| COMNAP listed facilities | `comnap` | `comnap-labels` | symbol | ≥6 | `name_eng` / `name_off`; offset below icon. |

**Not in `style.json` / pack** (QGIS-only or dropped): RAMP2 Hillshade 2× v. exag.;
ETOPO1…Hillshade 5× (low/high); **Rock_outcrop**. See `map_pack_build/README.md`.

### Bozeman / local testing

| OSM theme | `source-layer` | Style layer `id` | Type | Notes |
|-----------|----------------|------------------|------|-------|
| Parks | `parks` | `bozeman-parks` | fill | `#c5d9b8` @ 0.7 |
| Waterways | `waterways` | `bozeman-waterways` | line | `#6fa3c7`, width by zoom |
| Roads (major) | `roads` | `bozeman-roads-major-casing` | line | Filter: motorway…tertiary; gray casing |
| Roads (major) | `roads` | `bozeman-roads-major` | line | Same filter; white fill |
| Roads (minor) | `roads` | `bozeman-roads-minor` | line | ≥12; residential / service / etc. |
| Places | `places` | `bozeman-places` | symbol | ≥10; `name` + `place` size match |

There is **no** `water` polygon layer in `bozeman.mbtiles` (do not re-add
`bozeman-water` unless the tiles gain that layer).

### Runtime overlays (not in `style.json`)

| Feature | Where | Notes |
|---------|-------|-------|
| Colony boxes + labels | `MapLibreMapView.kt` / `ColonyMapGeometry.kt` | GeoJSON from Room; colors match Map legend |
| GPS puck | `MapLibreMapView.kt` | Above graticule, under text stack |

---

## Suggested zoom range

- **minZoom (camera):** 6
- **maxZoom:** 14
- **tile detail:** vector z5–14; hillshade / bathymetry z5–12

## Production notes

1. Clip Quantarctica Detailed basemap (+ COMNAP) to the envelope in QGIS.
2. Warp RAMP2 hillshade to EPSG:3857, land-clip to high coastline polygons; build
   bathymetry from Int16 ETOPO1; export vectors to GeoJSON (EPSG:4326).
3. tippecanoe → `region.mbtiles`; gdal2tiles / MBTiles for rasters.
4. Keep `style.json` on `mbtiles://__PACK_ROOT__/…` and local glyphs/icons only.
5. Bump `PACK_VERSION` (current: `2025.10.01l`) after any pack or style change.
6. Verify airplane mode: tiles and labels still render.
7. Prefer Git LFS for `*.mbtiles` (~209 MB total).

### Known detail limits

- **Bozeman / Baxter:** offline OSM extract (z9–16). Use **Bozeman** or **My location**.
- Shoreline sharpness from ADD **high-res coastline** vectors; RAMP2 hillshade is
  clipped to land/ice and shows through translucent fills.
- Glyph folder must be named `Open Sans Regular` (real space).
- COMNAP labels come from vector tiles (`name_eng` / `name_off`).

## License / attribution

Basemap derived from **Quantarctica** (Norwegian Polar Institute) and its included
datasets. Cite Quantarctica and each source dataset used. Show credit on the Map
screen. Individual Quantarctica layers may carry additional terms — review before
redistribution.
