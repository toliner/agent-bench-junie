package dev.toliner.survivorgame.core.world

import dev.toliner.survivorgame.core.math.Vec2

data class SpriteView(val spriteId: String, val position: Vec2, val radius: Float)

data class WorldSnapshot(
    val player: SpriteView,
    val enemies: List<SpriteView>,
    val bullets: List<SpriteView>,
    val timeSeconds: Float,
    val playerHp: Int,
)