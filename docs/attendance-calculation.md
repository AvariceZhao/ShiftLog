# Attendance Statistics

Home effective-attendance and history cycle-summary cards share AttendanceCalculator.
The selected pay cycle is retained; set the start day to 1 for a calendar-month report.

- Workdays follow the national holiday/make-up calendar and manual per-date overrides.
- Expected time is the entire cycle's workdays multiplied by 480 minutes.
- Punch time is the stored clock-in to clock-out span, without lunch subtraction.
- Effective time is punch time. For compatibility, previously recorded course minutes
  are still included; the app no longer offers a course-time input.
- Credited attendance time sums min(effective time, 480 minutes) per workday.
- Full-cycle attendance rate is credited time / expected time. It is undefined when
  there are no workdays, and overtime cannot compensate for another day's missing hours.
- The home and history cards display CURRENT attendance rate: credited time divided
  by 480 minutes times elapsed/closed workdays. Include workdays whose scheduled end
  has passed, and any started workday already containing a completed punch pair or
  course-only time. Exclude future and still-open workdays from both numerator and
  denominator. With no eligible day the rate is undefined; a past cycle has the same
  rate as the full-cycle calculation.
- Both cards show only four metrics in a two-by-two layout: credited attendance days,
  current attendance rate, capped attendance duration in hours/minutes, absence credits.
- Time distribution uses >=8h, >=6h and <8h, and <6h. These categories remain calculated
  separately from configurable attendance/absence credit tiers but are not shown on cards.
- Default credits: <6h = 0 attendance and 0.5 absence; 6-8h = 0.5 attendance and
  0 absence; >=8h = 1 attendance and 0 absence. Saved custom rules remain unchanged.
- A workday without punches or course time becomes one full absence after shift end.
- Leave days are separately counted, with neither attendance nor absence credits.
  They remain in the workday/time denominator, matching the report's fixed divisor.
- Rest days contribute no hours, attendance, absence or leave counts.
- Future dates and open incomplete shifts are pending. An incomplete punch cannot
  establish a time span; after shift end its duration is course time (or zero) and
  it is evaluated by the corresponding tier, with a separate incomplete-day count.
- Each workday belongs to exactly one distribution category: full, half, short,
  fully absent, leave, or pending. Incomplete punches are a subset, not extra days.
- The reported example's 135.5 absence total contradicts its written rules; the
  implementation follows the written defaults rather than inferring new penalties.

Course minutes are stored per shift date, preserved by punching, and included in
JSON backups and CSV exports/imports. Database version 4 migrates existing records
with zero course minutes. Export columns distinguish raw, effective and capped time.
