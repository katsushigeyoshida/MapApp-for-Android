package co.jp.yoshida.mapapp

import android.graphics.PointF
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class PointD {
    var x = 0.0
    var y = 0.0

    constructor() {

    }

    /**
     * Constructors
     */
    constructor(x: Int, y: Int): this() {
        this.x = x.toDouble()
        this.y = y.toDouble()
    }

    /**
     * Constructors
     */
    constructor(x: Float, y: Float): this() {
        this.x = x.toDouble()
        this.y = y.toDouble()
    }

    /**
     * Constructors
     */
    constructor(x: Double, y: Double): this() {
        this.x = x
        this.y = y
    }

    /**
     * Constructors
     */
    constructor(pos: PointD): this() {
        x = pos.x
        y = pos.y
    }

    /**
     * Constructors
     */
    constructor(pos: PointF): this() {
        x = pos.x.toDouble()
        y = pos.y.toDouble()
    }

    /**
     * 原点に対する角度(rad)
     * return       角度(rad)
     */
    fun angle(): Double {
        return atan2(y, x)
    }

    /**
     * 指定点を原点とした角度(rad)
     * p            指定点
     * return       角度(rad)
     */
    fun angle(p: PointD): Double {
        return atan2(y - p.y, x - p.x)
    }

    /**
     * 極座標からの取り込み
     * r            長さ
     * th           角度(rad)
     */
    fun fromPoler(r: Double, th:Double) {
        x = r * cos(th)
        y = r * sin(th)
    }

    /**
     * Point座標にオフセット値を加算する
     * offset       オフセット値
     */
    fun offset(offset: PointD) {
        x += offset.x
        y += offset.y
    }

    /**
     * Point座標にオフセット値を加算する
     */
    fun offset(dx: Double, dy: Double) {
        x += dx
        y += dy
    }

    /**
     * 指定座標とのオフセット値を求める
     * p        指定座標
     * return   オフセット値(PointD)
     */
    fun getOffset(p:PointD): PointD {
        return PointD(p.x - x, p.y - y)
    }

    /**
     * 原点(0,0)からの距離を求める
     */
    fun length(): Double {
        return sqrt(x * x + y * y)
    }

    /**
     * 指定点との距離を求める
     */
    fun distance(pos: PointD): Double {
        val dx = this.x - pos.x
        val dy = this.y - pos.y
        return sqrt(dx * dx + dy * dy)
    }

    /**
     * 指定点との中間点の座標を求める
     */
    fun center(pos: PointD): PointD {
        return PointD((this.x + pos.x) / 2.0, (this.y + pos.y) / 2.0)
    }

    /**
     * PointDへコピー
     */
    fun toCopy(): PointD {
        return PointD(x, y)
    }

    /**
     * PointFに変換する
     */
    fun toFloat(): PointF {
        return PointF(x.toFloat(), y.toFloat())
    }

    /**
     * 文字列出力
     */
    override fun toString(): String {
        return x.toString() + "," + y.toString()
    }

    /**
     * PointDが80,0)の時、true を返す
     */
    fun isEmpty(): Boolean {
        if (x == 0.0 && y == 0.0)
            return true
        else
            return false
    }

    /**
     * 点座標の回転
     *
     */
    fun rotatePoint(cp: PointD, rotate: Double) {
        var p = PointD(x - cp.x, y - cp.y)
        p.rotateOrg(rotate)
        x = p.x + cp.x
        y = p.y + cp.y
    }

    /**
     * 原点を中心に点を回転
     * rotate   回転角(rad)
     */
    fun rotateOrg(rotate: Double) {
        val tx = x * cos(rotate) - y * sin(rotate)
        val ty = x * sin(rotate) + y * cos(rotate)
        x = tx
        y = ty
    }

    /**
     * 原点を中心に拡大縮小
     * scale        拡大率
     */
    fun scale(scale: Double) {
        x *= scale
        y *= scale
    }

    /**
     * 原点を指定して拡大縮小
     * cp           原点
     * scale        拡大率
     */
    fun scalePoint(cp: PointD, scale: Double) {
        x = cp.x + (x - cp.x) * scale
        y = cp.y + (y - cp.y) * scale
    }
}