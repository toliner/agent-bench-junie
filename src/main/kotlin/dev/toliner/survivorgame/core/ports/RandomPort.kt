package dev.toliner.survivorgame.core.ports

/**
 * 乱数生成に関するポート。
 * 再現性を担保するためテストでは決定的な実装を用いる。
 */
interface RandomPort {
    /**
     * 0以上1未満の乱数を返す。
     */
    fun nextFloat01(): Float
}