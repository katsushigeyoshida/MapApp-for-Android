package co.jp.yoshida.mapapp

import java.util.Calendar
import java.util.Date
import kotlin.math.max
import kotlin.math.min

/**
 * ====  グラフ作成用データ  ====
 */
class GpsTraceListGraphData() {
    var collectUnit = CollectUnit.Time      //  集計単位
    var unitPostion = 0                     //  集計単位ごとの位置(回の時は常に秒)
    var category = ""                       //  分類(散歩、ウォーキング、ジョギング・・・)
    var dateTime = Date()                   //  開始日
    var distance = 0.0                      //  移動距離(km)
    var lapTime = 0.0                       //  移動時間(sec)
    var speed = 0.0                         //  速度(km/h)
    var maxElevator = 0.0                   //  最大高度(m)
    var minElevator = 0.0                   //  最小高度(m)
    var elevationDiff = 0.0                 //  標高差
    var stepCount = 0                       //  歩数
    var dataCount = 0                       //  累積データ数
    var caloriesBurned = 0.0                //  消費カロリー

    var wait = 70.0                         //  体重(kg)
    //　メッツ値 消費カロリー(kcal) = メッツ値 x 体重(kg) x 活動実施時間(hour)
    val mets = mapOf<String, Double>(
        "散歩" to 3.0,
        "ウォーキング" to 5.0,
        "ジョギング" to 7.0,
        "ランニング" to 9.0,
        "山歩き" to 6.0,
        "サイクリング" to 8.0,
        "自転車" to 4.0,
        "車・バス・鉄道" to 0.0,
        "飛行機" to 0.0,
        "旅行" to 0.0,
    )

    val klib = KLib()

    /**
     * GpsTraceDataからGraphDataに変換
     * data : GpsTraceData
     * collectUnit : 集計単位(回,日,主,月)
     */
    fun setData(data: GpsTraceData, collectUnit: CollectUnit = CollectUnit.Time) {
        category = data.mCategory
        dateTime = data.mFirstTime
        distance = data.mDistance
        lapTime  = data.getLapTime()
        speed    = distance / lapTime * 3600
        maxElevator = data.mMaxElevation
        minElevator = data.mMinElevation
        elevationDiff = maxElevator - minElevator
        stepCount   = data.mStepCount
        dataCount = 1
        caloriesBurned = getColoriesBurned(category, wait, lapTime)
        this.collectUnit = collectUnit
        unitPostion = collectUnitPosition(dateTime, collectUnit)
    }

    /**
     * GraphDataをコピー
     */
    fun setData(data: GpsTraceListGraphData, collectUnit: CollectUnit = CollectUnit.Time) {
        category = data.category
        dateTime = data.dateTime
        distance = data.distance
        lapTime  = data.lapTime
        speed    = distance / lapTime * 3600
        maxElevator = data.maxElevator
        minElevator = data.minElevator
        elevationDiff = data.elevationDiff
        stepCount   = data.stepCount
        dataCount = 1
        caloriesBurned = getColoriesBurned(category, wait, lapTime)
        this.collectUnit = collectUnit
        unitPostion = collectUnitPosition(dateTime, collectUnit)
    }

    /**
     * 累積データの加算
     * data : GpsTraceData
     * collectUnit : 集計単位(回,日,主,月)
     */
    fun addData(data: GpsTraceData, collectUnit: CollectUnit = CollectUnit.Time) {
        category = data.mCategory
        dateTime = data.mFirstTime
        distance += data.mDistance
        lapTime  += data.getLapTime()
        speed    = distance / lapTime * 3600
        maxElevator = max(maxElevator, data.mMaxElevation)
        minElevator = min(minElevator, data.mMinElevation)
        elevationDiff += data.mMaxElevation - minElevator
        stepCount += data.mStepCount
        caloriesBurned += getColoriesBurned(data.mCategory, wait, data.getLapTime())
        dataCount++
    }
    /**
     * 累積データの加算
     * data : GraphData
     * collectUnit : 集計単位(回,日,主,月)
     */
    fun addData(data: GpsTraceListGraphData, collectUnit: CollectUnit) {
        if (this.collectUnit != collectUnit) return
        category = data.category
        dateTime = data.dateTime
        distance += data.distance
        lapTime  += data.lapTime
        speed    = distance / lapTime * 3600
        maxElevator = max(maxElevator, data.maxElevator)
        minElevator = min(minElevator, data.minElevator)
        elevationDiff += data.elevationDiff
        stepCount += data.stepCount
        caloriesBurned += getColoriesBurned(data.category, wait, data.lapTime)
        dataCount++
    }

    /**
     * 最大値を求める(distance,lapTime,maxElevator,minElevator(min),stepCount)
     */
    fun maxData(data: GpsTraceListGraphData) {
        distance    = max(distance, data.distance)
        lapTime     = max(lapTime, data.lapTime)
        speed       = max(speed, data.speed)
        maxElevator = max(maxElevator, data.maxElevator)
        minElevator = min(minElevator, data.minElevator)
        elevationDiff = max(elevationDiff, data.elevationDiff)
        stepCount   = max(stepCount,data.stepCount)
        caloriesBurned = max(caloriesBurned, data.caloriesBurned)
        dataCount++
    }

    /**
     * 最省値を求める(distance,lapTime,maxElevator,minElevator(max),stepCount)
     */
    fun minData(data: GpsTraceListGraphData) {
        distance    = min(distance, data.distance)
        lapTime     = min(lapTime, data.lapTime)
        speed       = min(speed, data.speed)
        maxElevator = min(maxElevator, data.maxElevator)
        minElevator = max(minElevator, data.minElevator)
        elevationDiff = min(elevationDiff, data.elevationDiff)
        stepCount   = min(stepCount,data.stepCount)
        caloriesBurned = min(caloriesBurned, data.caloriesBurned)
        dataCount++
    }

    /**
     * 集計単位別に開始日を位置に変換
     * 回:常に0, 日:年の日(1-365),週:年の週(1-52),月:月(1-12)
     */
    fun collectUnitPosition(date: Date, collectUnit: CollectUnit): Int {
        val cl = Calendar.getInstance()
        cl.setTime(date)
        return when (collectUnit) {
            CollectUnit.Time  -> cl.get(Calendar.SECOND) + cl.get(Calendar.MINUTE) * 60 +
                    cl.get(Calendar.HOUR_OF_DAY) * 3600 + cl.get(Calendar.DAY_OF_YEAR) * 24 * 3600
            CollectUnit.Day   -> cl.get(Calendar.DAY_OF_YEAR)
            CollectUnit.Week  -> klib.getWeekOfYear(date)
            CollectUnit.Month -> cl.get(Calendar.MONTH)+1
            else -> 0
        }
    }

    /**
     * データの種別名から値を選択
     */
    fun getDataType(type: String): Double {
        return when (type) {
            "移動距離"    -> distance
            "移動時間"    -> lapTime
            "速度"       -> speed
            "最大高度"    -> maxElevator
            "累積標高差"   -> elevationDiff
            "歩数"        -> stepCount.toDouble()
            "消費カロリー" -> caloriesBurned
            else         -> 0.0
        }
    }

    /**
     * 消費カロリー(kcol)
     * category: 分類, wait: 体重(kg), lapTime: 経過時間(sec)
     */
    fun getColoriesBurned(category: String, wait: Double, lapTime: Double): Double {
        return (mets[category]?:0.0) * wait * lapTime / 3600.0
    }

    override fun toString(): String {
        return "${distance}, ${lapTime}, ${maxElevator}, ${minElevator}, ${elevationDiff} ${stepCount}, ${dataCount} ${unitPostion}"
    }
}
