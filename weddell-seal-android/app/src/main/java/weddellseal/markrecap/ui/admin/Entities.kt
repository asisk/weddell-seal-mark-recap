package weddellseal.markrecap.ui.admin

enum class FileType(val label: String) {
    WEDCHECK("WedCheck File"),
    WEDDATACURRENT("WedData Current"),
    WEDDATAFULL("WedData Full"),
    OBSERVERS("Observer Initials"),
    COLONIES("Seal Colony Locations")
}

enum class FileAction(val label: String) {
    UPLOAD("Upload"),
    DOWNLOAD("Download"),
    PENDING("Pending")
}

enum class FileStatus(val message: String) {
    IDLE(""),
    SUCCESS("Successful"),
    ERROR("Failed")
}

enum class ExportType(val label: String) {
    ALL("All Observations"),
    CURRENT("Current Observations"),
}