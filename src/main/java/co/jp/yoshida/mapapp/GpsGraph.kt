package co.jp.yoshida.mapapp

import android.graphics.Insets
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import co.jp.yoshida.mapapp.databinding.ActivityGpsGraphBinding

class GpsGraph : AppCompatActivity() {
    val TAG = "GpsGraph"

    val klib = KLib()

    lateinit var mGpsGraphView: GpsGraphView
    lateinit var binding: ActivityGpsGraphBinding
    lateinit var linearLayoutGraphview: LinearLayout
    lateinit var spVertical: Spinner
    lateinit var spHorizontal: Spinner
    lateinit var spAverageCount: Spinner
    lateinit var btZoomUp: Button
    lateinit var btZoomDown: Button
    lateinit var btMoveLeft: Button
    lateinit var btMoveRight: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
        setContentView(R.layout.activity_gps_graph)

        val filePath = intent.getStringExtra("FILE")
        val gpsTitle = intent.getStringExtra("TITLE")
        title = klib.getFileNameWithoutExtension(gpsTitle.toString())

        var gpxReader = GpxReader(GpxReader.DATATYPE.gpxData)
        if (0 < filePath.toString().indexOf(".csv")) {
            //  csvファイルからGPSデータの取得
            var gpsTrace = GpsTrace()
            var gpsData = gpsTrace.loadGpsData(filePath.toString()) //  CSV形式のファイルからGPSデータを読み込む
            if (0 < gpsData.size) {
                gpxReader.location2GpsData(gpsData, gpsTrace.mStepCount, false)
            } else {
                Toast.makeText(this, "データがありません", Toast.LENGTH_LONG).show()
                return
            }
        } else if (0 < filePath.toString().indexOf(".gpx")) {
            //  gpxファイルからGPSデータの取得
            if (0 > gpxReader.getGpxRead(filePath.toString())) {
                Toast.makeText(this, "データがありません", Toast.LENGTH_LONG).show()
                return
            }
        } else {
            Toast.makeText(this, "データがありません", Toast.LENGTH_LONG).show()
            return
        }

        gpxReader.setGpsInfoData()
        mGpsGraphView = GpsGraphView(this, gpxReader)

        //  初期化
        initControl()

        //  API30以上でのScreenSizek取得
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = this.windowManager.currentWindowMetrics
            val insets: Insets = windowMetrics.windowInsets
                .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
            val ScreenWidth = windowMetrics.bounds.width()
            val ScreenHeight = windowMetrics.bounds.height()
            val StatusBar = insets.top
            val NavigationBar = insets.bottom
            Log.d(TAG,"onCreate: ScreenSize "+ScreenWidth+" "+ScreenHeight+" "+StatusBar+" "+NavigationBar)
            mGpsGraphView.setInitGraphScreen(ScreenWidth, ScreenHeight - 600)    //  600はGraphエリア以外
        }
    }


    //  初期化処理
    fun initControl() {
        binding = ActivityGpsGraphBinding.inflate(layoutInflater)
        setContentView(binding.root)
        linearLayoutGraphview = binding.linearLayoutGraphview
        spVertical     = binding.spVertical
        spHorizontal   = binding.spHorizontal
        spAverageCount = binding.spAverageCount
        btZoomUp       = binding.button29
        btZoomDown     = binding.button30
        btMoveLeft     = binding.button31
        btMoveRight    = binding.button32

        linearLayoutGraphview.addView(mGpsGraphView)

        val context = this

        //  縦軸のデータ種別の設定
        spVertical.adapter = ArrayAdapter(this,
            android.R.layout.simple_spinner_dropdown_item, mGpsGraphView.mGraphYType)
        var pos = klib.getIntPreferences("GpsGraphYTypeTitle", 0, this)
        mGpsGraphView.mYTypeTitle = mGpsGraphView.mGraphYType[pos]
        spVertical.setSelection(pos)
        //  横軸のデータ種別の設定
        spHorizontal.adapter = ArrayAdapter(this,
            android.R.layout.simple_spinner_dropdown_item, mGpsGraphView.mGraphXType)
        pos = klib.getIntPreferences("GpsGraphXTypeTitle", 0, this)
        mGpsGraphView.mXTypeTitle = mGpsGraphView.mGraphXType[pos]
        spHorizontal.setSelection(pos)
        //  移動平均のデータ数の設定
        spAverageCount.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item, mGpsGraphView.mAverageCount
        )
        pos = klib.getIntPreferences("GpsGraphAverageCount", 0, this)
        mGpsGraphView.mAverageCountTitle = mGpsGraphView.mAverageCount[pos]
        spAverageCount.setSelection(pos)

        //  縦軸のデータ種別の処理
        spVertical.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                mGpsGraphView.mYTypeTitle = mGpsGraphView.mGraphYType[position]
                klib.setIntPreferences(position, "GpsGraphYTypeTitle", context)
                mGpsGraphView.reDraw()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }
        }

        //  横軸のデータ種別の処理
        spHorizontal.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                mGpsGraphView.mXTypeTitle = mGpsGraphView.mGraphXType[position]
                klib.setIntPreferences(position, "GpsGraphXTypeTitle", context)
                mGpsGraphView.reDraw()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }
        }

        //  移動平均のデータの処理
        spAverageCount.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                mGpsGraphView.mAverageCountTitle = mGpsGraphView.mAverageCount[position]
                klib.setIntPreferences(position, "GpsGraphAverageCount", context)
                mGpsGraphView.reDraw()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }
        }

        /**
         * [⊕]ボタン 横方向拡大
         */
        btZoomUp.setOnClickListener {
            mGpsGraphView.zoomDisp(2.0)
            mGpsGraphView.reDraw()
        }

        /**
         * [⊖]ボタン 横方向縮小
         */
        btZoomDown.setOnClickListener {
            mGpsGraphView.zoomDisp(0.5)
            mGpsGraphView.reDraw()
        }

        /**
         * グラフを左に移動
         */
        btMoveLeft.setOnClickListener {
            mGpsGraphView.moveDisp(-0.5)
            mGpsGraphView.reDraw()
        }

        /**
         * グラフを右に移動
         */
        btMoveRight.setOnClickListener {
            mGpsGraphView.moveDisp(0.5)
            mGpsGraphView.reDraw()
        }
    }}