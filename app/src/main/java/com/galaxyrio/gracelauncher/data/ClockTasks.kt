package com.galaxyrio.gracelauncher.data

import org.json.JSONArray
import org.json.JSONObject

enum class ClockTaskField { Title, Countdown, DueDate, DueTime, StartDate, Priority, List, Notes, Color }

data class ClockTasks(
    val order: List<ClockTaskField> = ClockTaskField.entries,
    val selected: Set<ClockTaskField> = setOf(ClockTaskField.Title, ClockTaskField.Countdown),
    val typography: ClockRowTypography = ClockRowTypography(),
    val taskCount: Int = 1,
    val useLatinLetters: Boolean = false,
    val alignment: ClockAlignment = ClockAlignment.Left,
) {
    val visibleFields: List<ClockTaskField> get() = normalized().order.filter { it in selected }

    fun normalized() = copy(order = (order + ClockTaskField.entries).distinct(), typography = typography.normalized(),
        taskCount = taskCount.coerceIn(0, 9))

    internal fun json(): JSONObject = normalized().let { value -> JSONObject().apply {
        put("order", JSONArray(value.order.map { it.name }))
        put("selected", JSONArray(value.order.filter { it in value.selected }.map { it.name }))
        put("typography", JSONObject().apply {
            put("size", value.typography.size)
            put("weight", value.typography.weight)
            put("fontShadow", value.typography.fontShadow)
            put("fontId", value.typography.fontId ?: JSONObject.NULL)
        })
        put("taskCount", value.taskCount)
        put("useLatinLetters", value.useLatinLetters)
        put("alignment", value.alignment.name)
    } }

    companion object {
        internal fun decode(json: JSONObject?): ClockTasks {
            val defaults = ClockTasks()
            if (json == null) return defaults
            fun fields(key: String): List<ClockTaskField>? = json.optJSONArray(key)?.let { array ->
                (0 until array.length()).mapNotNull { index ->
                    ClockTaskField.entries.firstOrNull { it.name == array.optString(index) }
                }.distinct()
            }
            val typography = json.optJSONObject("typography")
            return ClockTasks(
                order = fields("order") ?: defaults.order,
                selected = fields("selected")?.toSet() ?: defaults.selected,
                typography = ClockRowTypography(
                    size = typography?.optInt("size", defaults.typography.size) ?: defaults.typography.size,
                    weight = typography?.optInt("weight", defaults.typography.weight) ?: defaults.typography.weight,
                    fontShadow = typography?.optInt("fontShadow", defaults.typography.fontShadow) ?: defaults.typography.fontShadow,
                    fontId = typography?.optString("fontId")?.takeIf { it.isNotBlank() && it != "null" },
                ),
                taskCount = json.optInt("taskCount", defaults.taskCount),
                useLatinLetters = json.optBoolean("useLatinLetters", defaults.useLatinLetters),
                alignment = ClockAlignment.entries.firstOrNull { it.name == json.optString("alignment") } ?: defaults.alignment,
            ).normalized()
        }
    }
}
