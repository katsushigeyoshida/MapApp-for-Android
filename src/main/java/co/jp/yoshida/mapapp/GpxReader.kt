package co.jp.yoshida.mapapp

import android.location.Location
import android.util.Log
import java.io.File
import java.util.Date

/**
 * GPXファイルからGPSデータを取得
 */
class GpxReader(var mDataType: DATATYPE = DATATYPE.gpxData) {
    val TAG = "GpxReader"
    enum class DATATYPE {gpxData, gpxSimleData}         //  gpxSimpleDataは座標データのみ)
    var mListData = mutableListOf<String>()             //  GPXのリストデータ
    var mListGpsData = mutableListOf<GpsData>()         //  GPSのリストデータ(gpxData)
    var mListGpsPointData = mutableListOf<PointD>()     //  GPSの緯度経度座標データリスト(gpxSimpleData)
    var mGpsInfoData = GpsInfoData()                    //  GPSデータの情報

    val klib = KLib()

    /**
     * GPXファイルからGPSデータの取得
     */
    fun getGpxRead(path: String):Int {
        var buffer = loadData(path)         //  ファイルデータの読込
        if (0 < buffer.length) {
            setListData(buffer.toString())  //  データのリスト化
            if (0 < mListData.size) {
                getGpxData()                //  リストデータからGPSデータを取得
                return mListGpsPointData.size
            }
        }
        return -1
    }

    /**
     * GPXデータからgpsデータの取得しリスト化
     */
    fun getGpxData() {
        var data = GpsData()
        var gpxOn = false
        var trkOn = false
        var trksegOn = false
        mListGpsData.clear()
        mListGpsPointData.clear()
        var i = 0
        while (i < mListData.size) {
            if (mListData[i].indexOf("<gpx ") == 0 || mListData[i].indexOf("<gpx>") == 0) {
                gpxOn = true
            } else if (mListData[i].indexOf("</gpx>") == 0) {
                gpxOn = false
            }
            if (gpxOn) {
                //  TRACKデータの取得開始・終了
                if (mListData[i].indexOf("<trk>") == 0) {
                    trkOn = true
                } else if (mListData[i].indexOf("</trk>") == 0) {
                    trkOn = false
                }
            }
            if (trkOn) {
                //  TRACKデータの取得開始・終了
                if (mListData[i].indexOf("<trkseg>") == 0) {
                    trksegOn = true;
                } else if (mListData[i].indexOf("</trkseg>") == 0) {
                    trksegOn = false;
                }
            }
            if (trksegOn) {
                //  TRACKポイントデータの取得開始・終了
                if (mListData[i].indexOf("<trkpt ") == 0) {
                    //  緯度経度座標の取得
                    val loc = PointD(getParameeorDataValue("lon", mListData[i]), getParameeorDataValue("lat", mListData[i]))
                    mListGpsPointData.add(loc)
//                    mGpsInfoData.setArea(loc)
//                    mGpsInfoData.addDistance(loc)
                    if (mDataType == DATATYPE.gpxData) {
                        data = GpsData()
                        data.mLatitude = loc.y      //  緯度
                        data.mLongitude = loc.x     //  経度
                        if (1 < mListGpsPointData.size) {
                            data.mDistance = klib.cordinateDistance(loc, mListGpsPointData[mListGpsPointData.size - 2])
                        } else {
                            data.mDistance = 0.0
                        }
                    } else if (mDataType == DATATYPE.gpxSimleData) {
                    }
                } else if (mListData[i].indexOf("<trkpt>") == 0) {
                    //  トラックデータの開始
                    if (mDataType == DATATYPE.gpxData) {
                        data = GpsData()
                    }
                } else if (mListData[i].indexOf("<ele>") == 0) {
                    //  標高値の取得
                    i++
                    var elevator = mListData[i].trim().toDoubleOrNull()?:0.toDouble() //  標高
//                    mGpsInfoData.setMinMaxElevator(elevator)
                    if (mDataType == DATATYPE.gpxData) {
                        data.mElevator = elevator
                    }
                } else if (mListData[i].indexOf("<time>") == 0) {
                    //  測定時間の取得
                    i++
                    var dateTime = klib.string2Date(mListData[i].trim())      //  日付時間
//                    mGpsInfoData.setDateTime(dateTime)
                    if (mDataType == DATATYPE.gpxData) {
                        data.mDate = dateTime
                        if (0 < mListGpsData.size)
                            data.mLap = dateTime.time - mListGpsData[mListGpsData.size - 1].mDate.time
                    }
                } else if (mListData[i].indexOf("</trkpt>") == 0) {
                    //  トラックデータの終了
                    if (mDataType == DATATYPE.gpxData) {
                        if (data.mLap != 0L)
                            data.mSpeed = data.mDistance / data.mLap * 1000 * 60 * 60   //  速度 km/h
                        mListGpsData.add(data)
                    }
                } else if (mListData[i].indexOf("</trkseg>") == 0) {
                    break
                } else if (mListData[i].indexOf("</trk>") == 0) {
                    break
                } else if (mListData[i].indexOf("</gpx>") == 0) {
                    break
                }
            }
            i++
        }
    }


    /**
     * アイテム内のパラメータデータの取得
     * para     パラメータ名
     * data     アイテムデータ
     * return   パラメータの実数データ
     */
    fun getParameeorDataValue(para: String, data: String): Double {
        return getParameeorData(para, data).toDoubleOrNull()?:0.toDouble()
    }

    /**
     * アイテム内のパラメータのデータ抽出
     * para     パラメータ名
     * data     アイテムデータ
     * return   抽出データ
     */
    fun getParameeorData(para: String, data: String): String {
        val n: Int = data.indexOf(para)
        if (0 <= n) {
            val m: Int = data.indexOf('"', n)
            val l: Int = data.indexOf('"', m + 1)
            return data.substring(m + 1, l)
        }
        return ""
    }

    /**
     *  GPXデータをアイテム単位でリスト化
     *  fileData    GPXテキストデータ
     */
    fun setListData(fileData: String) {
        mListData.clear()
        var buffer = ""
        var itemOn = false
        for (i in fileData.indices) {
            when (fileData[i]) {
                '<' -> {
                    if (0 < buffer.length) {
                        mListData.add(buffer)
                        buffer = ""
                    }
                    itemOn = true
                    buffer += fileData[i]
                }
                '>' -> {
                    buffer += fileData[i]
                    mListData.add(buffer)
                    buffer = ""
                    itemOn = false
                }
                '\r' -> {
                    if (!itemOn) {
                        if (0 < buffer.length) {
                            mListData.add(buffer)
                            buffer = ""
                        }
                    } else {
                        if (0 < buffer.length && buffer[buffer.lastIndex] != ' ')
                            buffer += ' '
                    }
                }
                ' ' -> {
                    if (buffer.length == 0)
                        continue
                    else if (1 < (fileData.length - i) && fileData[i + 1] == ' ')
                        continue
                    else
                        buffer += fileData[i]
                }
                else -> {
                    buffer += fileData[i]
                }
            }
        }
    }

    /**
     *  GPXファイルからのデータの読込
     *  path    GPXファイル名
     *  return  ファイルデータ
     */
    fun loadData(path: String): StringBuilder {
        var buffer = StringBuilder()

        val file = File(path)
        if (file.exists()) {
            val bufferReader = file.bufferedReader()    //  UTF8 (SJISの時はCharset.forName("MS932")を追加)
            var str = bufferReader.readLine()
            //  BOM付きの場合、BOM削除
            if (str.startsWith("\uFEFF"))
                str = str.substring(1)
            while (str != null && 0 < str.length) {
                buffer.append(str)
                buffer.append('\r')
                str = bufferReader.readLine()
            }
            bufferReader.close()
        }
        return buffer
    }

    /**
     * LoactionデータをGpsDataに変換して取り込む
     * listLocation     Locationデータのリスト
     * speedRef         速度の測定値を使う(false: 距離と時間から速度を求める)
     */
    fun location2GpsData(listLocation: List<Location>, stepCount: List<Int>, speedRef: Boolean = true) {
        mListGpsData.clear()
        for (i in 0..(listLocation.size - 1)) {
            var gpsData = GpsData()
            gpsData.mDate = Date(listLocation[i].time)
            gpsData.mLatitude = listLocation[i].latitude
            gpsData.mLongitude = listLocation[i].longitude
            gpsData.mElevator = listLocation[i].altitude
            if (i == 0) {
                gpsData.mDistance = 0.0 //  km
                gpsData.mLap = 0        //  ms
                gpsData.mStepCount = 0  //  歩数
            } else {
                gpsData.mDistance = klib.cordinateDistance(
                    PointD(listLocation[i - 1].longitude, listLocation[i - 1].latitude),
                    PointD(listLocation[i].longitude, listLocation[i].latitude))
                gpsData.mLap = listLocation[i].time - listLocation[i - 1].time
                gpsData.mStepCount = stepCount[i] - stepCount[i - 1]
            }
            if (gpsData.mLap == 0L) {
                gpsData.mSpeed = 0.0
            } else if (listLocation[i].speed.toDouble() == 0.0 || !speedRef) {
                gpsData.mSpeed = gpsData.mDistance / gpsData.mLap * 1000 * 3600
            } else {
                gpsData.mSpeed = listLocation[i].speed.toDouble() * 3.6     //  m/s → km/h 変換
            }
            mListGpsData.add(gpsData)
        }
    }

    /**
     * GPSの情報データの取得設定(mGpsInfoDataに設定)
     */
    fun setGpsInfoData() {
        if (0 < mListGpsData.size)
            mGpsInfoData = getGpsInfoData(mListGpsData)
    }

    /**
     * GpsDataリストからGpsInfoDataを取得する
     * listGpsData      GpsDataのリスト
     * retrun           GpsInfoData
     */
    fun getGpsInfoData(listGpsData: List<GpsData>): GpsInfoData {
        var gpsInfodata = GpsInfoData()
        for (gpsData in listGpsData) {
            gpsInfodata.setArea(PointD(gpsData.mLongitude, gpsData.mLatitude))
            gpsInfodata.setDateTime(gpsData.mDate)
            gpsInfodata.setMinMaxElevator(gpsData.mElevator)
            gpsInfodata.addDistance(PointD(gpsData.mLongitude, gpsData.mLatitude))
        }
        return gpsInfodata
    }
}

/**
 * GPS位置データ
 */
class GpsData {
    var mDate = Date()      //  時間  SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
    var mLatitude = 0.0     //  緯度(deg)
    var mLongitude = 0.0    //  経度(deg)
    var mElevator = 0.0     //  高度(m)
    var mDistance = 0.0     //  前回からの移動距離(km)
    var mLap: Long = 0      //  前回からの移動時間(ms)
    var mSpeed = 0.0        //  速度(km/h)
    var mStepCount: Int = 0 //  前回からの歩数
}

/**
 * GPSデータの情報データ
 */
class GpsInfoData {
    var mFirstTime = Date(0)                    //  開始時間
    var mLastTime = Date(0)                     //  終了時間
    var mDistance = 0.0                             //  移動距離8km)
    var mMinElevator = Double.MAX_VALUE             //  最小標高
    var mMaxElevator = Double.MIN_VALUE             //  最大標高
    var mArea = RectD()                             //  トレース領域

    private var mPrevPoint = PointD(Double.NaN, Double.NaN)
    private val klib = KLib()

    /**
     * 開始・終了時間のための測定時間の取得
     * date     測定時間
     */
    fun setDateTime(date: Date) {
        if (mFirstTime.time == 0L) {
            mFirstTime = date
            mLastTime = date
        } else {
            if (mFirstTime.time > date.time)
                mFirstTime = date
            if (mLastTime.time < date.time)
                mLastTime = date
        }
    }

    /**
     * 距離の累積
     * loc      位置座標
     */
    fun addDistance(loc: PointD) {
        if (mPrevPoint.x != Double.NaN) {
            mDistance += klib.cordinateDistance(mPrevPoint, loc)
        }
        mPrevPoint = loc
    }

    /**
     * 最小・再考標高のための標高データの取得
     * elevator     標高8m)
     */
    fun setMinMaxElevator(elevator: Double) {
        mMinElevator = Math.min(mMinElevator, elevator)
        mMaxElevator = Math.max(mMaxElevator, elevator)
    }

    /**
     * トレース領域の取得
     * cp       位置座標
     */
    fun setArea(cp: PointD) {
        if (mArea.isEmpty()) {
            mArea.left = cp.x
            mArea.top = cp.y
            mArea.right = cp.x
            mArea.bottom = cp.y
        } else {
            mArea.extension(cp)
        }
    }
}