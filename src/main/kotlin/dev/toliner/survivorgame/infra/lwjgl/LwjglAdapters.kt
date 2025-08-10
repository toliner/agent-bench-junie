package dev.toliner.survivorgame.infra.lwjgl

import dev.toliner.survivorgame.core.math.Vec2
import dev.toliner.survivorgame.core.ports.InputPort
import dev.toliner.survivorgame.core.ports.RandomPort
import dev.toliner.survivorgame.core.ports.TimePort
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.system.MemoryStack

class LwjglInput(private val window: Long) : InputPort {
    override fun movementAxis(): Vec2 {
        var x = 0f
        var y = 0f
        if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS || glfwGetKey(window, GLFW_KEY_LEFT) == GLFW_PRESS) x -= 1f
        if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS || glfwGetKey(window, GLFW_KEY_RIGHT) == GLFW_PRESS) x += 1f
        if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS || glfwGetKey(window, GLFW_KEY_UP) == GLFW_PRESS) y += 1f
        if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS || glfwGetKey(window, GLFW_KEY_DOWN) == GLFW_PRESS) y -= 1f
        return Vec2(x, y)
    }
}

class LwjglTime : TimePort {
    private var last = glfwGetTime().toFloat()
    private var now = last
    override fun deltaTimeSeconds(): Float {
        now = glfwGetTime().toFloat()
        val dt = now - last
        last = now
        return dt
    }
    override fun nowSeconds(): Float = now
}

class LwjglRandom(seed: Long = 0L) : RandomPort {
    private val rnd = java.util.Random(seed)
    override fun nextFloat01(): Float = rnd.nextFloat()
}
