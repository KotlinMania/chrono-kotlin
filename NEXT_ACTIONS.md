# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 29/41 (70.7%)
- **Function parity:** 556/1373 matched (target 816) — 40.5%
- **Class/type parity:** 68/145 matched (target 130) — 46.9%
- **Combined symbol parity:** 624/1518 matched (target 946) — 41.1%
- **Average inline-code cosine:** 0.13 (function body across 19 matched files)
- **Average documentation cosine:** 0.62 (doc text across 19 matched files)
- **Cheat-zeroed Files:** 14
- **Critical Issues:** 29 files with <0.60 function similarity
- **Needs Review:** 0 files with 0.60-0.84 function similarity
- **Excellent:** 0 files with >=0.85 function similarity

## Priority 1: Fix Incomplete High-Dependency Files

No incomplete high-dependency files detected.

## Priority 2: Port Missing High-Value Files

Critical missing files (>10 dependencies):

No missing high-value files detected.

## Detailed Work Items

Every matched file is listed below with function and type symbol parity.

### 1. weekday

- **Target:** `chrono.Weekday [ZERO]`
- **Similarity:** 0.00
- **Dependents:** 6
- **Priority Score:** 6082810.0
- **Functions:** 17/23 matched (target 22)
- **Missing functions:** `fmt`, `fmt`, `fmt`, `format`, `test_formatting_alignment`, `test_rkyv_validation`
- **Types:** 3/5 matched (target 4)
- **Missing types:** `Error`, `Value`
- **Tests:** 3/5 matched

### 2. tz_info.timezone

- **Target:** `tzinfo.TimeZone`
- **Similarity:** 0.23
- **Dependents:** 4
- **Priority Score:** 4054507.8
- **Functions:** 34/39 matched (target 41)
- **Missing functions:** `from_file`, `unix_leap_time`, `unix_leap_time`, `as_ref`, `fmt`
- **Types:** 6/6 matched (target 7)
- **Missing types:** _none_
- **Tests:** 10/10 matched

### 3. date

- **Target:** `chrono.Date`
- **Similarity:** 0.08
- **Dependents:** 3
- **Priority Score:** 3376409.2
- **Functions:** 26/62 matched (target 34)
- **Missing functions:** `and_hms_milli`, `and_hms_micro`, `and_hms_nano`, `succ`, `succ_opt`, `pred`, `pred_opt`, `offset`, `with_timezone`, `checked_add_signed`, `checked_sub_signed`, `signed_duration_since`, `years_since`, `map_local`, `format_with_items`, `format`, `format_localized_with_items`, `format_localized`, `eq`, `partial_cmp`, `cmp`, `hash`, `add`, `add_assign`, `sub`, `sub_assign`, `sub`, `fmt`, `fmt`, `format`, `arbitrary`, `test_years_elapsed`, `test_date_add_assign`, `test_date_add_assign_local`, `test_date_sub_assign`, `test_date_sub_assign_local`
- **Types:** 1/2 matched (target 1)
- **Missing types:** `Output`
- **Tests:** 0/5 matched

### 4. offset.utc

- **Target:** `offset.Utc`
- **Similarity:** 0.24
- **Dependents:** 2
- **Priority Score:** 2051407.6
- **Functions:** 8/12 matched (target 13)
- **Missing functions:** `now`, `fmt`, `fmt`, `format`
- **Types:** 1/2 matched (target 1)
- **Missing types:** `Offset`

### 5. time_delta

- **Target:** `chrono.TimeDelta`
- **Similarity:** 0.11
- **Dependents:** 1
- **Priority Score:** 1680408.9
- **Functions:** 36/101 matched (target 52)
- **Missing functions:** `checked_div`, `min_value`, `max_value`, `is_zero`, `from_std`, `to_std`, `neg`, `neg`, `add`, `sub`, `add_assign`, `sub_assign`, `mul`, `div`, `sum`, `sum`, `fmt`, `fmt`, `description`, `arbitrary`, `test_serde`, `test_serde_oob_panic`, `test_duration`, `test_duration_num_days`, `test_duration_num_seconds`, `test_duration_seconds_max_allowed`, `test_duration_seconds_max_overflow`, `test_duration_seconds_max_overflow_panic`, `test_duration_seconds_min_allowed`, `test_duration_seconds_min_underflow`, `test_duration_seconds_min_underflow_panic`, `test_duration_as_seconds_f64`, `test_duration_as_seconds_f32`, `test_duration_subsec_nanos`, `test_duration_subsec_micros`, `test_duration_subsec_millis`, `test_duration_num_milliseconds`, `test_duration_milliseconds_max_allowed`, `test_duration_milliseconds_max_overflow`, `test_duration_milliseconds_min_allowed`, `test_duration_milliseconds_min_underflow`, `test_duration_milliseconds_min_underflow_panic`, `test_duration_num_microseconds`, `test_duration_microseconds_max_allowed`, `test_duration_microseconds_max_overflow`, `test_duration_microseconds_min_allowed`, `test_duration_microseconds_min_underflow`, `test_duration_num_nanoseconds`, `test_duration_nanoseconds_max_allowed`, `test_duration_nanoseconds_max_overflow`, `test_duration_nanoseconds_min_allowed`, `test_duration_nanoseconds_min_underflow`, `test_max`, `test_min`, `test_duration_ord`, `test_duration_checked_ops`, `test_duration_abs`, `test_duration_mul`, `test_duration_div`, `test_duration_sum`, `test_duration_fmt`, `test_to_std`, `test_from_std`, `test_duration_const`, `test_rkyv_validation`
- **Types:** 1/3 matched (target 6)
- **Missing types:** `Output`, `OutOfRangeError`
- **Tests:** 0/45 matched

### 6. format.locales

- **Target:** `format.Locales`
- **Similarity:** 0.25
- **Dependents:** 1
- **Priority Score:** 1111907.5
- **Functions:** 7/18 matched (target 7)
- **Missing functions:** `d_fmt`, `d_t_fmt`, `t_fmt`, `t_fmt_ampm`, `default_locale`, `short_months`, `long_months`, `short_weekdays`, `long_weekdays`, `am_pm`, `decimal_point`
- **Types:** 1/1 matched
- **Missing types:** _none_

### 7. weekday_set

- **Target:** `chrono.WeekdaySet`
- **Similarity:** 0.18
- **Dependents:** 1
- **Priority Score:** 1103208.2
- **Functions:** 20/29 matched (target 34)
- **Missing functions:** `fmt`, `format`, `len`, `fmt`, `from_iter`, `iter_all`, `assert_8th_bit_invariant`, `debug_prints_8th_bit_if_not_zero`, `bitwise_set_operations_preserve_8th_bit_invariant`
- **Types:** 2/3 matched
- **Missing types:** `Item`
- **Tests:** 1/5 matched

### 8. naive.isoweek

- **Target:** `naive.IsoWeek`
- **Similarity:** 0.08
- **Dependents:** 1
- **Priority Score:** 1081309.2
- **Functions:** 4/12 matched (target 7)
- **Missing functions:** `fmt`, `format`, `test_iso_week_extremes`, `test_iso_week_equivalence_for_first_week`, `test_iso_week_equivalence_for_last_week`, `test_iso_week_ordering_for_first_week`, `test_iso_week_ordering_for_last_week`, `test_rkyv_validation`
- **Types:** 1/1 matched
- **Missing types:** _none_
- **Tests:** 0/6 matched

### 9. datetime.mod

- **Target:** `chrono.DateTime [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 561710.0
- **Functions:** 61/114 matched (target 89)
- **Missing functions:** `timestamp_nanos`, `years_since`, `to_rfc2822`, `with_time`, `default`, `default`, `default`, `from`, `from`, `from`, `from`, `from`, `from`, `map_local`, `parse_from_rfc2822`, `parse_from_str`, `parse_and_remainder`, `format_with_items`, `format`, `format_localized_with_items`, `format_localized`, `eq`, `partial_cmp`, `cmp`, `hash`, `add`, `add`, `add_assign`, `add_assign`, `add`, `add`, `sub`, `sub`, `sub_assign`, `sub_assign`, `sub`, `sub`, `sub`, `sub`, `add`, `sub`, `fmt`, `format`, `fmt`, `fmt`, `from_str`, `from`, `from`, `from`, `from`, `from`, `from`, `arbitrary`
- **Types:** 1/3 matched (target 5)
- **Missing types:** `Output`, `Err`

### 10. date.mod

- **Target:** `naive.NaiveDate [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 501810.0
- **Functions:** 67/109 matched (target 78)
- **Missing functions:** `arbitrary`, `parse_from_str`, `parse_and_remainder`, `and_hms_milli`, `and_hms_micro`, `and_hms_nano`, `format_with_items`, `format`, `format_localized_with_items`, `format_localized`, `iter_days`, `iter_weeks`, `yof`, `year`, `month`, `day`, `ordinal`, `weekday`, `add`, `add_assign`, `add`, `sub`, `add`, `sub`, `sub`, `sub_assign`, `sub`, `from`, `next`, `size_hint`, `next_back`, `next`, `size_hint`, `next_back`, `fmt`, `format`, `fmt`, `default`, `fmt`, `test_serde_serialize`, `test_serde_deserialize`, `test_serde_bincode`
- **Types:** 2/9 matched (target 2)
- **Missing types:** `Output`, `NaiveDateDaysIterator`, `Item`, `NaiveDateWeeksIterator`, `Err`, `FormatWrapped`, `Value`
- **Tests:** 0/3 matched

### 11. naive.datetime.mod

- **Target:** `naive.NaiveDateTime [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 358110.0
- **Functions:** 45/78 matched (target 67)
- **Missing functions:** `from_timestamp`, `from_timestamp_millis`, `from_timestamp_micros`, `from_timestamp_nanos`, `from_timestamp_opt`, `parse_from_str`, `parse_and_remainder`, `timestamp_nanos`, `timestamp_subsec_millis`, `timestamp_subsec_micros`, `timestamp_subsec_nanos`, `format_with_items`, `format`, `from`, `add`, `add`, `add_assign`, `add_assign`, `add`, `add`, `sub`, `sub`, `sub_assign`, `sub_assign`, `sub`, `sub`, `sub`, `add`, `sub`, `fmt`, `format`, `fmt`, `default`
- **Types:** 1/3 matched (target 2)
- **Missing types:** `Output`, `Err`

### 12. time.mod

- **Target:** `naive.NaiveTime [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 295110.0
- **Functions:** 21/48 matched (target 35)
- **Missing functions:** `arbitrary`, `from_hms_milli`, `from_hms_micro`, `from_hms_nano`, `from_num_seconds_from_midnight`, `parse_from_str`, `parse_and_remainder`, `format_with_items`, `format`, `hms`, `nanosecond`, `num_seconds_from_midnight`, `add`, `add_assign`, `add`, `add_assign`, `add`, `sub`, `sub_assign`, `sub`, `sub_assign`, `sub`, `sub`, `fmt`, `format`, `fmt`, `default`
- **Types:** 1/3 matched (target 2)
- **Missing types:** `Output`, `Err`

### 13. offset.mod

- **Target:** `offset.Offset [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 263810.0
- **Functions:** 9/34 matched (target 9)
- **Missing functions:** `and_time`, `and_hms_opt`, `and_hms_milli_opt`, `and_hms_micro_opt`, `and_hms_nano_opt`, `ymd`, `ymd_opt`, `yo`, `yo_opt`, `isoywd`, `isoywd_opt`, `timestamp`, `timestamp_opt`, `timestamp_millis`, `timestamp_millis_opt`, `timestamp_nanos`, `timestamp_micros`, `datetime_from_str`, `from_local_date`, `from_utc_date`, `test_fixed_offset_min_max_dates`, `test_negative_millis`, `test_negative_nanos`, `test_nanos_never_panics`, `test_negative_micros`
- **Types:** 3/4 matched (target 7)
- **Missing types:** `LocalResult`
- **Tests:** 0/5 matched

### 14. format.mod

- **Target:** `format.Mod [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 262910.0
- **Functions:** 1/14 matched (target 15)
- **Missing functions:** `fmt`, `format`, `format`, `num`, `num0`, `nums`, `fixed`, `internal_fixed`, `to_owned`, `fmt`, `description`, `from_str`, `from_str`
- **Types:** 2/15 matched (target 3)
- **Missing types:** `Void`, `Pad`, `Numeric`, `InternalNumeric`, `Fixed`, `InternalFixed`, `InternalInternal`, `OffsetFormat`, `OffsetPrecision`, `Colons`, `Item`, `ParseResult`, `Err`

### 15. naive.internals

- **Target:** `naive.Internals`
- **Similarity:** 0.11
- **Dependents:** 0
- **Priority Score:** 112808.9
- **Functions:** 15/26 matched (target 17)
- **Missing functions:** `fmt`, `valid`, `fmt`, `test_year_flags_ndays_from_year`, `test_year_flags_nisoweeks`, `test_mdf_valid`, `check`, `test_mdf_fields`, `test_mdf_with_fields`, `check`, `test_mdf_new_range`
- **Types:** 2/2 matched
- **Missing types:** _none_
- **Tests:** 0/9 matched

### 16. offset.fixed

- **Target:** `offset.FixedOffset`
- **Similarity:** 0.14
- **Dependents:** 0
- **Priority Score:** 112308.6
- **Functions:** 11/20 matched (target 17)
- **Missing functions:** `local_minus_utc`, `from_str`, `fmt`, `fmt`, `format`, `arbitrary`, `test_date_extreme_offset`, `test_parse_offset`, `test_rkyv_validation`
- **Types:** 1/3 matched (target 1)
- **Missing types:** `Err`, `Offset`
- **Tests:** 0/3 matched

### 17. lib

- **Target:** `chrono.OutOfRange [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 111210.0
- **Functions:** 0/8 matched (target 3)
- **Missing functions:** `invalid_ts`, `fmt`, `new`, `fmt`, `fmt`, `format`, `expect`, `test_type_sizes`
- **Types:** 1/4 matched (target 1)
- **Missing types:** `Duration`, `SerdeError`, `ArchivedDuration`
- **Tests:** 0/1 matched

### 18. naive.datetime.serde

- **Target:** `commonMain.kotlin.io.github.kotlinmania.chrono.naive.serde.Mod`
- **Similarity:** 0.18
- **Dependents:** 0
- **Priority Score:** 106508.2
- **Functions:** 47/53 matched (target 91)
- **Missing functions:** `fmt`, `visit_str`, `test_serde_serialize`, `test_serde_deserialize`, `test_serde_bincode`, `test_serde_bincode_optional`
- **Types:** 8/12 matched (target 17)
- **Missing types:** `FormatWrapped`, `NaiveDateTimeVisitor`, `Value`, `Test`
- **Tests:** 0/4 matched

### 19. local.mod

- **Target:** `local.Local [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 92710.0
- **Functions:** 16/24 matched (target 31)
- **Missing functions:** `offset_from_utc_datetime`, `offset_from_local_datetime`, `offset_from_local_datetime`, `offset_from_utc_datetime`, `partial_cmp`, `cmp`, `compare_lookup`, `test_rkyv_validation`
- **Types:** 2/3 matched (target 4)
- **Missing types:** `Offset`
- **Tests:** 7/9 matched

### 20. naive.mod

- **Target:** `naive.Days [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 71710.0
- **Functions:** 8/15 matched (target 12)
- **Missing functions:** `eq`, `hash`, `test_naiveweek`, `test_naiveweek_min_max`, `test_naiveweek_checked_no_panic`, `test_naiveweek_eq`, `test_naiveweek_hash`
- **Types:** 2/2 matched
- **Missing types:** _none_
- **Tests:** 0/5 matched

### 21. month

- **Target:** `chrono.Month [ZERO]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 63310.0
- **Functions:** 23/27 matched (target 28)
- **Missing functions:** `fmt`, `fmt`, `format`, `test_rkyv_validation`
- **Types:** 4/6 matched (target 5)
- **Missing types:** `Error`, `Value`
- **Tests:** 8/9 matched

### 22. local.windows

- **Target:** `local.Windows [ZERO]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 61010.0
- **Functions:** 3/9 matched (target 11)
- **Missing functions:** `offset_from_utc_datetime`, `offset_from_local_datetime`, `from_local_time`, `system_time_from_naive_date_time`, `local_to_utc_time`, `system_time_as_unix_seconds`
- **Types:** 1/1 matched (target 3)
- **Missing types:** _none_
- **Tests:** 1/5 matched

### 23. tz_info.mod

- **Target:** `tzinfo.Mod [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 50610.0
- **Functions:** 0/5 matched (target 7)
- **Missing functions:** `fmt`, `from`, `from`, `from`, `from`
- **Types:** 1/1 matched (target 20)
- **Missing types:** _none_

### 24. traits

- **Target:** `chrono.Traits`
- **Similarity:** 0.30
- **Dependents:** 0
- **Priority Score:** 41207.0
- **Functions:** 6/10 matched (target 6)
- **Missing functions:** `test_num_days_from_ce_against_alternative_impl`, `in_between`, `num_days_from_ce`, `test_num_days_in_month`
- **Types:** 2/2 matched
- **Missing types:** _none_
- **Tests:** 0/4 matched

### 25. local.tz_data

- **Target:** `local.TzData [ZERO]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 32310.0
- **Functions:** 17/20 matched (target 24)
- **Missing functions:** `for_zone`, `test_ohos_machine_tz_data_loading`, `test_android_machine_tz_data_loading`
- **Types:** 3/3 matched (target 4)
- **Missing types:** _none_
- **Tests:** 9/11 matched

### 26. local.unix

- **Target:** `local.Unix`
- **Similarity:** 0.11
- **Dependents:** 0
- **Priority Score:** 31008.9
- **Functions:** 5/8 matched
- **Missing functions:** `offset_from_utc_datetime`, `offset_from_local_datetime`, `offset`
- **Types:** 2/2 matched (target 4)
- **Missing types:** _none_

### 27. tz_info.rule

- **Target:** `tzinfo.Rule`
- **Similarity:** 0.36
- **Dependents:** 0
- **Priority Score:** 23606.4
- **Functions:** 30/32 matched (target 31)
- **Missing functions:** `from`, `from`
- **Types:** 4/4 matched (target 11)
- **Missing types:** _none_
- **Tests:** 9/9 matched

### 28. tz_info.parser

- **Target:** `tzinfo.Parser`
- **Similarity:** 0.09
- **Dependents:** 0
- **Priority Score:** 12209.1
- **Functions:** 17/18 matched (target 19)
- **Missing functions:** `seek_after`
- **Types:** 4/4 matched
- **Missing types:** _none_

### 29. local.win_bindings

- **Target:** `local.WinBindings`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 710.0
- **Functions:** 2/2 matched (target 8)
- **Missing functions:** _none_
- **Types:** 5/5 matched
- **Missing types:** _none_

## Success Criteria

For each file to be considered "complete":
- **Similarity ≥ 0.85** (Excellent threshold)
- All public APIs ported
- All tests ported
- Documentation ported
- port-lint header present

