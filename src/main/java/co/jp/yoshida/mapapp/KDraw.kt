package co.jp.yoshida.mapapp

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.Size
import android.view.SurfaceHolder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * グラフィック描画処理
 */
class KDraw {
    val TAG = "KDraw"

    private lateinit var mSurfaceHolder: SurfaceHolder  //  SurfaceViewのHolder
    private lateinit var mBackCanvas: Canvas            //  SurfaceViewの保存画面
    private lateinit var mBitmap: Bitmap                //  SurfaceViewのBitmap
    private var mLoackCanvas = false                    //  SurfaceViewのLoacCanvasの状態

    var mCanvas = Canvas()                              //  CANVAS
    var mScreenInverted = false                         //  スクリーン座標の倒立表示
    var mScreen = Size(0, 0)                //  スクリーンの大きさ
    var mView  = RectD()                                //  ビューエリア(スクリーン座標)
    var mWorld = RectD()                                //  ビューエリアに対するワールド座標
    var mPaint = Paint()                                //  描画属性
    var mColor = "Black"                                //  文字以外の色
    var mUseCenter = false                              //  円弧表示で扇型/円弧の
    var mStrokWidth = 3f                                //  線分の太さ
    var mStrokStyle = Paint.Style.STROKE                //  塗潰しの有無(STROKE/FILL)
    var mTextBackColor = "White"                        //  テキストボックス(drawTextBox)の背景色
    var mTextBackStyle = Paint.Style.FILL_AND_STROKE    //  テキストボックスの背景スタイル
    var mTextColor = "Black"                            //  文字の色
    var mTextSize = 40f                                 //  文字サイズ
    var mTextStrokeWidth = 2f                           //  文字の太さ
    enum class HALIGNMENT { Left, Center, Right }
    enum class VALIGNMENT { Top, Center, Bottom }
    val mColor15 = listOf<Int>(     //  15色の設定(16色からwhiteを抜く)
        Color.BLACK,
        Color.rgb(0xc0,0xc0,0xc0),  //  silver
        Color.rgb(0x80,0,0),        //  maroon
        Color.rgb(0x80,0,0x80),     //  purple
        Color.GREEN,
        Color.rgb(0x80,0x80,0),     //  olive
        Color.rgb(0,0,0x80),        //  navy
        Color.rgb(0,0x80,0x80),     //  teal
        Color.GRAY,
        Color.RED,
        Color.rgb(0xff,0,0xff),     //  fuchsia
        Color.rgb(0,0xff,0),        //  lime
        Color.YELLOW,
        Color.BLUE,
        Color.rgb(0,0xff,0xff)      //  aqua
    )
    val mColor16 = listOf<Int>(     //  16色の設定
        Color.BLACK,Color.
        rgb(0xc0,0xc0,0xc0),        //  silver
        Color.rgb(0x80,0,0),        //  maroon
        Color.rgb(0x80,0,0x80),     //  purple
        Color.GREEN,
        Color.rgb(0x80,0x80,0),     //  olive
        Color.rgb(0,0,0x80),        //  navy
        Color.rgb(0,0x80,0x80),     //  teal
        Color.GRAY,
        Color.WHITE,
        Color.RED,
        Color.rgb(0xff,0,0xff),     //  fuchsia
        Color.rgb(0,0xff,0),        //  lime
        Color.YELLOW,
        Color.BLUE,
        Color.rgb(0,0xff,0xff)      //  aqua
    )
    val mColorMap = mapOf(
        "Black" to Color.BLACK,
        "Red" to Color.RED,
        "Blue" to Color.BLUE,
        "Green" to Color.GREEN,
        "Yellow" to Color.YELLOW,
        "White" to Color.WHITE,
        "Cyan" to Color.CYAN,
        "Gray" to Color.GRAY,
        "LightGray" to Color.LTGRAY,
        "Magenta" to Color.MAGENTA,
        "DarkGray" to Color.DKGRAY,
        "Transparent" to Color.TRANSPARENT,
        "Silver" to Color.rgb(0xc0,0xc0,0xc0),
        "Maroon" to Color.rgb(0x80,0,0),
        "Purple" to Color.rgb(0x80,0,0x80),
        "Olive" to Color.rgb(0x80,0x80,0),
        "Navy" to Color.rgb(0,0,0x80),
        "Teal" to Color.rgb(0,0x80,0x80),
        "Fuchsia" to Color.rgb(0xff,0,0xff),
        "Lime" to Color.rgb(0,0xff,0),
        "Aqua" to Color.rgb(0,0xff,0xff),
        "Lavender" to Color.rgb(0xe6,0xe6,0xfa),
        "GhostWhite" to Color.rgb(0xf8,0xf8,0xff),
        "AliceBlue" to Color.rgb(0xf0,0xf8,0xff),
        "Gold" to Color.rgb(0xff,0xd7,0x00),
        "Chocolate" to Color.rgb(0xd2,0x69,0x1e),
        "DarkBlue" to Color.rgb(0x00,0x00,0x8b)
    )

    private var mMatrix = Array(4, {arrayOf<Double>(0.0,0.0)})    //  3D座標変換パラメータ

    private val klib = KLib()

    init {
        mPaint.color = klib.mColorMap[mColor]?:Color.BLACK
        mPaint.strokeWidth = mStrokWidth
        mPaint.style = Paint.Style.STROKE
        mPaint.textSize = mTextSize
        matrixClear()
    }

    /**
     * スクリーンの大きさ設定
     */
    fun setInitScreen(width: Int, height: Int) {
        mScreen = Size(width, height)
        mView = RectD(0.0, 0.0, width.toDouble(), height.toDouble())
        mWorld = RectD(0.0, 0.0, width.toDouble(), height.toDouble())
    }

    /**
     * SurfaceViewを使用するための初期化
     * constructor で設定
     * surfaceHolder    holder
     * callback         this
     */
    fun initSurface(surfaceHolder: SurfaceHolder, callback: SurfaceHolder.Callback) {
        mSurfaceHolder = surfaceHolder
        mSurfaceHolder.addCallback(callback)
        mLoackCanvas = false
    }

    /**
     * SurfaceView画面の初期化
     * surfaceCreated で設定
     * backColor        背景色
     * Width            スクリーンの幅
     * Height           スクリーンの高さ
     */
    fun initSurfaceScreen(backColor: Int, Width: Int, Height: Int) {
        setOffScreen(Width, Height)
        lockCanvas()
        mCanvas.drawColor(backColor)
        unlockCanvasAndPost()
    }

    /**
     * SurfaceView での終了処理(メモリ開放)
     * surfaceDestroyed で設定
     */
    fun termSurface() {
        mBitmap.recycle()
    }

    /**
     * SurfaceViewの描画の前処理
     */
    fun lockCanvas() {
        if (!mLoackCanvas)
            mBackCanvas = mSurfaceHolder.lockCanvas()
        if (mBackCanvas != null) {
            mCanvas = Canvas(mBitmap)
        } else {
            mCanvas = mBackCanvas
        }
        mLoackCanvas = true
    }

    /**
     * SurfaceViewの描画の後処理
     */
    fun unlockCanvasAndPost() {
        if (mBitmap != null) {
            var paint = Paint()
            mBackCanvas.drawBitmap(mBitmap, 0f, 0f, paint)
        }
        if (mLoackCanvas)
            mSurfaceHolder.unlockCanvasAndPost(mBackCanvas)
        mLoackCanvas = false
    }

    /**
     * SurfaceViwで使用するBitmapの初期化
     */
    fun setOffScreen(Width: Int, Height: Int) {
        mBitmap = Bitmap.createBitmap(Width, Height, Bitmap.Config.ARGB_8888)
    }

    /**
     * アスペクト比固定
     */
    fun setAspectFix() {
        val scaleX = mView.width() / mWorld.width()
        val scaleY = mView.height() / mWorld.height()
        var top    = mWorld.top
        var bottom = mWorld.bottom
        var left   = mWorld.left
        var right  = mWorld.right
        if (scaleX < scaleY) {
            top    -= (mView.height() / scaleX - mWorld.height()) / 2.0
            bottom += (mView.height() / scaleX - mWorld.height()) / 2.0
        } else {
            left   -= (mView.width() / scaleY - mWorld.width()) / 2.0
            right  += (mView.width() / scaleY - mWorld.width()) / 2.0
        }
        mWorld = RectD(left, top, right, bottom)
    }


    /**
     * 背景色を設定する
     * color
     */
    fun backColor(color: Int) {
        mCanvas.drawColor(color)
    }

    /**
     * 描画属性の設定
     * color        色名(Black,...)
     * strokeWidth  線幅
     * strokeStyle  塗潰し(Paint.Style.FILL/FILL_AND_STROKE/STROKE)
     */
    fun setProperty(color: String, strokeWidth: Double = 1.0, strokeStyle: Paint.Style = Paint.Style.STROKE) {
        mColor = color
        mStrokWidth = strokeWidth.toFloat()
        mStrokStyle = strokeStyle
        setProperty()
    }

    /**
     * 図形描画属性の反映
     */
    fun setProperty() {
        mPaint.color = mColorMap[mColor]?:Color.BLACK
        mPaint.strokeWidth = mStrokWidth
        mPaint.style = mStrokStyle
    }

    /**
     * 文字列の描画属性を設定(スクリーン座標)
     * size     文字高さ(スクリーン座標)
     * strokWidth   文字の太さ(スクリーン座標)
     */
    fun setTextProperty(size: Double, strokWidth: Double = 1.0, color: String = "Black", strokeStyle: Paint.Style = Paint.Style.FILL) {
        mStrokStyle = strokeStyle
        mColor = color
        mTextSize = size.toFloat()
        mTextStrokeWidth = strokWidth.toFloat()
        setTextProperty()
    }

    /**
     * テキスト描画属性の反映
     */
    fun setTextProperty(){
        setProperty()
        mPaint.textSize = mTextSize
        mPaint.strokeWidth = mTextStrokeWidth
    }

    /**
     * 描画カラーの設定
     * color        色名(Black,...)
     */
    fun setColor(color: String) {
        mColor = color
        mPaint.color = mColorMap[mColor]?:Color.BLACK
    }

    /**
     * 描画スタイルを設定
     * style        塗潰しタイプ(Paint.Style.STROKE,FILL,STROKE?AND_FILL)
     */
    fun setStyle(style: Paint.Style) {
        mStrokStyle = style
        mPaint.style = mStrokStyle
    }

    fun setTextColor(color: String) {
        mTextColor = color
    }

    /**
     * 文字サイズを設定する
     */
    fun setTextSize(size: Double) {
        mTextSize = size.toFloat()
        mPaint.textSize = mTextSize
    }

    /**
     * 文字列の描画属性を設定(ワールド座標)
     * size     文字高さ(ワールド座標)
     * strokWidth   文字の太さ(スクリーン座標)
     */
    fun setWorldTextProperty(size: Double, strokWidth: Double) {
        mTextSize = cnvWorld2ScreenY(size).toFloat()
        mTextStrokeWidth = strokWidth.toFloat()
        setTextProperty()
    }


    //  ----  3D座標での描画  ----

//    fun draw3DWline(sp: Point3D, ep: Point3D) {
//        val sp3 = affinTransform(sp)
//        val ep3 = affinTransform(ep)
//        drawWLine(sp3.toPointXY(), ep3.toPointXY())
//    }

//    fun draw3DWCircle(cp: Point3D, r: Double) {
//        val cp3 = affinTransform(cp)
//        drawWCircle(cp3.toPointXY(), r)
//    }

//    fun draw3DWText(text: String, p: Point3D, rotate: Double = 0.0, ha: HALIGNMENT = HALIGNMENT.Left, va: VALIGNMENT = VALIGNMENT.Bottom) {
//        drawWText(text, p.toPointXY(),rotate, ha, va)
//    }

    //  ----  ワールド座標での描画  ----

    /**
     * ワールド座標で線分の描画
     * l        線分クラス
     */
//    fun drawWLine(l: LineD) {
//        drawLine(cnvWorld2View(l.ps), cnvWorld2View(l.pe))
//    }

    /**
     * ワールド座標に線分を描画
     * sp       始点座標
     * ep       終点座標
     */
    fun drawWLine(sp: PointD, ep: PointD) {
        drawLine(cnvWorld2View(sp), cnvWorld2View(ep))
    }

    /**
     * ワールド座標に四角形を描画
     * rect     四角形座標
     */
    fun drawWRect(rect: RectD) {
        drawRect(cnvWorld2View(rect))
    }

    /**
     * ワールド座標で円の描画
     * c        円クラス
     */
//    fun drawWCircle(c: CircleD) {
//        drawCircle(cnvWorld2View(c.cp), cnvWorld2ScreenX(c.r))
//    }

    /**
     * ワールド座標で円の描画
     * cp       中心座標
     * r        半径
     */
    fun drawWCircle(cp: PointD, r: Double) {
        drawCircle(cnvWorld2View(cp), cnvWorld2ScreenX(r))
    }

    /**
     * ワールド座標で円弧の描画
     * arc      円弧の構造体
     */
//    fun drawWArc(arc: ArcD) {
//        drawWArc(arc.cp, arc.r, arc.sa, arc.ea)
//    }

    /**
     * ワールド座標で円弧の描画
     *  cp      中心座標
     *  r       半径
     *  sa      開始角(rad)
     *  ea      修了角(rad)
     */
//    fun drawWArc(cp: PointD, r:Double, sa: Double, ea: Double) {
//        drawArc(cnvWorld2View(cp), cnvWorld2ScreenX(r), sa, ea)
//    }

    /**
     * ワールド座標でパス(ポリゴン)の描画
     * wplist       座標リスト
     */
    fun drawWPath(wplist: List<PointD>) {
        val plist = mutableListOf<PointD>()
        for (wp in wplist)
            plist.add(cnvWorld2View(wp))
        drawPath(plist)
    }

    /**
     * ワールド座標に文字列を描画
     * text     文字列
     * p        文字圏点(文字列の左下)
     * rotate   文字列の回転(degree,時計回り)
     * ha       水平方向アライメント
     * va       水超方向アライメント
     */
    fun drawWText(text: String, p: PointD, rotate: Double = 0.0, ha: HALIGNMENT = HALIGNMENT.Left, va: VALIGNMENT = VALIGNMENT.Bottom) {
        drawText(text, cnvWorld2View(p), rotate, ha, va)
    }

    /**
     * ワールド座標に文字列と四角形を描画
     * mPaint.style を Paint.Style.FILL にすると文字列の背景を塗潰す
     * text     文字列
     * p        文字圏点(文字列の左下)
     * rotate   文字列の回転(degree,時計回り)
     * ha       水平方向アライメント
     * va       水超方向アライメント
     */
    fun drawWTextWithBox(text: String, p: PointD, rotate: Double = 0.0, ha: HALIGNMENT = HALIGNMENT.Left, va: VALIGNMENT = VALIGNMENT.Bottom) {
        drawTextWithBox(text, cnvWorld2View(p), rotate, ha, va)
    }


    //  ----  スクリーン座標での描画  ----

    /**
     * スクリーン座標に線分を描画
     * sp       始点座標
     * ep       終点座標
     */
    fun drawLine(sp: PointD, ep: PointD) {
        setProperty()
        var sp2 = invertedPointD(sp).toFloat()
        var ep2 = invertedPointD(ep).toFloat()
        mCanvas.drawLine(sp2.x, sp2.y, ep2.x, ep2.y, mPaint)
    }

    /**
     * スクリーン座標に円の描画
     * cp       円の中心座標
     * r        半径
     */
    fun drawCircle(cp: PointD, r: Double) {
        var rect = RectD(cp, r)
        mCanvas.drawOval(invertedRectD(rect).toRectF(), mPaint)
    }

    /**
     * スクリーン座標に円弧の描画
     * cp       円弧の中心座標
     * r        半径
     * sa       開始角(rad)(-π～π)
     * ea       終了角(radd)(sa < ea)
     */
//    fun drawArc(cp: PointD, r: Double, sa: Double, ea: Double) {
//        var rect = CircleD(cp, r).toRectD()
//        val useCenter = mUseCenter                  //  扇型/円弧(中心への腺の有無)
//        var ssa = R2D(sa)
//        ssa = if (180 < ssa) ssa - 360.0 else ssa   //  start angle(-π～π)
//        var swa = R2D(ea - sa)                  //  sweepangle
//        if (mScreenInverted) {
//            ssa *= -1.0
//            swa *= -1.0
//        }
//        mCanvas.drawArc(invertedRectD(rect).toRectF(),
//            ssa.toFloat(), swa.toFloat(), useCenter, mPaint)
//    }

    /**
     * スクリーン座標に四角形を描画
     * rect     四角形座標
     */
    fun drawRect(rect: RectD) {
        setProperty()
        var rect2 = invertedRectD(rect).toRectF()
        mCanvas.drawRect(rect2, mPaint)
    }

    /**
     *  スクリーン座標でパス(ポリゴン)の描画
     *  plist       座標リスト
     */
    fun drawPath(plist: List<PointD>) {
        var path = Path()
        path.setFillType(Path.FillType.EVEN_ODD)    //  Pathの塗潰し方法
        //  EVEN_ODD            内側が奇数のエッジ交差によって計算
        //  INVERSE_EVEN_ODD    内側ではなくパスの外側に描画
        //  WINDING             符号付きエッジ交差の 0 以外の合計で「内側」が計算
        //  INVERSE_WINDING     内側ではなくパスの外側に描画
        val mp = invertedPointD(plist[0]).toFloat()
        path.moveTo(mp.x, mp.y)
        for (p in plist){
            val ip = invertedPointD(p).toFloat()
            path.lineTo(ip.x, ip.y)
        }
        path.close()
        mCanvas.drawPath(path, mPaint)
    }

    /**
     * スクリーン座標に文字列を描画
     * text     文字列
     * p        文字圏点(文字列の左下)
     */
    fun drawText(text: String, p: PointD) {
        setTextProperty()
        mPaint.style = Paint.Style.FILL
        var p2 = invertedPointD(p).toFloat()
        mCanvas.drawText(text, p2.x, p2.y - getDecent().toFloat(), mPaint)
    }


    /**
     * スクリーン座標に文字列を描画
     * text     文字列
     * p        文字圏点(文字列の左下)
     * rotate   文字列の回転(degree,時計回り)
     * ha       水平方向アライメント
     * va       水超方向アライメント
     */
    fun drawText(text: String, p: PointD, rotate: Double, ha: HALIGNMENT = HALIGNMENT.Left, va: VALIGNMENT= VALIGNMENT.Bottom) {
        var p2 = invertedPointD(p).toFloat()
        mCanvas.translate(p2.x, p2.y)
        mCanvas.rotate(rotate.toFloat())
        setTextProperty()
        mPaint.style = Paint.Style.FILL
        val offset = getAligmentOffset(text, ha, va)
        mCanvas.drawText(text, offset.x.toFloat(), offset.y.toFloat(), mPaint)
        mCanvas.rotate(-rotate.toFloat())
        mCanvas.translate(-p2.x, -p2.y)
    }

    /**
     * 文字列の背景表示
     * text     文字列
     * p        文字圏点(文字列の左下)
     * rotate   文字列の回転(degree,時計回り)
     * ha       水平方向アライメント
     * va       水超方向アライメント
     */
    fun drawTextBox(text: String, p: PointD, rotate: Double, ha: HALIGNMENT = HALIGNMENT.Left, va: VALIGNMENT= VALIGNMENT.Bottom) {
        var p2 = invertedPointD(p).toFloat()
        mPaint.color = mColorMap[mTextBackColor]?:Color.WHITE
        mPaint.style = mTextBackStyle
        mCanvas.translate(p2.x.toFloat(), p2.y.toFloat())
        mCanvas.rotate(rotate.toFloat())
        val offset = getAligmentOffset(text, ha, va)
        if (mScreenInverted)
            offset.y -= mTextSize
        mCanvas.drawRect(getStringRect(text, offset).toRectF(), mPaint)
        mCanvas.rotate(-rotate.toFloat())
        mCanvas.translate(-p2.x.toFloat(), -p2.y.toFloat())
    }

    /**
     * スクリーン座標に文字列と四角形を描画
     * mPaint.style を Paint.Style.FILL にすると文字列の背景を塗潰す
     * text     文字列
     * p        文字圏点(文字列の左下)
     * rotate   文字列の回転(degree,時計回り)
     * ha       水平方向アライメント
     * va       水超方向アライメント
     */
    fun drawTextWithBox(text: String, p: PointD, rotate: Double = 0.0, ha: HALIGNMENT = HALIGNMENT.Left, va: VALIGNMENT = VALIGNMENT.Bottom) {
        drawTextBox(text, p, rotate, ha, va)
        drawText(text, p, rotate, ha, va)
    }


    //  ----  文字処理関数  ----

    /**
     * 文字列のアライメントからオフセット値を求める
     * text     文字列
     * ha       水平アライメント
     * va       垂直アライメント
     * return   オフセット値(左下を原点とする)
     */
    private fun getAligmentOffset(text: String, ha: HALIGNMENT, va: VALIGNMENT): PointD {
        val mt = measureText(text)
        val hoffset = when (ha) {
            HALIGNMENT.Left -> 0.0
            HALIGNMENT.Center -> mt / 2.0
            HALIGNMENT.Right -> mt
        }
        val voffset = when (va) {
            VALIGNMENT.Top -> mPaint.textSize
            VALIGNMENT.Center -> mPaint.textSize / 2.0
            VALIGNMENT.Bottom -> 0.0
        }
        return PointD(-hoffset.toDouble(), voffset.toDouble())
    }

    /**
     * 文字列の領域座標の取得
     * @param text      文字列
     * @param pos       起点座標
     * @return          文字列の領域
     */
    fun getStringRect(text: String, pos: PointD): RectD {
        if (mScreenInverted)
            pos.y += mPaint.textSize
        return RectD(pos.x, pos.y - getTextSize(),
            pos.x + measureText(text), pos.y + getDecent())
    }

    /**
     * 文字サイズを取得する
     * @return      the paint's text size in pixel units.
     */
    fun getTextSize(): Double {
        return mPaint.getTextSize().toDouble()
    }

    /**
     * 文字のベースラインよりも下の高さを取得
     * @return
     */
    fun getDecent(): Double {
        return mPaint.descent().toDouble()
    }

    /**
     * 文字列の長さを取得
     * @param   text
     * @return          The width of the text
     */
    fun measureText(text: String): Double {
        return mPaint.measureText(text).toDouble()
    }

    //  ----  座標変換  ----

    /**
     * ワールド座標からビュー(スクリーン)座標に変換
     * wp           ワールド点座標
     * return       ビュー(スクリーン)点座標
     */
    fun cnvWorld2View(wp: PointD): PointD {
        val sp = PointD()
        sp.x = (wp.x - mWorld.left) * mView.width() / mWorld.width() + mView.left
        sp.y = (wp.y - mWorld.top) * mView.height() / mWorld.height() + mView.top
        return  sp
    }

    /**
     * ビュー(スクリーン)座標からワールド座標に変換
     * vp           ビュー(スクリーン)座標
     * return       ワールド座標
     */
    fun cnvView2World(vp: PointD): PointD {
        val wp = PointD()
        wp.x = (vp.x - mView.left) * mWorld.width() / mView.width() + mWorld.left
        wp.y = (vp.y - mView.top) * mWorld.width() / mView.width() + mWorld.top
        return wp
    }

    /**
     * ワールド座標からビュー(スクリーン)座標に変換
     * wrect        ワールド四角座標
     * return       ビュー(スクリーン)四角座標
     */
    fun cnvWorld2View(wrect: RectD): RectD {
        val ps: PointD = cnvWorld2View(PointD(wrect.left, wrect.top))
        val pe: PointD = cnvWorld2View(PointD(wrect.right, wrect.bottom))
        return RectD(ps.x, ps.y, pe.x, pe.y)
    }

    /**
     * X方向の長さをスクリーン座標からワールド座標に変換
     * sl       スクリーン座標の長さ
     */
    fun cnvScreen2WorldX(sl: Double): Double {
        return sl * mWorld.width() / mView.width()
    }

    /**
     * Y方向の長さをスクリーン座標からワールド座標に変換
     * sl       スクリーン座標の長さ
     */
    fun cnvScreen2WorldY(sl: Double): Double {
        return sl * mWorld.height() / mView.height()
    }

    /**
     * X方向の長さをワールド座標からスクリーン座標に変換
     * wl       ワールド座標の長さ
     */
    fun cnvWorld2ScreenX(wl: Double): Double {
        return wl * mView.width() / mWorld.width()
    }

    /**
     * Y方向の長さをワールド座標からスクリーン座標に変換
     * wl       ワールド座標の長さ
     */
    fun cnvWorld2ScreenY(wl: Double): Double {
        return wl * mView.height() / mWorld.height()
    }

    /**
     * 座標をスクリーン座標で(倒立に)変換
     * p        点座標
     * return   点座標
     */
    fun invertedPointD(p: PointD): PointD {
        return PointD(p.x, if (mScreenInverted) mScreen.height - p.y else p.y)
    }

    /**
     * 四角形をスクリーン座標で(倒立に)変換
     * rect     四角座標
     * return   四角座標
     */
    fun invertedRectD(rect: RectD): RectD {
        return RectD(invertedPointD(rect.leftTop()), invertedPointD(rect.rightBottom()))
    }

    //  ---  三次元変換(アフィン変換)マトリックスパラメータの設定

    /**
     *  変換パラメータでアフィン変換を行う
     *  p       3D座標
     *  return  変換後の3D座標
     */
//    fun affinTransform(p: Point3D): Point3D {
//        var mp = arrayOf(
//            arrayOf(p.x, p.y, p.z, 1.0)
//        )
//        val rp = klib.matrixMulti(mp, mMatrix)
//        return Point3D(rp[0][0], rp[0][1], rp[0][2])
//    }

    /**
     * 座標変換パラメータの詩を幾何
     */
    fun matrixClear() {
        mMatrix = klib.unitMatrix(4)
    }

    /**
     * 平行移動パラメータ設定
     * dx       X軸方向移動量
     * dy       y軸方向移動量
     * dz       z軸方向移動量
     */
    fun setTranslate(dx: Double, dy: Double, dz: Double) {
        var mp = arrayOf(
            arrayOf(1.0, 0.0, 0.0, 0.0),
            arrayOf(0.0, 1.0, 0.0, 0.0),
            arrayOf(0.0, 0.0, 1.0, 0.0),
            arrayOf(dx,  dy,  dz,  1.0)
        )
        mMatrix = klib.matrixMulti(mMatrix, mp)
    }

    /**
     * X軸回転パラメータ設定
     * th       回転角(rad)
     */
    fun setRotateX(th: Double) {
        var mp = arrayOf(
            arrayOf(1.0,      0.0,     0.0, 0.0),
            arrayOf(0.0,  cos(th), sin(th), 0.0),
            arrayOf(0.0, -sin(th), cos(th), 0.0),
            arrayOf(0.0,      0.0,     0.0, 1.0)
        )
        mMatrix = klib.matrixMulti(mMatrix, mp)
    }

    /**
     * Y軸回転パラメータ設定
     * th       回転角(rad)
     */
    fun setRotateY(th: Double) {
        var mp = arrayOf(
            arrayOf(cos(th), 0.0, -sin(th), 0.0),
            arrayOf(    0.0, 1.0,      0.0, 0.0),
            arrayOf(sin(th), 0.0,  cos(th), 0.0),
            arrayOf(    0.0, 0.0,      0.0, 1.0)
        )
        mMatrix = klib.matrixMulti(mMatrix, mp)
    }

    /**
     * Z軸回転パラメータ設定
     * th       回転角(rad)
     */
    fun setRotateZ(th: Double) {
        var mp = arrayOf(
            arrayOf( cos(th), sin(th), 0.0, 0.0),
            arrayOf(-sin(th), cos(th), 0.0, 0.0),
            arrayOf(     0.0,     0.0, 1.0, 0.0),
            arrayOf(     0.0,     0.0, 0.0, 1.0)
        )
        mMatrix = klib.matrixMulti(mMatrix, mp)
    }

    /**
     * スケール変換パラメータ設定]
     * sx       X方向縮尺
     * sy       Y方向縮尺
     * sz       Z方向縮尺
     */
    fun setScale(sx: Double, sy: Double, sz: Double) {
        var mp = arrayOf(
            arrayOf( sx, 0.0, 0.0, 0.0),
            arrayOf(0.0,  sy, 0.0, 0.0),
            arrayOf(0.0, 0.0,  sz, 0.0),
            arrayOf(0.0, 0.0, 0.0, 1.0)
        )
        mMatrix = klib.matrixMulti(mMatrix, mp)
    }


    //  ----  単位変換 ----

    fun D2R(deg: Double): Double {
        return deg * PI / 180.0
    }

    fun R2D(rad: Double): Double {
        return rad * 180.0 / PI
    }
}