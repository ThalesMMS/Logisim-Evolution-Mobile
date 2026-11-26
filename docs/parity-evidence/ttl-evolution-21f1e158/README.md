# TTL pinouts and internal structure compared with Evolution 21f1e158 (#381, #382)

Mobile follows two Evolution changes made after the pinned `d58a61e0`:

- #381: the 74266 ([PR 3209](https://github.com/logisim-evolution/logisim-evolution/pull/3209)) and 7476 ([PR 3201](https://github.com/logisim-evolution/logisim-evolution/pull/3201)) pinouts, corrected to their datasheets, with the pin names those PRs and [PR 3210](https://github.com/logisim-evolution/logisim-evolution/pull/3210) (74194) introduce.
- #382: the internal structure of the 15 models redrawn by PRs 3170 to 3220: 7447, 7476, 7485, 7487, 7493, 74123, 74138, 74161, 74163, 74194, 74266, 74283, 74365, 74390 and 744017.

[`docs/ttl-validation.md`](../../ttl-validation.md#pinouts-after-the-pin) describes the pinouts and the effect on saved circuits.

- Mobile: `1579fdd4e4ad091a4223675a5df9b38fd553f8e3` (`logs/mobile-revision.txt`), clean worktree apart from this directory (`logs/worktree-status.txt`).
- Evolution `21f1e15829e550c491d75953f40484be2ed31c66`: `logisim-evolution-5.1.0dev-all.jar` built from that revision with `./gradlew shadowJar`, SHA-256 `806c030e4000809acf1ec4db043d51c027e2f297643e26413808d863898f36d7`.
- Evolution `d58a61e0c290361722b43df5b1a6b86c1f7c7f0d` (the pin): JAR built the same way on this machine, SHA-256 `5c0c89b426578ffa6988244d477eace70bbaeaf09687890e893f6c258194695a`. It differs from the macOS build recorded elsewhere (`d0b287d1…`), which was not available.
- Environment: Windows 11, Git Bash, Temurin JDK 21.0.12.1. JVM and host tests only; no emulator or device.

## Commands

```sh
scripts/oracle/ttl/run.sh EVOLUTION_21F1E158_JAR
scripts/oracle/ttl/run.sh EVOLUTION_D58A61E0_JAR --no-build
EVOLUTION_REVISION=21f1e158 REDRAWN="7447 7476 7485 7487 7493 74123 74138 74161 74163 74194 74266 74283 74365 74390 744017" \
    scripts/oracle/ttl/run_internal.sh EVOLUTION_21F1E158_JAR
```

## Results

| Run | Result |
| --- | --- |
| `run.sh` against `21f1e158` | 444,000 combinational and 95,232 sequential values and delays, no mismatch; the extracted pin metadata equals `TtlCatalog.kt` ([log](logs/run-21f1e158.txt), [metadata](oracle-21f1e158/metadata.tsv)) |
| `run.sh` against `d58a61e0` | 966 combinational mismatches, all on the 74266, and 1,022 sequential mismatches, all on the 7476 ([combinational](oracle-d58a61e0/combinational-result.txt), [sequential](oracle-d58a61e0/sequential-result.txt), [log](logs/run-d58a61e0.txt)). These are the corrected pinouts; the other 83 models match. `run.sh` stops after the combinational mismatch, so the sequential comparison against the pin was run on its own with the same `TtlMobileCompare`. |
| `run_internal.sh` against `21f1e158` | Generated data up to date; 85/85 ANSI, 30/30 IEC and 18/18 state rasters pass ([log](logs/run-internal-21f1e158.txt), [results](internal/)) |
| `:core:jvmTest --tests '*TtlInternal*'`, `:composeApp:jvmTest --tests '*TtlInternalStructureHostTest'` | Pass, within `run_internal.sh` |

`internal/*/composites/` keeps the redrawn models: Evolution on the left, Mobile in the middle, and the overlay (black in both, red Evolution only, blue Mobile only). `internal/*/results.tsv` lists all models.

## Recording

The internal-structure data comes from two recordings. The 15 redrawn models are recorded here from the `21f1e158` JAR. The other 70 keep the entries recorded on macOS from the pinned JAR for #374 ([evidence](../ttl-internal-structure-bd0e4e77/README.md)), because Evolution's text anchors depend on the platform's Java font metrics: regenerating all 85 models on Windows moved centred labels by half a unit (for example the 7400 GND label from x 130 to 130.5) without any drawing change. `generate_internal.py --base --only` merges them, and the header of `TtlInternalStructureData.kt` names both sources. Between `d58a61e0` and `21f1e158`, Evolution changed only these 14 classes under `std/ttl`, so the other entries stay valid.

At `21f1e158` the 7476, 7493, 74161, 74163 and 74390 no longer paint register value dots, and the 7493, 74266 and 74365 draw differently with IEC gates. Mobile follows both.

## Open items

- A run of `run_internal.sh` with `REDRAWN` on macOS will report the 15 entries as stale, since their anchors were measured on Windows. Regenerate them there with `UPDATE=1`.
- The temporal sweep rows of the 74266 and 7476 against the pin are in [evolution-followups-d58a61e0](../evolution-followups-d58a61e0/README.md). The full `temporal-kinds` receipt still names the macOS JAR, so the `TTL/*` rows keep their recheck flag until that campaign runs again.
