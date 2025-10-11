package co.jp.yoshida.mapapp

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.util.Consumer
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import co.jp.yoshida.mapapp.databinding.ActivityMapInfoDataBinding

class MapInfoDataActivity : AppCompatActivity() {
    val TAG = "MapInfoDataActivity"

    lateinit var binding: ActivityMapInfoDataBinding
    lateinit var constraintLayout: ConstraintLayout
    lateinit var spMapInfoTitle: Spinner
    lateinit var etMapWebAddress: EditText
    lateinit var etDataId: EditText
    lateinit var etDataExt: EditText
    lateinit var etElevatorId: EditText
    lateinit var etBaseMapInfoId: EditText
    lateinit var cbBaseMapUp: CheckBox
    lateinit var etTransrate: EditText
    lateinit var etSummary: EditText
    lateinit var etZoom: EditText
    lateinit var etArea: EditText
    lateinit var etComment: EditText
    lateinit var etReferenceName: EditText
    lateinit var etReferenceAddress: EditText
    lateinit var btRemoveData: Button
    lateinit var btAddData: Button
    lateinit var btUpdateData: Button
    lateinit var btEnd: Button

    var mDataFolder = ""                            //  データ保存ディレクトリ
    var mMapInfoDataPath = "MapDataList.csv"        //  地図データリストファイル名
    var mMapInfoData = MapInfoData()                //  地図のデータ情報

    var klib = KLib()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
        setContentView(R.layout.activity_map_info_data)
        initControl()
        //  データの設定
        mDataFolder = klib.getPackageNameDirectory(this)
        mMapInfoDataPath = mDataFolder + "/" + mMapInfoDataPath
        mMapInfoData.loadMapInfoData(mMapInfoDataPath)  //  地図データリストの読込
        Log.d(TAG,"onCreate "+mMapInfoDataPath)
        init()
    }

    /**
     * コントロールの設定
     */
    fun initControl() {
        binding = ActivityMapInfoDataBinding.inflate(layoutInflater)
        setContentView(binding.root)
        constraintLayout   = binding.main
        spMapInfoTitle     = binding.spinner
        etMapWebAddress    = binding.editTextTextPersonName
        etDataId           = binding.editTextTextPersonName2
        etDataExt          = binding.editTextTextPersonName12
        etElevatorId       = binding.editTextTextPersonName4
        etBaseMapInfoId    = binding.editTextTextPersonName5
        cbBaseMapUp        = binding.checkBox
        etTransrate        = binding.editTextTextPersonName3
        etSummary          = binding.editTextTextPersonName6
        etZoom             = binding.editTextTextPersonName7
        etArea             = binding.editTextTextPersonName8
        etComment          = binding.editTextTextPersonName9
        etReferenceName    = binding.editTextTextPersonName10
        etReferenceAddress = binding.editTextTextPersonName11
        btRemoveData       = binding.button3
        btAddData          = binding.button6
        btUpdateData       = binding.button4
        btEnd              = binding.button5
    }


    /**
     * 初期化処理
     */
    fun init() {
        //  地図タイトルのspinner設定
        spSetMapInfoData()
        spMapInfoTitle.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                if (0 <= position)
                    setMapInfoData(position)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
//                TODO("Not yet implemented")
            }
        }

        //  [削除]ボタン処理
        btRemoveData.setOnClickListener {
            removeMapInfoData(spMapInfoTitle.selectedItem.toString())
            spSetMapInfoData()
        }

        //  [追加]ボタン処理
        btAddData.setOnClickListener {
            klib.setInputDialog(this, "MapデータのIDタイトル",
                spMapInfoTitle.selectedItem.toString(), iInputOperation)
        }

        //  [更新]ボタン処理
        btUpdateData.setOnClickListener {
            updateMapInfoData(spMapInfoTitle.selectedItem.toString())
        }

        //  [終了]ボタン処理(保存・終了)
        btEnd.setOnClickListener {
            klib.messageDialog2(this, "データ保存", "データを保存して終了します。", iResultOperation)
        }
    }

    //  データ追加のタイトル入力関数インターフェース
    var iInputOperation = Consumer<String> { s ->
        updateMapInfoData(s)
        spSetMapInfoData()
        val no = mMapInfoData.mMapData.indexOfFirst { it[0].compareTo(s) == 0 }
        Log.d(TAG,"iInputOperation: " + no + " " + s)
        if (0 <= no)
            spMapInfoTitle.setSelection(no)
    }

    //  終了・保存処理の関数インターフェース
    var iResultOperation = Consumer<String> { s ->
        if (s.compareTo("OK") == 0) {
            mMapInfoData.saveMaoInfoData(mMapInfoDataPath)
            setResult(RESULT_OK)
            finish()
        } else if (s.compareTo("No") == 0) {
            setResult(RESULT_CANCELED)
            finish()
        } else {
        }
    }

    /**
     * 地図データのタイトルをSpnnerに設定
     */
    fun spSetMapInfoData() {
        var mapTitle = mutableListOf<String>()
        for (i in mMapInfoData.mMapData.indices)
            mapTitle.add(mMapInfoData.mMapData[i][0])
        var mapTitleAdapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, mapTitle)
        spMapInfoTitle.adapter = mapTitleAdapter
    }

    /**
     * 地図データをコントロールに設定
     * no       地図データのID番号
     */
    fun setMapInfoData(no: Int) {
        etDataId.setText(mMapInfoData.mMapData[no][1])
        etDataExt.setText(mMapInfoData.mMapData[no][2])
        etSummary.setText(mMapInfoData.mMapData[no][3])
        etZoom.setText(mMapInfoData.mMapData[no][4])
        etArea.setText(mMapInfoData.mMapData[no][5])
        etComment.setText(mMapInfoData.mMapData[no][6])
        etMapWebAddress.setText(mMapInfoData.mMapData[no][7])
        etReferenceName.setText(mMapInfoData.mMapData[no][8])
        etReferenceAddress.setText(mMapInfoData.mMapData[no][9])
        etElevatorId.setText(mMapInfoData.mMapData[no][10])
        etBaseMapInfoId.setText(mMapInfoData.mMapData[no][11])
        etTransrate.setText(mMapInfoData.mMapData[no][12])
        cbBaseMapUp.isChecked = mMapInfoData.mMapData[no][13].compareTo("true") == 0
    }

    /**
     * コントロールのデータを地図データリストに更新または追加
     * idName       地図タイトル(リストにないタイトルは追加になる)
     */
    fun updateMapInfoData(idName: String) {
        val no = mMapInfoData.mMapData.indexOfFirst { it[0].compareTo(idName) == 0 }
        var mapData = listOf<String>(
            idName,
            etDataId.text.toString(),
            etDataExt.text.toString(),
            etSummary.text.toString(),
            etZoom.text.toString(),
            etArea.text.toString(),
            etComment.text.toString(),
            etMapWebAddress.text.toString(),
            etReferenceName.text.toString(),
            etReferenceAddress.text.toString(),
            etElevatorId.text.toString(),
            etBaseMapInfoId.text.toString(),
            etTransrate.text.toString(),
            cbBaseMapUp.isChecked.toString()
        )
        if (0 <= no)
            mMapInfoData.mMapData[no] = mapData
        else
            mMapInfoData.mMapData.add(mapData)
    }

    /**
     * 地図データリストからデータを削除
     * idName       地図タイトル
     */
    fun removeMapInfoData(idName: String) {
        val no = mMapInfoData.mMapData.indexOfFirst { it[0].compareTo(idName) == 0 }
        mMapInfoData.mMapData.removeAt(no)
    }

}