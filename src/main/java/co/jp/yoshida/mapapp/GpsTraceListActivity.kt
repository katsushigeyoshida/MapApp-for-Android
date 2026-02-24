package co.jp.yoshida.mapapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.util.Consumer
import androidx.core.view.size
import co.jp.yoshida.mapapp.databinding.ActivityGpsTraceListBinding
import kotlin.text.split

/**
 * GPSトレースデータ(CSVファイル)の管理
 */
class GpsTraceListActivity : AppCompatActivity() {
    val TAG = "GpsTraceListActivity"

    lateinit var binding: ActivityGpsTraceListBinding
    lateinit var spYear: Spinner
    lateinit var spMonth: Spinner
    lateinit var spGroup: Spinner
    lateinit var spCategory: Spinner
    lateinit var btUpdate: Button
    lateinit var btTrash: Button
    lateinit var btExport: Button
    lateinit var btAdd: Button
    lateinit var btSort: Button
    lateinit var btRoute: Button
    lateinit var btSelect: Button
    lateinit var btGraph: Button
    lateinit var lvDataList: ListView

    val REQUESTCODE_GPXEDIT = 3
    val REQUESTCODE_GPXFILELIST = 4
    val REQUESTCODE_CSVEDIT = 5

    val mItemClickMenu = listOf<String>(
        "編集", "経路表示", "位置移動", "グラフ表示", "ゴミ箱", "GPXエクスポート"
    )

    val mUpdateMenu = listOf<String>(
        "再表示", "リストファイルの更新", "データファイルのない項目削除", "初期化(データファイル再読込み)",   // "リストファイルバックアップ"
    )
    val mRemoveMenu = listOf<String>(           //  非選択メニュー
        "表示分ゴミ箱", "全ゴミ箱解除", "ゴミ箱から削除"
    )
    val mSelectRemoveMenu = listOf<String>(     //  選択時メニュー
        "ゴミ箱", "ゴミ箱解除", "完全削除"
    )
    val mExportMenu = listOf<String>(           //  選択時メニュー
        "GPXエキスポート", "データファイル移動"
    )
    val mAddListMenu = listOf<String>(

    )
    val mSortMenu = listOf<String>(             //
        "日付順", "タイトル順", "距離順", "標高順", "標準タイトル", "データパスタイトル"
    )
    val mRootDispMenu = listOf<String>(         //  経路表示メニュー
        "全経路表示", "全経路非表示", "経路反転表示"
    )
    val mSelectRootDispMenu = listOf<String>(         //  経路表示メニュー
        "選択経路表示", "経路結合"
    )

    var mSelectListPosition = -1
    var mSelectList = false
    var mGpsTraceList = GpsTraceList()              //  GPXファイルリスト
    var mGpsTraceFileFolder = ""                    //  GPXファイルリストパス
    var mGpsTraceListPath = ""
    var mSpinnerEnabled = true                      //  Spinner の有効可否
    var mListTitleType = 0                          //  表示リストの形式(0:標準タイトル1:データパスタイトル)

    val klib = KLib()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_gps_trace_list)
        title = "GPSトレース"

        mGpsTraceFileFolder = intent.getStringExtra("GPSTRACEFOLDER").toString()
        mGpsTraceListPath = intent.getStringExtra("GPSTRACELISTPATH").toString()

        mGpsTraceList.mC = this
        mGpsTraceList.init(mGpsTraceFileFolder, mGpsTraceListPath)
        mGpsTraceList.loadListFile()
        mGpsTraceList.getFileData(20)
        if (0 < mGpsTraceList.mErrorMessage.length) {
            klib.messageDialog(this, "エラー", mGpsTraceList.mErrorMessage)
            mGpsTraceList.mErrorMessage = ""
        }

        initControl()
    }

    override fun onPause() {
        super.onPause()
        mGpsTraceList.saveListFile()
        val intent = Intent()
        intent.putExtra("年", mGpsTraceList.getListFileCurYear().toInt())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent? ) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
//            REQUESTCODE_CSVEDIT -> {
//                if (resultCode == RESULT_OK) {
//                    mGpsTraceList.loadListFile()
//                    pushSelectItem()
//                    setSpinnerData()
//                    setDataList()
//                    popSelectItem(0)
//                }
//            }
        }
    }

    /**
     * コントロールの初期化とデータの設定
     */
    fun initControl() {
        binding = ActivityGpsTraceListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        spYear     = binding.spinner7
        spMonth    = binding.spinner14
        spGroup    = binding.spinner8
        spCategory = binding.spinner9
        btUpdate   = binding.button17
        btTrash    = binding.button18
        btExport   = binding.button28
        btAdd      = binding.button27
        btSort     = binding.button19
        btRoute    = binding.button20
        btSelect   = binding.button25
        btGraph    = binding.button
        lvDataList = binding.gpsTraceListView

        setSpinnerData()
        setDataList(true)

        //  [データ年]選択でのフィルタ処理
        spYear.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long ) {
                var year = getCurYear()
                if (year < 1900)
                    year = klib.getNowDate("yyyy").toInt()
                mGpsTraceList.clearVisible()
                mGpsTraceList.saveListFile()
                mGpsTraceList.loadListFile(year)

                if (mSpinnerEnabled) {
                    pushSelectItem()
                    setSpinnerMonth(year)
                    setSpinnerGroup(year)
                    setSpinnerCategory(year)
                    setDataList()
                    popSelectItem(1)
                } else
                    mSpinnerEnabled = true
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        //  [データ月]選択でのフィルタ処理
        spMonth.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long ) {
                if (mSpinnerEnabled)
                    setDataList()
                else
                    mSpinnerEnabled = true
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        //  [グループ]選択でのフィルタ処理
        spGroup.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (mSpinnerEnabled)
                    setDataList()
                else
                    mSpinnerEnabled = true
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        //  [分類]選択でのフィルタ処理
        spCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long ) {
                if (mSpinnerEnabled)
                    setDataList()
                else
                    mSpinnerEnabled = true
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }


        /**
         * [選択モード]切替
         */
        btSelect.setOnClickListener {
            mSelectList = !mSelectList
            setDataList()
        }

        /**
         * [経路表示]
         */
        btRoute.setOnClickListener {
            if (mSelectList) {
                klib.setMenuDialog(this, "経路表示設定メニュー", mSelectRootDispMenu, iSelectRouteDispOperatin)
            } else {
                klib.setMenuDialog(this, "経路表示設定メニュー", mRootDispMenu, iRouteDispOperatin)
            }
        }

        /**
         * [ソート]
         */
        btSort.setOnClickListener {
            klib.setMenuDialog(this, "ソートメニュー", mSortMenu, iSortOperatin)
        }

        /**
         * [文字列検索リスト]
         */
        btSort.setOnLongClickListener {
            klib.setInputDialog(this, "検索文字", "", iSearchOperation)
            true
        }

        /**
         * [追加]
         */
        btAdd.setOnClickListener {
            goGpsCsvEdit("")
        }

        /**
         * [エクスポート]
         */
        btExport.setOnClickListener {
            if (mSelectList) {
                klib.setMenuDialog(this, "エキスポートメニュー", mExportMenu, iExportMenuOperation)
            }
        }

        /**
         * [ゴミ箱]
         */
        btTrash.setOnClickListener {
            if (mSelectList) {
                klib.setMenuDialog(this, "削除メニュー", mSelectRemoveMenu, iSelectRemoveOperation)
            } else {
                klib.setMenuDialog(this, "削除メニュー", mRemoveMenu, iRemoveOperation)
            }
        }

        /**
         * GpsTraceListのグラフ表示
         */
        btGraph.setOnClickListener {
            goGpsTraceListGraph()
        }

        /**
         * [データ更新]
         */
        btUpdate.setOnClickListener {
            if (mSelectList) {
                selectDataUpDate()
            } else {
                klib.setMenuDialog(this, "更新メニュー", mUpdateMenu, iUpdateOperatin)
            }
            setDataList()
        }

        //  一覧リストの項目クリックで項目編集
        lvDataList.onItemClickListener = object : AdapterView.OnItemClickListener {
            override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                mSelectListPosition = position
                if (mSelectList)
                    return
                itemClickMenu()
            }
        }

        //  一覧リストの項目長押しで選択モード
        lvDataList.onItemLongClickListener = object : AdapterView.OnItemLongClickListener {
            override fun onItemLongClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long): Boolean {
                //  選択(選択モードの変更)
//                mSelectList = !mSelectList
//                setDataList()
//                return true     //    Short Click無効
                return false
            }
        }
    }


    //  GPXトレースの経路表示処理
    var iRouteDispOperatin = Consumer<String> { s ->
        if (s.compareTo(mRootDispMenu[0]) == 0) {
            //  全経路表示
            mGpsTraceList.setVisibleAll()
        } else if (s.compareTo(mRootDispMenu[1]) == 0) {
            //  全経路非表示
            mGpsTraceList.clearVisible()
        } else if (s.compareTo(mRootDispMenu[2]) == 0) {
            //  反転
            mGpsTraceList.reverseVisible()
        } else if (s.compareTo(mRootDispMenu[3]) == 0) {
            //  選択モードに変更
            if (mSelectList) {
                mGpsTraceList.setVisible(selectItemListNo())    //  表示のチェック設定
                mSelectList = false
            }
        } else
            return@Consumer
        setDataList()
    }

    //  選択したGPXトレースの経路設定、経路結合
    var iSelectRouteDispOperatin = Consumer<String> { s ->
        if (s.compareTo(mSelectRootDispMenu[0])== 0) {
            //  選択経路設定
            var gpsTraceListNo = selectItemListNo()
            for(n in gpsTraceListNo)
                mGpsTraceList.mFilterDataList[n].mVisible = true
            mSelectList = !mSelectList
        } else if (s.compareTo(mSelectRootDispMenu[1])== 0) {
            //  経路結合
            var gpsTraceListNo = selectItemListNo()
            mGpsTraceList.appendDataFile(gpsTraceListNo)
            mSelectList = !mSelectList
        } else
            return@Consumer
        setDataList()
    }

    //  ソートの設定
    var iSortOperatin = Consumer<String> { s ->
        if (s.compareTo(mSortMenu[0])== 0) {
            //  日付順
            mGpsTraceList.setDataListSortType(GpsTraceList.DATALISTSORTTYPE.DATE)
        } else if (s.compareTo(mSortMenu[1])== 0) {
            //  タイトル順
            mGpsTraceList.setDataListSortType(GpsTraceList.DATALISTSORTTYPE.TITLE)
        } else if (s.compareTo(mSortMenu[2])== 0) {
            //  距離順
            mGpsTraceList.setDataListSortType(GpsTraceList.DATALISTSORTTYPE.DISTANCE)
        } else if (s.compareTo(mSortMenu[3])== 0) {
            //  標高順
            mGpsTraceList.setDataListSortType(GpsTraceList.DATALISTSORTTYPE.ELEVATOR)
        } else if (s.compareTo(mSortMenu[4])== 0) {
            //  標準タイトル
            mListTitleType = 0
        } else if (s.compareTo(mSortMenu[5])== 0) {
            //  データパスタイトル
            mListTitleType = 1
        }
        setDataList()
    }

    //  文字列検索リスト表示
    var iSearchOperation = Consumer<String> { s ->
        setDataList(false, s)
    }

    //  エキスポート・移動
    var iExportMenuOperation = Consumer<String> { s ->
        if (s.compareTo(mExportMenu[0]) == 0) {
            //  エキスポート
            klib.folderSelectDialog(this, mGpsTraceFileFolder, iGpsFilesExportOperation)
        } else if (s.compareTo(mExportMenu[1]) == 0) {
            //  データファイルの移動
            klib.folderSelectDialog(this, mGpsTraceFileFolder, iDataFilesMove)
        }
    }

    //  GPXエクスポートで選択したフォルダへ変換
    var iGpsFilesExportOperation = Consumer<String> { s ->
        if (0 < s.length) {
            selectExport(s)
        }
    }

    /**
     * 選択された項目のデータを指定フォルダにGPX変換ファイルを出力
     * outFolder            出力先フォルダ
     */
    fun selectExport(outFolder: String) {
        var gpsTraceList = selectItemList()
        GpsTraceList.gpxExport(gpsTraceList, outFolder)
        if (0 < gpsTraceList.size)
            Toast.makeText(this, gpsTraceList.size.toString() + " 個のデータをエクスポート", Toast.LENGTH_LONG).show()
        mSelectList = false
        setDataList()
    }

    //  データファイルの移動
    var iDataFilesMove = Consumer<String> { s ->
        if (0 < s.length) {
            dataFilesMove(s)
        }
    }

    /**
     * 選択されたデータファイルの移動
     * outFolder        移動先フォルダ
     */
    fun dataFilesMove(moveFolder: String) {
        var gpsTraceListNo = selectItemListNo()
        mGpsTraceList.gpxFilesMove(gpsTraceListNo, moveFolder)
        if (0 < gpsTraceListNo.size)
            Toast.makeText(this,
                gpsTraceListNo.size.toString() + " 個のデータを移動",
                Toast.LENGTH_LONG).show()
        mSelectList = false
        setDataList()
    }

    //  ゴミ箱・削除処理
    var iRemoveOperation = Consumer<String> { s ->
        if (s.compareTo(mRemoveMenu[0]) == 0) {
            //  表示全ゴミ箱
            mGpsTraceList.setTrashData(dispItemlistNo())
            setDataList()
        } else if (s.compareTo(mRemoveMenu[1]) == 0) {
            //  全ゴミ箱解除
            mGpsTraceList.setAllUnTrashData()
            setDataList()
        } else if (s.compareTo(mRemoveMenu[2]) == 0) {
            //  ゴミ箱から削除
            klib.messageDialog(
                this, "確認", "ゴミ箱の全データをファイルごと削除します",
                iAllTrashRemoveOperation)
        }
    }

    //  選択項目のゴミ箱または削除処理
    var iSelectRemoveOperation = Consumer<String> { s ->
        if (s.compareTo(mSelectRemoveMenu[0]) == 0) {
            //  選択ゴミ箱
            if (mSelectList) {
                mGpsTraceList.setTrashData(selectItemListNo())
                setSpinnerData()
                mSelectList = false
                setDataList()
            }
        } else if (s.compareTo(mSelectRemoveMenu[1]) == 0) {
            //  選択ゴミ箱解除
            if (mSelectList) {
                mGpsTraceList.setUnTrashData(selectItemListNo())
                mSelectList = false
                setDataList()
            }
        } else if (s.compareTo(mSelectRemoveMenu[2]) == 0) {
            //  選択削除
            if (mSelectList) {
                var gpsTraceList = selectItemList()
                if (0 < gpsTraceList.size) {
                    val fileName = klib.getName(gpsTraceList[0].mFilePath)
                    klib.messageDialog(
                        this, "確認", fileName + "など\n選択した項目をデータファイルごと削除します",
                        iFileRemoveOperation)
                }
            }
        }
    }

    //  選択表示項目を削除
    var iFileRemoveOperation = Consumer<String> { s ->
        if (s.compareTo("OK") == 0) {
            mGpsTraceList.removeDataFile(selectItemList())
            mSelectList = false
            setDataList()
        }
    }

    //  表示されているすべてのゴミ箱の項目を削除
    var iAllTrashRemoveOperation = Consumer<String> { s ->
        if (s.compareTo("OK") == 0) {
            mGpsTraceList.removeTrashDataFile(dispItemList())
            setDataList()
        }
    }

    /**
     * 選択項目をファイルからデータを更新する
     * データファイルの再読み込みでデータを更新
     */
    fun selectDataUpDate() {
        var gpsTraceListNo = selectItemListNo()
        var count = mGpsTraceList.reloadDataFiles(gpsTraceListNo)
        if (0 < count)
            Toast.makeText(this,
                count.toString() + " 個のデータを更新しました",
                Toast.LENGTH_LONG).show()
        mSelectList = !mSelectList
        setDataList()
    }

    //  データ項目の更新
    var iUpdateOperatin = Consumer<String> { s ->
        if (s.compareTo(mUpdateMenu[0]) == 0) {
            //  再表示
            mSelectList = false
            setDataList()
        } else if (s.compareTo(mUpdateMenu[1]) == 0) {
            //  データファイルの更新
            klib.messageDialog(this, "確認", "リストの更新をします", iItemUpdateOperation)
        } else if (s.compareTo(mUpdateMenu[2]) == 0) {
            //  データファイル確認
            val n = mGpsTraceList.existDataFileAll()
            Toast.makeText(this,
                "データのない " + n.toString() + " 個の項目を削除しました",
                Toast.LENGTH_LONG).show()
            setDataList()
        } else if (s.compareTo(mUpdateMenu[3]) == 0) {
            //  初期化(データファイル読み直し)
            klib.messageDialog(this, "確認", "リストの全項目を初期化します", iAllItemInitOperation)
        } else if (s.compareTo(mUpdateMenu[4]) == 0) {
            //  リストファイルバックアップ
            mGpsTraceList.backupListFile()
        }
    }

    //  すべての項目を初期化する(データファイル読み直し)
    var iAllItemInitOperation = Consumer<String> { s ->
        if (s.compareTo("OK") == 0) {
            mGpsTraceList.mDataList.clear()
            mGpsTraceList.getFileData()
            if (0 < mGpsTraceList.mErrorMessage.length)
                klib.messageDialog(this, "エラー", "データの読込に失敗\n"+mGpsTraceList.mErrorMessage)
            setDataList()
        }
    }

    //  データの更新(未登録データの登録)
    var iItemUpdateOperation = Consumer<String> { s ->
        if (s.compareTo("OK") == 0) {
            mGpsTraceList.getFileData()
            if (0 < mGpsTraceList.mErrorMessage.length)
                klib.messageDialog(this, "エラー", "データの読込に失敗\n"+mGpsTraceList.mErrorMessage)
            setDataList()
        }
    }

    /**
     * 項目クリック処理メニュー表示
     */
    fun itemClickMenu()  {
        klib.setMenuDialog(this, "操作メニュー", mItemClickMenu, iItemClickOperation)
    }

    //  項目クリック処理のインターフェース関数
    var iItemClickOperation = Consumer<String> { s ->
        val n = mSelectListPosition
        if (0 <= n) {
            if (s.compareTo(mItemClickMenu[0]) == 0) {
                //  編集
                if (0 <= n)
                    goGpsCsvEdit(mGpsTraceList.mFilterDataList[n].mFilePath)
            } else if (s.compareTo(mItemClickMenu[1]) == 0) {
                //  経路表示設定
                if (0 <= n) {
                    mGpsTraceList.clearVisible()
                    mGpsTraceList.mFilterDataList[n].mVisible = true
                    val coordinate = mGpsTraceList.mFilterDataList[n].mLocArea.center().toString()
                    mapMove(coordinate)
                }
            } else if (s.compareTo(mItemClickMenu[2]) == 0) {
                //  位置移動
                if (0 <= n) {
                    val coordinate = mGpsTraceList.mFilterDataList[n].mLocArea.center().toString()
                    mapMove(coordinate)
                }
            } else if (s.compareTo(mItemClickMenu[3]) == 0) {
                //  グラフ表示
                if (0 <= n) {
                    val gpsPath = mGpsTraceList.mFilterDataList[n].mFilePath
                    val title = mGpsTraceList.mFilterDataList[n].mTitle
                    goGpsGraph(gpsPath, title)
                }
            } else if (s.compareTo(mItemClickMenu[4]) == 0) {
                //  ゴミ箱
                if (0 <= n) {
                    mGpsTraceList.setTrashData(n)
                    setSpinnerData()
                    setDataList()
                }
            } else if (s.compareTo(mItemClickMenu[5]) == 0) {
                //  GPXエクスポート
                gpxExport(n)
            }
        }
    }

    /**
     * 指定のデータをGPXファイルに変換してエクスポートする
     * pos          データの表示位置
     */
    fun gpxExport(pos: Int) {
        mSelectListPosition = pos
        klib.folderSelectDialog(this, mGpsTraceFileFolder, iGpsExportOperation)
    }

    //  指定のフォルダーにGPXファイルを出力インターフェース関数
    var iGpsExportOperation = Consumer<String> { s ->
        if (0 <= mSelectListPosition) {
            var gpsTraceList = mutableListOf<GpsTraceData>()
            gpsTraceList.add(mGpsTraceList.mFilterDataList[mSelectListPosition])
            GpsTraceList.gpxExport(gpsTraceList, s)
        }
    }


    /**
     * GPSリストの項目編集画面に移行する
     * gxpFilePath      編集する項目のファイルパス
     */
    fun goGpsCsvEdit(gpsTraceFilePath: String) {
        val intent = Intent(this, GpxEditActivity::class.java)
        intent.putExtra("GPSTRACELISTPATH", mGpsTraceListPath)
        intent.putExtra("GPSTRACELISTCURPATH", mGpsTraceList.mGpsTraceListCurPath)
        intent.putExtra("GPSTRACEFILEPATH", gpsTraceFilePath)
        gpsCsvEditActivityLuncher.launch(intent)
    }

    /**
     * goGpsCsvEditの実行終了後の処理
     */
    private val gpsCsvEditActivityLuncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            mGpsTraceList.mDataList = mGpsTraceList.loadListFile(mGpsTraceList.mGpsTraceListCurPath)
            pushSelectItem()
            setSpinnerData()
            setDataList()
            popSelectItem(0)
        }
    }

    /**
     * GPSファイルのグラフ表示画面に移行する
     * gpsPath          GPSファイルのパス
     * title            グラフのタイトル
     */
    fun goGpsGraph(gpsPath: String, title: String) {
        if (klib.existsFile(gpsPath)) {
            val intent = Intent(this, GpsGraph::class.java)
            intent.putExtra("FILE", gpsPath)
            intent.putExtra("TITLE", title)
            startActivity(intent)
        }
    }

    /**
     * GPSトレースリストのグラフ表示
     */
    fun goGpsTraceListGraph() {
        val intent = Intent(this, GpsTraceListGraphActivity::class.java)
        intent.putExtra("GPSTRACEFOLDER", mGpsTraceFileFolder)
        intent.putExtra("GPSTRACELISTPATH", mGpsTraceListPath)
        startActivity(intent)
    }

    /**
     * 地図の中心を設定して地図表示に戻る
     * coordinate       位置座標文字列
     */
    fun mapMove(coordinate: String) {
        if (0 < coordinate.length) {
            if (0 < coordinate.length) {
                val intent = Intent()
                intent.putExtra("年", mGpsTraceList.getListFileCurYear().toInt())
                intent.putExtra("座標", coordinate)
                setResult(RESULT_OK, intent)
                finish()
            }
        }
    }

    /**
     * 選択されたItemリストの作成
     * return   GpsTraceDataリスト
     */
    fun selectItemList():List<GpsTraceData> {
        var checked = lvDataList.checkedItemPositions
        var gpsTraceList = mutableListOf<GpsTraceData>()
        for (i in 0..checked.size()-1){
            if (checked.valueAt(i)) {
                gpsTraceList.add(mGpsTraceList.mFilterDataList[checked.keyAt(i)])
            }
        }
        return gpsTraceList
    }

    /**
     * 表示リストの全データのリスト取得
     * return       データリスト
     */
    fun dispItemList():List<GpsTraceData> {
        var dataList = mutableListOf<GpsTraceData>()
        for (i in 0..lvDataList.size - 1){
            dataList.add(mGpsTraceList.mFilterDataList[i])
        }
        return dataList
    }

    /**
     * listViewに表示している項目をDataListの位置に変換する
     * return       データ位置リスト(List<Int>)
     */
    fun dispItemlistNo(): List<Int> {
        var dataList = mutableListOf<Int>()
        for (i in 0..lvDataList.size - 1) {
            dataList.add(i)
        }
        return dataList
    }

    /**
     * 選択されたItemのNoリストを作成
     * return       ItemNoリスト
     */
    fun selectItemListNo():List<Int> {
        var checked = lvDataList.checkedItemPositions
        var gpsTraceListNo = mutableListOf<Int>()
        for (i in 0..checked.size()-1){
            if (checked.valueAt(i)) {
                gpsTraceListNo.add(checked.keyAt(i))
            }
        }
        return gpsTraceListNo
    }

    /**
     * 年、分類、グループデータをspinnerに登録
     */
    fun setSpinnerData(){
        //  データの年をspinnerに登録
        spYear.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
            mGpsTraceList.getYearFileList())
        //  データの月をspinnerに登録
        setSpinnerMonth(getCurYear())
        //  グループをspinnerに登録
        setSpinnerGroup(getCurYear())
        //  分類をspinnerに登録
        setSpinnerCategory(getCurYear())
    }

    /**
     * 一覧リストを設定し、分類(spinner)の選択位置を設定
     *  firstYearPos 年の選択位置を1にする(最新年)
     *  sword        検索文字列
     */
    fun setDataList(firstYearPos: Boolean = false, sword:String ="") {
        val year     = spYear.selectedItem.toString()
        val month    = spMonth.selectedItem.toString()
        val group    = spGroup.selectedItem.toString()
        val category = spCategory.selectedItem.toString()
        var traceFolderLength = mGpsTraceFileFolder.lastIndexOf('/', mGpsTraceFileFolder.lastIndexOf('/') - 1) + 1

        if (mSelectList) {
            //  選択リスト
            var listTitleAdapter = ArrayAdapter(this, R.layout.my_simple_list_item_checked,
                mGpsTraceList.getListTitleData(year, month, category, group, sword, mListTitleType, traceFolderLength))
            lvDataList.choiceMode = ListView.CHOICE_MODE_MULTIPLE
            lvDataList.adapter = listTitleAdapter
            //  visibleをcheckに設定
            lvDataList.clearChoices()
            btExport.isEnabled = true
        } else {
            //  通常リスト
            var listTitleAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1,
                mGpsTraceList.getListTitleData(year, month, category, group, sword, mListTitleType, traceFolderLength))
            lvDataList.adapter = listTitleAdapter
            btExport.isEnabled = false
        }

        //  選択値を元に戻す
        val yearPos     = if (firstYearPos && 0 < spYear.adapter.count) 0
                            else mGpsTraceList.getYearFileList().indexOf(year)
        val monthPos    = mGpsTraceList.getMonthList(getCurYear(), mGpsTraceList.mAllListName).indexOf(month)
        val groupPos    = mGpsTraceList.getGroupList(getCurYear(), mGpsTraceList.mAllListName).indexOf(group)
        val categoryPos = mGpsTraceList.getCategoryList(getCurYear(), mGpsTraceList.mAllListName).indexOf(category)

        if (0 <= yearPos) spYear.setSelection(yearPos)
        if (0 <= monthPos) spMonth.setSelection(monthPos)
        if (0 <= groupPos) spGroup.setSelection(groupPos)
        if (0 <= categoryPos) spCategory.setSelection(categoryPos)

        title = "GPSトレースデータ(${lvDataList.adapter.count})"
    }

    /**
     * 設定されているspinnerの念を取得(「すべて」は0とする)
     */
    fun getCurYear(): Int {
        if (spYear.selectedItemPosition < 1)
            return 0
        else
            return spYear.selectedItem.toString().substring(0, 4).toInt()
    }

    /**
     * monthのspinner設定を年に合わせて設定する
     */
    fun setSpinnerMonth(year: Int) {
        spMonth.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
            mGpsTraceList.getMonthList(year,mGpsTraceList.mAllListName))
    }

    /**
     * categoryのspinner設定を年に合わせて設定する
     */
    fun setSpinnerGroup(year: Int) {
        spGroup.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
            mGpsTraceList.getGroupList(year,mGpsTraceList.mAllListName))
    }

    /**
     * categoryのspinner設定を年に合わせて設定する
     */
    fun setSpinnerCategory(year: Int) {
        spCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
            mGpsTraceList.getCategoryList(year,mGpsTraceList.mAllListName))
    }


    private  var mYearSelectItem = ""
    private  var mMonthSelectItem = ""
    private  var mGroupSelectItem = ""
    private  var mCategorySelectItem = ""

    /**
     * 年、月、グループ、分類の選択値を保存
     */
    fun pushSelectItem() {
        mYearSelectItem     = spYear.selectedItem.toString()
        mMonthSelectItem    = spMonth.selectedItem.toString()
        mGroupSelectItem    = spGroup.selectedItem.toString()
        mCategorySelectItem = spCategory.selectedItem.toString()
    }

    /**
     * 年、月、グループ、分類の選択値を戻す
     *   level 0:すべて 1:年をのぞく 2:年と月を除く3:年、月、グループを除く 3: すべてを除く
     */
    fun popSelectItem(level: Int = 1) {
        val yearPos     = mGpsTraceList.getYearFileList().indexOf(mYearSelectItem)
        val monthPos    = mGpsTraceList.getMonthList(getCurYear(), mGpsTraceList.mAllListName).indexOf(mMonthSelectItem)
        val groupPos    = mGpsTraceList.getGroupList(getCurYear(), mGpsTraceList.mAllListName).indexOf(mGroupSelectItem)
        val categoryPos = mGpsTraceList.getCategoryList(getCurYear(), mGpsTraceList.mAllListName).indexOf(mCategorySelectItem)
        if (level < 1 && 0 <= yearPos)     spYear.setSelection(yearPos)
        if (level < 2 && 0 <= monthPos)    spMonth.setSelection(monthPos)
        if (level < 3 && 0 <= groupPos)    spGroup.setSelection(groupPos)
        if (level < 4 && 0 <= categoryPos) spCategory.setSelection(categoryPos)
    }

}