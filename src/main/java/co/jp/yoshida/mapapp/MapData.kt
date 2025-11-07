package co.jp.yoshida.mapapp

import android.content.Context
import android.os.Build
import android.util.Log
import android.util.Size
import androidx.annotation.RequiresApi
import java.time.LocalDateTime

/**
 *  地図の位置情報
 *  国土地理院のタイル画像を画面上に配置するためのクラス
 *
 *  ZoomLevel : 0-20 (使われているのは20まで) 世界全体を表すためのレベルで数字が大きいほど詳細が見れる
 *              0だと一枚のタイル画像で世界全体を表示, 1だと2x2の4枚で表し,2だと3x3の9枚で表示する
 *  MapTitle  : 地図の種類、標準図や写真図などいろいろあり、MapInfoDataに記載
 *  Ext       : タイル画像の拡張子、主にpngで写真データはjpgになっている(MapInfoDataに記載)
 *  CellSize  : タイル画像の表示の大きさ、元データは256x256pixel
 *  ColCount  : 描画領域に表示するタイル画像の列数
 *  RowCount  : 描画領域に表示するタイル画像の行数、描画領域とColCountで決まる
 *  Start     : 描画領域に表示する左上のタイル画像の開始番号
 *  View      : 美容が領域のサイズ
 *
 *  座標系
 *  Screen      : 描画領域のスクリーン座標
 *  BaseMap     : ZoomLevel= 0 の時の座標、赤道の周長を1に換算して表す
 *  Map         : ZoomLevel ごとの座標、 BaseMap * 2^ZoomLevel となる
 *  Coordinates : メルカトル図法による緯度経度座標(度)
 *
 *  メルカトル図法の緯度変換
 *  幾何学的な円筒投影法だと f(Φ) = R x tan(Φ) であるが (Φ : 緯度(rad))
 *  メルカトル図法では f(Φ) = R x ln(tan(π/4 + Φ/2)) で表される
 *  逆変換は Φ = 2 x arcTan(exp(y/R)) - π/2
 */
class MapData(var context: Context, var mMapInfoData: MapInfoData) {
    val TAG = "MapData"

    val mZoomName = listOf(                             //  ズームレベル(spinner表示用)
        "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10",
        "11", "12", "13", "14", "15", "16", "17", "18")
    val mColCountName = listOf(                         //  列数(spinner表示用)
        "1", "2", "3", "4", "5", "6", "7", "8", "9", "10")
    var mMapTitle = "std"                               //  地図の名称
    var mMapTitleNum = 0                                //  地図の種類(No)
    var mExt = "png"                                    //  タイル画像の拡張子
    var mCellSize = 256f                                //  タイル画像の大きさ(辺の長さ)
    var mCellZoom = 1.0                                 //  タイル画像(セル)の拡大率
    var mZoom = 1                                       //  ズームレベル
    var mColCount = 2                                   //  表示列数
    var mRowCount = 2                                   //  表示行数
    var mStart = PointD(0.0, 0.0)                  //  表示開始座標(Map座標)
    var mView = Size(1000, 1000)            //  表示するViewの大きさ
    var mMapUrl = ""                                    //  地図データURL
    var mElevatorDataNo = 0                             //  使用標高データのNo
    var mBaseMapDataNo = 0                              //  重ね合わせ表示のベースマップDataNo
    var mBaseMapOver = false                            //  重ね合わせの順番(BaseMapを上)
    val mColorLegend = mutableMapOf<String, String>()   //  地図の色凡例データ

    var mBaseFolder = ""                                //  Mapデータ保存フォルダ
    var mDataFolder = ""                                //  App直下のデータフォルダ
    val mImageFileSet = mutableSetOf<String>()          //  ダウンロードしたファイルリスト(Web上に存在しないファイルも登録)
    var mDateTimeFolder = ""                            //  日時フォルダ名
    @RequiresApi(Build.VERSION_CODES.O)
    var mDispMapPreDateTime = LocalDateTime.now()       //
    val mImageFileSetName = "ImageFileSet.csv"          //  ダウンロードしたファイルリストのファイル名
    var mImageFileSetPath = ""                          //  ダウンロードしたファイルリストfパス

    val klib = KLib()

    //  MapDataをCSVファイルに保存する時のデータ配列のタイトル
    companion object {
        val mMapDataFormat = listOf("地図名", "地図ID", "拡張子", "セルサイズ", "ズームレベル",
            "列数", "行数", "X座標", "Y座標")
    }

    /**
     * データのコピーを作成
     */
    fun copyTo():MapData {
        var mapData = MapData(context, mMapInfoData)
        mapData.mMapTitle = mMapTitle
        mapData.mMapTitleNum = mMapTitleNum
        mapData.mExt = mExt
        mapData.mCellSize = mCellSize
        mapData.mZoom = mZoom
        mapData.mColCount = mColCount
        mapData.mRowCount = mRowCount
        mapData.mStart = PointD(mStart.x, mStart.y)
        mapData.mView = Size(mView.width,  mView.height)
        mapData.mMapUrl = mMapUrl
        mapData.mElevatorDataNo = mElevatorDataNo
        mapData.mBaseMapDataNo = mBaseMapDataNo
        mapData.mBaseFolder = mBaseFolder
        return mapData
    }

    /**
     *  クラスデータをテキストのリストで取得
     */
    fun getStringData(): List<String> {
        var dataList = mutableListOf<String>()
        dataList.add(mMapTitle)
        dataList.add(mMapTitleNum.toString())
        dataList.add(mExt)
        dataList.add(mCellSize.toString())
        dataList.add(mZoom.toString())
        dataList.add(mColCount.toString())
        dataList.add(mRowCount.toString())
        dataList.add(mStart.x.toString())
        dataList.add(mStart.y.toString())
        return dataList
    }


    /**
     *  テキストデータをクラスデータに設定
     */
    fun setStringData(data: List<String>) {
        mMapTitle    = data[0]
        mMapTitleNum = data[1].toInt()
        mExt         = data[2]
        mCellSize    = data[3].toFloat()
        mZoom        = data[4].toInt()
        mColCount    = data[5].toInt()
        mRowCount    = data[6].toInt()
        mStart.x     = data[7].toDouble()
        mStart.y     = data[8].toDouble()
        mMapInfoData.setMapTitleNum(mMapTitleNum)
    }

    /**
     *  クラスデータのチェック(正規化)
     *  mMapTitleNum に合わせてデータを設定する
     */
    fun normarized(){
        mMapTitleNum = if (mMapTitleNum < 0 || mMapInfoData.mMapData.size <= mMapTitleNum) 0 else mMapTitleNum
        mMapInfoData.setMapTitleNum(mMapTitleNum)
        mMapTitle = mMapInfoData.mMapData[mMapTitleNum][1]
        mExt = mMapInfoData.mMapData[mMapTitleNum][2]
        mImageFileSetPath = mBaseFolder + mMapTitle + "/" + mImageFileSetName;
        mZoom = Math.max(mZoom, 0)
        val maxColCount = getMaxColCount()
        mStart.x  = Math.min(Math.max(mStart.x, 0.0), maxColCount.toDouble())
        mStart.y  = Math.min(Math.max(mStart.y, 0.0), maxColCount.toDouble())
        mColCount = Math.min(Math.max(mColCount, 1), 20)
        mColCount = Math.min(mColCount, maxColCount)
        mCellSize = getCellSize().toFloat()
        mRowCount = getRowCountF().toInt()
        mMapUrl = mMapInfoData.mMapData[mMapTitleNum][7]
        mElevatorDataNo = mMapInfoData.getElevatorDataNo(mMapInfoData.mMapData[mMapTitleNum][10])
        mBaseMapDataNo = mMapInfoData.getMapDataNo(mMapInfoData.mMapData[mMapTitleNum][11])
        mBaseMapOver = mMapInfoData.mMapData[mMapTitleNum][13].compareTo("true") == 0
        mDateTimeFolder = ""
        loadColorLegend()
        Log.d(TAG,"normarized: "+mImageFileSetPath)
    }

    /**
     * 日時付Webアドレスの処理
     * forth        強制過去データ削除
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun setDateTime(forth: Boolean = false) {
        mDateTimeFolder = ""
        mMapInfoData.mDispDateTime.clear()
        if (mMapInfoData.isDateTimeData(mMapUrl)) {
            mMapUrl = mMapInfoData.setMapWebAddress(mMapUrl)
            mDateTimeFolder = mMapInfoData.mDateTimeFolder
            if (forth ||
                (0 < mMapInfoData.mDateTimeFolder.length &&
                        mMapInfoData.mDispDateTime[0] != mDispMapPreDateTime)) {
                //  過去データを削除
                removeMapData(false)
                mDispMapPreDateTime = mMapInfoData.mDispDateTime[0]
            }
        }
    }


    /**
     * 表示中地図データの削除
     * msg      確認メッセージの有無(予定)
     */
    fun removeMapData(msg: Boolean = true) {
        var path = klib.getFullPath(getDownloadDataBaseFolder())
        if (!msg) {
            try {
                if (klib.existsFile(path)) {
                    klib.deleteDirectory(path)
                    removeImageFileList()
                }
                if (isMergeData()) {
                    //  重ね合わせデータの削除
                    path = klib.getFullPath(getDownloadDataBaseFolder(false))
                    if (klib.existsFile(path)) {
                        klib.deleteDirectory(path)
                        removeImageFileList()
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG,"removeMapData: " + e.message)
            }
        }
    }

    /**
     * ImageFileListから自分のURLを削除する
     */
    fun removeImageFileList() {
        var url = ""
        if (0 < mMapUrl.length) {
            url = mMapUrl.substring(0, klib.lastIndexOf(mMapUrl, "/", 3))
        } else {
            url = mMapInfoData.mGsiUrl + mMapTitle
        }
        var removeList = mutableListOf<String>()
        for (item in mImageFileSet) {
            if (0 <= item.indexOf(url))
                removeList.add(item)
        }
        for (item in removeList)
            mImageFileSet.remove(item)
    }

    /**
     * 標高データファイルの取得
     * x            X座標
     * y            Y座標
     * fileUpdate   データ取得モード
     */
    fun getElevatorDataFile(x: Int, y: Int, fileUdate: MainActivity.WebFileDownLoad) {
        val elevatorUrl = getElevatorWebAddress(x, y)
        val downloadPath = downloadElevatorPath(x, y)
        getDownLoadFile(elevatorUrl, downloadPath, fileUdate)
    }

    /**
     * 標高データのWebアドレスの取得
     * x            X座標
     * y            Y座標
     * return       Webアドレス
     */
    fun getElevatorWebAddress(x: Int, y: Int): String {
        val eleZoomMax = mMapInfoData.getElevatorMaxZoom(mElevatorDataNo)
        var pos = PointD(x, y)
        var eleZoom = mZoom
        if (eleZoomMax < mZoom) {
            //  標高データは最大ズームレベルまでなのでそれ以上は最大ズームレベルのデータを取得
            pos = cnvMapPositionZoom(eleZoomMax, pos)
            eleZoom = eleZoomMax
        }
        return mMapInfoData.getElevatorWebAddress(eleZoom, pos.x.toInt(), pos.y.toInt(), mElevatorDataNo)
    }

    /**
     * 標高データのダウンロード先パスの取得
     * x            X座標
     * y            Y座標
     * return       ファイルパス
     */
    fun downloadElevatorPath(x: Int, y:Int): String {
        var eleZoomMax = mMapInfoData.getElevatorMaxZoom(mElevatorDataNo)
        var pos = PointD(x, y)
        var eleZoom = mZoom
        if (eleZoomMax < mZoom) {
            //  標高データは最大ズームレベルまでなのでそれ以上は最大ズームレベルのデータを取得
            pos = cnvMapPositionZoom(eleZoomMax, pos)
            eleZoom = eleZoomMax
        }
        var downloadPath = mBaseFolder + mMapInfoData.getElevatorDataId(mElevatorDataNo) +
                "/" + eleZoom.toString() + "/" + pos.x.toInt() + "/" + pos.y.toInt() + "." +
                mMapInfoData.getElevatorDataExt(mElevatorDataNo)
        return downloadPath
    }

    /**
     * 地図データを取得
     * x            X座標
     * y            Y座標
     * fileUpdate   データ取得モード
     * return       ダウンロードパス
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun getMapData(x: Int, y: Int, fileUdate: MainActivity.WebFileDownLoad): String {
        if (isMergeData()) {
            //  重ね合わせデータの表示する場合
            return getMergeMapData(x, y, fileUdate)
        } else {
            //  単独のMapDataの表示する場合
            return getMapDataDownload(x, y, fileUdate)
        }
    }

    /**
     * 重ね合わせデータかの確認
     * return       true (重ね合わせデータ)
     */
    fun isMergeData(): Boolean {
        return 0 < mMapInfoData.getMapMergeDataId(mMapTitleNum).length
    }

    /**
     * 地図データの重ね合わせ処理
     * x            X座標
     * y            Y座標
     * fileUpdate   データ取得モード
     * return       重ね合わせ画像パス
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun getMergeMapData(x: Int, y: Int, fileUdate: MainActivity.WebFileDownLoad): String {
        val mergeDataPath = downloadMergeDataPath(x, y)
        if (klib.existsFile(mergeDataPath) && fileUdate == MainActivity.WebFileDownLoad.OFFLINE)
            return mergeDataPath

        //  重ね合わせデータ取得のためMapDataをコピー
        var baseMap = copyTo()
        baseMap.mMapTitleNum = mBaseMapDataNo
        val baseMapDataPath = baseMap.getMapDataDownload(x, y, fileUdate)
        val lapMapDataPath = getMapDataDownload(x, y, fileUdate)
        if (!klib.mkdir(klib.getFolder(mergeDataPath)))
            return ""

        val transparent = mMapInfoData.getMapOverlapTransparent(mMapTitleNum)   //  透過色の取得
        //  重ね合わせ処理
        if (mBaseMapOver) {
            return klib.imageComposite(lapMapDataPath, baseMapDataPath, mergeDataPath, transparent)
        } else {
            return klib.imageComposite(baseMapDataPath, lapMapDataPath, mergeDataPath, transparent)
        }
    }

    /**
     * 地図データをダウンロードする
     * x            X座標
     * y            Y座標
     * fileUpdate   データ取得モード
     * return       ダウンロードパス
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun getMapDataDownload(x: Int, y: Int, fileUdate: MainActivity.WebFileDownLoad): String {
        var mapUrl = mMapInfoData.getMapWebAddress(mZoom, x, y, mMapTitleNum)
        var downLoadPath = downloadPath(x, y)
        getDownLoadFile(mapUrl, downLoadPath, fileUdate)
        return downLoadPath
    }

    /**
     * 地図データのダウンロードパスを求める
     * x            X座標
     * y            Y座標
     * return       ダウンロードパス
     */
    fun downloadPath(x: Int, y: Int): String {
        return mBaseFolder + mMapInfoData.getMapDataId(mMapTitleNum) + mDateTimeFolder +
                "/" + mZoom.toString() + "/" + x + "/" + y + "." +
                mMapInfoData.getMapDataExt(mMapTitleNum)
    }

    /**
     * マージしたデータファイル名のパス(重ね合わせた地図データ)
     * x            X座標
     * y            Y座標
     * return       ダウンロードパス
     */
    fun downloadMergeDataPath(x: Int, y:Int): String {
        return mBaseFolder + mMapInfoData.getMapDataId(mMapTitleNum) +
                "_" + mMapInfoData.getMapMergeDataId(mMapTitleNum) +
                (if (mMapInfoData.getMapMergeOverlap(mMapTitleNum)) "_up" else "") +
                "/" + mZoom.toString() + "/" + x + "/" + y + "." +
                mMapInfoData.getMapDataExt(mMapTitleNum)
    }

    /**
     * 地図データを保存するフォルダ名の取得
     * merge        重ね合わせの有無
     * return       フォルダ名
     */
    fun getDownloadDataBaseFolder(merge: Boolean = true): String {
        if (merge && isMergeData()) {
            return mBaseFolder + "/" + mMapInfoData.getMapDataId(mMapTitleNum) +
                    "_" + mMapInfoData.getMapMergeDataId(mMapTitleNum) +
                    (if (mMapInfoData.getMapMergeOverlap(mMapTitleNum)) "_up" else "")
        } else {
            return mBaseFolder + "/" +  mMapInfoData.getMapDataId(mMapTitleNum)
        }
    }

    /**
     * タイルデータをWebからダウンロードする
     * dataUrl          データWebアドレス
     * downLoadFile     ダウンロードパス
     * fileUpdate       データ取得モード
     */
    fun getDownLoadFile(dataUrl: String, downLoadFile: String, fileUdate: MainActivity.WebFileDownLoad) {
        if ((fileUdate == MainActivity.WebFileDownLoad.ALLUPDATE || !klib.existsFile(downLoadFile)) &&
            fileUdate != MainActivity.WebFileDownLoad.OFFLINE) {
            if (fileUdate != MainActivity.WebFileDownLoad.NORMAL || !mImageFileSet.contains(dataUrl)) {
                var downLoad = DownLoadWebFile(dataUrl, downLoadFile)
                downLoad.start()
                while (downLoad.isAlive()) {
                    Thread.sleep(100L)
                }
                mImageFileSet.add(dataUrl)  //  ダウンロードしたファイルをリスト登録
            }
        }
    }

    /**
     *  画面の移動
     *  dx,dy : MAP座標の増分
     */
    fun setMove(dx: Double, dy: Double) {
        mStart.x = mStart.x + dx
        mStart.y = mStart.y + dy
        normarized()
    }

    /**
     *  ズームアップした時の表示位置を更新
     *  curZoom     変更前のzoom値
     *  nextZoom    変更後のzoom値
     */
    fun setZoomUpPos(curZoom: Int, nextZoom: Int) {
        setZoom((nextZoom - curZoom).toDouble())
    }

    /**
     *  地図の中心で拡大縮小
     *  dZoom : Zoomの増分
     */
    fun setZoom(dZoom: Double) {
        var ctr = PointD(mStart.x + getColCountF() / 2.0, mStart.y + getRowCountF() / 2.0)
        setZoom(dZoom, ctr)
    }

    /**
     *  指定店を中心に拡大縮小
     *  dZoom   Zoom増分
     *  ctr     拡大縮小の中心点(Map座標)
     */
    fun setZoom(dZoom: Double, ctr: PointD) {
        mCellZoom += dZoom
        var zoom = mCellZoom.toInt() - 1
        mCellZoom = 1 + mCellZoom % 1.0
        mCellSize = getCellSize().toFloat()
        if (mZoom + zoom <= mMapInfoData.getMapDataMaxZoom(mMapTitleNum)) {
            mStart.x = ctr.x * Math.pow(2.0, zoom.toDouble()) - getColCountF() / 2.0
            mStart.y = ctr.y * Math.pow(2.0, zoom.toDouble()) - getRowCountF() / 2.0
            mZoom += zoom
            normarized()
        }
    }

    /**
     * CellSizeとCellZoomをリセットする
     */
    fun cellZoomReset() {
        mCellZoom = 1.0
        mCellSize = getCellSize().toFloat()
    }

    /**
     * ズームレベルを変更したときのMap座標を求める
     * nextZoom     変更するズームレベル
     * mp           変換元のMap座標
     * return       変換後のMap座標
     */
    fun cnvMapPositionZoom(nextZoom: Int, mp: PointD): PointD {
        var cmp = PointD()
        cmp.x = mp.x * Math.pow(2.0, nextZoom.toDouble() - mZoom.toDouble())
        cmp.y = mp.y * Math.pow(2.0, nextZoom.toDouble() - mZoom.toDouble())
        return cmp
    }

    /**
     *  列数を変更したときの表示位置を更新
     *  curColCount     変更前の列数
     *  nextColCount    変更後の列数
     */
    fun setColCountUpPos(curColCount: Int, nextColCount: Int) {
        mStart.x += (nextColCount - curColCount) / 2
        mStart.y += (nextColCount - curColCount) / 2
        mColCount = nextColCount
    }

    /**
     *  BaseMap座標で指定した位置を中心に移動する
     *  location    移動座標(BaseMap座標)
     */
    fun setLocation(location: PointD) {
        var ctr = baseMap2Map(location)
        mStart.x = ctr.x - getColCountF() / 2.0
        mStart.y = ctr.y - getRowCountF() / 2.0
    }

    /**
     *  パラメータをプリファレンスに保存
     */
    fun saveParameter() {
        klib.setIntPreferences(mMapTitleNum, "MapDataID", context)
        klib.setIntPreferences(mZoom, "MapZoomLevel", context)
        klib.setIntPreferences(mColCount, "MapColCount", context)
        klib.setFloatPreferences(mStart.x.toFloat(), "MapStartX", context)
        klib.setFloatPreferences(mStart.y.toFloat(), "MapStartY", context)
    }

    /**
     *  プリファレンスから保存パラメートを取得
     */
    fun loadParameter() {
        mMapTitleNum = klib.getIntPreferences("MapDataID", context)
        mZoom = klib.getIntPreferences("MapZoomLevel", 1, context)
        mColCount = klib.getIntPreferences("MapColCount", 2, context)
        mStart.x = klib.getFloatPreferences("MapStartX", context).toDouble()
        mStart.y = klib.getFloatPreferences("MapStartY", context).toDouble()
        normarized()
    }

    /**
     *  ダウンロードした画像ファイルリストの保存
     *  データのない画像名も登録する
     *  path        保存ファイルパス
     */
    fun saveImageFileSet(path: String) {
        if (mImageFileSet.count() == 0)
            return
        var dataList = mutableListOf<String>()
        for (data in mImageFileSet) {
            dataList.add(data)
        }
        klib.saveTextData(path, dataList)
    }

    fun saveImageFileSet(){
        saveImageFileSet(mImageFileSetPath)
    }

    /**
     *  ダウンロードした画像ファイルリストの取り込み
     *  path        ファイルパス
     */
    fun loadImageFileSet(path: String) {
        Log.d(TAG,"loadImageFileSet2: "+path)
        if (klib.existsFile(path)) {
            var dataList = klib.loadTextData(path)
            mImageFileSet.clear()
            for (data in dataList) {
                mImageFileSet.add(data)
            }
        }
    }

    fun loadImageFileSet() {
        loadImageFileSet(mImageFileSetPath)
    }

    /**
     *  色凡例データを求める
     *  rgb         色の値(RGB)
     *  return      凡例データ
     */
    fun getColorLegend(rgb: String): String {
        if (mColorLegend.containsKey(rgb.uppercase())) {
            var data = mColorLegend[rgb.uppercase()]
            if (data != null)
                return data
        }
        return ""
    }

    /**
     * 色凡例データの読込
     */
    fun loadColorLegend() {
        var path = mDataFolder + "/legend_" + mMapTitle + ".csv"
        mColorLegend.clear()
        if (klib.existsFile(path)) {
            var listData = klib.loadCsvData(path)
            for (data in listData) {
                if (data[0][0] != '#') {
                    if (1 < data.size && !mColorLegend.containsKey(data[0].uppercase()))
                        mColorLegend.put(data[0].uppercase(), data[1])
                }
            }
            Log.d(TAG,"ColorLegend: " + mColorLegend.size)
        }
    }


    /**
     *  Map座標の端数の取得
     */
    fun getStartOffset(): PointD {
        return PointD(mStart.x - Math.floor(mStart.x), mStart.y - Math.floor(mStart.y))
    }

    /**
     *  タイル画像のサイズを求める(一辺のながさ)
     *  return      タイル画像の1篇の長さ(スクリーン座標)
     */
    fun getCellSize(): Double {
        return if (mColCount == 1) {
            if (mView.width < mView.height) mView.width.toDouble() else mView.height.toDouble()
        } else {
            mView.width.toDouble() / mColCount.toDouble()
        } * mCellZoom
    }

    /**
     *  画面縦方向のタイル数を求める(整数単位に切上げ)
     *  return  表示列数(Int)
     */
    fun getRowCount(): Int {
        return mView.height / mCellSize.toInt()
    }

    /**
     *  画面横方向のタイル数を求める(Double)
     *  return  表示列数(Double)
     */
    fun getColCountF(): Double {
        return  (mView.width / mCellSize).toDouble()
    }

    /**
     *  画面縦方向のタイル数を求める(Double)
     *  return  表示列数(Double)
     */
    fun getRowCountF(): Double {
        return  (mView.height / mCellSize).toDouble()
    }

    /**
     *  ズームレベルでの最大列数(一周分)
     *  return  列数
     */
    fun getMaxColCount(): Int {
        return Math.pow(2.0, mZoom.toDouble()).toInt()
    }

    /**
     *  表示地図の中心座標(BaseMap座標)の取得
     *  return  中心座標(BaseMap座標)
     */
    fun getCenter(): PointD {
        return map2BaseMap(getMapCenter())
    }

    /**
     *  表示地図の中心座標(Map座標)の取得
     *  return  中心座標(Map座標)
     */
    fun getMapCenter(): PointD {
        return PointD(mStart.x + getColCountF() / 2.0, mStart.y + getRowCountF() / 2.0)
    }

    /**
     * 描画領域をBaseMap座標で取得
     * return   描画領域
     */
    fun getArea(): RectD {
        var bsp = map2BaseMap(mStart)
        var bep = map2BaseMap(PointD(mStart.x + getColCountF(), mStart.y + getRowCountF()))
        return RectD(bsp, bep)
    }

    /**
     * 描画領域を緯度経度座標で取得
     * return   描画領域
     */
    fun getAreaCoordinates(): RectD {
        var bsp = klib.baseMap2Coordinates(map2BaseMap(mStart))
        var bep = klib.baseMap2Coordinates(map2BaseMap(PointD(mStart.x + getColCountF(), mStart.y + getRowCountF())))
        return RectD(bsp, bep)
    }

    /**
     * 緯度経度座標(度)をスクリーン座標に変換
     * cp       緯度経度座標
     * return   スクリーン座標
     */
    fun coordinates2Screen(cp: PointD): PointD {
        return baseMap2Screen(coordinates2BaseMap(cp))
    }

    /**
     *  BaseMap座標をスクリーン座標に変換
     *  bp      BaseMap座標
     *  return  スクリーン座標
     */
    fun baseMap2Screen(bp: PointD): PointD {
        var mp = baseMap2Map(bp)
        return map2Screen(mp)
    }

    /**
     *  BaseMap座標をMap座標に変換
     *  bp      BaseMap座標
     *  retrurn Map座標
     */
    fun baseMap2Map(bp: PointD): PointD {
        var mp = PointD(0.0, 0.0)
        mp.x = bp.x * Math.pow(2.0, mZoom.toDouble())
        mp.y = bp.y * Math.pow(2.0, mZoom.toDouble())
        return mp
    }

    /**
     *  Map座標をスクリーン座標に変換
     *  mp      Map座標
     *  return  スクリーン座標
     */
    fun map2Screen(mp: PointD): PointD {
        var sp = PointD(0.0, 0.0)
        sp.x = (mp.x - mStart.x) * mCellSize
        sp.y = (mp.y - mStart.y) * mCellSize
        return sp
    }

    /**
     * スクリーン座標を緯度経度座標(度)に変換する
     * sp       スクリーン座標
     * return   緯度経度座標(度)
     */
    fun screen2Coordinates(sp: PointD): PointD {
        return baseMap2Coordinates(screen2BaseMap(sp))
    }

    /**  スクリーン座標をBaseMap座標に変換
     * sp       スクリーン座標
     * return   BaseMap座標
     */
    fun screen2BaseMap(sp: PointD): PointD {
        var mp = screen2Map(sp)
        return map2BaseMap(mp)
    }

    /**
     *  スクリーン座標をMap座標に変換
     *  sp      スクリーン座標
     *  return  Map座標
     */
    fun screen2Map(sp: PointD): PointD {
        var x = mStart.x + sp.x / mCellSize
        var y = mStart.y + sp.y / mCellSize
        return PointD(x, y)
    }

    /**
     *  Map座標をBaseMap座標に変換
     *  mp      Map座標
     *  return  BaseMap座標
     */
    fun map2BaseMap(mp: PointD): PointD {
        var x = mp.x / Math.pow(2.0, mZoom.toDouble())
        var y = mp.y / Math.pow(2.0, mZoom.toDouble())
        return PointD(x, y)
    }

    /**
     *  緯度経度座標(度)からメルカトル図法での距離を求める
     *  cp      cp.x : 経度、 cp.y : 緯度
     *  return  BaseMap座標
     */
    fun coordinates2BaseMap(cp: PointD): PointD {
        //  座標変換
        return PointD(
            cp.x / 360.0 + 0.5,
            0.5 - 0.5 / Math.PI * Math.log(Math.tan(Math.PI * (1 / 4.0 + cp.y / 360.0))))
    }

    /**
     *  メルカトル図法での距離から緯度経度座標(度)を求める
     *  bp.X : 経度方向の距離、 bp.Y : 緯度方向の距離
     *  return : BaseMap上の位置
     */
    fun baseMap2Coordinates(bp: PointD): PointD {
        var cp = PointD(Math.PI * (2.0 * bp.x - 1),
            2.0 * Math.atan(Math.exp((0.5 - bp.y) * 2.0 * Math.PI)) - Math.PI / 2.0)
        //  rad → deg
        cp.x *= 180.0 / Math.PI
        cp.y *= 180.0 / Math.PI
        return cp
    }
}