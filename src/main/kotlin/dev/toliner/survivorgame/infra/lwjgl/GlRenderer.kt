package dev.toliner.survivorgame.infra.lwjgl

import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL30.*
import org.lwjgl.opengl.GL11.*
import org.lwjgl.system.MemoryUtil

class GlRenderer {
    private var program = 0
    private var vao = 0
    private var vbo = 0

    fun init() {
        program = createProgram(
            vertexSrc = """
                #version 330 core
                layout (location = 0) in vec2 aPos;
                layout (location = 1) in vec2 aUV;
                uniform mat4 uProj;
                uniform vec2 uPos; // world position (center)
                uniform vec2 uSize; // world size (width,height)
                out vec2 vUV;
                void main(){
                  vec2 pos = aPos * uSize + uPos;
                  gl_Position = uProj * vec4(pos, 0.0, 1.0);
                  vUV = aUV;
                }
            """.trimIndent(),
            fragmentSrc = """
                #version 330 core
                in vec2 vUV;
                out vec4 FragColor;
                uniform sampler2D uTex;
                void main(){
                  FragColor = texture(uTex, vUV);
                }
            """.trimIndent()
        )
        // quad
        val vertices = floatArrayOf(
            // x,y, u,v
            -0.5f, -0.5f, 0f, 0f,
             0.5f, -0.5f, 1f, 0f,
             0.5f,  0.5f, 1f, 1f,
            -0.5f,  0.5f, 0f, 1f
        )
        val indices = intArrayOf(0,1,2, 2,3,0)
        vao = glGenVertexArrays()
        glBindVertexArray(vao)
        vbo = glGenBuffers()
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        val vbuf = MemoryUtil.memAllocFloat(vertices.size)
        vbuf.put(vertices).flip()
        glBufferData(GL_ARRAY_BUFFER, vbuf, GL_STATIC_DRAW)
        MemoryUtil.memFree(vbuf)
        val ebo = glGenBuffers()
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo)
        val ibuf = MemoryUtil.memAllocInt(indices.size)
        ibuf.put(indices).flip()
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, ibuf, GL_STATIC_DRAW)
        MemoryUtil.memFree(ibuf)
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * 4, 0L)
        glEnableVertexAttribArray(0)
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * 4, (2 * 4).toLong())
        glEnableVertexAttribArray(1)
        glBindVertexArray(0)
    }

    fun begin(width: Int, height: Int, worldHalfExtent: Float) {
        glViewport(0, 0, width, height)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glClearColor(0.08f, 0.08f, 0.1f, 1f)
        glClear(GL_COLOR_BUFFER_BIT)
        glUseProgram(program)
        val proj = orthoMatrix(-worldHalfExtent, worldHalfExtent, -worldHalfExtent * height/width.toFloat(), worldHalfExtent * height/width.toFloat())
        val loc = glGetUniformLocation(program, "uProj")
        glUniformMatrix4fv(loc, false, proj)
    }

    fun draw(tex: SvgTexture, cx: Float, cy: Float, size: Float) {
        glUseProgram(program)
        val locPos = glGetUniformLocation(program, "uPos")
        val locSize = glGetUniformLocation(program, "uSize")
        glUniform2f(locPos, cx, cy)
        glUniform2f(locSize, size, size)
        glActiveTexture(GL_TEXTURE0)
        tex.bind(0)
        val locTex = glGetUniformLocation(program, "uTex")
        glUniform1i(locTex, 0)
        glBindVertexArray(vao)
        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0)
        glBindVertexArray(0)
    }

    fun drawWH(tex: SvgTexture, cx: Float, cy: Float, width: Float, height: Float) {
        glUseProgram(program)
        val locPos = glGetUniformLocation(program, "uPos")
        val locSize = glGetUniformLocation(program, "uSize")
        glUniform2f(locPos, cx, cy)
        glUniform2f(locSize, width, height)
        glActiveTexture(GL_TEXTURE0)
        tex.bind(0)
        val locTex = glGetUniformLocation(program, "uTex")
        glUniform1i(locTex, 0)
        glBindVertexArray(vao)
        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0)
        glBindVertexArray(0)
    }

    fun dispose() {
        if (program != 0) glDeleteProgram(program)
        if (vbo != 0) glDeleteBuffers(vbo)
        if (vao != 0) glDeleteVertexArrays(vao)
    }

    private fun createProgram(vertexSrc: String, fragmentSrc: String): Int {
        val vs = glCreateShader(GL_VERTEX_SHADER)
        glShaderSource(vs, vertexSrc)
        glCompileShader(vs)
        checkCompile(vs)
        val fs = glCreateShader(GL_FRAGMENT_SHADER)
        glShaderSource(fs, fragmentSrc)
        glCompileShader(fs)
        checkCompile(fs)
        val prog = glCreateProgram()
        glAttachShader(prog, vs)
        glAttachShader(prog, fs)
        glLinkProgram(prog)
        if (glGetProgrami(prog, GL_LINK_STATUS) == GL_FALSE) {
            error("Program link error: ${'$'}{glGetProgramInfoLog(prog)}")
        }
        glDeleteShader(vs)
        glDeleteShader(fs)
        return prog
    }

    private fun checkCompile(shader: Int) {
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            error("Shader compile error: ${'$'}{glGetShaderInfoLog(shader)}")
        }
    }

    private fun orthoMatrix(l: Float, r: Float, b: Float, t: Float): java.nio.FloatBuffer {
        val n = -1f
        val f = 1f
        val m = floatArrayOf(
            2f / (r - l), 0f, 0f, 0f,
            0f, 2f / (t - b), 0f, 0f,
            0f, 0f, -2f / (f - n), 0f,
            -(r + l) / (r - l), -(t + b) / (t - b), -(f + n) / (f - n), 1f
        )
        val buf = MemoryUtil.memAllocFloat(16)
        buf.put(m).flip()
        return buf
    }
}