package co.jp.yoshida.mapapp

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.util.Log
import android.util.Size
import android.view.View
import java.util.Date
import kotlin.math.max
import kotlin.math.min

class GpsGraphView(context: Context, var mGpsData: GpxReader): View(context) {
    val TAG = "GpsGraphView"

    var mYTypeTitle = ""            //  縦軸の種別名
    var mXTypeTitle = ""            //  横軸の種別名
    var mYType = YTYPE.LapTime      //  縦軸の種別
    var mXType = XTYPE.Distance     //  横軸の種別
    var mAverageCountTitle = ""     //  移動平均のデータ数
    enum class YTYPE { LapTime, Distance, Elevator, ElevatorDiff, Speed, StepCount }
    enum class XTYPE { Distance, LapTime, DateTime }
    var mStepYSize = 1.0            //  縦軸目盛のステップサイズ
    var mStepXSize = 1.0            //  横軸目盛のステップサイズ
    var mLeftMargin = 180.0         //  左マージン(スクリーンサイズ)
    var mTopMargin = 230.0          //  下部(倒立時)マージン(スクリーンサイズ)
    var mRightMargine = 40.0        //  右マージン(スクリーンサイズ)
    var mBottomMargin = 20.0        //  上部(倒立時)マージン(スクリーンサイズ)
    var mStartPos = 0               //  表示開始データ位置
    var mEndPos = 0                 //  表示終了データ位置
    val mYTitle = listOf(
        "経過時間", "距離(km)", "標高(m)", "標高差(m)", "速度(km/h)", "歩数")      // 　縦軸タイトル
    val mXTitle = listOf("距離(km)", "経過時間", "日時")                         //  横軸タイトル
    val mGraphYType = listOf<String>(
        "経過時間", "距離(km)", "標高(m)", "標高差(m)", "速度(km/h)", "歩数")      //  縦軸のデータ種別
    val mGraphXType = listOf<String>("距離(km)", "経過時間", "日時")             //  横軸のデータ種別
    var mAverageCount = listOf<String>("なし", "2", "3", "4", "5", "7", "10", "15", "20",
        "25", "30", "40", "50", "100", "150", "200", "300", "400", "500", "1000")

    var kdraw = KDraw()
    var klib = KLib()

    init {
        kdraw.mScreen = Size(width, height) //  スクリーンサイズ設定(この時点では無効)
        kdraw.mScreenInverted = true        //  倒立表示設定
        setDataType()                       //  データ種別の初期設定
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        //  Android12以降、初期表示以降となるので初期時に別に設定が必要
        setInitGraphScreen(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        kdraw.mCanvas = canvas
        //  グラフ領域の補助線と目盛の表示
        initGraphArea()
        //  データの表示
        drawData()
    }

    /**
     * 再描画
     */
    fun reDraw() {
        invalidate()
    }

    /**
     * グラフエリアの初期設定
     * width        グラフエリアの幅
     * height       グラフエリアの高さ
     */
    fun setInitGraphScreen(width: Int, height:Int) {
        //  スクリーンサイズの設定
        kdraw.setInitScreen(width, height)
        //  倒立表示
        kdraw.mScreenInverted = true
        //  ビュー領域を設定
        kdraw.mView = RectD(mLeftMargin, mTopMargin, width - mRightMargine, height - mBottomMargin)
        //  最大値の初期設定
        var mYmax = mGpsData.mGpsInfoData.mMaxElevator
        var mXmax = mGpsData.mGpsInfoData.mDistance
        //  ワールド領域を初期設定
        kdraw.mWorld = RectD(0.0, 0.0, mXmax, mYmax)
        //  文字サイズと文字太さを初期設定
        kdraw.mTextSize = 30f
        kdraw.mTextStrokeWidth = 2f
        //  表示データ位置
        mStartPos = 0
        mEndPos = mGpsData.mListGpsData.lastIndex
        mStepYSize = klib.graphStepSize(kdraw.mWorld.height(), 5.0)
    }

    /**
     * グラフの画面表示
     */
    fun initGraphArea() {
        //  グラフの種類を設定
        setDataType()
        //  データの領域をワールド座標に設定
        kdraw.mWorld = getGraphArea()
        mStepYSize = klib.graphStepSize(kdraw.mWorld.height(), 5.0)
        //  ステップサイズや上部下部マージンを補正してワールド座標を再設定
        setWorldArea()
        //  目盛のステップサイズを設定
        setStepSize()
        //  グラフ枠の表示
        kdraw.backColor(Color.WHITE)
        kdraw.mColor = "Black"
        kdraw.drawWRect(RectD(kdraw.mWorld))
        //  補助線と目盛の表示
        setAxis()
    }

    /**
     * GPSデータの表示
     */
    fun drawData() {
        var averageCount = mAverageCountTitle.toIntOrNull()?:0
        var sx = getXData(mStartPos, mXType, true)
        var sy = getMovingAverage(mStartPos, averageCount, mYType)
        for (i in max(mStartPos,1)..mEndPos) {
            var ex = getXData(i, mXType) + sx
            var ey = 0.0
            if (mYType == YTYPE.Distance || mYType == YTYPE.LapTime) {
                ey = getYData(i, mYType) + sy
            } else {
                ey = getMovingAverage(i, averageCount, mYType)
            }
            kdraw.drawWLine(PointD(sx, sy), PointD(ex, ey))
            sx = ex
            sy = ey
        }
    }

    /**
     * 補助線と目盛を設定
     */
    fun setAxis() {
        //  横軸の目盛と補助線、タイトル
        var x = kdraw.mWorld.left
        xScaleDraw(x, PointD(x, kdraw.mWorld.top))
        while (x < (kdraw.mWorld.right - mStepXSize)) {
            if (x == kdraw.mWorld.left && x % mStepXSize != 0.0)
                x -= x % mStepXSize
            x += mStepXSize
            kdraw.drawWLine(PointD(x, kdraw.mWorld.top), PointD(x, kdraw.mWorld.bottom))
            if (kdraw.mWorld.left + kdraw.mWorld.width() * 0.05 < x &&
                x < kdraw.mWorld.right - kdraw.mWorld.width() * 0.05)
                xScaleDraw(x, PointD(x, kdraw.mWorld.top))
        }
        xScaleDraw(kdraw.mWorld.right, PointD(kdraw.mWorld.right, kdraw.mWorld.top))
        getXTitleDraw()

        //  縦軸の目盛と補助線、タイトル
        var y = kdraw.mWorld.top
        yScaleDraw(y, PointD(kdraw.mWorld.left, y))
        while (y < kdraw.mWorld.bottom ) {
            y += mStepYSize
            kdraw.drawWLine(PointD(kdraw.mWorld.left, y), PointD(kdraw.mWorld.right, y))
            yScaleDraw(y, PointD(kdraw.mWorld.left, y))
        }
        getYTitleDraw()
        Log.d(TAG,"setAxis "+mGpsData.mListGpsData.size+" "+
                mGpsData.mListGpsData.sumOf{it.mLap}+" "+
                (mGpsData.mListGpsData.sumOf { it.mLap } / mGpsData.mListGpsData.size))
    }

    /**
     * 横軸のタイトル表示
     */
    fun getXTitleDraw() {
        val xtitle = when (mXType) {
            XTYPE.Distance -> mXTitle[0]
            XTYPE.LapTime -> mXTitle[1]
            XTYPE.DateTime -> mXTitle[2]
        }
        kdraw.drawWText(xtitle, PointD(kdraw.mWorld.centerX(),
            kdraw.mWorld.top - kdraw.cnvScreen2WorldY(mTopMargin - 25.0)),
            0.0, KDraw.HALIGNMENT.Center, KDraw.VALIGNMENT.Bottom)
    }

    /**
     * 縦軸のタイトル表示
     */
    fun getYTitleDraw() {
        val ytitle = when (mYType) {
                YTYPE.LapTime -> mYTitle[0]
                YTYPE.Distance -> mYTitle[1]
                YTYPE.Elevator -> mYTitle[2]
                YTYPE.ElevatorDiff -> mYTitle[3]
                YTYPE.Speed -> mYTitle[4]
                YTYPE.StepCount -> mYTitle[5] +
                        String.format("(step/%.1fsec)",
                            (mGpsData.mListGpsData.sumOf { it.mLap } / 1000.0 / mGpsData.mListGpsData.size))
        }
        kdraw.drawWText(ytitle, PointD(kdraw.mWorld.left - kdraw.cnvScreen2WorldX(mLeftMargin - 20.0),
            kdraw.mWorld.centerY()),
            90.0, KDraw.HALIGNMENT.Center, KDraw.VALIGNMENT.Bottom)
    }

    /**
     * 横軸目盛の表示
     * value        目盛の値
     * pos          表示位置
     */
    fun xScaleDraw(value: Double, pos: PointD) {
        pos.y -= kdraw.cnvScreen2WorldY(kdraw.mTextSize.toDouble()) / 2.0
        when (mXType) {
            XTYPE.Distance -> {
                kdraw.drawWText("%,.2f".format(value), pos, 90.0, KDraw.HALIGNMENT.Left, KDraw.VALIGNMENT.Center)

            }
            XTYPE.LapTime -> {
                kdraw.drawWText(klib.lap2String(value.toLong()), pos, 90.0, KDraw.HALIGNMENT.Left, KDraw.VALIGNMENT.Center)
            }
            XTYPE.DateTime -> {
                var dateTime = Date((value - (9 * 60 * 60 * 1000)).toLong())
                kdraw.drawWText(klib.date2String(dateTime, "yyyy/MM/dd"), pos, 90.0, KDraw.HALIGNMENT.Left, KDraw.VALIGNMENT.Bottom)
                pos.x -= kdraw.cnvScreen2WorldX(kdraw.mTextSize.toDouble())
                kdraw.drawWText(klib.date2String(dateTime, "HH:mm:ss"), pos, 90.0, KDraw.HALIGNMENT.Left, KDraw.VALIGNMENT.Bottom)
            }
        }
    }

    /**
     * 縦軸目盛の表示
     * value        目盛の値
     * pos          表示位置
     */
    fun yScaleDraw(value: Double, pos: PointD) {
        pos.x -= kdraw.cnvScreen2WorldX(kdraw.mTextSize.toDouble()) / 2.0
        when (mYType) {
            YTYPE.LapTime -> {
                kdraw.drawWText(klib.lap2String(value.toLong()), pos, 0.0, KDraw.HALIGNMENT.Right, KDraw.VALIGNMENT.Center)
            }
            YTYPE.Distance -> {
                kdraw.drawWText("%,.2f".format(value), pos, 0.0, KDraw.HALIGNMENT.Right, KDraw.VALIGNMENT.Center)
            }
            YTYPE.Elevator -> {
                kdraw.drawWText("%,.0f".format(value), pos, 0.0, KDraw.HALIGNMENT.Right, KDraw.VALIGNMENT.Center)
            }
            YTYPE.ElevatorDiff -> {
                kdraw.drawWText("%,.0f".format(value), pos, 0.0, KDraw.HALIGNMENT.Right, KDraw.VALIGNMENT.Center)
            }
            YTYPE.Speed -> {
                kdraw.drawWText("%,.1f".format(value), pos, 0.0, KDraw.HALIGNMENT.Right, KDraw.VALIGNMENT.Center)
            }
            YTYPE.StepCount -> {
                kdraw.drawWText("%,.0f".format(value), pos, 0.0, KDraw.HALIGNMENT.Right, KDraw.VALIGNMENT.Center)
            }
        }
    }

    /**
     * グラフ補助線の間隔を設定
     */
    fun setStepSize() {
        //  縦軸目盛り線の間隔
        mStepYSize = klib.graphStepSize(kdraw.mWorld.height(), 5.0)
        //  横軸軸目盛り線の間隔
        when (mXType) {
            XTYPE.Distance -> {
                //  横軸目盛り距離
                mStepXSize = klib.graphStepSize(kdraw.mWorld.width(), 8.0)
            }
            XTYPE.LapTime->{
                //  横軸目盛り経過時間(s)
                val minit: Double = kdraw.mWorld.width() / 1000.0 / 60.0 //  m秒を分に換算
                if (60.0 * 24.0 * 10.0 < minit) {                       //  10日以上は日単位で計算
                    mStepXSize = klib.graphStepSize(minit / 60.0 / 24.0, 8.0, 24.0) * 24.0 * 60.0
                } else {
                    mStepXSize = klib.graphStepSize(minit, 8.0, 60.0)
                }
                mStepXSize *= 60.0 * 1000.0
            }
            XTYPE.DateTime ->{
                //  横軸目盛り経過時間(s)
                val minit: Double = kdraw.mWorld.width() / 1000.0 / 60.0 //  m秒を分に換算
                if (60.0 * 24.0 * 10.0 < minit) {                       //  10日以上は日単位で計算
                    mStepXSize = klib.graphStepSize(minit / 60.0 / 24.0, 8.0, 24.0) * 24.0 * 60.0
                } else {
                    mStepXSize = klib.graphStepSize(minit, 8.0, 60.0)
                }
                mStepXSize *= 60.0 * 1000.0
            }
        }
    }

    /**
     * ワールド座標領域をステップサイズや上下マージンを加えて設定
     */
    fun setWorldArea() {
        kdraw.mWorld.bottom = klib.graphHeightSize(kdraw.mWorld.bottom, mStepYSize)
        kdraw.mWorld.top = if (mYType == YTYPE.ElevatorDiff) mGpsData.mGpsInfoData.mMinElevator
        else 0.0
        kdraw.mWorld.left = getXData(mStartPos, mXType, true)
        kdraw.mWorld.right = getXData(mEndPos, mXType, true)
    }
    /**
     * データ領域の取得
     */
    fun getGraphArea(): RectD {
        var rect = RectD()
        rect.top = if (mYType == YTYPE.ElevatorDiff) getMinYData(mYType) else 0.0
        rect.bottom = getMaxYData(mYType)
        rect.left = getXData(mStartPos, mXType, true,)
        rect.right = getXData(mEndPos, mXType, true)
        return rect
    }

    /**
     * グラフを左右方向に拡大
     * zoom         拡大率
     */
    fun zoomDisp(zoom: Double = 2.0) {
        var dispWidth = mEndPos - mStartPos
        var dispCenter = (mStartPos + mEndPos) / 2
        mStartPos = (dispCenter - dispWidth / (zoom * 2.0)).toInt()
        mEndPos = (dispCenter + dispWidth / (zoom * 2.0)).toInt()
        mStartPos = max(0, mStartPos)
        mEndPos = min(mGpsData.mListGpsData.lastIndex, mEndPos)
    }

    /**
     * グラフを左右に移動させる
     * move         -x:左 x:右
     */
    fun moveDisp(move: Double = 0.5) {
        var dispWidth = mEndPos - mStartPos
        var moveWidth = (dispWidth * move).toInt()
        if (0 < moveWidth) {
            if (mGpsData.mListGpsData.lastIndex <= mEndPos + moveWidth)
                moveWidth = mGpsData.mListGpsData.lastIndex - mEndPos
        } else {
            if (mStartPos + moveWidth < 0)
                moveWidth = -mStartPos
        }
        mStartPos += moveWidth
        mEndPos += moveWidth
        mStartPos = max(0, mStartPos)
        mEndPos = min(mGpsData.mListGpsData.lastIndex, mEndPos)
    }

    /**
     * Yデータの最大値を取得(mStarPosとmEndPosの間で)
     * type         データの種別
     * retrurn      最大値
     */
    fun getMaxYData(type: YTYPE): Double {
        var maxData = Double.MIN_VALUE
        var averageCount = mAverageCountTitle.toIntOrNull()?:0
        if (mYType == YTYPE.Distance || mYType == YTYPE.LapTime) {
            maxData = max(maxData, getYData(mEndPos, type, true))
        } else {
            for (i in max(mStartPos, 1)..mEndPos) {
                maxData = max(maxData, getMovingAverage(i, averageCount, type))
            }
        }
        return maxData
    }

    /**
     * Yデータの最小値を取得(mStarPosとmEndPosの間で)
     * type         データの種別
     * return       最小値
     */
    fun getMinYData(type: YTYPE): Double {
        var minData = Double.MAX_VALUE
        var averageCount = mAverageCountTitle.toIntOrNull()?:0
        if (mYType == YTYPE.Distance || mYType == YTYPE.LapTime) {
            minData = min(minData, getYData(mStartPos, type, true))
        } else {
            for (i in max(mStartPos, 1)..mEndPos) {
                minData = min(minData, getMovingAverage(i, averageCount, type))
            }
        }
        return minData
    }

    /**
     * データの移動平均を求める
     * pos          データの位置
     * averageCount 移動平均を求めるデータ数
     * dataType     データの種別
     * return       平均値
     */
    fun getMovingAverage(pos: Int, averageCount: Int, dataType: YTYPE): Double {
        var sum = 0.0
        var count = 0
        var startCount = averageCount / 2
        for (i in Math.max(0, pos - startCount)..Math.min(mGpsData.mListGpsData.size - 1, pos + startCount)) {
            when (dataType) {
                YTYPE.LapTime -> {
                    sum += mGpsData.mListGpsData[i].mLap.toDouble()
                }
                YTYPE.Distance -> {
                    sum += mGpsData.mListGpsData[i].mDistance
                }
                YTYPE.Elevator -> {
                    sum += mGpsData.mListGpsData[i].mElevator
                }
                YTYPE.ElevatorDiff -> {
                    sum += mGpsData.mListGpsData[i].mElevator
                }
                YTYPE.Speed -> {
                    sum += mGpsData.mListGpsData[i].mSpeed
                }
                YTYPE.StepCount -> {
                    sum += mGpsData.mListGpsData[i].mStepCount
                }
            }
            count++
        }
        return sum / count
    }

    /**
     * 横軸データの取得(一つ前との増分値/累積値) DateTimeの時は累積値の代わりにその日時
     * pos          データ位置
     * dataType     データの種別(Distance/LapTime/DateTime)
     * sum          増分と累積値の切替
     * return       データの値(km/ms/ms)
     */
    fun getXData(pos: Int, dataType: XTYPE, sum: Boolean = false): Double {
        return when (dataType) {
            XTYPE.Distance -> {
                if (sum)
                    mGpsData.mListGpsData.take(pos).sumOf { it.mDistance }
                else
                    mGpsData.mListGpsData[pos].mDistance
            }
            XTYPE.LapTime -> {
                if (sum)
                    mGpsData.mListGpsData.take(pos).sumOf { it.mLap.toDouble() }
                else
                    mGpsData.mListGpsData[pos].mLap.toDouble()
            }
            XTYPE.DateTime -> {
                if (sum)
                    mGpsData.mListGpsData[pos].mDate.time.toDouble()
                else
                    mGpsData.mListGpsData[pos].mLap.toDouble()
            }
        }
    }

    /**
     * 縦軸データの取得
     * pos      データ位置
     * dataType データの種別
     * return   データの値
     */
    fun getYData(pos: Int, dataType: YTYPE, sum: Boolean = false): Double {
        return when (dataType) {
            YTYPE.Distance -> {
                if (sum)
                    mGpsData.mListGpsData.take(pos).sumOf { it.mDistance }
                else
                    mGpsData.mListGpsData[pos].mDistance
            }
            YTYPE.LapTime -> {
                if (sum)
                    mGpsData.mListGpsData.take(pos).sumOf { it.mLap.toDouble() }
                else
                    mGpsData.mListGpsData[pos].mLap.toDouble()
            }
            YTYPE.Elevator -> {
                mGpsData.mListGpsData[pos].mElevator
            }
            YTYPE.ElevatorDiff -> {
                mGpsData.mListGpsData[pos].mElevator
            }
            YTYPE.Speed -> {
                mGpsData.mListGpsData[pos].mSpeed
            }
            YTYPE.StepCount -> {
                mGpsData.mListGpsData[pos].mStepCount.toDouble()
            }
        }
    }


    /**
     * データの種別を設定
     */
    fun setDataType() {
        if (mYTypeTitle.compareTo(mGraphYType[0]) == 0) {
            mYType = YTYPE.LapTime
        } else if (mYTypeTitle.compareTo(mGraphYType[1]) == 0) {
            mYType = YTYPE.Distance
        } else if (mYTypeTitle.compareTo(mGraphYType[2]) == 0) {
            mYType = YTYPE.Elevator
        } else if (mYTypeTitle.compareTo(mGraphYType[3]) == 0) {
            mYType = YTYPE.ElevatorDiff
        } else if (mYTypeTitle.compareTo(mGraphYType[4]) == 0) {
            mYType = YTYPE.Speed
        } else if (mYTypeTitle.compareTo(mGraphYType[5]) == 0) {
            mYType = YTYPE.StepCount
        }

        if (mXTypeTitle.compareTo(mGraphXType[0]) == 0) {
            mXType = XTYPE.Distance
        } else if (mXTypeTitle.compareTo(mGraphXType[1]) == 0) {
            mXType = XTYPE.LapTime
        } else if (mXTypeTitle.compareTo(mGraphXType[2]) == 0) {
            mXType = XTYPE.DateTime
        }
    }
}