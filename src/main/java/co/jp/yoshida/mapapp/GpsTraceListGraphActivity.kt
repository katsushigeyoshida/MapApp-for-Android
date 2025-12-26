package co.jp.yoshida.mapapp

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import co.jp.yoshida.mapapp.databinding.ActivityGpsTraceListGraphBinding

class GpsTraceListGraphActivity : AppCompatActivity() {
    val TAG = "GpsTraceListGraphActivity"

    lateinit var binding: ActivityGpsTraceListGraphBinding
    lateinit var mLinearLayoutGraphview: LinearLayout       //  グラフ
    lateinit var mSpYear: Spinner                   //  年
    lateinit var mSpMonth: Spinner                  //  開始月
    lateinit var mSpSpan: Spinner                   //  期間
    lateinit var mSpCollectUnit: Spinner            //  集計単位
    lateinit var mSpDataType: Spinner               //  測定種類
    lateinit var mSpCategory: Spinner               //  分類
    lateinit var mListGraphview: GpsTraceListGraphView       //  グラフ

    val mSpanMenu = listOf("年","半年","3ヶ月","1ヶ月")
    val mCollectUnitMenu = listOf("回","日","週","月")
    val mDataTypeMenu = listOf("移動距離","移動時間","速度","最大高度","累積標高差","歩数", "消費カロリー")

    var mGpsTraceList = GpsTraceList()              //  GPXファイルリスト
    var mGpsTraceFileFolder = ""                    //  GPXファイルリストパス
    var mGpsTraceListPath = ""

    var klib = KLib()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
        setContentView(R.layout.activity_gps_trace_list_graph)

        mGpsTraceFileFolder = intent.getStringExtra("GPSTRACEFOLDER").toString()
        mGpsTraceListPath = intent.getStringExtra("GPSTRACELISTPATH").toString()

        mGpsTraceList.mC = this
        mGpsTraceList.mGpsTraceFileFolder = mGpsTraceFileFolder
        mGpsTraceList.mGpsTraceListPath = mGpsTraceListPath
        mGpsTraceList.loadListFile()
        mListGraphview = GpsTraceListGraphView(this)

        initControl()
    }

    /**
     * コントロールの初期設定
     */
    fun initControl() {
        binding = ActivityGpsTraceListGraphBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mLinearLayoutGraphview = binding.gpsTraceListView
        mSpYear        = binding.spinner4
        mSpMonth       = binding.spinner5
        mSpSpan        = binding.spinner6
        mSpCollectUnit = binding.spinner10
        mSpDataType    = binding.spinner11
        mSpCategory    = binding.spinner12
        mLinearLayoutGraphview.addView(mListGraphview)

        setSpinnerData()        //  Spinnerのデータ設定

        //  年の選択
        mSpYear.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                setSpinnerCategory(getCurYear())
                graphView()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
//                TODO("Not yet implemented")
            }
        }
        //  開始月の選択
        mSpMonth.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                graphView()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
//                TODO("Not yet implemented")
            }
        }
        //  期間の選択
        mSpSpan.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                graphView()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
//                TODO("Not yet implemented")
            }
        }
        //  集計単位の選択(回,日,週,月)
        mSpCollectUnit.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                graphView()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
//                TODO("Not yet implemented")
            }
        }
        //  測定の種類(距離,時間,速度...)
        mSpDataType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                graphView()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
//                TODO("Not yet implemented")
            }
        }
        //  分類の選択(散歩､ジョギング...)
        mSpCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                graphView()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
//                TODO("Not yet implemented")
            }
        }
    }

    /**
     * Spinnerのデータ登録
     */
    fun setSpinnerData() {
        //  データの年をspinnerに登録
        mSpYear.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
            mGpsTraceList.getYearList())
        //  開始月をspinnerに登録
        var monthList = mutableListOf<String>()
        for (i in 1..12)
            monthList.add(i.toString()+"月")
        mSpMonth.adapter = ArrayAdapter(this,
            android.R.layout.simple_spinner_dropdown_item,
            monthList)
        //  表示期間をspinnerに登録
        mSpSpan.adapter = ArrayAdapter(this,
            android.R.layout.simple_spinner_dropdown_item,
            mSpanMenu)
        //  集計単位
        mSpCollectUnit.adapter = ArrayAdapter(this,
            android.R.layout.simple_spinner_dropdown_item,
            mCollectUnitMenu)
        //  データの種別(距離,時間,速度...)
        mSpDataType.adapter = ArrayAdapter(this,
            android.R.layout.simple_spinner_dropdown_item,
            mDataTypeMenu)
        //  分類をspinnerに登録(散歩,ジョギング...)
        setSpinnerCategory(getCurYear())
    }

    fun setSpinnerCategory(year: Int) {
        mSpCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
            mGpsTraceList.getCategoryList(year, mGpsTraceList.mAllListName))
    }

    /**
     * グラフの表示
     */
    fun graphView() {
        mListGraphview.mStartMonth  = getCurMonth()         //  開始月
        mListGraphview.mSpanMonth   = getCurSpan()          //  期間
        mListGraphview.mCollectUnit = getCurCollectUnit()   //  集計単位
        mListGraphview.mDataType    = getCurDataType()      //  データの種別(距離、時間、速度...)
        //  データの取得
        val graphData = mGpsTraceList.getGraphData(getCurYear(), getCurMonth(),
            getCurSpan(), getCurCategory(), getCurCollectUnit())
        var maxData = GraphData()           //  最大値データ
        //  グラフデータへの変換と最大値の取得
        mListGraphview.mGraphData.clear()
        for (data in graphData.values) {
            mListGraphview.mGraphData.add(data)
            if (maxData.dataCount == 0)
                maxData.setData(data)
            else
                maxData.maxData(data)
        }
        //  年間の累積データ
        mListGraphview.mYearData = mGpsTraceList.getYearData(getCurYear(), getCurMonth(), getCurSpan(), getCurCategory())
        //  グラフエリアの設定
        mListGraphview.setGraphArea(getCurYear(), getCurMonth(), getCurSpan(), getCurDataType(), maxData)
        //  表示
        mListGraphview.reDraw()
    }

    /**
     * 対象年(年spinner)の設定値の取得(年)
     */
    fun getCurYear(): Int {
        return mSpYear.selectedItem.toString().substring(0, 4).toInt()
    }

    /**
     * 開始月(月spinner)の設定値の取得(月)
     */
    fun getCurMonth(): Int {
        return klib.str2Integer(mSpMonth.selectedItem.toString())
    }

    /**
     * 期間(期間spinner)の設定値の取得(月)
     */
    fun getCurSpan(): Int {
        return when (mSpSpan.selectedItem.toString()) {
            "年"   -> 12
            "半年"  -> 6
            "3ヶ月" -> 3
            "1ヶ月" -> 1
            else   -> 12
        }
    }

    /**
     * 集計単位の設定値の取得(Time,Day,Week,Month)
     */
    fun getCurCollectUnit(): CollectUnit {
        return CollectUnit.lookup(mSpCollectUnit.selectedItem.toString())
    }

    /**
     * データ種別の設定値の取得(distance,lap...)
     */
    fun getCurDataType():String {
        return mSpDataType.selectedItem.toString()
    }

    /**
     * 分類の設定値の取得(すべて､散歩,ウォーキング...)
     */
    fun getCurCategory(): String {
        return mSpCategory.selectedItem.toString()
    }
}