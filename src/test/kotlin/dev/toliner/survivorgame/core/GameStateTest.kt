package dev.toliner.survivorgame.core

import dev.toliner.survivorgame.core.math.Vec2
import dev.toliner.survivorgame.core.ports.InputPort
import dev.toliner.survivorgame.core.ports.RandomPort
import dev.toliner.survivorgame.core.ports.TimePort
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe

private class FixedTime(private val dt: Float, private val now: Float) : TimePort {
    override fun deltaTimeSeconds(): Float = dt
    override fun nowSeconds(): Float = now
}
private class FixedInput(private val axis: Vec2) : InputPort {
    override fun movementAxis(): Vec2 = axis
}
private class SeqRandom(private val values: MutableList<Float>) : RandomPort {
    override fun nextFloat01(): Float = if (values.isNotEmpty()) values.removeAt(0) else 0.5f
}

class GameStateTest : StringSpec({
    "player moves with input and is clamped to arena" {
        val gs = GameState(arenaRadius = 2f, playerRadius = 1f, playerSpeed = 10f)
        val before = gs.playerPos
        gs.update(FixedInput(Vec2(1f, 0f)), FixedTime(1f, 0f), SeqRandom(mutableListOf()))
        // would try to move 10 units, but clamped to radius 1 (arena 2 - player 1)
        gs.playerPos.length().shouldBe(1f)
        gs.playerPos.x.shouldBe(1f)
        gs.playerPos.y.shouldBe(0f)
        before.length().shouldBeLessThan(gs.playerPos.length())
    }

    "enemy spawns after interval and moves towards player" {
        val gs = GameState(enemySpawnIntervalSec = 0.1f, enemySpeed = 5f)
        // advance time to trigger spawn
        gs.update(FixedInput(Vec2.ZERO), FixedTime(0.1f, 0.1f), SeqRandom(mutableListOf(0f)))
        val snap = gs.snapshot(0.1f)
        snap.enemies.size.shouldBe(1)
        // after one update post spawn, enemy should move closer than spawn radius on next update
        gs.update(FixedInput(Vec2.ZERO), FixedTime(0.1f, 0.2f), SeqRandom(mutableListOf()))
        val snap2 = gs.snapshot(0.2f)
        snap2.enemies[0].position.length().shouldBeLessThan(gs.arenaRadius)
    }

    "collision reduces player hp and removes enemy" {
        val gs = GameState(attackIntervalSec = 9999f)
        // Spawn one enemy at angle 0
        gs.playerPos = Vec2(0f, 0f)
        gs.update(FixedInput(Vec2.ZERO), FixedTime(gs.enemySpawnIntervalSec, gs.enemySpawnIntervalSec), SeqRandom(mutableListOf(0f)))
        // Run updates until collision occurs
        var steps = 0
        while (gs.playerHp == 5 && steps < 200) {
            gs.update(FixedInput(Vec2.ZERO), FixedTime(0.2f, gs.enemySpawnIntervalSec + steps * 0.2f), SeqRandom(mutableListOf()))
            steps++
        }
        val snap = gs.snapshot(gs.enemySpawnIntervalSec + steps * 0.2f)
        snap.playerHp.shouldBe(4)
    }

    "game over when hp <= 0" {
        val gs = GameState(playerHp = 1)
        // Force collision by placing enemy at player position via manual damage
        gs.apply { playerHp = 0 }
        gs.isGameOver().shouldBe(true)
    }

    "update is no-op when game over" {
        val gs = GameState(playerHp = 0)
        val beforePos = gs.playerPos
        gs.update(FixedInput(Vec2(1f, 0f)), FixedTime(1f, 1f), SeqRandom(mutableListOf()))
        gs.playerPos.shouldBe(beforePos)
    }

    "auto-attack spawns projectile and destroys enemy" {
        val gs = GameState(
            enemySpawnIntervalSec = 0.1f,
            enemySpeed = 0f,
            attackIntervalSec = 0.05f,
            projectileSpeed = 50f,
            projectileLifetimeSec = 5f,
        )
        // spawn one enemy at angle 0 (to the right)
        gs.update(FixedInput(Vec2.ZERO), FixedTime(0.1f, 0.1f), SeqRandom(mutableListOf(0f)))
        // advance until a projectile is fired and hits the enemy
        var tNow = 0.1f
        var hadBullet = false
        var steps = 0
        while (steps < 200 && true) {
            gs.update(FixedInput(Vec2.ZERO), FixedTime(0.05f, tNow + 0.05f), SeqRandom(mutableListOf()))
            tNow += 0.05f
            val snap = gs.snapshot(tNow)
            if (snap.bullets.isNotEmpty()) hadBullet = true
            if (snap.enemies.isEmpty()) {
                hadBullet.shouldBe(true)
                break
            }
            steps++
        }
        val finalSnap = gs.snapshot(tNow)
        finalSnap.enemies.shouldHaveSize(0)
    }

    "projectile expires if it doesn't reach the enemy in time" {
        val gs = GameState(
            enemySpawnIntervalSec = 0.05f,
            enemySpeed = 0f,
            attackIntervalSec = 0.05f,
            projectileSpeed = 1f,
            projectileLifetimeSec = 0.1f,
        )
        // spawn one enemy
        gs.update(FixedInput(Vec2.ZERO), FixedTime(0.05f, 0.05f), SeqRandom(mutableListOf(0f)))
        var tNow = 0.05f
        // allow a projectile to spawn
        gs.update(FixedInput(Vec2.ZERO), FixedTime(0.05f, 0.10f), SeqRandom(mutableListOf()))
        tNow = 0.10f
        // confirm a bullet exists at some point
        val snap1 = gs.snapshot(tNow)
        (snap1.bullets.isNotEmpty()).shouldBe(true)
        // advance beyond lifetime so it expires before reaching the enemy
        gs.update(FixedInput(Vec2.ZERO), FixedTime(0.2f, 0.30f), SeqRandom(mutableListOf()))
        val snap2 = gs.snapshot(0.30f)
        snap2.bullets.shouldHaveSize(0)
        // at least one enemy should still exist (could be >1 due to ongoing spawns)
        (snap2.enemies.size > 0).shouldBe(true)
    }

    "projectile despawns by range if target is too far" {
        val gs = GameState(
            enemySpawnIntervalSec = 0.05f,
            enemySpeed = 0f,
            attackIntervalSec = 0.05f,
            projectileSpeed = 20f,
            projectileLifetimeSec = 10f,
            projectileRange = 0.5f,
        )
        // Spawn one enemy at angle 0 (far at arena edge)
        gs.update(FixedInput(Vec2.ZERO), FixedTime(0.05f, 0.05f), SeqRandom(mutableListOf(0f)))
        // Single frame where one projectile spawns (dt equals interval) and moves beyond its range
        gs.update(FixedInput(Vec2.ZERO), FixedTime(0.05f, 0.10f), SeqRandom(mutableListOf()))
        val snap = gs.snapshot(0.10f)
        // Bullet should have despawned due to range; enemy should still exist
        snap.bullets.shouldHaveSize(0)
        (snap.enemies.size > 0).shouldBe(true)
    }
})
