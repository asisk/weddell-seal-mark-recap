# Parker 2025 Season Recap — Follow-up Plan

Source: Parker season recap conversation (audio / transcript). Use this as a living checklist for the next field season (last funded season through June 2027).

Parker’s biggest operational concern was **keeping tablets updated without internet**, not day-to-day app use. Highest-confidence app work is in **P0** and **P1**.

Status key: `[ ]` not started · `[~]` in progress / has a test · `[x]` done

---

## P0 — Data integrity bugs

### [~] Ghost data on observer / colony re-import

Re-uploading an observer list (or colony locations) kept old rows. A misspelled name stayed next to the corrected one. The only field workaround was reinstalling the app.

**Ask:** hard clear of prior rows on successful import, plus a way to correct lists in the UI (not only via reinstall).

**Started:** `ColonyObserverImportGhostDataInstrumentedTest` — import now clears prior rows before inserting a successful parse. Confirm this covers colonies and observers, and whether a user-facing “clear lists” control is still needed.

### [ ] Editing an entry creates fake untagged pups

After save, then edit, the app sometimes produced the real entry **plus** an extra untagged pup (and could repeat). Documented at least once. Deletion was backend-only.

**Ask:** stop creating the phantom pup; consider in-app delete only if we still need a safety net.

### [ ] Tag edit does not refresh Speeno unless you leave the field

Changing a tag number and saving without tapping out left Speeno stale. That produced duplicate entries that were not meant to be duplicates. Techs were told to tap out; not everyone did.

**Ask:** trigger lookup when the tag has **4 digits**, or after a short idle, or both. Parker has not seen a 3-digit tag in ~2 years. Avoid a long timeout — edits are often save-within-a-second. She is open to always-on lookup if it does not jam other work; profiling first.

### [ ] False sex-validation popups (~10 times)

“Are you sure this is a male / confirmed male” appeared when the seal was already male. Could not be reproduced. The message showed on screen but was **not saved in the export**, so notebooks had confirmation notes with nothing to match.

Suspected cause: tag/Speeno lookup if the tag was corrected quickly then saved.

**Ask:** only show confirmation against the Speeno that will actually be saved; persist the confirmation in the export when it is shown.

### [ ] Missed validation (one known case)

A female adult was entered as male with no confirmation comment and no recollection from the tech. Parker still has the record and can provide the full entry / tag number.

**Ask:** get that record from Parker and add a regression. Possible that confirmation was dismissed too fast, or Speeno lookup did not run.

### [~] White Island population / highlight logic

At White Island the app said “no population detected” (or similar). Seal lookup did not show last-seen population. GPS points were correct. Highlight logic (Erebus Bay vs other populations) seemed wrong for White Island. Parker emailed a write-up.

**Started:** `LookupCardWhiteIslandPopulationTest` — Population row disappeared when device colony matched White Island; lookup still returned population. Confirm highlight logic vs “always show last-seen population,” and revisit with Parker’s emailed notes.

---

## P1 — Field workflow that caused bad or extra data

### [ ] Edits create a new record instead of updating the original

Extra backend work (old values in comments vs changing male → female). Parker told techs: only edit if tag number, relatives/mom, or similar actually changed.

**Ask:** edit the record in place and keep the previous value in a comment, rather than a duplicate row. A separate “edits / original values” file was discussed as an alternative if she still wants an audit trail.

**Keep editable (Parker):**

- Tag event — must stay editable (new vs marked mix-ups)
- Tag number
- Number of relatives / mom
- Condition — optional; she does not care much

**Be careful with sex:** a later pee-check should be a notebook note, not a silent field change, because validation already ran. She does not want sex changed “willy-nilly.”

### [ ] `0000` Delta placeholders throw errors

Used for young pups and ~50–60 untagged adult moms this year. Not in the database / Speeno 0, so everyone got an error she told them to ignore. They will keep using this.

**Ask:** if tag is `0000` Delta, skip Speeno errors. Optional soft prompt: “Looks like this individual wasn’t tagged. Are you sure?” Never treat `0000` Delta as a real Speeno.

Related (maybe later): techs do not understand **no-tag female** vs **`0000` adult female**. They sometimes put `0000` Delta on both mom and pup when neither is tagged; Parker deletes those because they cannot be linked later. A placeholder button was discussed but she does not want to break the data book (`PHT` / similar). No clear UI solution yet.

### [ ] Retag reason not in the export / comments

Reason for retag is saved somewhere, but it does not appear in comments or where Parker looks. Techs typed it by hand. Original-tag columns still implied 2-of-4 vs 3-of-4, but not the reason.

**Ask:** write retag reason into the exported comments (or a dedicated column she can see).

### [ ] Census prefill buttons cannot be switched

Once you pick mom/pup (or similar), you cannot pick another (e.g. single) without a fight. Prefills were a huge hit otherwise.

**Ask:** allow switching. Confirm that the current entry will be discarded (“Are you sure you want to start your entry over?”). Losing prefilled fields is fine.

### [ ] Export filename is `observations`, not tablet letter

Parker typed the tablet name (e.g. **H** from the home screen) by hand every day.

**Ask:** include tablet name/letter in the export filename automatically.

---

## P2 — GPS / colony UX

GPS itself was “great” after the first week. Problems were mostly cold start and UI prominence.

### [ ] Override used too early

For the first week techs hit override as soon as they saw “seal colony not detected” (cold start or no-man’s-land). After coaching, GPS was fine. First fix ~2–3 minutes, mainly first time outside / after driving / indoors.

**Ask (pick one or combine):**

- Collapse override under colony so it is less prominent
- Confirm: “Have you left it on for ~3 minutes? Is GPS wrong?”
- Copy on override: only select colony if you still do not see one after GPS has had time

### [ ] Colony header stays on the old colony after a move

On the enter screen, the top still showed the previous colony (e.g. Hutton Cliffs) while GPS had already updated. Techs thought they were in the wrong colony. Actual save used current GPS.

**Ask:** keep the header in sync with the GPS used on save. Optional: tap colony to refresh GPS / colony; tap observers to return home and re-select. Parker also liked editing observer and colony from the tag/retag summary.

### [ ] Inconsistent lat/long precision

Sometimes 8 digits, sometimes 4. She rounded to ~5. Not a blocker.

**Ask:** consistent display precision (e.g. 5 decimal places) unless more is needed scientifically.

---

## P3 — UI / field usability

### [ ] Yellow highlights are hard to see

Lookup comments/notes (and similar yellow) fail in the field.

**Ask:** dark background + white text, matching tag/retag highlighting.

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

1. Finish / verify ghost import clear and White Island population display.
2. Speeno-on-tag-edit + phantom untagged pups (likely the same edit path).
3. Validation: false positives, the known miss (Parker’s record), persist confirmation in export.
4. `0000` Delta exception; retag reason in export; in-place edit vs duplicate records.
5. Census prefill switch; tablet letter in filename; yellow contrast; tissue placement.
6. GPS override / colony header sync.
7. File history, photo linking, confirmation banner copy.
8. Map / tracks / handover docs if time remains before the 2027 season.
