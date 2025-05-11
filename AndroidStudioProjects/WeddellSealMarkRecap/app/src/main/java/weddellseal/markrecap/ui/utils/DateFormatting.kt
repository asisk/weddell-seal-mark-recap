package weddellseal.markrecap.ui.utils

import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
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



