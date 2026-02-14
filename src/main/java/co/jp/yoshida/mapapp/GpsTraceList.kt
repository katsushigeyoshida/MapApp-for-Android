package co.jp.yoshida.mapapp

import android.content.Context
import android.graphics.Canvas
import android.util.Log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.Calendar


//  集計単位
enum class CollectUnit(val menu: String) {
    Time("回"), Day("日"), Week("週"), Month("月");
    //  値の逆引き
    companion object {
        fun lookup(menu: String): CollectUnit {
            return values().find { it.menu == menu } ?: throw IllegalArgumentException()
        }
    }
}


/**
 * ====  GPSのトレースデータ(ファイル)を一覧管理  ====
 */
class GpsTraceList {
    val TAG = "GpsTraceList"

    var mErrorMessage = ""                          //  エラー時の内容

    var mDataList = mutableListOf<GpsTraceData>()   //  GPSリストデータ
    var mFilterDataList = mutableListOf<GpsTraceData>()   //  GPSリストデータ
    var mGpsTraceListFolder = ""                    //  リストデータ保存フォルダ
    val mGpsTraceListFolderName = "GpsTraceList"    //  リストデータ保存フォルダ
    var mGpsTraceListPath = ""                      //  リストデータのファイル保存パス
    var mGpsTraceListCurPath = ""                   //  表示中のリストデータのパス
    var mGpsTraceFileFolder = ""                    //  トレースデータフォルダ
    var mDisp = true                                //  GPSトレース表示フラグ

    enum class DATALISTSORTTYPE {                   //  ソート形式
        Non, DATE, TITLE, DISTANCE, ELEVATOR
    }
    var mDataListSortCending = false                //  ソート方向降順
    var mDataListSortType = DATALISTSORTTYPE.DATE   //  ソート対象

    //  特殊グループ名
    val mAllListName = "すべて"                        //  spinnerの追加タイトル
    val mTrashGroup = "ゴミ箱"                         //  spinnerの追加タイトル

    //  カラーメニュー
    val mColorMenu = listOf("Black", "Red", "Blue", "Green", "Yellow", "White",
        "Cyan", "Gray", "LightGray", "Magenta", "DarkGray", "Transparent")
    //  分類メニュー
    val mCategoryMenu = mutableListOf<String>(
        "散歩", "ウォーキング", "ジョギング", "ランニング", "山歩き",
        "サイクリング", "自転車", "車・バス・鉄道", "飛行機", "旅行")

    lateinit var mC: Context
    val klib = KLib()

    companion object {
        //  GPSデータファイルをGPXに変換する
        var mGpxConvertOn = false                       //  GPXデータ変換フラグ
        val klib = KLib()

        /**
         * GPXファイルに変換する(非同期処理)
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
     * 初期化(保存フォルダ、トレースリストパスの設定、年別リストに変換)
     * gpsTraceFileFolder : GPXファイル保存フォルダ
     * gpsTraceListPath : 旧GPSトレースリストパス(GPSトレースリストフォルダ抽出用)
     */
    fun init(gpsTraceFileFolder: String, gpsTraceListPath: String) {
        mGpsTraceFileFolder = gpsTraceFileFolder
        mGpsTraceListPath   = gpsTraceListPath
        mGpsTraceListFolder = klib.combinedPath(klib.getFolder(gpsTraceListPath), mGpsTraceListFolderName)
        klib.mkdir(mGpsTraceListFolder)
        if (klib.existsFile(gpsTraceListPath)) {
            //  年別のリストに変換
            cnvYearDataFile()
            klib.renameFile(mGpsTraceListPath, mGpsTraceListPath + ".old")
        }
    }

    /**
     * 再帰的にファイルを検索してリストにないデータを追加する
     */
    fun getFileData(maxCount: Int = 0) {
        mErrorMessage = ""
        var year = getListFileCurYear()
        var fileList = klib.getFileList(mGpsTraceFileFolder, false, "GPS_*.csv")
        fileList += klib.getFileList(klib.combinedPath(mGpsTraceFileFolder, year), true, "GPS_*.csv")
        var count = 0
        for (i in fileList.indices) {
            try {
                if (0 < maxCount && maxCount <= count)
                    break;
                if (null == mDataList.find { it.mFilePath.compareTo(fileList[i].absolutePath, true) == 0 }) {  //  ファイル重複チェック
                    var gpsTraceData = GpsTraceData()
                    gpsTraceData.mFilePath = fileList[i].absolutePath
                    gpsTraceData.loadGpsData(false)
                    if (year == "" || 0 <= gpsTraceData.getYearStr().indexOf(year)) {
                        mDataList.add(gpsTraceData)
                        count++
                    }
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
     * 年別のトレースリストファイルの存在する年数リストを取得
     */
    fun getYearFileList(): List<String> {
        var yearList = mutableListOf<String>()
        val fileName = klib.getFileNameWithoutExtension(mGpsTraceListPath) + "_*.csv"
        var yearListFile = klib.getFileList(mGpsTraceListFolder, true, fileName)
        for (path in yearListFile) {
            val year = path.path.substring(path.path.length - 8, path.path.length - 4) +"年"
            if (!yearList.contains(year))
                yearList.add(year)
        }
        val year = klib.getNowDate("yyyy") +"年"
        if (!yearList.contains(year))
            yearList.add(year)
        yearList.sortDescending()
        return yearList
    }

    /**
     * 月リストの取得
     * firstItem        リストの最初に追加するアイテム
     */
    fun getMonthList(year: Int, firstItem:String = ""): List<String> {
        var monthList = mutableListOf<String>()
        for (i in mDataList.indices) {
            val month = mDataList[i].getMonthStr()
            if (!monthList.contains(month))
                monthList.add(month)
        }
        monthList.sortByDescending { klib.str2Integer(it) }
        if (0 < firstItem.length)
            monthList.add(0, firstItem)
        return monthList
    }

    /**
     * 分類リストの取得
     * year         対象年
     * firstItem    リストの最初に追加するアイテム
     */
    fun getCategoryList(year: Int, firstItem:String = ""): List<String> {
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
    fun getGroupList(year: Int, firstTitle: String = ""): List<String> {
        var groupList = mutableListOf<String>()
        for (i in mDataList.indices) {
            if (!groupList.contains(mDataList[i].mGroup))
                groupList.add(mDataList[i].mGroup)
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
//        if (mDisp) {
            for (gpsData in mDataList) {
                if (gpsData.mVisible &&
                    (gpsData.mLocArea.isEmpty() || !mapData.getAreaCoordinates().outside(gpsData.mLocArea))) {
                    if (gpsData.mLocData.size < 1)
                        gpsData.loadGpsData()
                    gpsData.draw(canvas, mapData)
                }
            }
//        }
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
    fun getListTitleData(year: String, month: String, category: String, group: String, titleType: Int = 0, pathOffset: Int = 0): List<String> {
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
                (month.compareTo(mAllListName) == 0 || gpsFileData.getMonthStr().compareTo(month) == 0) &&
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
     * 対象選択項目のデータをデータファイルから更新する
     * dispList     選択された表示中のデータNoリスト
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
        val dataFolder = klib.combinedPath(mGpsTraceFileFolder,  klib.date2String(gpsTraceData.mFirstTime, "yyyy"))
        var filePath = dataFolder + "/" + klib.getFileNameWithoutExtension(dataList[0].mFilePath) + "(" + count + ").csv"
        while (klib.existsFile(filePath)) {
            count++
            filePath = dataFolder + "/" + klib.getFileNameWithoutExtension(dataList[0].mFilePath) + "(" + count + ").csv"
        }
        gpsTraceData.saveCsvTraceData(filePath)
        getFileData()
    }

    /**
     * 最新の年別リストデータファイルを取り込む
     * exist : ファイル有無の確認(true: ファイルがない場合登録しない)
     */
    fun loadListFile(exists: Boolean = false) {
        var gpsTraceListPath = makeListFilePath(klib.getNowDate("yyyy"))
        mDataList = loadListFile(gpsTraceListPath, exists)
    }

    /**
     * 年を指定して年別リストデータファイルを取り込む
     * year : データの年
     * exist : ファイル有無の確認(true: ファイルがない場合登録しない)
     */
    fun loadListFile(year: Int, exists: Boolean = false) {
        var gpsTraceListPath = makeListFilePath(year.toString())
        mDataList = loadListFile(gpsTraceListPath, exists)
    }

    /**
     * トレースリストファイルの読込
     * gpsTraceListPath: トレースリストファイル名
     * exists : データファイルが存在チェック(true:データ削除)
     * return : GpsTraceDataのリスト
     */
    fun loadListFile(gpsTraceListPath:String, exists: Boolean = false): MutableList<GpsTraceData> {
        mGpsTraceListCurPath = gpsTraceListPath
        val dataList = mutableListOf<GpsTraceData>()
        var gpsDataList = klib.loadCsvData(gpsTraceListPath, GpsTraceData.mDataFormat)
        try {
            for (i in gpsDataList.indices) {
                val gpsTraceData = GpsTraceData()
                try {
                    gpsTraceData.getStringData(gpsDataList[i])
                } catch(e: Exception) {
                    mErrorMessage = "データ読込エラー" + e.message
                }
                if (exists && !klib.existsFile(gpsTraceData.mFilePath))  //  ファイルの存在チェック
                    continue
                if (null == dataList.find {
                        it.mFilePath.compareTo(
                            gpsTraceData.mFilePath,
                            true
                        ) == 0
                    } && //  ファイルの重複チェック
                    null == dataList.find { it.mFirstTime.time == gpsTraceData.mFirstTime.time })          //  開始時間の重複チェック
                    dataList.add(gpsTraceData)
            }
        } catch(e: Exception) {
            mErrorMessage = "トレースリスト読み込みエラー" + e.message
        }
        return dataList
    }

    /**
     * リストデータを保存
     */
    fun saveListFile() {
        saveListFile(mGpsTraceListCurPath)
    }

    /**
     * リストデータをファイルに保存
     */
    fun saveListFile(path: String) {
        saveListFile(mDataList, path)
    }

    /**
     * GPSリストデータをテキストに変換して保存
     * dataList : GpsTraceDataリスト
     * path : 保存ファイルパス
     */
    fun saveListFile(dataList: List<GpsTraceData>, path: String) {
        var gpsDataList = mutableListOf<List<String>>()
        for (i in dataList.indices) {
            try {
                gpsDataList.add(dataList[i].setStringData())
            } catch(e: Exception) {
                mErrorMessage = "保存データ作成エラー" + e.message
            }
        }
        klib.saveCsvData(path, GpsTraceData.mDataFormat, gpsDataList)
    }

    /**
     * GPSトレースリストファイルを年別のトレースリストファイルに変換する
     */
    fun cnvYearDataFile() {
        //  リストデータの読込
        var dataList = loadListFile(mGpsTraceListPath)
        if (dataList == null || dataList.count() == 0)
            return
        //  年度別データに変換
        dataList.sortWith({ a, b -> (a.mFirstTime.time / 1000 - b.mFirstTime.time / 1000).toInt() })
        var year = dataList[0].getYearStr()
        var gpsDataList = mutableListOf<List<String>>()
        for (gpsTraceData in dataList) {
            if (year == gpsTraceData.getYearStr()) {
                gpsDataList.add(gpsTraceData.setStringData())
            } else {
                val path = makeListFilePath(year)
                klib.mkdir(klib.getFolder(path))
                klib.saveCsvData(path, GpsTraceData.mDataFormat, gpsDataList)
                year = gpsTraceData.getYearStr()
                gpsDataList = mutableListOf()
                gpsDataList.add(gpsTraceData.setStringData())
            }
        }
        if (0 < gpsDataList.count()) {
            val path = makeListFilePath(year)
            klib.mkdir(klib.getFolder(path))
            klib.saveCsvData(path, GpsTraceData.mDataFormat, gpsDataList)
        }
    }

    /**
     * 年別のリストファイルパスの作成
     *  year   : リストΦの年(2024,2024年)
     *  return : ファイルパス
     */
    fun makeListFilePath(year: String): String {
        val fileName = klib.getFileNameWithoutExtension(mGpsTraceListPath)
        val ext = klib.getNameExt(mGpsTraceListPath)
        return  mGpsTraceListFolder + "/" + fileName +"_" + year.substring(0, 4) + "."+ ext
    }

    /**
     * 現在対象のトレーリストの年(mGpsTraceListCurPath)
     * return : 年 ("2026"..)
     */
    fun getListFileCurYear(): String {
        return mGpsTraceListCurPath.substring(mGpsTraceListCurPath.length - 8, mGpsTraceListCurPath.length - 4)
    }

    /**
     * バックアップフォルダにリストデータを日付を付けてファイル保存する
     */
    fun backupListFile() {
        var folder = klib.getFolder(mGpsTraceListPath) + "/Backup"
        if (klib.mkdir(folder)) {
            var fileName = klib.getFileNameWithoutExtension(mGpsTraceListPath)
            fileName += klib.getNowDate("_yyyyMMddHHmmss.") + klib.getNameExt(mGpsTraceListPath)
            var path = folder + "/" + fileName
            saveListFile(path)
        }
    }


    /**
     * GPSリストデータを対象年と分類でフィルタリングしてからグラフ用データに変換
     * グラフデータでは集計単位にあわせてデータをまとめる
     * グラフデータは集計単位位置をキーとしたMapデータ(kay:集計単位位置,value:処理や時間などの測定データ)
     * year : 抽出年
     * category : 分類
     * collectUnit : 集計単位(回,日,週,月)
     */
    fun getGraphData(year: Int, startMonth:Int, span:Int, category:String, collectUnit: CollectUnit): Map<Int, GpsTraceListGraphData> {
        var mapGpsTraceListGraphData = mutableMapOf<Int, GpsTraceListGraphData>()
        val cl = Calendar.getInstance()
        for (data in mDataList) {
            cl.setTime(data.mFirstTime)
            val month = cl.get(Calendar.MONTH) + 1
            if (cl.get(Calendar.YEAR) == year && (startMonth <= month && month < startMonth + span) &&
                (data.mCategory == category || category == "すべて" || category.isEmpty()) &&
                data.mGroup != "ゴミ箱") {
                val gpsTraceListGraphData = GpsTraceListGraphData()
                gpsTraceListGraphData.setData(data, collectUnit)
                if (!mapGpsTraceListGraphData.isEmpty() && mapGpsTraceListGraphData.containsKey(gpsTraceListGraphData.unitPostion)) {
                    //  同一集計単位のデータはデータ値を加算する
                    gpsTraceListGraphData.addData(mapGpsTraceListGraphData.get(gpsTraceListGraphData.unitPostion) as GpsTraceListGraphData, collectUnit)
                    mapGpsTraceListGraphData[gpsTraceListGraphData.unitPostion] = gpsTraceListGraphData
                } else {
                    //  新規登録
                    mapGpsTraceListGraphData.put(gpsTraceListGraphData.unitPostion, gpsTraceListGraphData)
                }
            }
        }
        return  mapGpsTraceListGraphData
    }

    /**
     * 集計データの取得
     * year : 年, startMonth : 開始月, span : 期間(月), catgory : 分類
     */
    fun getYearData(year: Int, startMonth:Int, span:Int, category: String): GpsTraceListGraphData {
        var yearData = GpsTraceListGraphData()
        val cl = Calendar.getInstance()
        for (data in mDataList) {
            cl.setTime(data.mFirstTime)
            val month = cl.get(Calendar.MONTH) + 1
            if (cl.get(Calendar.YEAR) == year && (startMonth <= month && month < startMonth + span)
                && (data.mCategory == category || category == "すべて" || category.isEmpty()) &&
                data.mGroup != "ゴミ箱") {
                yearData.addData(data)
            }
        }
        return yearData
    }
}


