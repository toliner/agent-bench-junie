package dev.toliner.survivorgame.app

import dev.toliner.survivorgame.core.GameState
import dev.toliner.survivorgame.infra.lwjgl.*
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL

class GameApp {
    fun run() {
        if (!glfwInit()) error("Failed to init GLFW")
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3)
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)
        val window = glfwCreateWindow(960, 540, "Survivor Mini", 0, 0)
        if (window == 0L) error("Failed to create window")
        glfwMakeContextCurrent(window)
        glfwSwapInterval(1)
        GL.createCapabilities()

        val renderer = GlRenderer()
        renderer.init()

        val playerTex = SvgLoader.loadFromResource("svg/player.svg", pixelsPerUnit = 128, unitSize = 1f)
        val enemyTex = SvgLoader.loadFromResource("svg/enemy.svg", pixelsPerUnit = 128, unitSize = 1f)
        val bulletTex = SvgLoader.loadFromResource("svg/bullet.svg", pixelsPerUnit = 128, unitSize = 1f)
        val gameOverTex = SvgLoader.loadFromResource("svg/gameover.svg", pixelsPerUnit = 64, unitSize = 1f)

        val input = LwjglInput(window)
        val time = LwjglTime()
        val rng = LwjglRandom(0)
        var game = GameState()

        var hudTex: SvgTexture? = null
        var lastHudLabel: String? = null

        fun buildHudSvg(label: String): String {
            // size in world units encoded as SVG width/height
            val w = 8f
            val h = 1.4f
            return """
                <svg xmlns='http://www.w3.org/2000/svg' width='${w}' height='${h}' viewBox='0 0 ${w} ${h}'>
                  <rect x='0' y='0' width='${w}' height='${h}' rx='0.1' ry='0.1' fill='rgba(0,0,0,0.4)' />
                  <g fill='#ffffff' font-family='Arial, Helvetica, sans-serif' font-size='0.9' font-weight='bold'>
                    <text x='0.4' y='0.98'>${label}</text>
                  </g>
                </svg>
            """.trimIndent()
        }

        while (!glfwWindowShouldClose(window)) {
            // delta time will be consumed inside core update
            game.update(input, time, rng)

            val fbw = IntArray(1)
            val fbh = IntArray(1)
            glfwGetFramebufferSize(window, fbw, fbh)
            renderer.begin(fbw[0], fbh[0], game.arenaRadius)

            val snap = game.snapshot(time.nowSeconds())
            // draw player
            renderer.draw(playerTex, snap.player.position.x, snap.player.position.y, snap.player.radius * 2)
            // draw bullets
            for (b in snap.bullets) {
                renderer.draw(bulletTex, b.position.x, b.position.y, b.radius * 2)
            }
            // draw enemies
            for (e in snap.enemies) {
                renderer.draw(enemyTex, e.position.x, e.position.y, e.radius * 2)
            }

            // HUD: show HP and elapsed time
            val timeStr = "%.1fs".format(kotlin.math.max(0f, snap.timeSeconds)).replace(',', '.')
            val hudLabel = "HP: ${snap.playerHp}    TIME: $timeStr"
            if (hudLabel != lastHudLabel) {
                hudTex?.dispose()
                val svg = buildHudSvg(hudLabel)
                hudTex = SvgLoader.loadFromString(svg, pixelsPerUnit = 64, unitSize = 1f)
                lastHudLabel = hudLabel
            }
            hudTex?.let { tex ->
                val vHalf = game.arenaRadius * (fbh[0].toFloat() / fbw[0].toFloat())
                val hudW = 8f
                val hudH = 1.4f
                val marginX = 0.4f
                val marginY = 0.4f
                val cx = -game.arenaRadius + marginX + hudW / 2f
                val cy = vHalf - marginY - hudH / 2f
                renderer.drawWH(tex, cx, cy, hudW, hudH)
            }

            // Game Over overlay
            if (game.isGameOver()) {
                // size overlay to cover a large portion of view; arenaRadius*1.6 width seems fine
                val overlaySize = game.arenaRadius * 1.6f
                renderer.draw(gameOverTex, 0f, 0f, overlaySize)
                // Restart on R key
                if (glfwGetKey(window, GLFW_KEY_R) == GLFW_PRESS) {
                    game = GameState()
                    lastHudLabel = null // force HUD refresh for new game
                }
            }

            glfwSwapBuffers(window)
            glfwPollEvents()
            if (glfwGetKey(window, GLFW_KEY_ESCAPE) == GLFW_PRESS) glfwSetWindowShouldClose(window, true)
        }
        renderer.dispose()
        playerTex.dispose()
        enemyTex.dispose()
        bulletTex.dispose()
        gameOverTex.dispose()
        hudTex?.dispose()
        glfwTerminate()
    }
}