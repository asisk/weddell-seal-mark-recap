package weddellseal.markrecap.frameworks.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class OfflineMapAssetsTest {

    @Test
    fun ensureCopiedReturnsPreparedPackWhenStylePresent() {
        val context = RuntimeEnvironment.getApplication()
        val prepared = OfflineMapAssets.ensureCopied(context)
        assertNotNull(prepared)
        assertTrue(prepared!!.styleFile.isFile)
        assertEquals("2025.09.24d", prepared.packVersion)
        // Absolute paths must be baked in for MapLibre Native.
        val styleText = prepared.styleFile.readText()
        assertFalse(styleText.contains(OfflineMapAssets.PACK_ROOT_TOKEN))
        assertTrue(styleText.contains("mbtiles://${prepared.styleFile.parentFile!!.absolutePath}/region.mbtiles"))
        assertTrue(styleText.contains("glyphs/{fontstack}/{range}.pbf"))
        // MapLibre percent-decodes file:// glyph paths → folders must use real spaces.
        val glyphsDir = File(prepared.styleFile.parentFile, "glyphs/Open Sans Regular")
        assertTrue(glyphsDir.isDirectory)
        assertTrue(File(glyphsDir, "0-255.pbf").isFile)
    }

    @Test
    fun rewriteStylePackRootReplacesToken() {
        val dir = File(RuntimeEnvironment.getApplication().filesDir, "rewrite-test").apply {
            mkdirs()
        }
        val style = File(dir, "style.json")
        style.writeText("""{"sources":{"b":{"url":"mbtiles://${OfflineMapAssets.PACK_ROOT_TOKEN}/region.mbtiles"}}}""")
        OfflineMapAssets.rewriteStylePackRoot(style, dir)
        val text = style.readText()
        assertFalse(text.contains(OfflineMapAssets.PACK_ROOT_TOKEN))
        assertTrue(text.contains("mbtiles://${dir.absolutePath}/region.mbtiles"))
    }
}
