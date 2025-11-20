package co.jp.yoshida.mapapp

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.util.Log
import android.util.Size
import android.view.View
import kotlin.math.min


class GpsTraceListGraphView(context: Context): View(context) {
    private val TAG = "GpsTraceListGraphView"

    private var mWorldLeft = 0.0            //  グラフエリア左端
    private var mWorldTop = 0.0             //  グラフエリア上端
    private var mWorldRight = 100.0         //  グラフエリア右端
    private var mWorldBottom = 365.0        //  グラフエリア下端
    private var mLeftGapRaito = 0.1         //  グラフエリアの左ギャップ比率
    private var mBottomGapRaito = 0.08      //  グラフエリアの下ギャップ比率
    private var mRightGapRaito = 0.02       //  グラフエリアの右ギャップ比率
    private var mTopGapRaito = 0.05         //  グラフエリアの上ギャップ比率
    private var mFontSize = 40.0            //  文字の大きさ

    var mStartDate: String = "2025/01/01"       //  開始日付
    var mLastDate: String = "2026/01/01"        //  終了日付
    var mStartDay = 0                           //  開始日(Day of Year)
    var mLastDay = 0                            //  終了日(Day of Year)
    var mYear = 2025                            //  対象年
    var mStartMonth = 1                         //  開始月
    var mSpanMonth = 12                         //  表示期間
    var mDataType: String = ""                  //  データの種類(距離,時間...)
    var mCollectUnit = CollectUnit.Time         //  集計単位
    var mGraphData = mutableListOf<GraphData>(GraphData())  //  表示データ
    var mYearData = GraphData()                 //  年間の累積データ

    var mTotalDistance = 0f                     //  年間合計距離
    var mTotalLap = 0f                          //  年間合計時間
    var mMaxElevator = 0f                       //  最大標高
    var mTotalElevator = 0f                     //  年間累積標高

    val mDataTypeMap = mapOf(
        "移動距離" to "(km)",
        "移動時間" to "(時:分)",
        "速度" to "(km/h)",
        "最大高度" to "(m)",
        "累積標高差" to "(m)",
        "歩数" to ""
    )

    var kdraw = KDraw()
    var klib = KLib()

    init {
        kdraw.mScreen = Size(width, height) //  スクリーンサイズ設定(この時点では無効)
        kdraw.mScreenInverted = false       //  倒立表示設定
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        //  Android12以降、初期表示以降となるので初期時に別に設定が必要
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        kdraw.mCanvas = canvas
        //  グラフ領域の補助線と目盛の表示
        initScreen()
        setViewArea()
        setWorldArea(mWorldLeft, mWorldTop, mWorldRight, mWorldBottom)
        //  補助線の表示
        drawAxis(mDataType)
        //  データの表示
        drawData(mCollectUnit, mDataType)
        drawYearData()
    }


    /**
     * 再描画
     */
    fun reDraw() {
        invalidate()
    }

    /**
     * データの表示
     */
    fun drawData(collectUnit: CollectUnit, dataType: String) {
        //  文字の大きさ
        kdraw.mTextSize = 30f
        //  棒の幅
        kdraw.mStrokWidth = when (collectUnit) {
            CollectUnit.Time  -> 2f
            CollectUnit.Day   -> kdraw.cnvWorld2ScreenY(0.8).toFloat()
            CollectUnit.Week  -> kdraw.cnvWorld2ScreenY(5.0).toFloat()
            CollectUnit.Month -> kdraw.cnvWorld2ScreenY(20.0).toFloat()
            else -> 1f
        }
        //  棒の色
        kdraw.mColor = "Gray"
        val xOffset = (mWorldRight - mWorldLeft) * 0.02
        val yOffset = (mWorldBottom - mWorldTop) * 0.003
        //  データの描画
        for (data in mGraphData) {
            val year = klib.date2Year(data.dateTime)
            val weekOffset = klib.date2DayOfWeek(klib.setDate(year, 1, 1)) - 1
            val day = when (collectUnit) {
                CollectUnit.Time  -> klib.date2DayOfYear(data.dateTime)
                CollectUnit.Day   -> data.unitPostion.toDouble() + 0.5
                CollectUnit.Week  -> data.unitPostion * 7.0 - 7.0 - weekOffset + 3.5
                CollectUnit.Month -> klib.date2MonthOfYearDay(data.dateTime) + 15.0
                else -> 0.0
            }
            val value = data.getDataType(dataType)
            kdraw.drawWLine(PointD(0.0, day), PointD(value, day))
            if (collectUnit == CollectUnit.Week || collectUnit == CollectUnit.Month ||
                (mSpanMonth == 3 && collectUnit != CollectUnit.Time) ||mSpanMonth == 1)
                kdraw.drawWText(scaleFormat(value, dataType), PointD(value + xOffset, day - yOffset),
                    0.0, KDraw.HALIGNMENT.Left, KDraw.VALIGNMENT.Center)
            Log.d(TAG,"drawData "+year+" "+data.dateTime+" "+data.unitPostion+" "+day+" "+value)
        }
    }

    fun drawYearData() {
        kdraw.mTextSize = 35f
        kdraw.mColor = "Blue"
        var yearData =
                String.format("距離 %,.0fkm", mYearData.distance) + " " +
                String.format("時間 %,.0fh",mYearData.lapTime/3600) + " " +
                String.format("最大標高 %,.0fm",mYearData.maxElevator) + " " +
                String.format("累積標高差 %,.0fm",mYearData.elevationDiff)
        kdraw.drawText(yearData, PointD(0.0, 0.0),
            0.0, KDraw.HALIGNMENT.Left, KDraw.VALIGNMENT.Top)
        yearData =
                String.format("歩数 %,.0f", mYearData.stepCount.toDouble()) + " " +
                String.format("データ数 %,.0f", mYearData.dataCount.toFloat())
        kdraw.drawText(yearData, PointD(0.0, kdraw.mTextSize * 1.1),
            0.0, KDraw.HALIGNMENT.Left, KDraw.VALIGNMENT.Top)
    }

    /**
     * グラフ枠とメモリの表示
     */
    fun drawAxis(dataType: String) {
        //  グラフの枠
        kdraw.mTextSize = 40f
        kdraw.mStrokWidth = 3f
        kdraw.mColor = "Black"
        kdraw.drawWRect(RectD(kdraw.mWorld))
        //  横補助線(期間)
        for(month in mStartMonth..(mStartMonth + mSpanMonth - 1)) {
            val dateYs = klib.date2DayOfYear(klib.setDate(mYear, month, 1))
            var dateYe = klib.date2DayOfYear(klib.setDate(mYear, month + 1, 1))
            if (month != (mStartMonth + mSpanMonth - 1))
                kdraw.drawWLine(PointD(mWorldLeft, dateYe),PointD(mWorldRight, dateYe))
            if (month == 12) {
                val dateYe = klib.date2DayOfYear(klib.setDate(mYear, month, 31)) + 1
                kdraw.drawWText(String.format("%d月",month), PointD(mWorldLeft,(dateYs+dateYe)/2),
                    0.0,KDraw.HALIGNMENT.Right, KDraw.VALIGNMENT.Center)
            } else {
                kdraw.drawWText(String.format("%d月",month), PointD(mWorldLeft,(dateYs+dateYe)/2),
                    0.0,KDraw.HALIGNMENT.Right, KDraw.VALIGNMENT.Center)
            }
        }
        //  縦補助線
        kdraw.mStrokWidth = 1f
        val stepx = if (dataType == "移動時間") graphTimeStepSize(mWorldRight, 5.0)
                    else klib.graphStepSize(mWorldRight, 5.0)
        var x = 0.0
        while (x < mWorldRight) {
            kdraw.drawWLine(PointD(x, mWorldTop), PointD(x, mWorldBottom))
            kdraw.drawWText(scaleFormat(x,dataType), PointD(x, mWorldBottom),
                    0.0, KDraw.HALIGNMENT.Center, KDraw.VALIGNMENT.Top)
            x += stepx
        }
        //  横軸タイトル
        val offset = kdraw.cnvScreen2WorldY(kdraw.mTextSize.toDouble()) * 1.2
        val ctr = PointD(mWorldLeft, mWorldBottom + offset).center(PointD(mWorldRight, mWorldBottom + offset))
        kdraw.drawWText(dataType+mDataTypeMap[dataType], ctr, 0.0, KDraw.HALIGNMENT.Center, KDraw.VALIGNMENT.Top)
    }

    /**
     * 目盛の数値を文字列に変換
     */
    fun scaleFormat(x: Double, dataType: String): String {
        return if (dataType == "移動時間"){
            klib.Sec2Time(x.toLong()).substring(0, 5)   //  時間(hh:mm)
        } else {
            String.format("%,.1f", x)                        //  時間以外
        }
    }

    /**
     * データが移動時間の時の補助線間隔
     * lap : 時間(sec), targetStep : 最大の補助線数
     */
    fun graphTimeStepSize(lap: Double, targetStep: Double): Double {
        if (lap < 60 * 2) {
            return klib.graphStepSize(lap, targetStep)
        } else if (lap < 3600 * 2) {
            return klib.graphStepSize(lap / 60.0, targetStep) * 60.0
        } else {
            return klib.graphStepSize(lap / 3600.0, targetStep) * 3600.0
        }
    }

    /**
     * グラフエリアの設定
     * year: 対象年, startMonth: 開始月, spanMonth: 期間(月), dataType: データ種別, maxData: 最大値データ
     */
    fun setGraphArea(year: Int, startMonth: Int, spanMonth: Int, dataType:String, maxData: GraphData) {
        mYear = year
        mStartMonth = startMonth
        mSpanMonth  = spanMonth
        //  開始日
        val startDate = klib.setDate(year, startMonth, 1)
        //  終了日
        var lastDate = klib.setDate(year, min(12, startMonth + spanMonth - 1),1)
        val maxDayMonth = klib.date2MaxDayOfMonth(lastDate)
        lastDate = klib.setDate(year, min(12, startMonth + spanMonth - 1),maxDayMonth)
        //  目盛の間隔
        var maxValue = maxData.getDataType(dataType) * 1.2
        maxValue = if (maxValue == 0.0) mWorldRight else maxValue
        val stepSize = klib.graphStepSize( maxValue, 5.0)
        //  グラフ領域
        mWorldLeft   = 0.0
        mWorldRight  = klib.graphHeightSize(maxValue, stepSize)
        mWorldTop    = klib.date2DayOfYear(startDate) - 1
        mWorldBottom = klib.date2DayOfYear(lastDate) + 1
        Log.d(TAG,"setGraphArea "+mWorldLeft+" "+mWorldRight+" "+mWorldTop+" "+mWorldBottom)
    }

    /**
     * グラフ領域に上下左右のマージンを付加してワールド座標を設定
     * top: 上座標, right: 右座標, bottom: 下座標, left: 左座標
     */
    fun setWorldArea(left: Double, top: Double, right: Double, bottom: Double) {
        kdraw.mWorld.left   = left
        kdraw.mWorld.top    = top
        kdraw.mWorld.right  = right
        kdraw.mWorld.bottom = bottom
    }

    /**
     * グラフの領域設定
     */
    fun setViewArea() {
        kdraw.mView.left   = width * mLeftGapRaito
        kdraw.mView.top    = height * mTopGapRaito
        kdraw.mView.right  = width * (1 -  mRightGapRaito)
        kdraw.mView.bottom = height * (1 - mBottomGapRaito)
    }

    /**
     * グラフ領域の初期化
     */
    fun initScreen() {
        kdraw.backColor(Color.WHITE)
        //  スクリーンサイズの設定
        kdraw.setInitScreen(width, height)
        //  倒立表示
        kdraw.mScreenInverted = false
        //  文字サイズと文字太さを初期設定
        kdraw.mTextSize = mFontSize.toFloat()
        kdraw.mTextStrokeWidth = 2f
    }

}