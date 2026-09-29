package app.xalidmuslim.azkar.persistence

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import java.time.LocalDate

internal object AzkarPreferenceKeys {
    val RussianFont = stringPreferencesKey("reader_russian_font")
    val ArabicFont = stringPreferencesKey("reader_arabic_font")
    val ArabicSize = floatPreferencesKey("reader_arabic_size")
    val RussianSize = floatPreferencesKey("reader_russian_size")
    val LineHeight = floatPreferencesKey("reader_line_height")
    val ReaderStyle = stringPreferencesKey("reader_style")
    val ShowTranslation = booleanPreferencesKey("reader_show_translation")
    val ShowSources = booleanPreferencesKey("reader_show_sources")
    val ShowNotes = booleanPreferencesKey("reader_show_notes")
    val Theme = stringPreferencesKey("reader_theme")
    val ViewMode = stringPreferencesKey("reader_view_mode")

    fun progress(date: LocalDate, stableDhikrId: String) =
        intPreferencesKey("progress_${date}_${stableDhikrId}")
}
