package dev.toliner.survivorgame.core

import dev.toliner.survivorgame.core.math.Vec2
import dev.toliner.survivorgame.core.ports.InputPort
import dev.toliner.survivorgame.core.ports.RandomPort
import dev.toliner.survivorgame.core.ports.TimePort
import dev.toliner.survivorgame.core.world.SpriteView
import dev.toliner.survivorgame.core.world.WorldSnapshot
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private data class Enemy(var pos: Vec2, val speed: Float, val radius: Float)
private data class Projectile(var pos: Vec2, var vel: Vec2, val radius: Float, var life: Float, var remainingRange: Float)

class GameState(
    val arenaRadius: Float = 20f,
    val playerRadius: Float = 0.8f,
    val playerSpeed: Float = 8f,
    val enemyRadius: Float = 0.7f,
    val enemySpeed: Float = 2.5f,
    val enemySpawnIntervalSec: Float = 1.2f,
    // attack parameters
    val attackIntervalSec: Float = 0.6f,
    val projectileSpeed: Float = 12f,
    val projectileRadius: Float = 0.2f,
    val projectileLifetimeSec: Float = 2.5f,
    val projectileRange: Float = 5f,
    var playerHp: Int = 5,
    private var timeAccumulator: Float = 0f,
    private var attackAccumulator: Float = 0f,
    var playerPos: Vec2 = Vec2(0f, 0f),
) {
    private val enemies: MutableList<Enemy> = mutableListOf()
    private val projectiles: MutableList<Projectile> = mutableListOf()
    fun isGameOver(): Boolean = playerHp <= 0

    fun update(input: InputPort, time: TimePort, rng: RandomPort): GameState {
        if (isGameOver()) return this
        val dt = time.deltaTimeSeconds().coerceAtLeast(0f)
        timeAccumulator += dt
        attackAccumulator += dt
        // Move player
        val move = input.movementAxis().normalized() * (playerSpeed * dt)
        var newPos = playerPos + move
        if (newPos.length() > (arenaRadius - playerRadius)) {
            val dir = newPos.normalized()
            newPos = dir * (arenaRadius - playerRadius)
        }
        playerPos = newPos

        // spawn enemies
        spawnEnemyIfNeeded(rng)

        // auto-attack: spawn projectile towards nearest enemy at fixed interval
        spawnProjectileIfNeeded()

        // move enemies toward player
        for (e in enemies) {
            val dir = Vec2(playerPos.x - e.pos.x, playerPos.y - e.pos.y)
            val angle = atan2(dir.y, dir.x)
            val step = Vec2(cos(angle), sin(angle)) * (e.speed * dt)
            e.pos = e.pos + step
        }

        // move projectiles and expire
        if (projectiles.isNotEmpty()) {
            val it = projectiles.iterator()
            while (it.hasNext()) {
                val p = it.next()
                p.pos = p.pos + (p.vel * dt)
                p.life -= dt
                p.remainingRange -= (kotlin.math.sqrt((p.vel.x * p.vel.x + p.vel.y * p.vel.y)) * dt)
                if (p.life <= 0f || p.remainingRange <= 0f) it.remove()
            }
        }

        // collisions
        damageIfCollide()
        destroyEnemiesHitByProjectiles()

        return this
    }

    private fun spawnEnemyIfNeeded(rng: RandomPort) {
        while (timeAccumulator >= enemySpawnIntervalSec) {
            timeAccumulator -= enemySpawnIntervalSec
            // spawn at random angle at arena edge
            val t = rng.nextFloat01() * (2f * Math.PI.toFloat())
            val spawnPos = Vec2(cos(t) * (arenaRadius - enemyRadius), sin(t) * (arenaRadius - enemyRadius))
            enemies += Enemy(spawnPos, enemySpeed, enemyRadius)
        }
    }

    private fun damageIfCollide() {
        val remaining = mutableListOf<Enemy>()
        for (e in enemies) {
            val d = (e.pos - playerPos).length()
            if (d <= (e.radius + playerRadius)) {
                playerHp -= 1
            } else {
                remaining += e
            }
        }
        enemies.clear()
        enemies.addAll(remaining)
    }

    private fun spawnProjectileIfNeeded() {
        while (attackAccumulator >= attackIntervalSec) {
            attackAccumulator -= attackIntervalSec
            if (enemies.isEmpty()) continue
            // find nearest enemy
            var nearest: Enemy? = null
            var bestDist = Float.MAX_VALUE
            for (e in enemies) {
                val d = (e.pos - playerPos).length()
                if (d < bestDist) { bestDist = d; nearest = e }
            }
            nearest?.let { target ->
                val dir = (target.pos - playerPos).normalized()
                val vel = dir * projectileSpeed
                projectiles += Projectile(playerPos, vel, projectileRadius, projectileLifetimeSec, projectileRange)
            }
        }
    }

    private fun destroyEnemiesHitByProjectiles() {
        if (projectiles.isEmpty() || enemies.isEmpty()) return
        val newEnemies = mutableListOf<Enemy>()
        val remainingProjectiles = mutableListOf<Projectile>()
        // For simplicity: if a projectile hits any enemy, remove that enemy and the projectile (one hit per projectile)
        val enemyHit = BooleanArray(enemies.size)
        for (p in projectiles) {
            var hitIndex = -1
            for ((idx, e) in enemies.withIndex()) {
                if (enemyHit[idx]) continue
                val d = (e.pos - p.pos).length()
                if (d <= (e.radius + p.radius)) { hitIndex = idx; break }
            }
            if (hitIndex >= 0) {
                enemyHit[hitIndex] = true
                // projectile consumed (do not add to remaining)
            } else {
                remainingProjectiles += p
            }
        }
        // collect enemies not hit
        for ((idx, e) in enemies.withIndex()) {
            if (!enemyHit[idx]) newEnemies += e
        }
        enemies.clear(); enemies.addAll(newEnemies)
        projectiles.clear(); projectiles.addAll(remainingProjectiles)
    }

    fun snapshot(nowSeconds: Float): WorldSnapshot {
        val player = SpriteView("player", playerPos, playerRadius)
        val es = enemies.map { SpriteView("enemy", it.pos, it.radius) }
        val bs = projectiles.map { SpriteView("bullet", it.pos, it.radius) }
        return WorldSnapshot(player, es, bs, nowSeconds, playerHp)
    }
}
