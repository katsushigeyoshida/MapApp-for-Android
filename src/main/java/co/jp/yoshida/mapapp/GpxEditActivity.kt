package co.jp.yoshida.mapapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.util.Consumer
import co.jp.yoshida.mapapp.databinding.ActivityGpxEditBinding

/**
 * 他のアプリで作成されたGPXファイルを登録・編集する
 * 1.新規に開いた時はGPXファイルを選択して登録
 * 2.既存データの登録項目の変種
 * 3.共有で外部アプリからの呼び出しに対応
 */
class GpxEditActivity : AppCompatActivity() {
    val TAG = "GpxEditActivity"

    lateinit var binding: ActivityGpxEditBinding
    lateinit var constraintLayout: ConstraintLayout
    lateinit var edTitle: EditText
    lateinit var edGroup: EditText
    lateinit var edGpxPath: EditText
    lateinit var edComment: EditText
    lateinit var tvYear: TextView
    lateinit var tvGpxInfo: TextView
    lateinit var btGruopRef: Button
    lateinit var btGpxPathRef: Button
    lateinit var btGraph: Button
    lateinit var btOK: Button
    lateinit var btCancel: Button
    lateinit var spColor: Spinner
    lateinit var spCategory: Spinner

    var mGpxDataListPath = ""                           //  GPXファイルリストパス
    var mGpxFilePath = ""                               //  GPXファイルパス
    var mGpxFilePos = -1                                //  選択されたGPXファイル位置
    var mNewFile = false                                //  新規登録

    var mGpsTraceList = GpsTraceList()                   //  GPXファイルリスト
    val klib = KLib()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
        setContentView(R.layout.activity_gpx_edit)

        this.title = "GPSファイル登録"

        mGpxDataListPath = klib.getStrPreferences("GpsTraceListPath", this).toString()
        mGpsTraceList.mGpsTraceListPath = mGpxDataListPath
        mGpsTraceList.loadListFile()

        val intent = getIntent()
        val action = intent.getAction();
        var gpxTitle = ""
        if(Intent.ACTION_VIEW.equals(action)) {
            //  外部アプリから共有で起動された場合(共有 FileProvider使用)
            val type = intent.type      //  mime type [application/gpx]
            val data = intent.data      //  URI
            if (data != null) {
                mGpxFilePath = klib.getUriPath(this, data)
            }
        } else if(Intent.ACTION_SEND.equals(action)) {
            //  外部アプリ(gpsInfo)から共有で起動された場合
            val extras = intent.getExtras()
            if (extras != null) {
                mGpxFilePath = extras.getCharSequence(Intent.EXTRA_TEXT).toString()
                gpxTitle = extras.getCharSequence(Intent.EXTRA_TITLE).toString()
            }
        } else {
            //  内部からの呼び出し
            mGpxDataListPath = intent.getStringExtra("GPSTRACELISTPATH").toString()
            mGpxFilePath = intent.getStringExtra("GPSTRACEFILEPATH").toString()
            if (mGpxFilePath.length == 0 && !klib.existsFile(mGpxFilePath))
                mNewFile = true
        }

        initControl()
        setDataGpxFile(mGpxFilePath, gpxTitle)
    }


    /**
     * コントロールの初期化
     */
    fun initControl() {
        binding = ActivityGpxEditBinding.inflate(layoutInflater)
        setContentView(binding.root)
        constraintLayout = binding.main
        edTitle      = binding.editTextTextPersonName13
        edGroup      = binding.editTextTextPersonName14
        edGpxPath    = binding.editTextTextPersonName15
        edComment    = binding.editTextTextPersonName16
        tvGpxInfo    = binding.textView31
        tvYear       = binding.textView32
        btGruopRef   = binding.button7
        btGpxPathRef = binding.button8
        btGraph      = binding.button15
        btOK         = binding.button9
        btCancel     = binding.button10
        spColor      = binding.spinner2
        spCategory   = binding.spinner3

        edTitle.setText("")
        edGroup.setText("")
        edGpxPath.setText("")
        edComment.setText("")
        tvGpxInfo.setText("")
        if (mNewFile)
            btGpxPathRef.isEnabled = true
        else
            btGpxPathRef.isEnabled = false

        //  線分の色のSpinner
        var colorAdapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, mGpsTraceList.mColorMenu
        )
        spColor.adapter = colorAdapter

        //  分類のSpinner
        var categoryAdapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, mGpsTraceList.mCategoryMenu)
        spCategory.adapter = categoryAdapter

        //  グループ設定
        btGruopRef.setOnClickListener {
            var groupList = mGpsTraceList.getGroupList("")
            klib.setMenuDialog(this, "グループ", groupList, iGpxGroup)
        }

        //  GPXファイルをパスの選択と設定
        btGpxPathRef.setOnClickListener {
            var gpxFileFolder = klib.getStrPreferences("GpxFileFolder", this)
            if (gpxFileFolder == null || !klib.isDirectory(gpxFileFolder))
                gpxFileFolder = klib.getPackageNameDirectory(this)
            //  ファイル選択
            klib.fileSelectDialog(this, gpxFileFolder, "*.gpx", true, iGpxFilePath)
        }

        //  GPXファイルをグラフ表示する
        btGraph.setOnClickListener {
            val gpxPath = edGpxPath.text.toString()
            val title = edTitle.text.toString()
            goGpsGraph(gpxPath, title)
        }

        //  登録処理
        btOK.setOnClickListener {
            if (mNewFile && 0 <= mGpsTraceList.findGpsFile(edGpxPath.text.toString()) ){
                klib.messageDialog(this, "確認", "既にファイルが登録されています")
            } else {
                val gpsFileData = GpsTraceList.GpsTraceData()
                gpsFileData.mFilePath = edGpxPath.text.toString()
                gpsFileData.loadGpsData(false)
                gpsFileData.mTitle = edTitle.text.toString()
                gpsFileData.mGroup = edGroup.text.toString()
                gpsFileData.mCategory = mGpsTraceList.mCategoryMenu[spCategory.selectedItemPosition]
                gpsFileData.mLineColor = mGpsTraceList.mColorMenu[spColor.selectedItemPosition]
                gpsFileData.mComment = edComment.text.toString()
                if (0 <= mGpxFilePos) {
                    mGpsTraceList.mDataList[mGpxFilePos] = gpsFileData
                } else {
                    mGpsTraceList.mDataList.add(gpsFileData)
                }
                mGpsTraceList.saveListFile()

                setResult(RESULT_OK)
                finish()
            }
        }

        //  キャンセル
        btCancel.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    //  グループ名をコントロールに設定する関数インターフェース
    var iGpxGroup = Consumer<String> { s ->
        edGroup.setText(s)
    }

    //  GPXファイルパスをコントロールに設定する関数インターフェース
    var  iGpxFilePath = Consumer<String>() { s ->
        Toast.makeText(this, s, Toast.LENGTH_LONG).show()
        if (0 < s.length) {
            if (0 <= mGpsTraceList.findGpsFile(s) ){
                klib.messageDialog(this, "確認", "既にファイルが登録されています")
            } else {
                edGpxPath.setText(s)
                //  選択ファイルのフォルダを保存
                klib.setStrPreferences(klib.getFolder(s), "GpxFileFolder", this)
                if (edTitle.text.length == 0)
                    edTitle.setText(klib.getFileNameWithoutExtension(s))
                //  GPSデータの取得と登録
                setGpxFileInfo(s)
            }
        }
    }

    /**
     * GPXファイルのデータをコントロールに7設定する
     * gpxFilePath      GPXファイルのパス名
     */
    fun setDataGpxFile(gpxFilePath: String, title: String = "") {
        if (!klib.existsFile(gpxFilePath))
            return
        mGpxFilePos = mGpsTraceList.findGpsFile(gpxFilePath)
        if (0 <= mGpxFilePos) {
            //  既存データ
            setGpxFileInfo(mGpsTraceList.mDataList[mGpxFilePos].mFilePath)
            edTitle.setText(mGpsTraceList.mDataList[mGpxFilePos].mTitle)
            edGroup.setText(mGpsTraceList.mDataList[mGpxFilePos].mGroup)
            edGpxPath.setText(mGpsTraceList.mDataList[mGpxFilePos].mFilePath)
            edComment.setText(mGpsTraceList.mDataList[mGpxFilePos].mComment)
            spColor.setSelection(mGpsTraceList.mColorMenu.indexOf(mGpsTraceList.mDataList[mGpxFilePos].mLineColor))
            spCategory.setSelection(mGpsTraceList.mCategoryMenu.indexOf(mGpsTraceList.mDataList[mGpxFilePos].mCategory))
        } else {
            //  新規データ
            setGpxFileInfo(gpxFilePath, title)
        }
    }

    /**
     * GPXファイルからGPX情報と測定年をコントロールに登録
     */
    fun setGpxFileInfo(gpxFilePath: String, title: String = "") {
        if (!klib.existsFile(gpxFilePath))
            return
        val gpsFileData = GpsTraceList.GpsTraceData()
        gpsFileData.mFilePath = gpxFilePath
        gpsFileData.loadGpsData()
        edTitle.setText(if (title.isEmpty()) klib.getFileNameWithoutExtension(gpxFilePath) else title)
        spCategory.setSelection(mGpsTraceList.mCategoryMenu.indexOf(gpsFileData.mCategory))
        edGpxPath.setText(gpxFilePath)
        if (gpsFileData.mGpsDataSize == 0)
            gpsFileData.loadGpsData(false)
        tvGpxInfo.setText(gpsFileData.getInfoData())
        tvYear.setText(klib.date2String( gpsFileData.mFirstTime, "yyyy年"))
    }

    /**
     * GPXファイルのグラフ表示画面に移行する
     * gpxPath          GPXファイルのパス
     * title            グラフのタイトル
     */
    fun goGpsGraph(gpxPath: String, title: String) {
        if (klib.existsFile(gpxPath)) {
            val intent = Intent(this, GpsGraph::class.java)
            intent.putExtra("FILE", gpxPath)
            intent.putExtra("TITLE", title)
            startActivity(intent)
        }
    }

}