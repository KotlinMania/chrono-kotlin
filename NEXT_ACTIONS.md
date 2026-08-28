# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 21/41 (51.2%)
- **Function parity:** 329/1266 matched (target 446) — 26.0%
- **Class/type parity:** 24/145 matched (target 32) — 16.6%
- **Combined symbol parity:** 353/1411 matched (target 478) — 25.0%
- **Average inline-code cosine:** 0.35 (function body across 11 matched files)
- **Average documentation cosine:** 0.66 (doc text across 11 matched files)
- **Cheat-zeroed Files:** 11
- **Critical Issues:** 21 files with <0.60 function similarity

## Priority 1: Fix Incomplete High-Dependency Files

No incomplete high-dependency files detected.

## Priority 2: Port Missing High-Value Files

Critical missing files (>10 dependencies):

No missing high-value files detected.

## Detailed Work Items

Every matched file is listed below with function and type symbol parity.

### 1. weekday

- **Target:** `chrono.Weekday [ZERO] [PROVENANCE-FALLBACK]`
- **Similarity:** 0.00
- **Dependents:** 6
- **Priority Score:** 6132610.0
- **Functions:** 11/21 matched (target 15)
- **Missing functions:** `fmt`, `format`, `serialize`, `expecting`, `visit_str`, `deserialize`, `test_formatting_alignment`, `test_serde_serialize`, `test_serde_deserialize`, `test_rkyv_validation`
- **Types:** 2/5 matched (target 3)
- **Missing types:** `Error`, `WeekdayVisitor`, `Value`
- **Tests:** 1/5 matched
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `src/weekday.rs` vs expected `weekday.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `src/weekday.rs` vs expected `weekday.rs`
- **Proposed provenance header:** `// port-lint: source weekday.rs` (current: `// port-lint: source src/weekday.rs`)
- **Proposed provenance header:** `// port-lint: source weekday.rs` (current: `// port-lint: source src/weekday.rs`)
- **Lint issues:** 2

### 2. date

- **Target:** `chrono.Date [PROVENANCE-FALLBACK]`
- **Similarity:** 0.33
- **Dependents:** 3
- **Priority Score:** 3346106.8
- **Functions:** 26/59 matched (target 34)
- **Missing functions:** `and_hms_milli`, `and_hms_micro`, `and_hms_nano`, `succ`, `succ_opt`, `pred`, `pred_opt`, `offset`, `with_timezone`, `checked_add_signed`, `checked_sub_signed`, `signed_duration_since`, `years_since`, `map_local`, `format_with_items`, `format`, `format_localized_with_items`, `format_localized`, `eq`, `partial_cmp`, `cmp`, `hash`, `add`, `add_assign`, `sub`, `sub_assign`, `fmt`, `arbitrary`, `test_years_elapsed`, `test_date_add_assign`, `test_date_add_assign_local`, `test_date_sub_assign`, `test_date_sub_assign_local`
- **Types:** 1/2 matched (target 1)
- **Missing types:** `Output`
- **Tests:** 0/5 matched
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `src/date.rs` vs expected `date.rs`
- **Proposed provenance header:** `// port-lint: source date.rs` (current: `// port-lint: source src/date.rs`)
- **Lint issues:** 1

### 3. offset.utc

- **Target:** `offset.Utc`
- **Similarity:** 0.59
- **Dependents:** 2
- **Priority Score:** 2031204.1
- **Functions:** 8/10 matched
- **Missing functions:** `fmt`, `format`
- **Types:** 1/2 matched (target 1)
- **Missing types:** `Offset`

### 4. time_delta

- **Target:** `chrono.TimeDelta`
- **Similarity:** 0.27
- **Dependents:** 1
- **Priority Score:** 1670107.2
- **Functions:** 34/98 matched (target 40)
- **Missing functions:** `checked_div`, `min_value`, `max_value`, `is_zero`, `from_std`, `to_std`, `neg`, `add`, `sub`, `add_assign`, `sub_assign`, `mul`, `div`, `sum`, `fmt`, `description`, `arbitrary`, `serialize`, `deserialize`, `test_serde`, `test_serde_oob_panic`, `test_duration`, `test_duration_num_days`, `test_duration_num_seconds`, `test_duration_seconds_max_allowed`, `test_duration_seconds_max_overflow`, `test_duration_seconds_max_overflow_panic`, `test_duration_seconds_min_allowed`, `test_duration_seconds_min_underflow`, `test_duration_seconds_min_underflow_panic`, `test_duration_as_seconds_f64`, `test_duration_as_seconds_f32`, `test_duration_subsec_nanos`, `test_duration_subsec_micros`, `test_duration_subsec_millis`, `test_duration_num_milliseconds`, `test_duration_milliseconds_max_allowed`, `test_duration_milliseconds_max_overflow`, `test_duration_milliseconds_min_allowed`, `test_duration_milliseconds_min_underflow`, `test_duration_milliseconds_min_underflow_panic`, `test_duration_num_microseconds`, `test_duration_microseconds_max_allowed`, `test_duration_microseconds_max_overflow`, `test_duration_microseconds_min_allowed`, `test_duration_microseconds_min_underflow`, `test_duration_num_nanoseconds`, `test_duration_nanoseconds_max_allowed`, `test_duration_nanoseconds_max_overflow`, `test_duration_nanoseconds_min_allowed`, `test_duration_nanoseconds_min_underflow`, `test_max`, `test_min`, `test_duration_ord`, `test_duration_checked_ops`, `test_duration_abs`, `test_duration_mul`, `test_duration_div`, `test_duration_sum`, `test_duration_fmt`, `test_to_std`, `test_from_std`, `test_duration_const`, `test_rkyv_validation`
- **Types:** 1/3 matched (target 1)
- **Missing types:** `Output`, `OutOfRangeError`
- **Tests:** 0/45 matched

### 5. weekday_set

- **Target:** `chrono.WeekdaySet [PROVENANCE-FALLBACK]`
- **Similarity:** 0.48
- **Dependents:** 1
- **Priority Score:** 1083005.2
- **Functions:** 20/27 matched (target 34)
- **Missing functions:** `fmt`, `format`, `from_iter`, `iter_all`, `assert_8th_bit_invariant`, `debug_prints_8th_bit_if_not_zero`, `bitwise_set_operations_preserve_8th_bit_invariant`
- **Types:** 2/3 matched
- **Missing types:** `Item`
- **Tests:** 1/5 matched
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `src/weekday_set.rs` vs expected `weekday_set.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `src/weekday_set.rs` vs expected `weekday_set.rs`
- **Proposed provenance header:** `// port-lint: source weekday_set.rs` (current: `// port-lint: source src/weekday_set.rs`)
- **Proposed provenance header:** `// port-lint: source weekday_set.rs` (current: `// port-lint: source src/weekday_set.rs`)
- **Lint issues:** 2

### 6. naive.isoweek

- **Target:** `naive.IsoWeek`
- **Similarity:** 0.20
- **Dependents:** 1
- **Priority Score:** 1081308.0
- **Functions:** 4/12 matched (target 7)
- **Missing functions:** `fmt`, `format`, `test_iso_week_extremes`, `test_iso_week_equivalence_for_first_week`, `test_iso_week_equivalence_for_last_week`, `test_iso_week_ordering_for_first_week`, `test_iso_week_ordering_for_last_week`, `test_rkyv_validation`
- **Types:** 1/1 matched
- **Missing types:** _none_
- **Tests:** 0/6 matched

### 7. format.locales

- **Target:** `format.Locales`
- **Similarity:** 0.38
- **Dependents:** 1
- **Priority Score:** 1041206.2
- **Functions:** 7/11 matched (target 7)
- **Missing functions:** `d_fmt`, `d_t_fmt`, `t_fmt`, `t_fmt_ampm`
- **Types:** 1/1 matched
- **Missing types:** _none_

### 8. date.mod

- **Target:** `naive.NaiveDate [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 400210.0
- **Functions:** 62/93 matched (target 73)
- **Missing functions:** `arbitrary`, `parse_from_str`, `parse_and_remainder`, `and_hms_milli`, `and_hms_micro`, `and_hms_nano`, `format_with_items`, `format`, `format_localized_with_items`, `format_localized`, `iter_days`, `iter_weeks`, `yof`, `add`, `add_assign`, `sub`, `sub_assign`, `from`, `next`, `size_hint`, `next_back`, `fmt`, `from_str`, `default`, `serialize`, `expecting`, `visit_str`, `deserialize`, `test_serde_serialize`, `test_serde_deserialize`, `test_serde_bincode`
- **Types:** 1/9 matched (target 1)
- **Missing types:** `Output`, `NaiveDateDaysIterator`, `Item`, `NaiveDateWeeksIterator`, `Err`, `FormatWrapped`, `NaiveDateVisitor`, `Value`
- **Tests:** 0/3 matched

### 9. offset.mod

- **Target:** `offset.Offset [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 373810.0
- **Functions:** 0/34 matched (target 0)
- **Missing functions:** `single`, `earliest`, `latest`, `map`, `and_then`, `and_time`, `and_hms_opt`, `and_hms_milli_opt`, `and_hms_micro_opt`, `and_hms_nano_opt`, `unwrap`, `with_ymd_and_hms`, `ymd`, `ymd_opt`, `yo`, `yo_opt`, `isoywd`, `isoywd_opt`, `timestamp`, `timestamp_opt`, `timestamp_millis`, `timestamp_millis_opt`, `timestamp_nanos`, `timestamp_micros`, `datetime_from_str`, `from_local_date`, `from_local_datetime`, `from_utc_date`, `from_utc_datetime`, `test_fixed_offset_min_max_dates`, `test_negative_millis`, `test_negative_nanos`, `test_nanos_never_panics`, `test_negative_micros`
- **Types:** 1/4 matched (target 1)
- **Missing types:** `MappedLocalTime`, `LocalResult`, `TimeZone`
- **Tests:** 0/5 matched

### 10. datetime.mod

- **Target:** `chrono.DateTime [STUB] [PROVENANCE-FALLBACK]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 348810.0
- **Functions:** 53/85 matched (target 67)
- **Missing functions:** `from_utc`, `timestamp_nanos`, `checked_add_days`, `checked_sub_days`, `overflowing_naive_local`, `years_since`, `to_rfc2822`, `to_rfc3339`, `to_rfc3339_opts`, `with_time`, `default`, `from`, `map_local`, `parse_from_rfc2822`, `parse_from_rfc3339`, `parse_from_str`, `parse_and_remainder`, `format_with_items`, `format`, `format_localized_with_items`, `format_localized`, `eq`, `partial_cmp`, `cmp`, `hash`, `add`, `add_assign`, `sub`, `sub_assign`, `fmt`, `from_str`, `arbitrary`
- **Types:** 1/3 matched (target 1)
- **Missing types:** `Output`, `Err`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `src/datetime/mod.rs` vs expected `datetime/mod.rs`
- **Proposed provenance header:** `// port-lint: source datetime/mod.rs` (current: `// port-lint: source src/datetime/mod.rs`)
- **Lint issues:** 1

### 11. naive.datetime.mod

- **Target:** `naive.NaiveDateTime [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 276810.0
- **Functions:** 40/65 matched (target 53)
- **Missing functions:** `from_timestamp`, `from_timestamp_millis`, `from_timestamp_micros`, `from_timestamp_nanos`, `from_timestamp_opt`, `parse_from_str`, `parse_and_remainder`, `timestamp_nanos`, `timestamp_subsec_millis`, `timestamp_subsec_micros`, `timestamp_subsec_nanos`, `overflowing_add_offset`, `overflowing_sub_offset`, `checked_add_days`, `checked_sub_days`, `format_with_items`, `format`, `from`, `add`, `add_assign`, `sub`, `sub_assign`, `fmt`, `from_str`, `default`
- **Types:** 1/3 matched (target 1)
- **Missing types:** `Output`, `Err`

### 12. format.mod

- **Target:** `naive.NaiveWeek [STUB] [PROVENANCE-FALLBACK]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 262610.0
- **Functions:** 0/11 matched (target 10)
- **Missing functions:** `fmt`, `format`, `num`, `num0`, `nums`, `fixed`, `internal_fixed`, `to_owned`, `kind`, `description`, `from_str`
- **Types:** 0/15 matched (target 1)
- **Missing types:** `Void`, `Pad`, `Numeric`, `InternalNumeric`, `Fixed`, `InternalFixed`, `InternalInternal`, `OffsetFormat`, `OffsetPrecision`, `Colons`, `Item`, `ParseError`, `ParseErrorKind`, `ParseResult`, `Err`
- **Provenance warning:** port-lint provenance header matched only by basename: `naive/mod.rs` vs expected `format/mod.rs`
- **Proposed provenance header:** `// port-lint: source format/mod.rs` (current: `// port-lint: source naive/mod.rs`)
- **Lint issues:** 1

### 13. local.mod

- **Target:** `offset.MappedLocalTime [STUB] [PROVENANCE-FALLBACK]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 232310.0
- **Functions:** 0/20 matched (target 6)
- **Missing functions:** `offset_from_utc_datetime`, `offset_from_local_datetime`, `today`, `now`, `from_offset`, `offset_from_local_date`, `offset_from_utc_date`, `new`, `partial_cmp`, `cmp`, `lookup_with_dst_transitions`, `verify_correct_offsets`, `verify_correct_offsets_distant_past`, `verify_correct_offsets_distant_future`, `test_local_date_sanity_check`, `test_leap_second`, `test_lookup_with_dst_transitions`, `compare_lookup`, `test_lookup_with_dst_transitions_limits`, `test_rkyv_validation`
- **Types:** 0/3 matched (target 4)
- **Missing types:** `Local`, `Offset`, `Transition`
- **Tests:** 0/9 matched
- **Provenance warning:** port-lint provenance header matched only by basename: `offset/mod.rs` vs expected `offset/local/mod.rs`
- **Proposed provenance header:** `// port-lint: source offset/local/mod.rs` (current: `// port-lint: source offset/mod.rs`)
- **Lint issues:** 1

### 14. time.mod

- **Target:** `naive.NaiveTime [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 194010.0
- **Functions:** 20/37 matched (target 30)
- **Missing functions:** `arbitrary`, `from_hms_milli`, `from_hms_micro`, `from_hms_nano`, `from_num_seconds_from_midnight`, `parse_from_str`, `parse_and_remainder`, `format_with_items`, `format`, `hms`, `add`, `add_assign`, `sub`, `sub_assign`, `fmt`, `from_str`, `default`
- **Types:** 1/3 matched (target 1)
- **Missing types:** `Output`, `Err`

### 15. month

- **Target:** `chrono.Month`
- **Similarity:** 0.19
- **Dependents:** 0
- **Priority Score:** 173108.1
- **Functions:** 11/25 matched (target 15)
- **Missing functions:** `fmt`, `format`, `serialize`, `expecting`, `visit_str`, `deserialize`, `test_month_enum_try_from`, `test_month_enum_primitive_parse`, `test_month_enum_succ_pred`, `test_month_partial_ord`, `test_months_as_u32`, `test_serde_serialize`, `test_serde_deserialize`, `test_rkyv_validation`
- **Types:** 3/6 matched (target 3)
- **Missing types:** `Error`, `MonthVisitor`, `Value`
- **Tests:** 0/8 matched

### 16. naive.mod

- **Target:** `naive.Days [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 141610.0
- **Functions:** 1/14 matched (target 2)
- **Missing functions:** `first_day`, `checked_first_day`, `last_day`, `checked_last_day`, `days`, `checked_days`, `eq`, `hash`, `test_naiveweek`, `test_naiveweek_min_max`, `test_naiveweek_checked_no_panic`, `test_naiveweek_eq`, `test_naiveweek_hash`
- **Types:** 1/2 matched (target 1)
- **Missing types:** `NaiveWeek`
- **Tests:** 0/5 matched

### 17. offset.fixed

- **Target:** `offset.FixedOffset`
- **Similarity:** 0.47
- **Dependents:** 0
- **Priority Score:** 102205.3
- **Functions:** 11/19 matched (target 14)
- **Missing functions:** `local_minus_utc`, `from_str`, `fmt`, `format`, `arbitrary`, `test_date_extreme_offset`, `test_parse_offset`, `test_rkyv_validation`
- **Types:** 1/3 matched (target 1)
- **Missing types:** `Err`, `Offset`
- **Tests:** 0/3 matched

### 18. naive.internals

- **Target:** `naive.Internals`
- **Similarity:** 0.41
- **Dependents:** 0
- **Priority Score:** 92605.9
- **Functions:** 15/24 matched (target 17)
- **Missing functions:** `fmt`, `valid`, `test_year_flags_ndays_from_year`, `test_year_flags_nisoweeks`, `test_mdf_valid`, `check`, `test_mdf_fields`, `test_mdf_with_fields`, `test_mdf_new_range`
- **Types:** 2/2 matched
- **Missing types:** _none_
- **Tests:** 0/8 matched

### 19. lib

- **Target:** `chrono.OutOfRange [STUB] [PROVENANCE-FALLBACK]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 91010.0
- **Functions:** 0/6 matched (target 3)
- **Missing functions:** `invalid_ts`, `fmt`, `new`, `format`, `expect`, `test_type_sizes`
- **Types:** 1/4 matched (target 1)
- **Missing types:** `Duration`, `SerdeError`, `ArchivedDuration`
- **Tests:** 0/1 matched
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `src/lib.rs` vs expected `lib.rs`
- **Proposed provenance header:** `// port-lint: source lib.rs` (current: `// port-lint: source src/lib.rs`)
- **Lint issues:** 1

### 20. traits

- **Target:** `chrono.Traits`
- **Similarity:** 0.52
- **Dependents:** 0
- **Priority Score:** 31104.8
- **Functions:** 6/9 matched (target 6)
- **Missing functions:** `test_num_days_from_ce_against_alternative_impl`, `in_between`, `test_num_days_in_month`
- **Types:** 2/2 matched
- **Missing types:** _none_
- **Tests:** 0/3 matched

### 21. tz_info.mod

- **Target:** `offset.TimeZone [STUB] [PROVENANCE-FALLBACK]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 30310.0
- **Functions:** 0/2 matched (target 3)
- **Missing functions:** `fmt`, `from`
- **Types:** 0/1 matched
- **Missing types:** `Error`
- **Provenance warning:** port-lint provenance header matched only by basename: `offset/mod.rs` vs expected `offset/local/tz_info/mod.rs`
- **Proposed provenance header:** `// port-lint: source offset/local/tz_info/mod.rs` (current: `// port-lint: source offset/mod.rs`)
- **Lint issues:** 1

## Success Criteria

For each file to be considered "complete":
- **Similarity ≥ 0.85** (Excellent threshold)
- All public APIs ported
- All tests ported
- Documentation ported
- port-lint header present

