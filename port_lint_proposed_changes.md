# port-lint Proposed Changes

**Generated:** 2026-08-28
**Source:** tmp/chrono/src
**Target:** src/commonMain/kotlin/io/github/kotlinmania/chrono

These are review proposals only. They are emitted when a Rust -> Kotlin pair matches only after fallback normalization, so the existing `port-lint` header is not an exact provenance match.

| Target file | Current header | Proposed header | Source path | Reason |
|-------------|----------------|-----------------|-------------|--------|
| `src/commonMain/kotlin/io/github/kotlinmania/chrono/Weekday.kt` | `// port-lint: source src/weekday.rs` | `// port-lint: source weekday.rs` | `weekday.rs` | `port-lint provenance header matched only after fallback normalization: 'src/weekday.rs' vs expected 'weekday.rs'` |
| `src/commonTest/kotlin/io/github/kotlinmania/chrono/WeekdayTest.kt` | `// port-lint: source src/weekday.rs` | `// port-lint: source weekday.rs` | `weekday.rs` | `port-lint provenance header matched only after fallback normalization: 'src/weekday.rs' vs expected 'weekday.rs'` |
| `src/commonMain/kotlin/io/github/kotlinmania/chrono/Date.kt` | `// port-lint: source src/date.rs` | `// port-lint: source date.rs` | `date.rs` | `port-lint provenance header matched only after fallback normalization: 'src/date.rs' vs expected 'date.rs'` |
| `src/commonMain/kotlin/io/github/kotlinmania/chrono/WeekdaySet.kt` | `// port-lint: source src/weekday_set.rs` | `// port-lint: source weekday_set.rs` | `weekday_set.rs` | `port-lint provenance header matched only after fallback normalization: 'src/weekday_set.rs' vs expected 'weekday_set.rs'` |
| `src/commonTest/kotlin/io/github/kotlinmania/chrono/WeekdaySetTest.kt` | `// port-lint: source src/weekday_set.rs` | `// port-lint: source weekday_set.rs` | `weekday_set.rs` | `port-lint provenance header matched only after fallback normalization: 'src/weekday_set.rs' vs expected 'weekday_set.rs'` |
| `src/commonMain/kotlin/io/github/kotlinmania/chrono/DateTime.kt` | `// port-lint: source src/datetime/mod.rs` | `// port-lint: source datetime/mod.rs` | `datetime/mod.rs` | `port-lint provenance header matched only after fallback normalization: 'src/datetime/mod.rs' vs expected 'datetime/mod.rs'` |
| `src/commonMain/kotlin/io/github/kotlinmania/chrono/naive/NaiveWeek.kt` | `// port-lint: source naive/mod.rs` | `// port-lint: source format/mod.rs` | `format/mod.rs` | `port-lint provenance header matched only by basename: 'naive/mod.rs' vs expected 'format/mod.rs'` |
| `src/commonMain/kotlin/io/github/kotlinmania/chrono/offset/MappedLocalTime.kt` | `// port-lint: source offset/mod.rs` | `// port-lint: source offset/local/mod.rs` | `offset/local/mod.rs` | `port-lint provenance header matched only by basename: 'offset/mod.rs' vs expected 'offset/local/mod.rs'` |
| `src/commonMain/kotlin/io/github/kotlinmania/chrono/OutOfRange.kt` | `// port-lint: source src/lib.rs` | `// port-lint: source lib.rs` | `lib.rs` | `port-lint provenance header matched only after fallback normalization: 'src/lib.rs' vs expected 'lib.rs'` |
| `src/commonMain/kotlin/io/github/kotlinmania/chrono/offset/TimeZone.kt` | `// port-lint: source offset/mod.rs` | `// port-lint: source offset/local/tz_info/mod.rs` | `offset/local/tz_info/mod.rs` | `port-lint provenance header matched only by basename: 'offset/mod.rs' vs expected 'offset/local/tz_info/mod.rs'` |
