package weddellseal.markrecap.ui.utils

import java.text.SimpleDateFormat
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

fun getCoordinatesLastUpdatedDate(): String {
    val currentDateTime =
        ZonedDateTime.now(ZoneId.systemDefault()) // Get the current date and time with timezone
    val formatter = DateTimeFormatter.ofPattern(
        "MM.dd.yyyy HH:mm:ss a z",
        Locale.US
    ) // Define the desired format
    return currentDateTime.format(formatter) // Format the current date and time
}


