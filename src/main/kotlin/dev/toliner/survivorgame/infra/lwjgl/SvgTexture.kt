package dev.toliner.survivorgame.infra.lwjgl

import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE
import org.lwjgl.opengl.GL13.glActiveTexture
import org.lwjgl.opengl.GL13.GL_TEXTURE0
import org.lwjgl.system.MemoryUtil
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.InputStream
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import org.apache.batik.anim.dom.SAXSVGDocumentFactory
import org.apache.batik.bridge.BridgeContext
import org.apache.batik.bridge.GVTBuilder
import org.apache.batik.bridge.UserAgentAdapter
import org.apache.batik.ext.awt.RenderingHintsKeyExt
import org.apache.batik.gvt.GraphicsNode
import org.apache.batik.util.XMLResourceDescriptor

class SvgTexture(private val textureId: Int, val width: Int, val height: Int) {
    fun bind(unit: Int = 0) {
        glActiveTexture(GL_TEXTURE0 + unit)
        glBindTexture(GL_TEXTURE_2D, textureId)
    }
    fun dispose() { glDeleteTextures(textureId) }
}

object SvgLoader {
    fun loadFromResource(path: String, pixelsPerUnit: Int = 64, unitSize: Float = 1f): SvgTexture {
        val stream = SvgLoader::class.java.classLoader.getResourceAsStream(path)
            ?: error("Resource not found: $path")
        stream.use {
            val img = rasterizeSvgToImage(it, pixelsPerUnit, unitSize)
            return uploadImageToTexture(img)
        }
    }

    fun loadFromString(svg: String, pixelsPerUnit: Int = 64, unitSize: Float = 1f): SvgTexture {
        val stream = ByteArrayInputStream(svg.toByteArray(Charsets.UTF_8))
        stream.use {
            val img = rasterizeSvgToImage(it, pixelsPerUnit, unitSize)
            return uploadImageToTexture(img)
        }
    }

    private fun uploadImageToTexture(img: BufferedImage): SvgTexture {
        val (w, h) = img.width to img.height
        val buffer = imageToByteBuffer(img)
        val tex = glGenTextures()
        glBindTexture(GL_TEXTURE_2D, tex)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer)
        MemoryUtil.memFree(buffer)
        return SvgTexture(tex, w, h)
    }

    private fun rasterizeSvgToImage(stream: InputStream, ppu: Int, unitSize: Float): BufferedImage {
        val parser = XMLResourceDescriptor.getXMLParserClassName()
        val f = SAXSVGDocumentFactory(parser)
        val doc = f.createDocument("http://example.com/inline.svg", stream)
        val ua = UserAgentAdapter()
        val ctx = BridgeContext(ua)
        ctx.setDynamicState(BridgeContext.DYNAMIC)
        val builder = GVTBuilder()
        val root: GraphicsNode = builder.build(ctx, doc)
        val width = (doc.documentElement.getAttribute("width").toFloatOrNull() ?: 1f)
        val height = (doc.documentElement.getAttribute("height").toFloatOrNull() ?: 1f)
        val scale = (ppu / unitSize)
        val wpx = (width * scale).toInt().coerceAtLeast(1)
        val hpx = (height * scale).toInt().coerceAtLeast(1)
        val img = BufferedImage(wpx, hpx, BufferedImage.TYPE_INT_ARGB)
        val g2d = img.createGraphics()
        g2d.setRenderingHint(RenderingHintsKeyExt.KEY_TRANSCODING, RenderingHintsKeyExt.VALUE_TRANSCODING_PRINTING)
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.scale(scale.toDouble(), scale.toDouble())
        root.paint(g2d)
        g2d.dispose()
        return img
    }

    private fun imageToByteBuffer(img: BufferedImage): ByteBuffer {
        val w = img.width
        val h = img.height
        val buffer = MemoryUtil.memAlloc(w * h * 4)
        val pixels = IntArray(w * h)
        img.getRGB(0, 0, w, h, pixels, 0, w)
        // OpenGL expects v=0 at the bottom; BufferedImage gives rows top-to-bottom.
        // Write rows in bottom-to-top order to correct vertical orientation.
        for (y in h - 1 downTo 0) {
            for (x in 0 until w) {
                val argb = pixels[y * w + x]
                val a = (argb ushr 24) and 0xFF
                val r = (argb ushr 16) and 0xFF
                val g = (argb ushr 8) and 0xFF
                val b = (argb) and 0xFF
                buffer.put(r.toByte())
                buffer.put(g.toByte())
                buffer.put(b.toByte())
                buffer.put(a.toByte())
            }
        }
        buffer.flip()
        return buffer
    }
}