package weddellseal.markrecap.ui.utils

import android.util.Log
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

// yyyy-MM-dd
fun getCurrentDateFormatted(): String {
    val currentDate = LocalDate.now()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    return currentDate.format(formatter)
}

// HH:mm:ss
fun getCurrentTimeFormatted(): String {
    val currentTime = LocalTime.now()
    val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    return currentTime.format(formatter)
}

fun getFileExportDateTime(): String {
    val dateTimeFormat =
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
    return dateTimeFormat.format(Date())
}

// yyyy-MM-dd HH:mm:ss
fun formatFileUploadedDateTime(input: Long): String {
    return SimpleDateFormat(
        "yyyy-MM-dd HH:mm:ss",
        Locale.US
    ).format(Date(input))
}

fun getCurrentYear(): Int {
    return LocalDate.now().year
}

fun getLastYear(): Int {
    return LocalDate.now().year - 1
}

fun getTwoYearsAgo(): Int {
    return LocalDate.now().year - 2
}

fun getYearWithinTenYears(): Int {
    return LocalDate.now().year - 6
}

fun getOverTenYearsAgo(): Int {
    return LocalDate.now().year - 12
}

// What’s my system’s current offset from UTC?
// Apply that offset to the UTC Instant to get the local wall-clock time with the numeric offset
// If the device is set to America/Denver on Aug 14, 2025, at 12:00 local: offset.offset → -06:00 (because MDT is UTC-6 in summer)
// If the device is in Antarctica/Casey: offset.offset → +08:00
fun getCoordinatesLastUpdatedDate(): String {
    val offset =
        ZonedDateTime.now(ZoneId.systemDefault())
    val nowAtOffset = Instant.now().atOffset(offset.offset)
    val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd    HH:mm:ss    xxx")
    return nowAtOffset.format(formatter) + "  UTC"
}


