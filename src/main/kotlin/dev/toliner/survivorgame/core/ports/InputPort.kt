package dev.toliner.survivorgame.core.ports

import dev.toliner.survivorgame.core.math.Vec2

/**
 * 入力に関するポート。
 * コアロジックはこのインターフェース経由でユーザー入力を参照する。
 */
interface InputPort {
    /**
     * 移動入力ベクトル（-1..1）。正規化されていなくてもよい。
     * 例: WASD/矢印キーの合成。
     */
    fun movementAxis(): Vec2
}