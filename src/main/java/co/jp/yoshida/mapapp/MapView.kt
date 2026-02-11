package co.jp.yoshida.mapapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.util.Log
import android.util.SizeF
import android.view.View
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class MapView(context: Context, var mMapData: MapData): View(context) {
    val TAG = "MapView"

    var mWidth = mMapData.mView.width       //  地図のlinerLayoutの幅(MainActivityで取得)
    var mHeight = mMapData.mView.height     //  地図のlinerLayoutの高さ(MainActivityで取得)
    var mColCount = 2                       //  セルの横方向ま数
    var mRowCount = 2                       //  セルの縦方向の数
    var mCellSize = 0                       //  セルの大きさ

    var mGCells = mutableListOf<GCell>()
    var mOffset = PointD(0.0, 0.0)

    var mMarkList = MarkList()              //  マークのデータ
    var mMeasure = Measure()                //  距離測定実行中のデータ
    var mGpsTrace = GpsTrace()              //  GPSトレース実行中のデータ
    var mGpsTraceList = GpsTraceList()      //  GPSトレースデータ
    //    var mGpsDataList = GpsDataList()        //  GPSデータ
    var mElevator = 0.0                     //  標高値(Mainから設定)
    var mLastSpeedAveSize = 16              //  GPSトレース速度の移動平均データ数(表示用)
    var mCenterColor = ""                   //  中心の色(凡例で使用)
    var mComment = ""                       //  コメント表示(凡例データなどせ)
    var mMessage = ""                       //  非同期のメッセージ表示
    var mAzimuth = 0                        //  方位(Deg)
    var mOrientation = true                 //  方位センサーの有無
    var mInfoTextSize = 32.0                //  画面左上の情報表示文字サイズ
    var mDispDateTime = mutableListOf<LocalDateTime>()
    var mDateTimeFormat = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
    var mDispCommentOffset = 0f             //  OSversionによって座標などのコメント位置を補正)

    var klib = KLib()
    val kdraw = KDraw()

    init {
        mCellSize = (mWidth / mColCount).toInt()
        setCellBoard()
        mDispCommentOffset = if (klib.getOSVersion().toInt() < 14) 50f else 0f
    }

    //  描画処理
    override fun onDraw(canvas: Canvas) {
//        super.onDraw(canvas)
        kdraw.mCanvas = canvas
        //  セルの表示
        for (cell in mGCells) {
            cell.draw(canvas)
        }
        //  マークのの表示
        mMarkList.draw(canvas, mMapData)
        //  測定線の表示
        mMeasure.draw(canvas, mMapData)
        //  トレース中のGPSデータの表示
        mGpsTrace.draw(canvas, mMapData)
        //  GPSトレースリスト内のトレースデータ表示
        mGpsTraceList.draw(canvas, mMapData)
        //  中心線
        drawCross()
        //  座標と標高、凡例の表示
        drawCoordinates(mMapData)
        //  縮尺表示
        drawScaler()
        //  方位の表示
        if (mOrientation)
            drawOrientation(mAzimuth)
    }

    //  再表示
    fun reDraw() {
        invalidate()
    }

    //  座標表示と標高、GPSトレースデータ、地質図凡例表示
    fun drawCoordinates(mapData: MapData) {
        //  表示位置
        var x = 10f
        var y = 40f + mDispCommentOffset
        //  中心座標と標高
        var bp = mapData.screen2BaseMap(getCenter())
        var cp = mapData.baseMap2Coordinates(bp)
        var coordeMsg = " 座標 "+"%3.6f".format(cp.y)+","+"%3.6f".format(cp.x)
        var ele = mElevator
        var acc = ""
        var type = "MAP"
        if (mGpsTrace.mTraceOn && 0 < mGpsTrace.mGpsPointData.size) {
            //  GPS起動時の標高データ
            ele = mGpsTrace.mGpsLastElevator    //  標高(m)
            acc = " 精度 " + mGpsTrace.mGpsLastAccury.toInt().toString()
            type = "GPS"
        }
        coordeMsg += " 標高 " + "%,4.1f m".format(ele) + acc + " (" + type + ") 色 " + mCenterColor

        kdraw.setColor("Blue")
        kdraw.setTextSize(mInfoTextSize)
        kdraw.drawTextWithBox(coordeMsg, PointD(x.toDouble(), y.toDouble()))

        //  GPSトレース中の距離と歩数の表示
        if (type.compareTo("GPS") == 0 && 0 < mGpsTrace.mGpsPointData.size) {
            var moveMsg = " 移動距離 " + "%,.2f km".format(mGpsTrace.totalDistance())
            val min = mGpsTrace.lastLap() / 60.0
            if (min / 60.0 < 1.0 )
                moveMsg += " 経過時間 " + "%,.1f min".format(min)
            else
                moveMsg += " 経過時間 " + "%d h".format((min / 60).toInt()) + " %2d min".format((min % 60).toInt())
            moveMsg += " 速度 " + "%,.2f km/h".format(mGpsTrace.lastSpeed(mLastSpeedAveSize))
            moveMsg += " 歩数 " + mGpsTrace.stepCount().toString()
            y += mInfoTextSize.toFloat() + 10f
            kdraw.drawTextWithBox(moveMsg, PointD(x.toDouble(), y.toDouble()))
        }
        //  色凡例を表示
        if (0 < mComment.length) {
            y += mInfoTextSize.toFloat() + 10f
            kdraw.drawTextWithBox(mComment, PointD(x.toDouble(), y.toDouble()))
        }
        //  測定距離
        if (mMeasure.mMeasureMode && 1 < mMeasure.mPositionList.size) {
            y += mInfoTextSize.toFloat() + 10f
            var dis = mMeasure.measure(mMapData)
            kdraw.drawTextWithBox("測定距離 %.3f km".format(dis), PointD(x.toDouble(), y.toDouble()))
        }
        //  メッセージ表示(非同期処理など)
        if (0 < mMessage.length) {
            y += mInfoTextSize.toFloat() + 10f
            kdraw.drawTextWithBox(mMessage, PointD(x.toDouble(), y.toDouble()))
        }
        //  気象データなどの測定時間と予想時刻の表示
        if (1 < mDispDateTime.size) {
            y += mInfoTextSize.toFloat() + 10f
            kdraw.drawTextWithBox("測定" + mDispDateTime[0].format(mDateTimeFormat), PointD(x.toDouble(), y.toDouble()))
            y += mInfoTextSize.toFloat() + 10f
            kdraw.drawTextWithBox("予測" + mDispDateTime[1].format(mDateTimeFormat), PointD(x.toDouble(), y.toDouble()))
        }
    }

    /**
     * 方位の表示
     * orientation : 北の向き(degree)
     */
    fun drawOrientation(orientation: Int) {
        //  方位の表示円
        kdraw.setProperty("Blue", 4.0, Paint.Style.STROKE )
        val r = mWidth / 20.0
        var ctr = PointD(r + 10.0, mHeight - r - 10.0)
        kdraw.drawCircle(ctr, r)
        //  方位矢印(三角形)の作成
        var path = mutableListOf<PointD>()
        var angle = (-orientation - 90).toDouble()
        var p = ctr.plus(PointD(r * cos(angle / 180 * PI), r * sin(angle / 180 * PI)))
        path.add(p)
        p = ctr.plus(PointD(r * cos((angle + 150) / 180 * PI), r * sin((angle + 150) / 180 * PI)))
        path.add(p)
        p = ctr.plus(PointD(r * cos((angle - 150) / 180 * PI), r * sin((angle - 150) / 180 * PI)))
        path.add(p)
        p = ctr.plus(PointD(r * cos(angle / 180 * PI), r * sin(angle / 180 * PI)))
        path.add(p)
        kdraw.setProperty("Blue", 4.0, Paint.Style.FILL )
        kdraw.drawPath(path)
    }

    //  中心線表示
    fun drawCross() {
        kdraw.setProperty("Blue", 4.0, Paint.Style.FILL)
        var ctr = PointD(mWidth / 2.0, mHeight / 2.0)
        kdraw.drawLine(PointD(ctr.x - 50.0, ctr.y), PointD(ctr.x + 50.0, ctr.y))
        kdraw.drawLine(PointD(ctr.x, ctr.y - 50.0), PointD(ctr.x, ctr.y + 50.0))
    }

    //  縮尺表示
    fun drawScaler() {
        kdraw.setColor("Green")
        kdraw.mStrokWidth = 10f
        val l = 0.4
        val epx = 0.95
        val epy = 0.98
        val sp = PointD(mMapData.mView.width * epx, mMapData.mView.height * epy)
        var ep = PointD(mMapData.mView.width * (epx - l), mMapData.mView.height * epy)
        val scp = mMapData.screen2Coordinates(sp)
        val ecp = mMapData.screen2Coordinates(ep)
        val ls = klib.cordinateDistance(scp, ecp)
        val mls = klib.floorStepSize(ls)
        ep.x = mMapData.mView.width * (epx - l * mls / ls)
        kdraw.drawLine(sp, ep)
        kdraw.setTextSize(mInfoTextSize)
        kdraw.setColor("Black")
        kdraw.drawText("%.2f km".format(mls), PointD(mMapData.mView.width * (epx - l * mls / ls * 0.75), mMapData.mView.height * epy))
    }

    //  セルにデータを設定
    fun setCellBoard() {
        if (mWidth <= 0 || mHeight <= 0 || mColCount == 0 || mRowCount == 0)
            return
        setCelSize()
        mGCells.clear()
        var sx = 0f
        var width = 0f
        var height = 0f
        for (x: Int in 0..mColCount) {
            var sy = 0f
            if (mWidth <= sx)
                break;
            for (y: Int in 0..mRowCount) {
                if (mHeight <= sy)
                    break;
                //  セルの設定
                val gcell = GCell()
                gcell.mId = getId(x, y)
                gcell.mGCellType = GCELLTYPE.RECT
                gcell.mBackColor = Color.RED
                gcell.mPosition = PointF(sx, sy)
                width = mCellSize.toFloat()
                height = mCellSize.toFloat()
                //  画像のトリミング
                if (mWidth < sx + width) {
                    width = mWidth - sx
                    gcell.mTrimmingRect.right = width / mCellSize
                } else if (x == 0 && mOffset.x != 0.0) {
                    gcell.mTrimmingRect.left = mOffset.x.toFloat()
                    width = mCellSize * (1.0 - mOffset.x).toFloat()
                } else {
                    gcell.mTrimmingRect.right = 1f
                }
                if (mHeight < sy + height) {
                    height = mHeight - sy
                    gcell.mTrimmingRect.bottom = height / mCellSize
                } else if (y == 0 && mOffset.y != 0.0) {
                    gcell.mTrimmingRect.top = mOffset.y.toFloat()
                    height = mCellSize * (1.0 - mOffset.y).toFloat()
                } else {
                    gcell.mTrimmingRect.bottom = 1f
                }
                //  画僧サイズの登録
                gcell.mSize = SizeF(width, height)
                mGCells.add(gcell)
                sy += height
            }
            sx += width
        }
    }

    //  列数からセルサイズと行数を求める
    fun setCelSize() {
//        mCellSize = (mWidth * mCellZoom / mColCount).toInt()
        mRowCount = mHeight / mCellSize + 1
    }

    //  Cellにイメージファイルのパスを設定
    fun setCellImage(col: Int, row: Int, imageFile: String) {
        var id = getId(col, row)
        for (cell in mGCells) {
            if (cell.mId == id) {
                cell.mImageFile = imageFile
                break
            }
        }
    }

    /**
     * セルのBitmapの取得
     *  col     列
     *  row     行
     *  return  Bitmap
     */
    fun getCellImage(col: Int, row: Int): Bitmap? {
        var id = getId(col, row)
        for (cell in mGCells) {
            if (cell.mId == id) {
                return cell.getBitmapImage()
            }
        }
        return null
    }

    //  行列からIDを求める
    fun getId(col: Int, row: Int): Int {
        return col + row * (mColCount + 1)
    }

    //  IDから列番を求める
    fun getIdCol(id: Int): Int {
        return id % (mColCount + 1)
    }

    //  IDから行番を求める
    fun getIdRow(id: Int): Int {
        return id / (mColCount + 1)
    }

    //  画面(スクリーン)の中心座標
    fun getCenter(): PointD {
        return PointD(mWidth.toDouble() / 2.0, mHeight.toDouble() / 2.0)
    }
}