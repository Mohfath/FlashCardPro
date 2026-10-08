package com.matt.flashcard

/** [country] is an ISO 3166 region code, turned into a flag emoji by [flag]. */
data class Language(val code: String, val name: String, val country: String, val latin: Boolean = true) {
    val flag: String get() = country.uppercase().map { String(Character.toChars(0x1F1E6 + (it - 'A'))) }.joinToString("")
}

/** Common languages, alphabetical by name. */
val Languages = listOf(
    Language("en", "English", "GB"),
    Language("zh", "Chinese", "CN", latin = false),
    Language("hi", "Hindi", "IN", latin = false),
    Language("es", "Spanish", "ES"),
    Language("fr", "French", "FR"),
    Language("ar", "Arabic", "SA", latin = false),
    Language("pt", "Portuguese", "PT"),
    Language("ru", "Russian", "RU", latin = false),
    Language("id", "Indonesian", "ID"),
    Language("de", "German", "DE"),
    Language("ja", "Japanese", "JP", latin = false),
    Language("tr", "Turkish", "TR"),
    Language("vi", "Vietnamese", "VN"),
    Language("ko", "Korean", "KR", latin = false),
    Language("it", "Italian", "IT"),
    Language("th", "Thai", "TH", latin = false),
    Language("fa", "Persian", "IR", latin = false),
    Language("pl", "Polish", "PL"),
    Language("uk", "Ukrainian", "UA", latin = false),
    Language("nl", "Dutch", "NL"),
    Language("hr", "Croatian", "HR"),
    Language("el", "Greek", "GR", latin = false),
    Language("cs", "Czech", "CZ"),
    Language("sv", "Swedish", "SE"),
    Language("ro", "Romanian", "RO"),
    Language("hu", "Hungarian", "HU"),
    Language("da", "Danish", "DK"),
).sortedBy { it.name }

fun languageFor(code: String): Language? = Languages.firstOrNull { it.code == code }

internal fun languageName(code: String) = languageFor(code)?.name?.uppercase() ?: code.uppercase()
