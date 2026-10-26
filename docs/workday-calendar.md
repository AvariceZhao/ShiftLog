# Workday Calendar

The bundled mainland China calendar covers 2024-2026. Holiday ranges and make-up
workdays are transcribed from the annual State Council notices:

- 2024: https://www.gov.cn/zhengce/content/202310/content_6911527.htm
- 2025: https://www.gov.cn/zhengce/content/202411/content_6986382.htm
- 2026: https://www.gov.cn/zhengce/zhengceku/202511/content_7047091.htm

An explicit per-date WORKDAY or REST_DAY override takes priority over the bundled
calendar and the global calendar setting. AUTO uses the official holiday and
make-up dates, then a Monday-Friday working week. With the setting disabled,
AUTO treats every date as a workday.

Rest-day punch timestamps remain stored for reference, but are excluded from
hours, attendance, absence, incomplete punches, late/early counts and leave-day
totals. Rest days remain visible and editable in the history calendar.

Absence is finalized only when the shift's scheduled end has passed, including
overnight shifts. Dates without a published/bundled annual schedule fall back to
Monday-Friday and display a coverage notice in the home and history screens.
Online data is fetched from NateScarlet/holiday-cn, which includes source notice
URLs and both holidays and make-up workdays:

- https://github.com/NateScarlet/holiday-cn
- Primary: https://raw.githubusercontent.com/NateScarlet/holiday-cn/master/{year}.json
- Alternate host: https://cdn.jsdelivr.net/gh/NateScarlet/holiday-cn@master/{year}.json

WorkManager schedules a connected-network check every 12 hours (actual execution
can be deferred by Android). App startup/resume checks the same per-year freshness
timestamps. Failed years retry after one hour, and the settings screen supports a
forced refresh. All cached years, record years, the preceding year, the current
year and the next year are considered; viewing a historical cycle requests its
years and the adjacent year as well.

Existing years are fetched again, not treated as permanently downloaded. A valid
complete annual snapshot replaces the previous snapshot, including removing old
exceptions. Manual date overrides always take priority. Changes flow through
ClockRepository.settings so home, history and widgets recalculate. A newer year's
notice takes priority for cross-year date exceptions.

Empty, January-only, malformed, oversized and wrong-year responses do not replace
cached schedules. The parser requires official government notice URLs and holiday
coverage across the major annual periods before treating a year as complete. This
also avoids treating an unpublished future-year placeholder as a known schedule.
Downloaded data is stored separately in DataStore; it survives process restarts
and is available offline. Cache metadata and public downloaded calendars are not
part of user settings backups, and are fetched again after restoring onto a new
device. Bundled dates remain the fallback when no complete online year is cached.

This is a local calendar, not an integration with DingTalk. Company-specific
schedules can be matched through per-date overrides. JSON backups and the CSV
schedule column preserve these overrides.
