package co.jp.yoshida.mapapp

import android.location.Location

class GpxWriter {

    var mFilePath = ""
    var mGpxHeaderCreater = "GPS Logger for Android"

    val klib = KLib()

    /**
     * 保存ファイル名の設定
     */
    fun Open(path: String) {
        mFilePath = path
    }

    /**
     * GPXファイルのヘッダ部作成保存
     * creater      Creater名(default: GPS Logger for Android)
     * name         track名(default: なし)
     */
    fun init(creater: String = "",name: String = "") {
        klib.writeFileData(mFilePath, initData(creater, name))
    }

    /**
     * GPCデータのヘッダ部作成
     * creator      作成者
     * name         名前
     * return       GPX文字列データ
     */
    fun initData(creater: String = "",name: String = ""): String {
        // GPXヘッダ作成
        mGpxHeaderCreater = if (0 < creater.length) creater else mGpxHeaderCreater
        var buffer = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
//        buffer += "<gpx xmlns=\"http://www.topografix.com/GPX/1/1\">\n"
        buffer += "<gpx version=\"1.0\" creator=\"" + mGpxHeaderCreater + "\">\n"
        buffer += "<trk>\n"
        if (0 < name.length)
            buffer += "<name>" + name + "</name>\n"
        buffer += "<trkseg>\n"
        return buffer
    }

    /**
     * Loactionデータの追加
     * location     位置情報(latitude,longtude,altitude,time)
     */
    fun appendData(location: Location) {
        klib.writeFileDataAppend(mFilePath, locationData(location))
    }

    /**
     * 座標データのGPXデータ作成
     * location     GPS座標データ
     * return       GPX文字列データ
     */
    fun locationData(location: Location): String {
        // 位置データ
        var buffer = "<trkpt lat=\"" + location.latitude.toString() +
                "\" lon=\"" + location.longitude.toString() + "\">"
        buffer += "<ele>" + location.altitude.toString() + "</ele>"
        buffer += "<time>" + klib.getLocationTime(location) + "</time>"
        buffer += "</trkpt>"
        buffer += "\n"
        return buffer
    }

    /**
     * GPXファイルの終了処理
     */
    fun close() {
        klib.writeFileDataAppend(mFilePath, closeData())
    }

    /**
     * GPX終了部のデータ作成
     * return   GPX文字列データ
     */
    fun closeData(): String {
        // 終了コード出力
        var buffer = "</trkseg>\n";
        buffer += "</trk>\n";
        buffer += "</gpx>";
        buffer += "\n";
        return buffer
    }

    /**
     * GPSデータのGPXファイル全体書き込み(append形式)
     * path         GPXファイルパス
     * locations    位置情報リスト(List<Location>)
     */
    fun writeDataAppendAll(path: String, locations: List<Location>) {
        mFilePath = path
        init()
        for (location in locations) {
            appendData(location)
        }
        close()
    }

    /**
     * GPSデータのGPXファイル一括書き込み
     * path         GPXファイルパス
     * locations    位置情報リスト(List<Location>)
     */
    fun writeDataAll(path: String, locations: List<Location>) {
        mFilePath = path
        var buffer = initData()
        for (location in locations) {
            buffer += locationData(location)
        }
        buffer += closeData()
        klib.writeFileData(mFilePath, buffer)
    }

}