package com.clockin.app.holiday

import com.clockin.app.domain.HolidayCalendarYear
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.time.LocalDate

object HolidayCalendarJson {
    /** Empty and January-only future-year placeholders are not a complete annual schedule. */
    fun parse(text: String, expectedYear: Int): HolidayCalendarYear? {
        val root = JSONObject(text)
        require(root.getInt("year") == expectedYear) { "节假日年份不匹配" }
        val paperArray = root.getJSONArray("papers")
        val papers = (0 until paperArray.length()).map { index ->
            paperArray.getString(index).also { paper ->
                val uri = URI(paper)
                val host = uri.host?.lowercase().orEmpty()
                require(uri.scheme in setOf("https", "http") &&
                    (host == "gov.cn" || host.endsWith(".gov.cn"))) { "节假日公告来源无效" }
            }
        }
        val array = root.getJSONArray("days")
        val days = mutableMapOf<LocalDate, Boolean>()
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            val date = LocalDate.parse(item.getString("date"))
            require(date.year in (expectedYear - 1)..(expectedYear + 1)) { "节假日日期超出范围" }
            val offDay = item.get("isOffDay")
            require(offDay is Boolean) { "节假日类型无效" }
            require(!days.containsKey(date) || days[date] == offDay) { "节假日日期冲突" }
            days[date] = offDay
        }
        val holidayMonths = days.filter { (date, offDay) -> date.year == expectedYear && offDay }
            .keys.map { it.monthValue }.toSet()
        if (papers.isEmpty() || !holidayMonths.containsAll(setOf(1, 4, 5, 10)) ||
            holidayMonths.none { it in 2..3 }
        ) return null
        return HolidayCalendarYear(expectedYear, days.toMap(), papers)
    }

    fun encode(calendar: HolidayCalendarYear): JSONObject = JSONObject().apply {
        put("year", calendar.year)
        put("papers", JSONArray(calendar.papers))
        put("days", JSONArray().apply {
            calendar.days.toSortedMap().forEach { (date, offDay) ->
                put(JSONObject().put("date", date.toString()).put("isOffDay", offDay))
            }
        })
    }
}
