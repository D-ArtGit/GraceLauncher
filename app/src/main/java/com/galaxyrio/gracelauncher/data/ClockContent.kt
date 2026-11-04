package com.galaxyrio.gracelauncher.data

import org.json.JSONArray
import org.json.JSONObject

enum class ClockAgendaField { Title, Countdown, Time, Date, Duration, Location, Color, Calendar }

enum class ClockWeatherField { Icon, Temperature, Description, FeelsLike, Humidity, Wind, AirQuality, UV, Location, HighLow, UpdatedAt }

data class ClockWeather(
    val order: List<ClockWeatherField> = ClockWeatherField.entries,
    val selected: Set<ClockWeatherField> = setOf(ClockWeatherField.Icon, ClockWeatherField.Temperature),
) {
    val visibleFields: List<ClockWeatherField> get() = normalized().order.filter { it in selected }

    fun normalized() = copy(order = (order + ClockWeatherField.entries).distinct())

    internal fun json(): JSONObject = normalized().let { value -> JSONObject().apply {
        put("order", JSONArray(value.order.map { it.name }))
        put("selected", JSONArray(value.order.filter { it in value.selected }.map { it.name }))
    } }

    companion object {
        internal fun decode(json: JSONObject?): ClockWeather {
            val defaults = ClockWeather()
            if (json == null) return defaults
            return ClockWeather(
                order = json.enumList("order", ClockWeatherField.entries) ?: defaults.order,
                selected = json.enumList("selected", ClockWeatherField.entries)?.toSet() ?: defaults.selected,
            ).normalized()
        }
    }
}

data class ClockAgenda(
    val order: List<ClockAgendaField> = ClockAgendaField.entries,
    val selected: Set<ClockAgendaField> = setOf(ClockAgendaField.Title, ClockAgendaField.Countdown),
    val typography: ClockRowTypography = ClockRowTypography(),
    val eventCount: Int = 1,
    val useLatinLetters: Boolean = false,
) {
    val visibleFields: List<ClockAgendaField> get() = normalized().order.filter { it in selected }

    fun normalized() = copy(order = (order + ClockAgendaField.entries).distinct(), typography = typography.normalized(),
        eventCount = eventCount.coerceIn(0, 9))

    internal fun json(): JSONObject = normalized().let { value -> JSONObject().apply {
        put("order", JSONArray(value.order.map { it.name }))
        put("selected", JSONArray(value.order.filter { it in value.selected }.map { it.name }))
        put("typography", JSONObject().apply {
            put("size", value.typography.size)
            put("weight", value.typography.weight)
            put("fontShadow", value.typography.fontShadow)
            put("fontId", value.typography.fontId ?: JSONObject.NULL)
        })
        put("eventCount", value.eventCount)
        put("useLatinLetters", value.useLatinLetters)
    } }

    companion object {
        internal fun decode(json: JSONObject?): ClockAgenda {
            val defaults = ClockAgenda()
            if (json == null) return defaults
            val typography = json.optJSONObject("typography")
            return ClockAgenda(
                order = json.enumList("order", ClockAgendaField.entries) ?: defaults.order,
                selected = json.enumList("selected", ClockAgendaField.entries)?.toSet() ?: defaults.selected,
                typography = ClockRowTypography(
                    typography?.optInt("size", defaults.typography.size) ?: defaults.typography.size,
                    typography?.optInt("weight", defaults.typography.weight) ?: defaults.typography.weight,
                    typography?.optInt("fontShadow", defaults.typography.fontShadow) ?: defaults.typography.fontShadow,
                    typography?.optString("fontId")?.takeIf { it.isNotBlank() && it != "null" },
                ),
                eventCount = json.optInt("eventCount", defaults.eventCount),
                useLatinLetters = json.optBoolean("useLatinLetters", defaults.useLatinLetters),
            ).normalized()
        }
    }
}

private fun <T : Enum<T>> JSONObject.enumList(key: String, options: List<T>): List<T>? = optJSONArray(key)?.let { array ->
    (0 until array.length()).mapNotNull { index -> options.firstOrNull { it.name == array.optString(index) } }.distinct()
}
