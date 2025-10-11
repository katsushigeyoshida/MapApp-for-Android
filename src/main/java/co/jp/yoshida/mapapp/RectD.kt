package co.jp.yoshida.mapapp

import android.graphics.RectF
import kotlin.math.max
import kotlin.math.min

class RectD {
    var left   = 0.toDouble()
    var top    = 0.toDouble()
    var right  = 0.toDouble()
    var bottom = 0.toDouble()

    constructor() {
        
    }
    /**
     *  コンストラクタ
     */
    constructor(left: Double, top: Double, right: Double, bottom: Double): this() {
        this.left   = left
        this.top    = top
        this.right  = right
        this.bottom = bottom
        normalized()
    }

    /**
     *  コンストラクタ(2点指定)
     */
    constructor(sp: PointD, ep: PointD): this() {
        this.left   = sp.x
        this.top    = sp.y
        this.right  = ep.x
        this.bottom = ep.y
        normalized()
    }

    /**
     *  コンストラクタ(RectD)
     */
    constructor(rect: RectD): this() {
        this.left   = rect.left
        this.top    = rect.top
        this.right  = rect.right
        this.bottom = rect.bottom
        normalized()
    }

    /**
     *  コンストラクタ(円指定)
     */
    constructor(cp: PointD, r: Double): this() {
        this.left   = cp.x - r
        this.top    = cp.y - r
        this.right  = cp.x + r
        this.bottom = cp.y + r
        normalized()
    }

    /**
     *  コンストラクタ(座標点リスト)
     */
    constructor(plist: List<PointD>): this() {
        left   = plist[0].x
        right  = plist[0].x
        top    = plist[0].y
        bottom = plist[0].y
        for (p in plist) {
            left   = min(left, p.x)
            right  = max(right, p.x)
            top    = min(top, p.y)
            bottom = max(bottom, p.y)
        }
        normalized()
    }

    /**
     *  正規化(left < right && top < bottom)
     */
    private fun normalized() {
        if (right < left) {
            val t = right
            right = left
            left = t
        }
        if (bottom < top) {
            val t = top
            top = bottom
            bottom = t
        }
    }

    /**
     *  上下左右がすべて0の時true
     */
    fun isEmpty(): Boolean {
        return left == 0.0 && top == 0.0 && right == 0.0 && bottom == 0.0
    }

    /**
     *  四角形の幅
     */
    fun width(): Double {
        return right - left
    }

    /**
     *  四角形の高さ
     */
    fun height(): Double {
        return bottom - top
    }

    /**
     *  X座標中心
     */
    fun centerX(): Double {
        return (left + right) / 2.0
    }

    /**
     *  Y座標の中心
     */
    fun centerY(): Double {
        return (top + bottom) / 2.0
    }

    /**
     *  中心座標
     */
    fun center(): PointD{
        return PointD(centerX(), centerY())
    }

    /**
     * 左上の座標
     */
    fun leftTop(): PointD {
        return PointD(left, top)
    }

    /**
     * 左下の座標
     */
    fun leftBottom(): PointD {
        return PointD(left, bottom)
    }

    /**
     * 右下の座標
     */
    fun rightBottom(): PointD {
        return PointD(right, bottom)
    }

    /**
     * 右下の座標
     */
    fun rightTop(): PointD {
        return PointD(right, top)
    }

    /**
     *  内外判定(内側であればtrue)
     */
    fun inside(x: Double, y: Double): Boolean {
        return (left <= x && x <= right && top <= y && y <= bottom)
    }

    /**
     *  内外判定(内側であればtrue)
     */
    fun inside(p: PointD): Boolean {
        return inside(p.x, p.y)
    }

    /**
     *  内側判定 (すべてに内包していたらtrue)
     */
    fun inside(r: RectD): Boolean {
        return inside(r.leftTop()) && inside(r.rightBottom())
    }

    /**
     *  外側判定 (すべてが外部であればtrue)
     */
    fun outside(r: RectD): Boolean {
        return r.right < left || right < r.left || r.bottom < top || bottom < r.top
    }

    /**
     * 四角形のエリアを拡張の初期値を設定
     */
    fun setInitExtension() {
        this.left   = Double.MAX_VALUE
        this.top    = Double.MAX_VALUE
        this.right  = Double.MIN_VALUE
        this.bottom = Double.MIN_VALUE
    }

    /**
     * 四角形のエリアを拡張する
     */
    fun extension(pos: PointD) {
        if (pos.x < left) left = pos.x
        if (right < pos.x) right = pos.x
        if (pos.y < top) top = pos.y
        if (bottom < pos.y) bottom = pos.y
    }

    /**
     * 四角形を移動する
     * dp       移動量
     */
    fun offset(dp: PointD) {
        offset(dp.x, dp.y)
    }

    /**
     *  四角形を移動する
     *  dx      X方向の移動量
     *  dy      Y方向の移動量
     */
    fun offset(dx: Double, dy: Double) {
        left   += dx
        right  += dx
        top    += dy
        bottom += dy
    }

    /**
     * 指定点から四角形の中心点までのオフセット量を求める
     * p        指定点
     */
    fun getOffset(p: PointD): PointD {
        val cp = getCenter()
        return PointD(p.x - cp.x, p.y - cp.y)
    }

    /**
     * 指定点を四角形の中心に設定する
     * cp       設定する中心点
     */
    fun setCenter(cp: PointD) {
        offset(getOffset(cp))
    }

    /**
     * 四角形の中心点を求める
     */
    fun getCenter(): PointD {
        return PointD((left + right) / 2.0, (top + bottom) / 2.0)
    }

    /**
     * 指定点を中心点にして拡大縮小する
     * ctr      中心点
     * zoom     拡大縮小率
     */
    fun zoom(ctr: PointD, zoom: Double) {
        setCenter(ctr)
        this.zoom(zoom)
    }

    /**
     * 中心点を煕俊に拡大縮小する
     * zoom     拡大縮小率
     */
    fun zoom(zoom: Double) {
        val dx = (width() / zoom - width()) /2.0
        val dy = (height() / zoom - height()) / 2.0
        left   -= dx
        right  += dx
        top    -= dy
        bottom += dy
    }

    /**
     * RectFに変換する
     */
    fun toRectF(): RectF {
        return RectF(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
    }

    /**
     *  文字列出力
     */
    override fun toString(): String {
        return "%.3f".format(left) + ", " + "%.3f".format(top) + ", " +
                "%.3f".format(right) + ", " + "%.3f".format(bottom)
    }
}