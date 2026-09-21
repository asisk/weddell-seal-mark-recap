# Parker 2025 Season Recap — Follow-up Plan

Source: Parker season recap conversation (audio / transcript). Use this as a living checklist for the next field season (last funded season through June 2027).

Parker’s biggest operational concern was **keeping tablets updated without internet**, not day-to-day app use. Highest-confidence app work is in **P0** and **P1**.

Status key: `[ ]` not started · `[~]` in progress / has a test · `[x]` done

---

## P0 — Data integrity bugs

### [x] Ghost data on observer / colony re-import

Re-uploading an observer list (or colony locations) kept old rows. A misspelled name stayed next to the corrected one. The only field workaround was reinstalling the app.

**Done:** Successful import now clears prior rows, then inserts. Covered by `ColonyObserverImportGhostDataInstrumentedTest` (observers, colonies, and after a fresh ViewModel). Failed parses leave the previous list (by design).

A dedicated “clear lists” admin button was not added; re-import is the correction path.

### [x] Editing an entry creates fake untagged pups

After save, then edit, the app sometimes produced the real entry **plus** an extra untagged pup.

**Done:** Edit write skips unused pup slots unless `hasPupOne` / `hasPupTwo` and the slot is complete. Covered by `TagRetagViewModelTest` ghost/`0000D` cases.

### [x] Tag edit does not refresh Speno unless you leave the field

Changing a tag number and saving without tapping out left Speno stale.

**Done:**
- Save / Confirm & Save already commit pending tag text and await WedCheck before validating or writing.
- Typing a **4-digit** tag now looks up Speno immediately (no blur). 3-digit tags still commit on blur or Save.
- Validation banner is not shown until that save-path lookup finishes, so a stale match cannot flash a false sex warning.

### [x] False sex-validation popups (~10 times)

“Are you sure this is a male” appeared when the seal was already male. Confirmation showed on screen but was missing from comments (only the `edt` / `flaggedEntry` column).

**Done:**
- Skip sex check when WedCheck sex is blank (`NONE`) or `UNKNOWN`.
- Message uses “Male” / “Female”, not the enum name.
- “technician confirmed” is written to **both** `flaggedEntry` and comments.
- Stale-Speno banner race on Save is fixed (see Speno item).

### [~] Missed validation (one known case)

A female adult was entered as male with no confirmation comment. Parker can still provide the full entry.

**In code:** Marked + WedCheck female + entered male now flags “Sex doesn't match … Female”. Save waits for WedCheck before deciding. Still need Parker’s record if this happens again with a real tag.

### [x] White Island population / highlight logic

At White Island the app hid last-seen population. Highlight compared colony *location* to population name, so Erebus Bay colonies never matched.

**Done (lookup):** Population row always shows. Highlight when **GPS** has a colony and that colony’s population (White Island vs Erebus Bay) differs from last-seen. No highlight while GPS has not detected a colony. Manual colony override is ignored.

**Not changed for recap (tag/retag):** Keep the existing White Island photo prompt when entering a White Island seal and GPS is not at White Island. Do not generalize lookup highlight onto the enter screen.

---

## P1 — Field workflow that caused bad or extra data

### [x] Edits create a new record instead of updating the original

Extra backend work (old values in comments vs changing male → female). Parker told techs: only edit if tag number, relatives/mom, or similar actually changed.

**Ask:** edit the record in place and keep the previous value in a comment, rather than a duplicate row. A separate “edits / original values” file was discussed as an alternative if she still wants an audit trail.

**Keep editable (Parker):**

- Tag event — must stay editable (new vs marked mix-ups)
- Tag number
- Number of relatives / mom
- Condition — optional; she does not care much

**Be careful with sex:** a later pee-check should be a notebook note, not a silent field change, because validation already ran. She does not want sex changed “willy-nilly.”

**Plan:** In-place upsert. Do **not** add a separate edits file unless Parker asks for it later. `Seal.edits()` already builds “was / now” strings; `buildObservationRecord` already appends `Edited <date> at <time>: …`. Keep that comment trail; stop creating a second row.

Current behavior (intentional “Fix #5”):

- `buildObservationRecord` always sets `id = 0`, so Room inserts.
- Edit save in `TagRetagViewModel.writeObservationRecord` writes those new rows and leaves the original.
- Adding a pup deletes the mom row and inserts mom+pup so they sort together.
- Tests lock this in: `writeObservationRecord_appendsNewRowWhenEditing`, `editedObservationCreatesNewRow`.

Implementation:

1. **Preserve identity on edit.** When `seal.hasEdits && seal.observationID != 0`, write `id = observationID` and `updatedAt = now`. Pass through original `insertedAt` (add it to `Seal` from `ObservationRecord.toSeal()`; default `0` → “now” for new rows). New saves stay `id = 0`.
2. **Edit comments without doubling prefixes.** Today a reload puts the full comments blob on `seal.comment`, then save prepends pup-peed / retag / flagged again. On edit, use the comment field as the base and append only the `Edited …` line. First save keeps the current prefix builder.
3. **Adding a pup:** stop deleting the mom row. Update mom in place; insert the new pup with `id = 0`. Grouping in Recent Observations is by relative tags, not consecutive IDs.
4. **Removing a pup:** keep deleting the pup by `observationID`; update mom in place (relatives count).
5. **Sex on edit.** Do not silently apply a sex change. If sex differs from the loaded record, require Confirm & Save (existing confirmation path) so the change is explicit and lands in comments via `edits()`. Fix `SexSection`: edit mode for a primary with a pup currently shows `ageClass` instead of sex — show sex. Leave the control enabled; Parker’s “not willy-nilly” rule is confirmation + comment, not a hard lock (a pee-check still belongs in the notebook if they choose not to change the field).
6. **Do not change** original date/time, colony, observers, GPS, or census on edit (already using `originalMetadata` / `observationLocation`).

Files: `BuildObservationRecord.kt`, `TagRetagViewModel.writeObservationRecord`, `ObservationRecord.toSeal()`, `Seal.kt` (`insertedAt`), `SexSection.kt`.

Tests: flip the two Fix #5 tests to assert same `id` and non-null `updatedAt`; add cases for add-pup (mom id kept, pup id 0), remove-pup (delete pup, update mom), edit comments (one `Edited` line, no duplicated `Reason for Retag`); sex change in edit sets `entryNeedsConfirmation`.

**Done:** Edit save upserts the existing row (`id` + `insertedAt` preserved, `updatedAt` stamped). New pups still insert with `id = 0`; adding a pup no longer deletes mom. Removing a pup deletes that row and updates mom’s relatives in place. Comment-prefix doubling was already fixed. Sex stays editable (including mom with a pup); changing it vs the loaded record requires Confirm & Save with pee-check/notebook copy. Filling in sex on a newly added pup does not. Confirm & Save does not set `flaggedEntry` unless validation actually failed; previous values still land in comments via `edits()`.

Covered by `BuildObservationRecordTest`, `TagRetagViewModelTest`, `SealTest`, and `SealCardTest`.

### [x] `0000` Delta placeholders throw errors

Used for young pups and ~50–60 untagged adult moms this year. Not in the database / Speno 0, so everyone got an error she told them to ignore. They will keep using this.

**Done:** Tag `0000` Delta (`isDummyTag`) skips WedCheck lookup and Speno / tag-number validation. Save does not prompt “Seal not in database!”. Covered by `SealTest` and `TagRetagViewModelTest` (`attemptSave_markedDummyTag0000D_savesWithoutConfirmation`).

Optional soft prompt (“Looks like this individual wasn’t tagged. Are you sure?”) was not added.

Related (maybe later): techs do not understand **no-tag female** vs **`0000` adult female**. They sometimes put `0000` Delta on both mom and pup when neither is tagged; Parker deletes those because they cannot be linked later. A placeholder button was discussed but she does not want to break the data book (`PHT` / similar). No clear UI solution yet.

### [x] Retag reason not in the export / comments

Reason for retag is saved somewhere, but it does not appear in comments or where Parker looks. Techs typed it by hand. Original-tag columns still implied 2-of-4 vs 3-of-4, but not the reason.

**Done:** A selected retag reason is appended to comments (`Reason for Retag: …`) and exported with that comments field. NONE / UNKNOWN are omitted. Covered by `BuildObservationRecordTest` (`retagWithSelectedReasonIncludesReasonInComments`). The dedicated `retag_reason` DB column is still not a CSV column; Parker’s proofing path is comments.

### [x] Census prefill buttons cannot be switched

Once you pick mom/pup (or similar), you cannot pick another (e.g. single) without a fight. Prefills were a huge hit otherwise.

**Done:** Tapping a different prefill asks “Are you sure you want to start your entry over?” Confirm discards the current entry (tags, age, sex, relatives) and applies the new prefill. Census number, observers, colony, and GPS are kept. Same-prefill tap is a no-op. Blank form applies immediately with no dialog. Prefills stay hidden in edit mode.

Covered by `TagRetagViewModelTest` (blank form, same-prefill no-op, mom/pup → single male after confirm), `TagRetagHeaderTest` (dialog copy / confirm / cancel), and `SealCardTest` (`fieldResetCounter` remounts local age/sex/relatives).

### [x] Export filename is `observations`, not tablet letter

Parker typed the tablet name (e.g. **H** from the home screen) by hand every day.

**Ask:** include tablet name/letter in the export filename automatically.

**Plan:** The CreateDocument launcher currently suggests `observations_<yyyyMMdd_HHmmss>.csv` and `all_observations_<yyyyMMdd_HHmmss>.csv`. Device name is already on the home screen via `getDeviceName()` (`Settings.Global.DEVICE_NAME`) and is stored on each observation as `deviceID`.

Implementation:

1. Add a small pure helper, e.g. `observationExportSuggestedName(deviceName, fileDate, allRecords)` next to `getFileExportDateTime()`.
2. Sanitize the device name for a filename: keep letters, digits, `.`, `_`, `-`; replace other characters with `_`; collapse repeats; trim. If the result is empty or `Unknown Device`, fall back to `tablet`.
3. Suggested names: `observations_H_20260819_155900.csv` and `all_observations_H_20260819_155900.csv` (letter after `observations`, date kept). The system save dialog can still rename; we only change the default.
4. Wire it in `ExportObservations` `LaunchedEffect(Unit)` using `LocalContext` + `getDeviceName`. No DB or export-content change.

Files: `DateFormatting.kt` (or a tiny `ExportFileNames.kt`), `ExportObservations.kt`, unit test for the helper (spaces, punctuation, empty, `allRecords`).

Do not change archive filenames or import filename checks (`observers`, `Colony_Locations`, `WedCheck`).

**Done:** CreateDocument now suggests `observations_<device>_<yyyyMMdd_HHmmss>.csv` (and `all_observations_…` for export all). Device name comes from `getDeviceName()` (home-screen tablet letter). Empty / `Unknown Device` / punctuation-only names fall back to `tablet`. Covered by `ExportFileNamesTest`. Archive and import filenames are unchanged.

---

## P2 — GPS / colony UX

GPS itself was “great” after the first week. Problems were mostly cold start and UI prominence.

### [ ] Override used too early / GPS slow on cold start

For the first week techs hit override as soon as they saw “seal colony not detected” (cold start or no-man’s-land). After coaching, GPS was fine. First fix ~2–3 minutes, mainly first time outside / after driving / indoors.

**Ask:** make override harder to use as the first reaction, and get a live fix in front of the technician as soon as the hardware allows. They must still be able to pick a colony by hand when GPS is genuinely wrong or they are between colonies.

**Decide (recommended combo):**

1. **Start GPS as soon as the app opens** (permissions already granted), not when Home composes.
2. **Honest acquiring copy** — “GPS often takes 2–3 minutes the first time outside or after driving. Wait before overriding.”
3. **Quieter Override** — not a checkbox in the main Colony row; a secondary control under the colony text.
4. **Confirm before override** — “Have you left GPS on for about 3 minutes? Only override if the colony is still missing or wrong.”
5. **Last known location is display-only.** Never use it to auto-detect colony or to save. That is the stale-colony bug.

Do **not** add a foreground service, background location, or coarse/network priority. No internet in the field, so assisted GPS will not help. Colony boxes need a precise fix. Cold start will still take ~2–3 minutes; this plan shaves app delay and stops early override.

Current behavior:

- `HomeScreen` `ON_RESUME` → `HomeViewModel.onPermissionsResult(true)` → `startLocationUpdates()`.
- `FusedLocationSource` uses `PRIORITY_HIGH_ACCURACY`, 5s interval, `maxUpdateDelayMillis(5000)`, then the UI flow `sample(2000L)`.
- `getLastLocation()` is unused. `requestSingleUpdate()` exists and is never called.
- Colony row always shows an **Override** checkbox next to Colony. One tap unlocks the dropdown. While waiting: `…detecting proximity to a known colony…`. After a fix with no bounding-box hit: `Seal colony not detected`.
- Auto-detect writes a dummy colony with `ColonyPopulation.NOT_DETECTED` so `autoDetectedColony` is only null before the first live fix.
- Tag/Retag header does its own `LaunchedEffect(currentLocation)` keyed on the StateFlow **object**, so the name can stay on the previous colony after a move. Save already uses current GPS.

Implementation:

1. **Start GPS at process UI, not Home composition.** If `locationPermissionsGranted()`, call `homeViewModel.onPermissionsResult(true)` from `MainActivity.onCreate` (after the ViewModel exists). Keep Home `ON_RESUME` as a no-op if updates are already running (`isUpdating` already guards this). Still start from Home if the user grants permission on the disclosure screen then lands on Home. Do not stop GPS when leaving Home (already the case).

2. **Faster first live fix (seconds, not the satellite wait).**
   - On start: `getLastLocation()` for display only (see 3), plus `getCurrentLocation(PRIORITY_HIGH_ACCURACY)` (`requestSingleUpdate`) in parallel with `requestLocationUpdates`.
   - LocationRequest: keep high accuracy; `setIntervalMillis(1000)` (or `setMinUpdateIntervalMillis(0)`); drop or raise `setMaxUpdateDelayMillis` so the first update is not held for 5s.
   - Skip `sample(2000L)` until the first live fix has been delivered; sampling after that is fine.
   - Keep `distinctUntilChanged` at 0.5m.

3. **Cached vs live location.** Add something like `isLiveFix: Boolean` on the location the UI reads (or a small `LocationUiState`: `None` / `Cached(geo)` / `Live(geo)`).
   - Cached: show coordinates on `DeviceGPSRow` with copy like “Last known (not current)” and the gray/off location icon. Do **not** call `updateColonyForLocation`. Do **not** use in `getColonyLocation()` or Tag/Retag save.
   - First live fix: replace cached, green icon, run colony detect as today.
   - `GeoLocation.fromFusedLocation` should keep the provider timestamp when we have one (`location.time`), not always `now`, so cached age is honest.

4. **Colony row: wait copy + quieter override + confirm.**
   - Primary row: Colony label + auto-detected name, or acquiring copy (not the Override checkbox).
   - Acquiring (no live fix yet): “Waiting for GPS. This often takes 2–3 minutes the first time outside or after driving.”
   - Live fix, no colony: keep `Seal colony not detected` and the same wait/override hint.
   - Secondary control: text button “Override colony” under that text. Checking it shows a dialog (do **not** reuse `RemoveDialog` as-is — it always appends “Are you sure?”). Cancel / Override. Confirm sets `overrideColony = true` and shows the dropdown.
   - Already overriding: dropdown stays; “Use GPS” turns override off with **no** confirm and `clearColony()`.
   - Do not confirm when turning override off. Do not confirm if override is already on.

5. **Tag/Retag header uses the same live colony as save.** Collect `homeViewModel.autoDetectedColony` (and `overrideColony` / `selectedColony`) instead of a one-shot `LaunchedEffect(currentLocation)`. Header must not show a cached last-known colony. This is the P2 “header stays on the old colony” bug; do it in the same change so last-known cannot leak into the enter screen.

6. **Do not change** colony bounding-box SQL, White Island lookup highlight (already ignores override), save metadata when override is on, or battery/GPS-off workflow.

Files: `MainActivity.kt`, `FusedLocationSource.kt`, `LocationSource.kt` (optional `lastKnownLocation()`), `GeoLocation+FusedLocations.kt`, `HomeViewModel.kt`, `ColonyRow.kt`, `DeviceGPSRow.kt`, `TagRetagAppBar.kt`, new small confirm dialog (or `RemoveDialog` with optional “Are you sure?”), `FakeLocationSource.kt`.

Tests:

- `HomeViewModel`: grant permission starts updates; cached location does not set `autoDetectedColony`; live location does; `getColonyLocation()` ignores cached; override off still uses live GPS.
- `FusedLocationSource` / helper: first emission is not delayed by `sample(2000)` (pure helper if the sample/skip is extracted).
- Compose: ColonyRow — Override is not a main-row checkbox; acquiring copy visible; Override button opens confirm; Cancel leaves GPS mode; confirm shows dropdown; Use GPS clears override.
- `TagRetagAppBar`: header follows `autoDetectedColony` after a second live colony (not stuck on the first).
- Keep existing `onPermissionsResult_startsAndStopsLocationUpdates` / `onCleared_stopsLocationUpdates`.

### [ ] Colony header stays on the old colony after a move

On the enter screen, the top still showed the previous colony (e.g. Hutton Cliffs) while GPS had already updated. Techs thought they were in the wrong colony. Actual save used current GPS.

**Ask:** keep the header in sync with the GPS used on save. Optional (later): tap colony to refresh GPS / colony; tap observers to return home and re-select. Parker also liked editing observer and colony from the tag/retag summary.

**Plan:** Folded into the GPS cold-start / override item (step 5). Header collects live `autoDetectedColony` (or override selection). Do not add tap-to-refresh or in-header observer/colony edit in this drop.

### [x] Inconsistent lat/long precision

Sometimes 8 digits, sometimes 4. She rounded to ~5. Not a blocker.

**Ask:** consistent display precision (e.g. 5 decimal places) unless more is needed scientifically.

**Done:** Device GPS, colony coordinates, lookup lat/long, the edit header, recent observations, saved records, and CSV export all use five decimal places (~1 m). Extra GPS digits are rounded. Blank or non-numeric stored values are left as-is.

---

## P3 — UI / field usability

### [x] Yellow highlights are hard to see

Lookup comments/notes (and similar yellow) fail in the field.

**Ask:** dark background + white text, matching tag/retag highlighting.

**Done:** Lookup notes, tissue Need, Dead, and tag/retag WedCheck comments use black background and white text (`FieldHighlight`), matching selected tag/retag buttons. Covered by `LookupCardHighlightTest`, `TabbedCardsTest`, `SealLookupHighlightInstrumentedTest`, and `TagRetagWedCheckCommentHighlightInstrumentedTest`. Population mismatch / White Island photo / save-validation banners are still the old yellow (different warning treatment).

### [ ] Tissue control is too easy to hit

It sits on the path to Save and got tapped by accident.

**Ask:** move comments and tissue off the direct path to Save (side of the screen).

### [ ] Confirmation without a notebook note

Techs confirm and later say they “just agreed with the data.” Forced in-app notes are too slow.

**Ask:** a line on the confirm/save banner: if confirming, write a note in the book.

### [ ] Photos are disconnected from records

Techs took photos on phones and never handed them over; or took them on the tablet and did not say so. Camera quality was good.

**Ask:** link the photo to the record; clearer in-app prompt to take the photo on the tablet (especially for messy tags). Parker should be able to see that a photo exists without hunting the gallery.

### [ ] Export vs archive is easy to mix up

She hits Export when she means Archive. Workflow is export → import/check → archive later (do not delete until data is confirmed). She sometimes forgot to archive, which hurt proofing.

**Ask:** no strong change yet. Optional visual cue / short instructions on the admin page. Date-range export was discussed; she never had to use “export all” as a recovery path. Training data is easy to delete on the backend. Revisit after she decides whether date-range export would help.

### [ ] Last imported screen is import-only

For Nate (and anyone less used to the process), import/export/archive gaps are easy to miss.

**Ask:** recent activity / file history: import, export, and archive (another row with icons is fine).

### [ ] Admin Home tap is easy to hit by mistake

Late at night she hit Home when she did not mean to. Recoverable; low priority.

### [ ] No in-app way to turn a tagged animal into untagged

One infected tag had to be removed after gear was already returned; they had to go back out. Rare.

**Ask:** a note-driven workflow to mark as untagged is probably enough. Do not over-build.

---

## Ops / hardware (not app code)

These are Parker’s largest worries. Track here so they are not lost; they may not be tickets in this repo.

- [ ] Tablets have no internet. OS updates go through **Samsung Smart Switch**, which is glitchy (unclear success, sometimes needs reboot). It worked this season; longevity is the concern.
- [ ] Shipping tablets home each year is bad (lithium batteries, individual shipping, vessel timing → ~one month of update window). USB OS update would be better if it exists.
- [ ] New APK was easy: load one tablet, Bluetooth the rest. Risk is **APK vs Samsung OS drift**. Lean toward updating OS; test on one of the extra tablets first (they have 10, typically used 6–8).
- [ ] Starlink is not a real fallback: 2.5 GB/week, effectively less because of background/government traffic; flaky. Worst case she downloaded at home/dorm and transferred.
- [ ] McMurdo IT can sometimes put tablets on the network; adds complication. Keep as last resort.
- [ ] Batteries lasted all day at 80% storage, devices off. No backup battery needed. Assume they will degrade; not urgent.

---

## Wishlist (after P0–P2)

Nice-to-haves Parker and Annie discussed. Do not start until integrity and field-workflow bugs are settled.

- Map of GPS vs colonies; dots for tagged individuals; filters (pups tagged this year, animals that need retag, census density). Historic 5-year pup counts by colony would need extra files.
- Tap colony to refresh GPS; optional “confirmed” checkmark.
- Survey-route design using last-census GPS points (people learn colony names faster from bounding boxes).
- Track line of where the snowmobile/group has been; study-area / ice-edge boundaries (shapefile or georeferenced layer — likely not in-house).
- In-app documentation of validation logic for whoever takes the project over.

---

## What worked (do not break)

Keep these stable unless a P0/P1 fix requires a change.

- APK sideload + Bluetooth to the rest of the fleet
- GPS after cold start; batteries all day
- Observer initials
- Tag / retag (~200 adult retags, tissue lookup, no backend cleanup)
- Pup tabs / second pup / twins
- Dead pup re-sight with no extra message
- Census number + prefill buttons (huge hit; one group missed the census button on census 1 because they were not told to tap it)
- Recent observations (primary during census); View used by Parker only
- Import UI and last-imported list
- Easy to train; fewer entry mistakes than paper

---

## Suggested order of work

1. ~~Finish / verify ghost import clear and White Island population display.~~
2. ~~Speno-on-tag-edit + phantom untagged pups.~~
3. ~~Validation: false positives, persist confirmation in export.~~ Missed-validation: still collect Parker’s record if it happens again.
4. ~~`0000` Delta exception; retag reason in export.~~ ~~In-place edit vs duplicate records.~~
5. ~~Census prefill switch.~~ ~~Tablet letter in filename;~~ ~~yellow contrast;~~ tissue placement.
6. GPS cold start (start earlier, acquiring copy) + quieter override with confirm; colony header follows live GPS.
7. File history, photo linking, confirmation banner copy.
8. Map / tracks / handover docs if time remains before the 2027 season.

---

## Next release — Parker-facing list

Use this as the talking list for what the next APK will include. Ops/hardware and the map wishlist stay out of this drop.

- **Edits update the original record** instead of creating a second copy. Previous values stay in comments (tag event, tag number, relatives/mom, condition). Sex change still requires Confirm & Save so it is not silent.
- **Export filename includes the tablet letter** automatically (e.g. `observations_H_20260825_155900.csv`), so you do not have to type it by hand.
- **Harder-to-miss highlights** on lookup notes, tissue Need, Dead, and WedCheck comments: dark background with white text, matching tag/retag buttons.
- **Tissue and comments moved off the path to Save** so they are not tapped by accident.
- **GPS cold start + colony override.** Start GPS when the app opens. Show “waiting, often 2–3 minutes the first time.” Override is a secondary control with a confirm (“have you waited ~3 minutes?”). Last known coordinates may display as cached; they are never used as the colony that is saved. The enter-screen colony name stays in sync with live GPS.
- **Consistent lat/long precision** (about 5 decimal places).
- **Confirm & Save reminder:** if you confirm a mismatch, write a note in the notebook.

Still worth a yes/no with Parker (not assumed in this drop):

- Last Imported screen also lists exports and archives (for Nate).
- Clearer visual distinction between Export and Archive.
- Photos attached to the record (in-app camera was removed; this is a larger build).
- One-off “mark as untagged” after a tag is pulled — notebook note vs in-app workflow.
