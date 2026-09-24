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

## Required files (add before field release)

| File | Purpose |
|------|---------|
| `PACK_VERSION` | Stamp; bump whenever any pack file below changes |
| `style.json` | MapLibre style with **no HTTP** glyph/sprite/tile URLs |
| `region.mbtiles` | Vector MBTiles for the envelope (Quantarctica-derived) |
| `hillshade.mbtiles` | Optional raster hillshade (RAMP2 clip → EPSG:3857) |
| `bozeman.mbtiles` | Local Gallatin Valley / Bozeman OSM basemap (Baxter Meadows) |
| `glyphs/` | Bundled Open Sans Regular/Bold PBF glyphs for labels |
| `sprites/` | Optional; only if the style uses icons |

## Suggested zoom range

- **minZoom (camera):** 6
- **maxZoom:** 14
- **tile detail:** vector z5–14, hillshade z5–12

## Production notes

1. Clip Quantarctica Detailed basemap (+ COMNAP / place names) to the envelope in QGIS.
2. Warp hillshade to EPSG:3857; export vectors to GeoJSON (EPSG:4326).
3. Build vector tiles with tippecanoe → `region.mbtiles`; raster tiles via gdal2tiles.
4. Author a fully local `style.json` using `mbtiles://__PACK_ROOT__/…` (rewritten at install).
5. Bump `PACK_VERSION` (current: `2025.09.24d`).
6. Verify airplane mode / no `INTERNET` permission: tiles and labels still render.
7. If pack assets exceed ~15–20 MB, prefer Git LFS for `*.mbtiles`
   (`region` ~25 MB + `hillshade` ~30 MB + `bozeman` ~11 MB).

### Known detail limits

- **Bozeman / Baxter:** offline OSM extract for Gallatin Valley (z9–16); roads, parks,
  waterways, places. Use the **Bozeman** button or **My location** when testing locally.
- Shoreline sharpness (Antarctica) comes from ADD **high-res coastline** vectors.
  RAMP2 hillshade is **clipped to those land/ice polygons** and fades at higher zooms.
- Topography (Antarctica): land-clipped hillshade + ADD contours.
- Glyph folders use real spaces (`Open Sans Regular`).
- Overview place-name clip was empty; COMNAP station labels come from the vector tiles.

## License / attribution

Basemap derived from **Quantarctica** (Norwegian Polar Institute) and its included
datasets. Cite Quantarctica and each source dataset used. Show credit on the Map
screen. Individual Quantarctica layers may carry additional terms — review before
redistribution.
