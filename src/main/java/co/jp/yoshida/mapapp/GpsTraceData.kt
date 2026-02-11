package co.jp.yoshida.mapapp

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.location.Location
import android.location.LocationManager
import java.util.Date
import kotlin.math.max
import kotlin.math.min


/**
 * ====  GPSデータ情報クラス  =====
 */
class GpsTraceData() {
    val TAG = "GpsTraceData"

    var mGpsTraceData = mutableListOf<List<String>>()       //  GpsTraceData
    var mLocData = mutableListOf<PointD>()  //  位置座標データ
    var mLocationData = mutableListOf<Location>()   //  Locationデータ
    var mStepCountList = mutableListOf<Int>()   //  歩数データ
    var mTitle = ""                         //  タイトル
    var mGroup = ""                         //  グループ名
    var mCategory = ""                      //  分類
    var mComment = ""                       //  コメント
    var mFilePath = ""                      //  gpxファイルパス
    var mVisible = false                    //  表示の可否
    var mLineColor = "Green"                //  表示線分の色
    var mThickness = 4f;                    //  表示線分の太さ
    var mLocArea = RectD()                  //  位置領域(緯度経度座標)
    var mDistance = 0.0                     //  移動距離(km)
    var mMinElevation = 0.0                 //  最小標高(m)
    var mMaxElevation = 0.0                 //  最高標高(m)
    var mFirstTime = Date()                 //  開始時間
    var mLastTime = Date()                  //  終了時間
    var mStepCount = 0                      //  歩数
    var mGpsDataSize = 0                    //  GPSデータサイズ

    val klib = KLib()

    companion object {
        //  GPSファイルリストのタイトル
        var mDataFormat = listOf<String>(
            "Title", "Group", "Category", "Comment", "FilePath", "Visible", "Color", "Thickness",
            "Left", "Top", "Right", "Bottom", "Distance", "MinElevator", "MaxElevator",
            "FirstTime", "LastTime", "StepCount", "DataSize"
        )
        //  GPSデータファイル(csv)のタイトル
        var mGpsFormat = listOf<String>(
            "DateTime","Time","Latitude","Longitude","Altitude","Speed","Bearing","Accuracy","StepCount"
        )
        //  旧タイトル(Longitudeのタイトル名が間違っていた)
        var mGpsFormat2 = listOf<String>(
            "DateTime","Time","Latitude","Longtude","Altitude","Speed","Bearing","Accuracy","StepCount"
        )
    }

    /**
     * コンストラクタ GpsTraceDataのコピー
     * gpsTraceData     GpsTracedata
     */
    constructor(gpsTraceData: GpsTraceData): this() {
        mTitle        = gpsTraceData.mTitle
        mGroup        = gpsTraceData.mGroup
        mCategory     = gpsTraceData.mCategory
        mComment      = gpsTraceData.mComment
        mFilePath     = gpsTraceData.mFilePath
        mVisible      = gpsTraceData.mVisible
        mLineColor    = gpsTraceData.mLineColor
        mThickness    = gpsTraceData.mThickness
        mLocArea      = gpsTraceData.mLocArea
        mDistance     = gpsTraceData.mDistance
        mMinElevation = gpsTraceData.mMinElevation
        mMaxElevation = gpsTraceData.mMaxElevation
        mFirstTime    = gpsTraceData.mFirstTime
        mLastTime     = gpsTraceData.mLastTime
        mStepCount    = gpsTraceData.mStepCount
        mGpsDataSize  = gpsTraceData.mGpsDataSize
    }

    /**
     * データの開始日時の年を取出す(xxxx年)
     * return           xxxx年
     */
    fun getYearStr(): String {
        val tz = Date().getTimezoneOffset() / 60 + 9    //  タイムゾーン(時)
        return klib.date2String(mFirstTime, "yyyy年", tz)
    }

    /**
     * データの開始日時の月を取出す(xx月)
     * return           xx月
     */
    fun getMonthStr(): String {
        val tz = Date().getTimezoneOffset() / 60 + 9    //  タイムゾーン(時)
        return klib.date2String(mFirstTime, "M月", tz)
    }

    /**
     * 開始時間を文字列で取得
     * return       開始時間の文字列
     */
    fun getFirstTimeStr(): String {
        val tz = Date().getTimezoneOffset() / 60 + 9    //  タイムゾーン(時)
        return klib.date2String(mFirstTime, "yyyy/MM/dd HH:mm:ss", tz)
    }

    /**
     * 経過時間(sec)
     */
    fun getLapTime(): Double {
        return (mLastTime.time - mFirstTime.time) / 1000.0
    }

    /**
     * 平均速度の取得(km/h)
     * return       速度(km/h)
     */
    fun getSpeed():Double {
        return mDistance/(mLastTime.time - mFirstTime.time)*60*60*1000
    }

    /**
     * 一覧リスト用タイトル
     */
    fun getListTitle(titleType: Int = 0, pathOffset: Int = 0): String {
        var title = if (mVisible) "*" else " "
        title += getFirstTimeStr() + " "
        title += mTitle +"\n"
        title += "[" + mCategory + "]"
        title += "[" + mGroup + "] "
        if (titleType== 1) {
            title += mFilePath.substring(pathOffset)
        } else {
            title += "%.2f km".format(mDistance)
            title += "(" + klib.lap2String(mLastTime.time - mFirstTime.time) + ") "
            title += "%.1f km/h".format(mDistance/(mLastTime.time - mFirstTime.time)*60*60*1000) + " "
            title += "%.0f m".format(mMinElevation) + "-" + "%.0f m".format(mMaxElevation)
        }
        return title
    }

    /**
     * GPSデータの情報を文字列化
     */
    fun getInfoData(): String {
        var buffer = ""
        val tz = Date().getTimezoneOffset() / 60 + 9
        val lap = (mLastTime.time - mFirstTime.time).toDouble() / 1000.0
        buffer += "開始時間 " + klib.date2String( mFirstTime, "yyyy/MM/dd HH:mm:ss", tz)
        buffer += "\n終了時間 " + klib.date2String( mLastTime, "yyyy/MM/dd HH:mm:ss", tz)
        buffer += "\n経過時間 " + klib.lap2String(mLastTime.time - mFirstTime.time)
        buffer += "\n移動距離 " + "%.2f km  ".format(mDistance)
        buffer += "速度　%.1f km/h  ".format(mDistance/(mLastTime.time - mFirstTime.time)*60*60*1000)
        buffer += "歩数 " + mStepCount
        buffer += "\n最大標高 %.0f m".format(mMaxElevation) + " 最小標高 %.0f m".format(mMinElevation)
        if (0 < mGpsDataSize)
            buffer += "\nデータ数 " + mGpsDataSize + "  平均測定間隔 " + String.format("%.1f sec", lap / mGpsDataSize)
        return buffer
    }

    /**
     * 一覧リストのリストデータからデータを取得する
     */
    fun getStringData(data: List<String>) {
        mLocData.clear()
        mStepCountList.clear()
        mTitle          = klib.revControlCode(data[0])
        mGroup          = klib.revControlCode(data[1])
        mCategory       = data[2]
        mComment        = klib.revControlCode(data[3])
        mFilePath       = data[4]
        mVisible        = data[5].toBoolean()
        mLineColor      = data[6]
        mThickness      = data[7].toFloat()
        mLocArea.left   = data[8].toDouble()
        mLocArea.top    = data[9].toDouble()
        mLocArea.right  = data[10].toDouble()
        mLocArea.bottom = data[11].toDouble()
        mDistance       = data[12].toDouble()
        mMinElevation   = data[13].toDouble()
        mMaxElevation   = data[14].toDouble()
        mFirstTime      = Date(data[15].toLong())
        mLastTime       = Date(data[16].toLong())
        mStepCount      = data[17].toInt()
        mGpsDataSize    = data[18].toInt()
    }

    /**
     * 一覧リストのリストデータにデータを設定する
     */
    fun setStringData(): List<String> {
        val data = mutableListOf<String>()
        data.add(klib.cnvControlCode(mTitle))
        data.add(klib.cnvControlCode(mGroup))
        data.add(mCategory)
        data.add(klib.cnvControlCode(mComment))
        data.add(mFilePath)
        data.add(mVisible.toString())
        data.add(mLineColor)
        data.add(mThickness.toString())
        data.add(mLocArea.left.toString())
        data.add(mLocArea.top.toString())
        data.add(mLocArea.right.toString())
        data.add(mLocArea.bottom.toString())
        data.add(mDistance.toString())
        data.add(mMinElevation.toString())
        data.add(mMaxElevation.toString())
        data.add(mFirstTime.time.toString())
        data.add(mLastTime.time.toString())
        data.add(mStepCount.toString())
        data.add(mGpsDataSize.toString())
        return data
    }

    /**
     *  GPS位置情報をトレースす表示する
     *  canvas      描画canvas
     *  mapData     地図座標データ
     */
    fun draw(canvas: Canvas, mapData: MapData) {
        if (1 < mLocData.size) {
            var paint = Paint()
            paint.color = if (klib.mColorMap[mLineColor] == null) Color.BLACK else klib.mColorMap[mLineColor]!!
            paint.strokeWidth = mThickness

            var sbp = mLocData[0]
            var sp = mapData.baseMap2Screen(klib.coordinates2BaseMap(sbp))
            for (i in 1..mLocData.lastIndex) {
                var ebp = mLocData[i]
                var ep = mapData.baseMap2Screen(klib.coordinates2BaseMap(ebp))
                canvas.drawLine(sp.x.toFloat(), sp.y.toFloat(), ep.x.toFloat(), ep.y.toFloat(), paint)
                sp = ep
            }
        }
    }

    /**
     * GpsTraceDataをString形式で読み込む
     */
    fun loadGpsTraceData() {
        mGpsTraceData.clear()
        appendGpsTraceData(mFilePath)
    }

    /**
     * String形式のGpsTraceDataをクリア
     */
    fun clearGpsTraceData() {
        mGpsTraceData.clear()
    }

    /**
     * GpsTraceDataをString形式で追加読み込む
     * filePath     データファイルパス
     */
    fun appendGpsTraceData(filePath: String) {
        if (klib.existsFile(filePath)) {
            if (klib.getNameExt(filePath).compareTo("csv", true) == 0) {
                loadCsvTraceData(filePath)
            } else if (klib.getNameExt(filePath).compareTo("gpx", true) == 0) {
                loadGpxTraceData(filePath)
            }
        }
    }

    /**
     * GpsTraceDataをString形式でCSV保存
     * filePath     データファイルパス
     */
    fun saveCsvTraceData(filePath: String) {
        klib.saveCsvData(filePath, mGpsFormat, mGpsTraceData)
    }

    /**
     * CSV形式のGpsTraceDataをString形式で読み込む
     * filePath     データファイルパス
     */
    fun loadCsvTraceData(filePath: String) {
        var gpsTraceData = klib.loadCsvData(filePath, mGpsFormat)
        if (0 < gpsTraceData.size) {
            //  旧データ(タイトルミス)?
            if (gpsTraceData[0].size == mGpsFormat2.size || gpsTraceData[0][3].isEmpty())
                gpsTraceData = klib.loadCsvData(filePath, mGpsFormat2)
        }
        for (data in gpsTraceData){
            mGpsTraceData.add(data)
        }
    }

    /**
     * GPXファイルをString形式のGpsTraceDataに変換して取り込む
     * filePath     GPXファイルパス
     */
    fun loadGpxTraceData(filePath: String) {
        var gpsReader = GpxReader(GpxReader.DATATYPE.gpxData)
        if (0 < gpsReader.getGpxRead(filePath)) {
            for (gpxData in gpsReader.mListGpsData) {
                //  "DateTime","Time","Latitude","Longitude","Altitude","Speed","Bearing","Accuracy","StepCount"
                var gpsData = mutableListOf<String>()
                gpsData.add(gpxData.mDate.toString())
                gpsData.add(gpxData.mLap.toString())
                gpsData.add(gpxData.mLatitude.toString())
                gpsData.add(gpxData.mLongitude.toString())
                gpsData.add(gpxData.mElevator.toString())
                gpsData.add(gpxData.mSpeed.toString())
                gpsData.add("")     //  Bearing(方位)
                gpsData.add("")     //  Accuracy (精度)
                gpsData.add("")     //  StepCount (歩数)
                mGpsTraceData.add(gpsData)
            }
        }
    }

    /**
     * GPSファイルの読込と情報設定
     * locsave      位置データを保存する
     * locatioSave  Locationデータを取得する
     */
    fun loadGpsData(locsave: Boolean = true, locationSave: Boolean = false) {
        if (klib.existsFile(mFilePath)) {
            if (klib.getNameExt(mFilePath).compareTo("csv", true) == 0) {
                loadCsvData(locsave, locationSave)
            } else if (klib.getNameExt(mFilePath).compareTo("gpx", true) == 0) {
                loadGpxData()
            }
        }
    }

    /**
     * GPXファイルデータの読込と情報設定
     */
    fun loadGpxData(){
        //  gpxファイルからGPSデータの取得
        var gpsReader = GpxReader(GpxReader.DATATYPE.gpxData)
        if (mTitle.length == 0)
            mTitle = klib.getFileNameWithoutExtension(mFilePath)
        if (0 < gpsReader.getGpxRead(mFilePath)) {
            //  GPSデータから位置リストを取得
            gpsReader.setGpsInfoData()
            mLocData      = gpsReader.mListGpsPointData
            mLocArea      = gpsReader.mGpsInfoData.mArea
            mDistance     = gpsReader.mGpsInfoData.mDistance
            mMinElevation = gpsReader.mGpsInfoData.mMinElevator
            mMaxElevation = gpsReader.mGpsInfoData.mMaxElevator
            mFirstTime    = Date(gpsReader.mGpsInfoData.mFirstTime.time)
            mLastTime     = Date(gpsReader.mGpsInfoData.mLastTime.time)
            mGpsDataSize  = gpsReader.mListGpsPointData.size
        }
        val lap = mLastTime.time - mFirstTime.time
        mCategory = data2Category(lap, mDistance, 0, mMaxElevation - mMinElevation)
    }

    /**
     * GPS記録データの読込(GPS Serviceで出力されたCSVファイルの読込)、Locationデータとして取り込む
     * locsave      位置データリストにも保存する
     * locatioSave  Locationデータを出得する
     */
    fun loadCsvData(locsave: Boolean = true, locationSave: Boolean = false) {
        mLocData.clear()
        mLocationData.clear()
        mStepCountList.clear()
        var listData = klib.loadCsvData(mFilePath, mGpsFormat)
        if (0 < listData.size) {
            //  旧データ(タイトルミス)?
            if (listData[0].size != mGpsFormat.size || listData[0][3].isEmpty())
                listData = klib.loadCsvData(mFilePath, mGpsFormat2)
        }
        mTitle = klib.getFileNameWithoutExtension(mFilePath)
        mFirstTime = Date(listData[0][1].toLong())
        mLastTime = Date(listData[listData.lastIndex][1].toLong())
        mStepCount = klib.str2Integer(listData[listData.lastIndex][8]) - klib.str2Integer(listData[0][8])
        mGpsDataSize = listData.size
        mDistance = 0.0
        mMinElevation = Double.MAX_VALUE
        mMaxElevation = Double.MIN_VALUE
        mLocArea.setInitExtension()
        var preLoc = PointD()
        for (data in listData) {
            if (data[0].compareTo("DateTime") != 0) {
                var location = Location(LocationManager.GPS_PROVIDER)
                location.time      = data[1].toLong()       //  Time      時間(ms)
                location.latitude  = data[2].toDouble()     //  Latitude  緯度
                location.longitude = data[3].toDouble()     //  Longitude 経度
                location.altitude  = data[4].toDouble()     //  Altitude  高度(m)
                location.speed     = data[5].toFloat()      //  Speed     速度(m/s)
                location.bearing   = data[6].toFloat()      //  Bearing   方位(度)
                location.accuracy  = data[7].toFloat()      //  Accuracy  精度(半径 m)
                val loc = PointD(location.longitude, location.latitude)
                if (!preLoc.isEmpty())
                    mDistance += klib.cordinateDistance(preLoc, loc)
                preLoc = loc
                //  座標データの保存
                if (locsave)
                    mLocData.add(loc)
                //  Locationデータの保存
                if (locationSave)
                    mLocationData.add(location)
                mLocArea.extension(loc)
                if (8 < data.size)
                    mStepCountList.add(klib.str2Integer(data[8]))         //  StepCount 歩数
                else
                    mStepCountList.add(0)
                mMinElevation = min(mMinElevation, location.altitude)
                mMaxElevation = max(mMaxElevation, location.altitude)
            }
        }
        val lap = mLastTime.time - mFirstTime.time
        mCategory = data2Category(lap, mDistance, mStepCount, mMaxElevation - mMinElevation)
    }

    /**
     * 経過時間、距離、歩数、標高差から分類を求める
     * lap              経過時間(ms)
     * distance         距離(km)
     * stepCount        歩数
     * elevator         標高差(m)
     * return           分類名
     */
    fun data2Category(lap: Long, distance: Double, stepCount: Int, elevator: Double): String {
        val speed = distance / (lap.toDouble() / 3600.0 / 1000.0)   //  速度(km/h)
        val stepDis = if (0 < stepCount) distance * 1000.0 / stepCount else -1.0  //  歩幅(m)
        if (stepDis < 10.0) {
            if (speed < 6.0) {                      //  速度6km/h以下
                return if (elevator < 300.0)        //  標高差 300m以下
                    "散歩" else "山歩き"
            } else if (speed < 12.0) {              //  速度 6-12のもめく
                return "ジョギング"
            } else if (speed < 30.0) {              //  速度 12-30km/h
                return "ランニング"
            } else {
                return "サイクリング"
            }
        } else {
            if (speed < 40.0) {                     //  速度 12-30km/h
                return "サイクリング"
            } else if (speed < 200.0)  {
                return "車・バス・鉄道"
            } else {
                return "飛行機"
            }
        }
        return "散歩"
    }

    /**
     * GPSデータをGPXファイルに変換する
     * exportPath       出力先フォルダ
     */
    fun gpxExport(exportPath: String) {
        if (0 < mLocationData.size) {
            if (klib.getNameExt(mFilePath).compareTo("gpx", true) == 0) {
                klib.copyfile(mFilePath, exportPath)
            } else {
                var gpxWriter = GpxWriter()
                gpxWriter.mGpxHeaderCreater = "MapApp GPS Logger for Android"
                gpxWriter.writeDataAll(exportPath, mLocationData)
            }
        }
    }
}
