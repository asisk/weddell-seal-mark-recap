package weddellseal.markrecap.domain.tagretag.data

data class TagProcessingResult(
    val tagValid: Boolean,
    val tagAlpha: String,
    val tagNumber: String
)

fun processTags(tag: String?): TagProcessingResult {
    var tagValid = false
    var finalTagAlpha = ""
    var finalTagNumber = ""

    fun validateTag(tag: String?) {

        //verify that the tag is the valid format
        if (tag.isNullOrBlank() || tag == "NA" || tag == "NoTag") return
        if (tag.dropLast(1) == "") return
        if (!tag.last().isLetter()) return

        tagValid = true
        finalTagAlpha = tag.last().toString()
        finalTagNumber = tag.dropLast(1)
    }

    validateTag(tag)

    return TagProcessingResult(tagValid, finalTagAlpha, finalTagNumber)
}