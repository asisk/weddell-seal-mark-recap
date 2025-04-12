package weddellseal.markrecap.ui.file

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