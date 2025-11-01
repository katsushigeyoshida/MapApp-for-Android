package co.jp.yoshida.mapapp

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.location.Location
import android.location.LocationManager
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.Date
import kotlin.math.max
import kotlin.math.min

/**
 * GPSのトレースデータ(ファイル)を一覧管理する
 */
class GpsTraceList {
    val TAG = "GpsTraceList"

    var mErrorMessage = ""                          //  エラー時の内容

    var mDataList = mutableListOf<GpsTraceData>()   //  GPSリストデータ
    var mFilterDataList = mutableListOf<GpsTraceData>()   //  GPSリストデータ
    var mGpsTraceListPath = ""                      //  リストデータのファイル保存パス
    var mGpsTraceFileFolder = ""                    //  トレースデータフォルダ
    var mDisp = true                                //  表示フラグ

    enum class DATALISTSORTTYPE {
        Non, DATE, TITLE, DISTANCE, ELEVATOR
    }
    var mDataListSortCending = false                //  ソート方向降順
    var mDataListSortType = DATALISTSORTTYPE.DATE   //  ソート対象

    //  特殊グループ名
    val mAllListName = "すべて"
    val mTrashGroup = "ゴミ箱"

    //  カラーメニュー
    val mColorMenu = listOf("Black", "Red", "Blue", "Green", "Yellow", "White",
        "Cyan", "Gray", "LightGray", "Magenta", "DarkGray", "Transparent")
    //  分類メニュー
    val mCategoryMenu = mutableListOf<String>(
        "散歩", "ウォーキング", "ジョギング", "ランニング", "山歩き", "自転車", "車・バス・鉄道", "飛行機", "旅行")

    lateinit var mC: Context
    val klib = KLib()

    companion object {
        //  GPSデータファイルをGPXに変換する
        var mGpxConvertOn = false                       //  GPXデータ変換フラグ
        val klib = KLib()

        /**
         * dataList         GPSデータリスト
         * exportFolder     GPXファイル出力先フォルダ
         */
        fun gpxExport(dataList: List<GpsTraceData>, exportFolder: String) {
            //  ファイル変換を非同期処理
            mGpxConvertOn = true
            GlobalScope.launch {
                for (gpsData in dataList) {
                    val outPath =
                        exportFolder + "/" + klib.getFileNameWithoutExtension(gpsData.mFilePath) + ".gpx"
                    gpsData.loadGpsData(false, true)
                    gpsData.gpxExport(outPath)
                }
                mGpxConvertOn = false
            }
        }
    }

    /**
     * 再帰的にファイルを検索してリストにないデータを追加する
     */
    fun getFileData() {
        mErrorMessage = ""
        Log.d(TAG,"getFileData "+mGpsTraceFileFolder)
        var fileList = klib.getFileList(mGpsTraceFileFolder, true, "*.csv")
        for (i in fileList.indices) {
            try {
                if (null == mDataList.find { it.mFilePath.compareTo(fileList[i].absolutePath, true) == 0 }) {  //  ファイル重複チェック
                    var gpsTraceData = GpsTraceData()
                    gpsTraceData.mFilePath = fileList[i].absolutePath
                    gpsTraceData.loadGpsData(false)
                    mDataList.add(gpsTraceData)
                }
            } catch (e: Exception) {
                mErrorMessage += fileList[i].absolutePath + " " + e.message + "\n"
            }
        }
    }

    /**
     * 年リストの取得
     * firstItem        リストの最初に追加するアイテム
     */
    fun getYearList(firstItem:String = ""): List<String> {
        var yearList = mutableListOf<String>()
        for (i in mDataList.indices) {
            val year = mDataList[i].getYearStr()
            if (!yearList.contains(year))
                yearList.add(year)
        }
        yearList.sortDescending()
        if (0 < firstItem.length)
            yearList.add(0, firstItem)
        return yearList
    }

    /**
     * 分類リストの取得
     * firstItem        リストの最初に追加するアイテム
     */
    fun getCategoryList(firstItem:String = ""): List<String> {
        var categoryList = mutableListOf<String>()
        for (i in mDataList.indices) {
            if (!categoryList.contains(mDataList[i].mCategory))
                categoryList.add(mDataList[i].mCategory)
        }
        categoryList.sortDescending()
        if (0 < firstItem.length)
            categoryList.add(0, firstItem)
        return categoryList
    }

    /**
     *  グループリストの取得
     *  firstTitle  リストの最初に追加するタイトル
     */
    fun getGroupList(firstTitle: String = ""): List<String> {
        var groupList = mutableListOf<String>()
        for (markData in mDataList) {
            if (!groupList.contains(markData.mGroup))
                groupList.add(markData.mGroup)
        }
        groupList.sortDescending()
        if (0 < firstTitle.length)
            groupList.add(0, firstTitle)
        return groupList
    }

    /**
     * 全データの表示フラグをクリア(非表示)にする
     */
    fun clearVisible() {
        for (i in mDataList.indices) {
            mDataList[i].mVisible = false
        }
    }

    /**
     * 全データの表示フラグを表示にする
     */
    fun setAllVisible() {
        for (i in mDataList.indices) {
            mDataList[i].mVisible = true
        }
    }

    /**
     * 表示フラグを反転する
     */
    fun reverseVisible() {
        for (i in mDataList.indices) {
            mDataList[i].mVisible = !mDataList[i].mVisible
        }
    }

    /**
     * リストデータから表示フラグを設定
     * selectList       選択リスト
     */
    fun setVisible(selectList: List<Int>) {
        clearVisible()
        for (i in selectList.indices)
            mFilterDataList[selectList[i]].mVisible = true
    }

    /**
     * 指定項目を[ゴミ箱]に設定
     * n            表示選択データNo
     */
    fun setTrashData(n: Int) {
        if (0 <= n && n < mFilterDataList.size)
            mFilterDataList[n].mGroup = mTrashGroup
    }

    /**
     * リストデータからグループを[ゴミ箱]に設定
     * selectList       表示選択Noリスト
     */
    fun setTrashData(selectList: List<Int>) {
        for (i in selectList.indices)
            mFilterDataList[selectList[i]].mGroup = mTrashGroup
    }

    /**
     * リストデータからグループのゴミ箱を解除(要グループ再設定)
     * selectList       表示選択Noリスト
     */
    fun setUnTrashData(selectList: List<Int>) {
        for (i in selectList.indices)
            if (mFilterDataList[selectList[i]].mGroup.compareTo(mTrashGroup) == 0)
                mFilterDataList[selectList[i]].mGroup = ""
    }

    /**
     * すべてのデータのゴミ箱を解除
     */
    fun setAllUnTrashData() {
        for (i in mDataList.indices) {
            if (mDataList[i].mGroup.compareTo(mTrashGroup) == 0)
                mDataList[i].mGroup = ""
        }
    }

    /**
     * リストデータからグループを設定
     * selectList       表示選択Noリスト
     */
    fun setGroupData(selectList: List<Int>, group: String) {
        for (i in selectList.indices)
            mFilterDataList[selectList[i]].mGroup = group
    }

    /**
     * リストデータからデータファイルを含めて削除(gpxファイルは除く)
     * selectList       表示選択データリスト
     */
    fun removeDataFile(selectList: List<GpsTraceData>) {
        for (i in selectList.indices) {
            val n = mDataList.indexOf(selectList[i])
            if (klib.getNameExt(mDataList[n].mFilePath).compareTo("gpx", true) != 0)
                klib.removeFile(mDataList[n].mFilePath)
            mDataList.removeAt(n)
        }
    }

    /**
     * リストデータからゴミ箱のデータをファイルを含めて削除(gpxファイルは除く)
     * firstTime        開始時間リスト
     */
    fun removeTrashDataFile(selectList: List<GpsTraceData>) {
        for (i in selectList.indices) {
            val n = mDataList.indexOf(selectList[i])
            if (klib.getNameExt(mDataList[n].mFilePath).compareTo("gpx", true) != 0 &&
                mDataList[n].mGroup.compareTo(mTrashGroup) == 0)
                klib.removeFile(mDataList[n].mFilePath)
            mDataList.removeAt(n)
        }
    }

    /**
     * データファイルの移動
     * dataList     選択されたリストのNo
     * moveFolder   移動先フォルダ
     */
    fun gpxFilesMove(dataList: List<Int>, moveFolder: String) {
        klib.mkdir(moveFolder)
        //  ファイル移動
        for (gpsDataNo in dataList) {
            var fileName = klib.getName(mFilterDataList[gpsDataNo].mFilePath)
            var outPath = moveFolder + "/" + fileName
            if (klib.moveFile(mFilterDataList[gpsDataNo].mFilePath, moveFolder))
                mFilterDataList[gpsDataNo].mFilePath = outPath
        }
    }

    /**
     *  GPSトレースの表示
     *  canvas      描画canvas
     *  mapData     地図位置情報
     */
    fun draw(canvas: Canvas, mapData: MapData) {
        if (mDisp) {
            for (gpsData in mDataList) {
                if (gpsData.mVisible &&
                    (gpsData.mLocArea.isEmpty() || !mapData.getAreaCoordinates().outside(gpsData.mLocArea))) {
                    if (gpsData.mLocData.size < 1)
                        gpsData.loadGpsData()
                    gpsData.draw(canvas, mapData)
                }
            }
        }
    }

    /**
     *  リストビューに表示するタイトルリストをつくる
     *  リストのソートとフィルタ処理をおこなう
     *  year: String        年フィルタ
     *  category: String,   分類フィルタ
     *  group: String,      グループフィルタ
     *  titleType: Int = 0, タイトル表示形式
     *  pathOffset: Int = 0 保存フォルダのオフセット値
     *  return              タイトルリスト
     */
    fun getListTitleData(year: String, category: String, group: String, titleType: Int = 0, pathOffset: Int = 0): List<String> {
        //  ソート処理
        if (mDataListSortCending) {
            if (mDataListSortType == DATALISTSORTTYPE.DATE) {
                mDataList.sortWith({ a, b -> (a.mFirstTime.time / 1000 - b.mFirstTime.time / 1000).toInt() })
            } else if (mDataListSortType == DATALISTSORTTYPE.TITLE) {
                mDataList.sortWith({ a, b -> a.mTitle.compareTo(b.mTitle) })
            } else if (mDataListSortType == DATALISTSORTTYPE.DISTANCE) {
                mDataList.sortWith({ a, b -> (a.mDistance * 1000 - b.mDistance * 1000).toInt() })
            } else if (mDataListSortType == DATALISTSORTTYPE.ELEVATOR) {
                mDataList.sortWith({ a, b -> (a.mMaxElevation - b.mMaxElevation).toInt() })
            }
        } else {
            if (mDataListSortType == DATALISTSORTTYPE.DATE) {
                mDataList.sortWith({ b, a -> (a.mFirstTime.time / 1000 - b.mFirstTime.time / 1000).toInt() })
            } else if (mDataListSortType == DATALISTSORTTYPE.TITLE) {
                mDataList.sortWith({ b, a -> a.mTitle.compareTo(b.mTitle) })
            } else if (mDataListSortType == DATALISTSORTTYPE.DISTANCE) {
                mDataList.sortWith({ b, a -> (a.mDistance * 1000 - b.mDistance * 1000).toInt() })
            } else if (mDataListSortType == DATALISTSORTTYPE.ELEVATOR) {
                mDataList.sortWith({ b, a -> (a.mMaxElevation - b.mMaxElevation).toInt() })
            }
        }
        //  表示タイトル設定(フィルタ処理)
        mFilterDataList.clear()
        var titleList = mutableListOf<String>()
        for (gpsFileData in mDataList) {
            if ((year.compareTo(mAllListName) == 0 || gpsFileData.getYearStr().compareTo(year) == 0) &&
                (category.compareTo(mAllListName) == 0 || gpsFileData.mCategory.compareTo(category) == 0) &&
                ((group.compareTo(mAllListName) == 0 && gpsFileData.mGroup.compareTo(mTrashGroup) != 0)
                        || gpsFileData.mGroup.compareTo(group) == 0)) {
                mFilterDataList.add(gpsFileData)
            }
        }
        for (gpsData in mFilterDataList) {
            titleList.add(gpsData.getListTitle(titleType, pathOffset))
        }
        return titleList
    }

    /**
     * ソートタイプの設定をおこなう
     * 現ソートタイプと同じであればソート方向を反転する
     * sortType         ソートタイプ
     */
    fun setDataListSortType(sortType: DATALISTSORTTYPE) {
        if (mDataListSortType == sortType) {
            mDataListSortCending = !mDataListSortCending
        } else {
            mDataListSortType = sortType
        }
    }

    /**
     * 対処恵項目のデータをデータファイルから更新する
     * dis@List     選択された表示中のデータNoリスト
     * return       更新データ数
     */
    fun reloadDataFiles(dispList: List<Int>): Int {
        var count = 0
        for(n in dispList) {
            if (!reloadDataFile(n))     //  データファイル読み直す
                count++
        }
        return count
    }

    /**
     * 対象項目のデータをデータファイルから更新する
     * データファイルがない場合は項目を削除
     * dispNo           表示データ位置
     * return           更新の可否
     */
    fun reloadDataFile(dispNo: Int): Boolean {
        var gpsData = mFilterDataList[dispNo]
        if (klib.existsFile(gpsData.mFilePath)) {
            val title = gpsData.mTitle
            val group = gpsData.mGroup
            val color = gpsData.mLineColor
            val comment = gpsData.mComment
            gpsData.loadGpsData()
            gpsData.mTitle = title
            gpsData.mGroup = group
            gpsData.mComment = comment
            gpsData.mLineColor = color
            return true
        } else {
            mDataList.removeAt(dispNo)
            return false
        }
    }

    /**
     * データファイルの存在の有無を確認しなければリストから削除
     */
    fun existDataFileAll(): Int {
        var count = 0
        for (i in mDataList.lastIndex downTo 0) {
            if (!klib.existsFile(mDataList[i].mFilePath)) {
                mDataList.removeAt(i)
                count++
            }
        }
        return count
    }

    /**
     * データファイル名からデータ位置を求める
     * return           データ登録位置
     */
    fun findGpsFile(gpsFilePath: String): Int {
        for (i in mDataList.indices) {
            if (mDataList[i].mFilePath.compareTo(gpsFilePath, true) == 0)
                return i
        }
        return -1
    }

    /**
     * データファイルを結合する
     * addDataNoList        表示リストのデータNoリスト
     */
    fun appendDataFile(addDataNoList: List<Int>) {
        //  データの抽出
        var dataList = mutableListOf<GpsTraceData>()
        for (n in addDataNoList) {
            dataList.add(mFilterDataList[n])
        }
        dataList.sortWith({ a, b -> (a.mFirstTime.time / 1000 - b.mFirstTime.time / 1000).toInt() })
        //  データの結合
        var gpsTraceData = GpsTraceData(dataList[0])
        gpsTraceData.loadGpsTraceData()
        for (n in 1..dataList.lastIndex) {
            gpsTraceData.appendGpsTraceData(dataList[n].mFilePath)
        }
        gpsTraceData.mGpsTraceData.removeAt(0); //  データの重複回避のため1行目を削除
        //  出力ファイル名の作成
        var count = 0
        var filePath = mGpsTraceFileFolder + "/" + klib.getFileNameWithoutExtension(dataList[0].mFilePath) + "(" + count + ").csv"
        while (klib.existsFile(filePath)) {
            count++
            filePath = mGpsTraceFileFolder + "/" + klib.getFileNameWithoutExtension(dataList[0].mFilePath) + "(" + count + ").csv"
        }
        gpsTraceData.saveCsvTraceData(filePath)
        getFileData()
    }

    /**
     * リストデータを取得する
     * exist        ファイル有無の確認(true: ファイルがない場合登録しない)
     */
    fun loadListFile(exist: Boolean = false){
        mDataList.clear()
        var gpsDataList = klib.loadCsvData(mGpsTraceListPath, GpsTraceData.mDataFormat)
        try {
            for (i in gpsDataList.indices) {
                val gpsTraceData = GpsTraceData()
                gpsTraceData.getStringData(gpsDataList[i])
                if (exist && !klib.existsFile(gpsTraceData.mFilePath))  //  ファイルの存在チェック
                    continue
                if (null == mDataList.find {
                        it.mFilePath.compareTo(
                            gpsTraceData.mFilePath,
                            true
                        ) == 0
                    } && //  ファイルの重複チェック
                    null == mDataList.find { it.mFirstTime.time == gpsTraceData.mFirstTime.time })          //  開始時間の重複チェック
                    mDataList.add(gpsTraceData)
            }
        } catch(e: Exception) {
            mErrorMessage = "トレースリスト読み込みエラー" + e.message
        }
    }

    /**
     * リストデータを保存
     */
    fun saveListFile() {
        var gpsDataList = mutableListOf<List<String>>()
        for (i in mDataList.indices) {
            gpsDataList.add(mDataList[i].setStringData())
        }
        klib.saveCsvData(mGpsTraceListPath, GpsTraceData.mDataFormat, gpsDataList)
    }


    /**
     * GPSデータ情報クラス
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
         * 開始時間を文字列で取得
         * return       開始時間の文字列
         */
        fun getFirstTimeStr(): String {
            val tz = Date().getTimezoneOffset() / 60 + 9    //  タイムゾーン(時)
            return klib.date2String(mFirstTime, "yyyy/MM/dd HH:mm:ss", tz)
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
            mTitle          = data[0]
            mGroup          = data[1]
            mCategory       = data[2]
            mComment        = data[3]
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
            data.add(mTitle)
            data.add(mGroup)
            data.add(mCategory)
            data.add(mComment)
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
                    return "自転車"
                }
            } else {
                if (speed < 40.0) {                     //  速度 12-30km/h
                    return "自転車"
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
}