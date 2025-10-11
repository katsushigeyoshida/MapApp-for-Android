package co.jp.yoshida.mapapp

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Job
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.max


/**
 *  GPSでの取得位置情報をCSV形式でデータ保存/読込、トレースを地図上に表示する
 */
class GpsTrace {
    val TAG = "GpsTrace"
    var mTraceOn = false                            //  GPSトレース中のフラグ
    var mGpxConvertOn = false                       //  GPXデータ変換フラグ
    lateinit var mGpxConvertJob: Job                //  GPXファイル変換Job
    var mGpsTraceFileFolder = ""                    //  GPSトレースデータを保存するフォルダ
    var mGpsPath = ""                               //  GPSトレースデータファイルパス
    var mGpsData = mutableListOf<Location>()        //  GPSトレースのデータリスト(フルデータ)
    var mGpsPointData = mutableListOf<PointD>()     //  GPSトレースのデータリスト(座標のみの簡易形式)
    var mGpsLastElevator = 0.0                      //  GPSトレースの標高最新値
    var mStepCount = mutableListOf<Int>()           //  歩数のレスとデータ
    var mGpsLap = mutableListOf<Long>()             //  GPS経過時間
    var mLineColor = Color.GREEN                    //  トレース中の線の色

    lateinit var mC: Context
    val klib = KLib()

    /**
     * データの初期化
     * c            コンテキスト
     * filefolder   トレースデータ保存先フォルダ
     * gpsPath      GPSトレース時のファイル名
     */
    fun init(c: Context, filefolder: String, gpsPath: String) {
        mC = c
        mGpsTraceFileFolder = filefolder
        mGpsPath = gpsPath
        //  トレースが継続中でなければトレースファイルを削除
        if (!klib.getBoolPreferences("GpsTraceContinue", mC)) {
            Log.d(TAG, "init: remove: " + mGpsPath)
            removeGpxFile(mGpsPath)
        }
    }

    /**
     * GPSトレース開始
     * count        継続保存(前回の値に追加)
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun start(cont: Boolean = false) {
        Log.d(TAG,"start:" + mGpsData.size )
        if (!cont) {
            removeGpxFile(mGpsPath)
            mGpsData.clear()
        }
        mTraceOn = true
        klib.setBoolPreferences(true, "GpsTraceContinue", mC)
        val ldt = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("YYYYMMdd_HHmmss")
        klib.setStrPreferences(ldt.format(formatter), "GpsTraceStartTime", mC)
    }

    /**
     * GPSトレース終了
     */
    fun end() {
        Log.d(TAG,"end:" + mGpsData.size + " " + mGpsPointData.size)
        mTraceOn = false
        klib.setBoolPreferences(false, "GpsTraceContinue", mC)
    }

    /**
     *  GPS位置情報をトレースす表示する
     *  canvas      描画キャンバス
     *  mapData     地図データクラス
     */
    fun draw(canvas: Canvas, mapData: MapData) {
        if (mTraceOn) {
            //  測定中のGPSデータの表示
            draw(canvas, mGpsPointData, mLineColor, mapData)
        }
    }

    /**
     * GPS位置情報トレースを地図上に表示(PointDデータ)
     * canvas           地図キャンバス
     * traceData        GPSトレースの位置情報リスト
     * color            線分の色
     * mapData          地図のMapData
     */
    fun draw(canvas: Canvas, traceData: List<PointD>, color: Int, mapData: MapData) {
        if (1 < traceData.size) {
            var paint = Paint()
            paint.color = color
            paint.strokeWidth = 6f

            var sbp = traceData[0]
            var sp = mapData.baseMap2Screen(klib.coordinates2BaseMap(sbp))
            for (i in 1..traceData.size - 1) {
                var ebp = traceData[i]
                var ep = mapData.baseMap2Screen(klib.coordinates2BaseMap(ebp))
                canvas.drawLine(sp.x.toFloat(), sp.y.toFloat(), ep.x.toFloat(), ep.y.toFloat(), paint)
                sp = ep
            }
        }
    }

    /**
     * GPS記録ファイルを削除
     * path     ファイル名(デフォルト:mGpxPath)
     */
    fun removeGpxFile(path: String = mGpsPath) {
        klib.removeFile(path)
    }

    /**
     * GPS記録ファイルに日時ファイル名を付けて指定フォルダに移動
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun moveGpsFile(moveFolder: String, orgPath: String = mGpsPath) {
        if (klib.existsFile(orgPath)) {
            val traceStarTime = klib.getStrPreferences("GpsTraceStartTime", mC)
            val destFolder = moveFolder + "/" + klib.getNowDate("YYYY")
            if (klib.mkdir(destFolder)) {
                var destPath = destFolder + "/" + "GPS_" + traceStarTime + ".csv"
                if (!klib.renameFile(orgPath, destPath)) {
                    Toast.makeText(mC, "ファイル保存エラー", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * GPS記録データの読込(GPS Serviceで出力されたCSVファイルの読込)、Locationデータとして取り込む
     * path     ファイル名(デフォルト:mGpxPath)
     */
    fun loadGpsData(path: String = mGpsPath): List<Location> {
        mGpsData.clear()
        mStepCount.clear()
        var listData = klib.loadCsvData(path)
        for (data in listData) {
            if (data[0].compareTo("DateTime") != 0) {
                var location = Location(LocationManager.GPS_PROVIDER)
                location.time      = data[1].toLong()       //  時間(ms)
                location.latitude  = data[2].toDouble()     //  緯度
                location.longitude = data[3].toDouble()     //  経度
                location.altitude  = data[4].toDouble()     //  高度(m)
                location.speed     = data[5].toFloat()      //  速度(m/s)
                location.bearing   = data[6].toFloat()      //  方位(度)
                location.accuracy  = data[7].toFloat()      //  精度(半径 m)
                mGpsData.add(location)
                if (8 < data.size)
                    mStepCount.add(data[8].toInt())         //  歩数
                else
                    mStepCount.add(0)
            }
        }
        return mGpsData
    }

    /**
     * GPS記録データを読込、座標データ(PointD))のみ取得
     * path     GPSデータのCSVファイル名
     */
    fun loadGpsPointData(path: String = mGpsPath) {
        mGpsPointData.clear()
        mGpsLap.clear()
        mStepCount.clear()
        var listData = klib.loadCsvData(path)
        for (data in listData) {
            if (data[0].compareTo("DateTime") != 0) {
                var location = PointD(data[3].toDouble(), data[2].toDouble())   //  経度,緯度
                mGpsPointData.add(location)
                mGpsLap.add(data[1].toLong())
                if (8 < data.size)
                    mStepCount.add(data[8].toInt())         //  歩数
                else
                    mStepCount.add(0)
            }
        }
        if (0 < listData.size) {
            mGpsLastElevator = listData.last()[4].toDouble()
        }
    }

    /**
     * CSV形式の位置情報ファイルの読込Locationデータに変換
     * path     CSVファイルパス
     * return   位置情報リスト(List<Location>)
     */
    fun loadGpxData(path: String): List<Location> {
        var gpxData = mutableListOf<Location>()
        var listData = klib.loadCsvData(path)
        for (data in listData) {
            if (data[0].compareTo("DateTime") != 0) {
                var location = Location(LocationManager.GPS_PROVIDER)
                location.time      = data[1].toLong()       //  時間(ms)
                location.latitude  = data[2].toDouble()     //  緯度
                location.longitude = data[3].toDouble()     //  経度
                location.altitude  = data[4].toDouble()     //  高度(m)
                location.speed     = data[5].toFloat()      //  速度(m/s)
                location.bearing   = data[6].toFloat()      //  方位(度)
                location.accuracy  = data[7].toFloat()      //  精度(半径 m)
                gpxData.add(location)
            }
        }
        return gpxData
    }

    /**
     * CSV形式の位置情報ファイルの読込PointDデータに変換
     * path     CSVファイルパス
     * return   位置情報リスト(List<PointD>)
     */
    fun loadGpxPointData(path: String = mGpsPath): List<PointD> {
        var gpxData = mutableListOf<PointD>()
        var listData = klib.loadCsvData(path)
        for (data in listData) {
            if (data[0].compareTo("DateTime") != 0) {
                var location = PointD(data[3].toDouble(), data[2].toDouble())   //  経度,緯度
                gpxData.add(location)
            }
        }
        return gpxData;
    }

    /**
     * GPSの最新の位置を取得
     */
    fun lastPosition(): PointD {
        Log.d(TAG,"lastPosition: "+mGpsPointData.size)
        if (0 < mGpsPointData.size)
            return mGpsPointData.last()
        else if (0 < mGpsData.size)
            return PointD(mGpsData[mGpsData.size - 1].longitude, mGpsData[mGpsData.size - 1].latitude)
        else
            return PointD()
    }

    /**
     * 経過時間(sec)
     */
    fun lastLap(): Double {
        return (mGpsLap[mGpsPointData.lastIndex] - mGpsLap[0]) / 1000.0
    }

    /**
     * 最新速度(km/h)
     * aveSize      移動平均のデータサイズ(1以上)
     * return       速度(km/h)
     */
    fun lastSpeed(aveSize: Int = 1): Double {
        val last = mGpsPointData.lastIndex
        if (1 < last) {
            val st = max(last - aveSize + 1, 2)
            var sum = 0.0
            for (i in st..last) {
                val distance = klib.cordinateDistance(mGpsPointData[i - 1], mGpsPointData[i])   //  (km)
                val lap = (mGpsLap[i] - mGpsLap[i - 1]) / 1000.0 / 3600.0                       //  (h)
                sum += if (lap <= 0) 0.0 else distance / lap
            }
            return sum / (last - st + 1)
        } else {
            return 0.0
        }
    }

    /**
     * 歩数(step count)
     */
    fun stepCount(): Int {
        return mStepCount[mStepCount.size - 1] -  mStepCount[0]
    }

    /**
     * 累積距離(km)
     */
    fun totalDistance(): Double {
        var distance = 0.0
        if (0 < mGpsPointData.size) {
            for (i in 1..mGpsPointData.lastIndex) {
                distance += klib.cordinateDistance(mGpsPointData[i - 1], mGpsPointData[i])
            }
        } else if (0 < mGpsData.size) {
            for (i in 1..mGpsData.lastIndex) {
                distance += klib.cordinateDistance(mGpsData[i - 1].longitude, mGpsData[i - 1].latitude,
                    mGpsData[i].longitude, mGpsData[i].latitude)
            }
        }
        return distance
    }

    /**
     * 最大高度(m)
     */
    fun maxElevator(): Double {
        var maxEle = mGpsData[0].altitude
        for (i in 1..(mGpsData.size - 1)) {
            maxEle = Math.max(maxEle, mGpsData[i].altitude)
        }
        return maxEle
    }

    /**
     * 最小高度(m)
     */
    fun minElevator(): Double {
        var minEle = mGpsData[0].altitude
        for (i in 1..(mGpsData.size - 1)) {
            minEle = Math.min(minEle, mGpsData[i].altitude)
        }
        return minEle
    }

}