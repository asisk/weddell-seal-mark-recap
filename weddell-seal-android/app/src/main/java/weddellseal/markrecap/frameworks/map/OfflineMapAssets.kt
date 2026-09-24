package weddellseal.markrecap.frameworks.map

import android.content.Context
import android.util.Log
import java.io.File
import java.io.IOException

private const val TAG = "OfflineMapAssets"
private const val ASSET_DIR = "map"
private const val PACK_VERSION_FILE = "PACK_VERSION"
private const val STYLE_FILE = "style.json"

/**
 * Copies the bundled MapLibre pack from assets to a seekable [filesDir] tree when
 * [PACK_VERSION] changes. First-open-only copy is intentionally not used.
 */
object OfflineMapAssets {

    /** Placeholder in bundled style.json; replaced with absolute filesDir/map path. */
    const val PACK_ROOT_TOKEN = "__PACK_ROOT__"

    data class PreparedPack(
        val styleFile: File,
        val packVersion: String,
    )

    /**
     * Ensures `filesDir/map` matches the assets pack stamp.
     * @return prepared style path, or null if the pack is incomplete (e.g. style missing).
     */
    fun ensureCopied(context: Context): PreparedPack? {
        val assetVersion = readAssetText(context, "$ASSET_DIR/$PACK_VERSION_FILE")?.trim().orEmpty()
        if (assetVersion.isEmpty()) {
            Log.w(TAG, "Missing assets/$ASSET_DIR/$PACK_VERSION_FILE")
            return null
        }

        val destRoot = File(context.filesDir, ASSET_DIR)
        val destVersionFile = File(destRoot, PACK_VERSION_FILE)
        val destStyle = File(destRoot, STYLE_FILE)
        val installedVersion = destVersionFile.takeIf { it.isFile }?.readText()?.trim().orEmpty()

        if (installedVersion != assetVersion || !destStyle.isFile) {
            Log.i(TAG, "Refreshing map pack: installed='$installedVersion' asset='$assetVersion'")
            copyPackAtomically(context, destRoot)
        }

        if (!destStyle.isFile) {
            Log.w(TAG, "Map pack incomplete: ${destStyle.absolutePath} missing after copy")
            return null
        }

        rewriteStylePackRoot(destStyle, destRoot)

        val version = destVersionFile.takeIf { it.isFile }?.readText()?.trim() ?: assetVersion
        return PreparedPack(styleFile = destStyle, packVersion = version)
    }

    fun styleUri(styleFile: File): String = "file://${styleFile.absolutePath}"

    /**
     * Rewrites `mbtiles://__PACK_ROOT__/…` to an absolute `mbtiles:///` path under [packRoot].
     */
    internal fun rewriteStylePackRoot(styleFile: File, packRoot: File) {
        if (!styleFile.isFile) return
        val original = styleFile.readText()
        if (!original.contains(PACK_ROOT_TOKEN)) return
        val rewritten = original.replace(PACK_ROOT_TOKEN, packRoot.absolutePath)
        styleFile.writeText(rewritten)
        Log.i(TAG, "Rewrote style pack root → ${packRoot.absolutePath}")
    }

    private fun copyPackAtomically(context: Context, destRoot: File) {
        val parent = destRoot.parentFile ?: context.filesDir
        val tempRoot = File(parent, "$ASSET_DIR.tmp-${System.currentTimeMillis()}")
        if (tempRoot.exists()) {
            tempRoot.deleteRecursively()
        }
        if (!tempRoot.mkdirs()) {
            throw IOException("Could not create temp map pack dir: ${tempRoot.absolutePath}")
        }

        try {
            copyAssetTree(context, ASSET_DIR, tempRoot)
            if (destRoot.exists()) {
                destRoot.deleteRecursively()
            }
            if (!tempRoot.renameTo(destRoot)) {
                // Fallback if rename across filesystems fails.
                tempRoot.copyRecursively(destRoot, overwrite = true)
                tempRoot.deleteRecursively()
            }
        } catch (e: Exception) {
            tempRoot.deleteRecursively()
            throw e
        }
    }

    private fun copyAssetTree(context: Context, assetPath: String, destDir: File) {
        val children = context.assets.list(assetPath) ?: emptyArray()
        if (children.isEmpty()) {
            // Leaf file.
            val parent = destDir.parentFile
            if (parent != null && !parent.isDirectory && !parent.mkdirs()) {
                throw IOException("Could not create ${parent.absolutePath}")
            }
            context.assets.open(assetPath).use { input ->
                destDir.outputStream().use { output -> input.copyTo(output) }
            }
            return
        }
        if (!destDir.mkdirs() && !destDir.isDirectory) {
            throw IOException("Could not create ${destDir.absolutePath}")
        }
        for (child in children) {
            val childAsset = "$assetPath/$child"
            val childDest = File(destDir, child)
            val grandChildren = context.assets.list(childAsset)
            if (grandChildren != null && grandChildren.isNotEmpty()) {
                copyAssetTree(context, childAsset, childDest)
            } else {
                val parent = childDest.parentFile
                if (parent != null && !parent.isDirectory && !parent.mkdirs()) {
                    throw IOException("Could not create ${parent.absolutePath}")
                }
                context.assets.open(childAsset).use { input ->
                    childDest.outputStream().use { output -> input.copyTo(output) }
                }
            }
        }
    }

    private fun readAssetText(context: Context, path: String): String? =
        try {
            context.assets.open(path).bufferedReader().use { it.readText() }
        } catch (_: IOException) {
            null
        }
}
