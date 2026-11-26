# Filtered temporal sweep after the Evolution follow-ups (#380, #381)

`receipt.json` reruns the `default`, `variants`, `negative` and `pairs` suites of the temporal sweep (`scripts/oracle/temporal-kinds/run.sh`) on the fixtures that #380 and #381 change: `FIXTURES='(inttofp|ttl_74266|ttl_7476)(__.*)?'`. It compares Mobile with the pinned Evolution `d58a61e0`, like the main `temporal-kinds` receipt, and its rows join the matrix rows of IntToFP, 74266 and 7476.

- Mobile: the revision in each suite of `receipt.json`, with `core/` equal to `1579fdd4`.
- Evolution `d58a61e0`: `logisim-evolution-5.1.0dev-all.jar` built on Windows from that revision, SHA-256 `5c0c89b426578ffa6988244d477eace70bbaeaf09687890e893f6c258194695a`. The main receipt names the macOS build `d0b287d1…`, which was not available, so these suites have their own receipt.
- Environment: Windows 11, Git Bash, Temurin JDK 21.0.12.1. `run.sh` now strips the CRLF that Java writes on Windows before comparing records.

## Command

```sh
python3 scripts/parity_matrix.py reverify evolution-followups-d58a61e0 --evolution-jar EVOLUTION_D58A61E0_JAR --suites default,variants,negative,pairs
```

## Results

| Suite | Fixtures | SAME in both modes | VALUE | Rows |
| --- | ---: | ---: | ---: | --- |
| default | 3 | 1 | 2 | [default.tsv](default.tsv) |
| variants | 18 | 12 | 6 | [variants.tsv](variants.tsv) |
| negative | 18 | 13 | 5 | [negative.tsv](negative.tsv) |
| pairs | 11 | 7 | 4 | [pairs.tsv](pairs.tsv) |

Every `inttofp` fixture is SAME in the settle and step modes. Every VALUE row is a `ttl_74266` or `ttl_7476` fixture.

The 74266 and 7476 rows differ from the pin because Mobile follows their corrected pinouts; `parity/matrix.json` accepts them as `evolution-ttl-74266-pinout-followups` and `evolution-ttl-7476-pinout-followups`. The IntToFP fixtures use random inputs that do not reach the double-rounding case of #380, so they stay SAME; the unit tests in `FloatingPointLibraryTest` cover that case.

The same suites against Evolution `21f1e158` (JAR SHA-256 `806c030e4000809acf1ec4db043d51c027e2f297643e26413808d863898f36d7`), whose pinouts Mobile now follows, are in `evolution-21f1e158/`: all 50 fixtures (3, 18, 18 and 11) are SAME in both modes. They are a record only and do not enter the matrix, which keeps `d58a61e0` as its reference.
