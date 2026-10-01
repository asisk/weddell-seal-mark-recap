# Basemap layer → offline MapLibre pack

This guide turns the QGIS **Basemap** layers (clipped Quantarctica) into the files the
Android app reads under `app/src/main/assets/map/`.

The app does **not** open GeoPackages or EPSG:3031 GeoTIFFs directly. Everything is
baked into:

| App file | Contents |
|----------|----------|
| `region.mbtiles` | Vector tiles (Mapbox PBF) for coastline, rock, moraines, lakes, contours, graticule, COMNAP |
| `hillshade.mbtiles` | Raster PNG tiles (land-clipped RAMP2 hillshade, EPSG:3857) |
| `style.json` | MapLibre style pointing at those MBTiles via `mbtiles://__PACK_ROOT__/…` |
| `glyphs/`, `icons/` | Labels + COMNAP symbol |
| `PACK_VERSION` | Bump whenever any of the above changes |

Colony boxes and GPS overlays are drawn by the app at runtime; they are **not** part
of this pack.

---

## Prerequisites

- QGIS project with clipped layers (e.g. `Desktop/map_pack_large_clipped/`)
- GDAL (`ogr2ogr`, `gdalwarp`, `gdal_translate`, `gdal2tiles.py`)
- [`tippecanoe`](https://github.com/felt/tippecanoe)
- Repo paths used below:

```bash
REPO=…/weddell-seal-android
BUILD=$REPO/map_pack_build
GEO=$BUILD/geojson
RASTER=$BUILD/raster
ASSETS=$REPO/app/src/main/assets/map
CLIPPED=~/Desktop/map_pack_large_clipped   # adjust if needed
```

### Locked geographic envelope

Match `app/src/main/assets/map/README.md` (colony CSV extent):

- **Lat:** [-78.08, -74.53]
- **Lon:** [163.07, 168.13]

Typical polar-stereographic clip window used for this pack (EPSG:3031):

```text
-projwin -96903.8777 -1048096.1223 843096.1223 -1829596.1223
```

---

## Layer checklist (QGIS panel order)

Process layers in the order they appear under **Basemap**. Each section states:

1. What the clipped source file is
2. How to convert it
3. Which `style.json` / tippecanoe layer name it becomes
4. Whether the **current** app style uses it

Shared vector export (EPSG:4326 GeoJSON — required for tippecanoe):

```bash
ogr2ogr -overwrite -f GeoJSON -t_srs EPSG:4326 \
  "$GEO/<name>.geojson" \
  "$CLIPPED/<clipped-source>.gpkg"
```

Preserve attribute names the style filters on (`SURFACE`, `cnt01hgt`, `HEIGHT`,
`name_eng`, etc.). Do not rename fields unless you also update `style.json`.

---

### 1. COMNAP listed facilities

| | |
|--|--|
| Source | `clipped_COMNAP listed facilities.gpkg` (points) |
| Export | `$GEO/comnap.geojson` |
| Tippecanoe layer | `comnap` |
| App layers | `comnap-facilities`, `comnap-labels` |

Needs `name_eng` (or `name_off`) for labels and the `research-station` icon in
`assets/map/icons/`.

```bash
ogr2ogr -overwrite -f GeoJSON -t_srs EPSG:4326 \
  "$GEO/comnap.geojson" \
  "$CLIPPED/clipped_COMNAP listed facilities.gpkg"
```

---

### 2–3. 15-min latitude + 30-min longitude

| | |
|--|--|
| Source | `clipped_15-min latitude.gpkg`, `clipped_30-min longitude.gpkg` |
| Export | Combined `$GEO/graticule.geojson` |
| Tippecanoe layer | `graticule` |
| App layers | `graticule`, `graticule-labels` |

The style expects one layer with properties:

- `kind`: `parallel` or `meridian`
- `label`: display string (e.g. `76.35°S`, `165.8°E`)
- `value`: numeric degrees

Export both grids to GeoJSON, then merge/normalize into that schema (see existing
`$GEO/graticule.geojson` as the template). Tippecanoe layer name must stay
`graticule`.

---

### 4. RAMP2 Hillshade *(primary topography raster)*

| | |
|--|--|
| Source | `_clippedRAMP2 Hillshade.tif` (Byte, EPSG:3031) |
| Build | Warp → land-clip → RGBA → tiles → `$RASTER/hillshade.mbtiles` |
| App source | `hillshade` → layer `hillshade` |

This is the hillshade the shipped style uses. Steps:

**A. Warp to Web Mercator**

```bash
gdalwarp -overwrite -t_srs EPSG:3857 -r bilinear \
  -co COMPRESS=DEFLATE -co TILED=YES \
  "$CLIPPED/_clippedRAMP2 Hillshade.tif" \
  "$RASTER/hillshade_3857.tif"
```

**B. Hard-clip to ADD high coastline polygons** (land / ice shelf / ice tongue only —
ocean becomes transparent)

```bash
# Cutline from coastline polygons already exported as GeoJSON (step 10)
ogr2ogr -t_srs EPSG:3857 -f GPKG -nlt MULTIPOLYGON -nln coastline_mask \
  "$RASTER/coastline_mask_3857.gpkg" "$GEO/coastline_high_poly.geojson"

ogr2ogr -overwrite -f GPKG -nln coastline_mask -dialect sqlite \
  -sql "SELECT ST_UnaryUnion(ST_Collect(geom)) AS geom FROM coastline_mask" \
  "$RASTER/coastline_mask_dissolved_3857.gpkg" "$RASTER/coastline_mask_3857.gpkg"

gdalwarp -overwrite -r bilinear \
  -cutline "$RASTER/coastline_mask_dissolved_3857.gpkg" -cl coastline_mask \
  -crop_to_cutline -wo CUTLINE_ALL_TOUCHED=FALSE \
  -dstnodata 255 \
  -co COMPRESS=DEFLATE -co TILED=YES \
  "$RASTER/hillshade_3857.tif" "$RASTER/hillshade_land_3857.tif"
```

**C. Gray → RGBA** (alpha = 0 where NoData) so MapLibre shows ocean as transparent.
Use a short Python/GDAL script writing `$RASTER/hillshade_land_rgba_3857.tif`
(see prior build artifact of that name).

**D. Tile and pack MBTiles (z5–12, XYZ → TMS Y for MBTiles)**

```bash
rm -rf "$RASTER/hillshade_tiles"
gdal2tiles.py --xyz --tilesize=256 --zoom=5-12 -r near --processes=4 \
  "$RASTER/hillshade_land_rgba_3857.tif" "$RASTER/hillshade_tiles"

# Pack XYZ tiles into MBTiles (flip Y to TMS) — reuse the packer from the last
# successful build, or gdal_translate -of MBTILES if the RGBA GeoTIFF tiles cleanly:
# gdal_translate -of MBTILES -co TILE_FORMAT=PNG \
#   "$RASTER/hillshade_land_rgba_3857.tif" "$RASTER/hillshade.mbtiles"
```

Copy the result to assets:

```bash
cp "$RASTER/hillshade.mbtiles" "$ASSETS/hillshade.mbtiles"
```

---

### 5. RAMP2 Hillshade 2× v. exag.

| | |
|--|--|
| Source | `_clippedRAMP2 Hillshade 2x v. exag..tif` |
| App | **Not used** by current `style.json` |

Optional alternate exaggerated relief. Same pipeline as step 4 if you switch the
hillshade source; otherwise skip.

---

### 6. Coastlines *(line)*

| | |
|--|--|
| Source | High-res ADD coastline **line** GeoPackage (e.g. `clipped_ADD_Coastlines__high__….gpkg` MultiLine) |
| Export | `$GEO/coastline_high_line.geojson` |
| Tippecanoe layer | `coastline_high_line` |
| App layer | `coastline-shore-line` (minzoom 8) |

Keep `SURFACE` (`grounding line`, `ice shelf`, …).

---

### 7. Contours

| | |
|--|--|
| Source (med) | `clipped_ADD v6 Contours (med).gpkg` → `$GEO/contours_med.geojson` |
| Source (low) | `clipped_ADD Contours (low, 1000m).gpkg` → `$GEO/contours_low.geojson` |
| Tippecanoe | `contours_med`, `contours_low` |
| App layers | `contours-med`, `contours-low`, `contour-labels` |

- Med contours must keep **`cnt01hgt`** (style indexes every 400 m for emphasis/labels).
- Low contours must keep **`HEIGHT`**.

If the QGIS panel only shows one Contours layer, still export **both** low and med
clips for the pack (low for z5–10, med from z8 up).

---

### 8. Moraines

| | |
|--|--|
| Source | `clipped_ADD Moraines (med).gpkg` |
| Export | `$GEO/moraines_med.geojson` |
| Tippecanoe layer | `moraines_med` |
| App layer | `moraines-fill` |

---

### 9. Rock_outcrop

| | |
|--|--|
| Source | `clipped_ADD Rock outcrop (med).gpkg` (or high if regenerating detail) |
| Export | `$GEO/rock_med.geojson` |
| Tippecanoe layer | `rock_med` |
| App layer | `rock-fill` |

---

### 10. Coastlines *(polygon)*

| | |
|--|--|
| Source (high) | High-res ADD coastline **polygon** GeoPackage |
| Source (med) | `clipped_ADD Coastlines (med).gpkg` |
| Export | `$GEO/coastline_high_poly.geojson`, `$GEO/coastline_med.geojson` |
| Tippecanoe | `coastline_high_poly`, `coastline_med` |
| App layers | `coastline-fill-high` (z≥9), `coastline-fill-med` (z≤10) |

Keep `SURFACE` values: `land`, `ice shelf`, `ice tongue`, ….

`coastline_high_poly` is also the **cutline** for the RAMP2 hillshade (step 4B).
Export polygons **before** finishing the hillshade land-clip if rebuilding both.

---

### 11–12. ETOPO1_IBCSO_RAMP2 Hillshade (5× low) / (5× high)

| | |
|--|--|
| Source | `clipped_ETOPO1_IBCSO_RAMP2 Hillshade (5x v. exag.) (low).tif`, `_clippedETOPO1_…_high….tif` |
| App | **Not used** by current `style.json` |

The shipped pack uses **RAMP2** hillshade (step 4), not ETOPO1 hillshades. Keep these
clips for QGIS reference or as a future alternate raster source.

---

### 13. Lakes

| | |
|--|--|
| Source | `clipped_ADD Lakes (high).gpkg` |
| Export | `$GEO/lakes_high.geojson` |
| Tippecanoe layer | `lakes_high` |
| App layer | `lakes-fill` |

---

### 14. ETOPO1_IBCSO_RAMP2 Elevation model

| | |
|--|--|
| Source | `clipped_ETOPO1_IBCSO_RAMP2 Elevation model.tif` (**Int16** meters, NoData `32767`) |
| App | **Not used** by current `style.json` |

Elevation is for QGIS / analysis only today. Topography in the app comes from
**hillshade + contours**.

If you later add a colored DEM:

1. Keep Int16 (do not export Byte / render).
2. Clip from the full Quantarctica `ETOPO1_IBCSO_RAMP2_2000m.tif`, not a rendered image.
3. Warp to EPSG:3857, style to RGBA (or pre-render), tile like hillshade, add a
   raster source + layer in `style.json`.

After reclips in QGIS, **remove and re-add** the layer (or Change Data Source) so
cached raster dimensions do not cause `RasterIO() … Access window out of range`.

---

## Build `region.mbtiles` (all vectors)

Once GeoJSONs for every **used** layer exist under `$GEO/`:

```bash
tippecanoe -o "$BUILD/region.mbtiles" --force \
  --minimum-zoom=5 --maximum-zoom=14 \
  --drop-densest-as-needed --extend-zooms-if-still-dropping \
  -L coastline_med:$GEO/coastline_med.geojson \
  -L coastline_high_poly:$GEO/coastline_high_poly.geojson \
  -L coastline_high_line:$GEO/coastline_high_line.geojson \
  -L rock_med:$GEO/rock_med.geojson \
  -L lakes_high:$GEO/lakes_high.geojson \
  -L contours_low:$GEO/contours_low.geojson \
  -L contours_med:$GEO/contours_med.geojson \
  -L moraines_med:$GEO/moraines_med.geojson \
  -L graticule:$GEO/graticule.geojson \
  -L comnap:$GEO/comnap.geojson

cp "$BUILD/region.mbtiles" "$ASSETS/region.mbtiles"
```

`-L <name>:file` **must** match `source-layer` names in `style.json`.

---

## Install into the app

1. Copy updated `region.mbtiles` and/or `hillshade.mbtiles` into `$ASSETS/`.
2. Bump `$ASSETS/PACK_VERSION` (e.g. `2025.10.01a` → next stamp) so the app
   re-copies the pack to `filesDir/map/` on next Map open.
3. Keep `style.json` free of `http(s):` glyph/sprite/tile URLs.
4. Verify on device/emulator in airplane mode (app has no `INTERNET` permission for tiles).

See also `app/src/main/assets/map/README.md` for pack layout, zoom ranges, and
attribution.

---

## Quick reference: QGIS layer → pack artifact

| # | QGIS Basemap layer | Intermediate | In app? |
|---|--------------------|--------------|---------|
| 1 | COMNAP listed facilities | `geojson/comnap.geojson` → `region.mbtiles` | Yes |
| 2 | 15-min latitude | → `graticule.geojson` | Yes |
| 3 | 30-min longitude | → `graticule.geojson` | Yes |
| 4 | RAMP2 Hillshade | → `hillshade.mbtiles` | Yes (primary) |
| 5 | RAMP2 Hillshade 2× | (optional alt hillshade) | No |
| 6 | Coastlines (line) | `coastline_high_line.geojson` | Yes |
| 7 | Contours | `contours_med` + `contours_low` | Yes |
| 8 | Moraines | `moraines_med.geojson` | Yes |
| 9 | Rock_outcrop | `rock_med.geojson` | Yes |
| 10 | Coastlines (polygon) | `coastline_high_poly` + `coastline_med` | Yes (+ hillshade cutline) |
| 11 | ETOPO1 Hillshade 5× (low) | — | No |
| 12 | ETOPO1 Hillshade 5× (high) | — | No |
| 13 | Lakes | `lakes_high.geojson` | Yes |
| 14 | ETOPO1 Elevation model | Int16 GeoTIFF only | No (QGIS only) |

---

## License / attribution

Basemap derived from **Quantarctica** (Norwegian Polar Institute) and included
datasets (ADD, RAMP2, ETOPO1/IBCSO, COMNAP, …). Cite Quantarctica and each source
before redistribution. Credit is shown on the Map screen in-app.
