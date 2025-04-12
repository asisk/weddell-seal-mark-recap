package weddellseal.markrecap.frameworks.room.files

data class FailedRow(
    val rowNumber: Int,  // The line number or index in the CSV where the failure occurred
    val errorMessage: String  // A message describing what went wrong (e.g., parsing error, validation issue)
) {
    override fun toString(): String {
        return "Row $rowNumber: $errorMessage"
    }
}