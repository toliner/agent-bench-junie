package dev.toliner.survivorgame.core.ports

/**
 * 時間に関するポート。
 * コアロジックはこのインターフェース経由で経過時間を取得する。
 */
interface TimePort {
    /**
     * 前フレームからの経過時間（秒）。
     */
    fun deltaTimeSeconds(): Float

    /**
     * 経過時間（秒）をリセットせずに累積で取得してもよい現在時刻（秒）。
     */
    fun nowSeconds(): Float
}