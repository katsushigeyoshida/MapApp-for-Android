package co.jp.yoshida.mapapp

import android.content.Context
import android.content.DialogInterface
import android.graphics.Canvas
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.util.Consumer
import java.io.File


/**
 *  マークデータのリスト管理
 */
class MarkList {
    val TAG = "MarkList"

    var mMarkList = mutableListOf<MarkData>()
    var mTolerance = 50                         //  指定値近傍マークとの選択範囲距離
    var mSaveFilePath = ""                      //  ファイル保存パス
    var mPreSelectGroup = ""                    //  タイトルリストで指定したグループ名
    var mMarkDisp = true                        //  マークの表示有無
    var mGroupDispList = mutableMapOf<String, Boolean>()
    var mMarkScale = 2.0f                       //  マークシンボルのスケール
    //  特殊グループ名
    val mAllListName = "すべて"
    val mTrashGroup = "ゴミ箱"

    enum class SORTTYPE {
        Non, Normal, Reverse, Distance
    }
    var mSortName = listOf<String>( "ソートなし", "昇順", "降順", "距離順")
    var mListSort = SORTTYPE.Non
    var mCenter = PointD(0.0, 0.0)          //  表示地図の中心緯度経度座標

    val klib = KLib()

    /**
     *  マークの検索
     *  title   検索するマーク名
     *  group   検索対象グループ名
     *  return  MarkData
     */
    fun getMarkData(title: String, group: String): MarkData? {
        for (markData in mMarkList) {
            if (markData.mTitle.compareTo(title) == 0) {
                if (group.length == 0)
                    return markData
                var groups = markData.mGroup.split(",").map { it.trim() }
                for (i in groups.indices) {
                    if (groups[i].compareTo(group) == 0) {
                        return markData
                    }
                }
            }
        }
        return null
    }

    /**
     *  タイトルリストの取得
     *  group   検索する対象グループ名
     *  return  検索したグループ名リスト
     */
    fun getTitleList(group: String):List<String> {
        mPreSelectGroup = group
        var titleList = mutableListOf<String>()
        for (markData in mMarkList) {
            if (!titleList.contains(markData.mTitle)) {
                if (group.length == 0) {
                    titleList.add(markData.mTitle)
                } else {
                    var groups = markData.mGroup.split(",").map { it.trim() }
                    for (i in groups.indices) {
                        if (groups[i].compareTo(group) == 0) {
                            titleList.add(markData.mTitle)
                            break
                        }
                    }
                }
            }
        }
        if (mListSort == SORTTYPE.Normal) {
            titleList.sortWith(Comparator{a,b -> a.compareTo(b)})
        } else if (mListSort == SORTTYPE.Reverse) {
            titleList.sortWith(Comparator{a,b -> b.compareTo(a)})
        } else if (mListSort == SORTTYPE.Distance) {
            titleList.sortWith(Comparator{x, y ->
                Math.signum(getMark(x, group).distance(mCenter) - getMark(y, group).distance(mCenter)).toInt()
            })
        }

        return titleList
    }

    /**
     *  グループリストの取得
     *  firstTitle  リストの最初に追加するタイトル
     *  return      グループ名リスト
     */
    fun getGroupList(firstTitle: String):List<String> {
        var groupList = mutableListOf<String>()
        if (0 < firstTitle.length)
            groupList.add(firstTitle)
        for (markData in mMarkList) {
            var groups = markData.mGroup.split(",").map { it.trim() }
            for (i in groups.indices) {
                if (!groupList.contains(groups[i]))
                    groupList.add(groups[i])
            }
        }
        return groupList
    }

    /**
     *  近傍マークの検索
     *  lovcation : 検索座標(BaseMap座標)
     *  mapData   : MapData
     *  return      MarkData
     */
    fun getMark(location: PointD, mapData: MapData): MarkData? {
        for (mark in mMarkList) {
            var listPos = mapData.baseMap2Screen(mark.mLocation)
            if (listPos.distance(location) < mTolerance)
                return mark
        }
        return null
    }

    /**
     *  指定座標の近傍にあるマークを検索しリストのインデックスを返す
     *  sp          検索マークの近傍座標(スクリーン座標)
     *  mapData     地図の表示パラメータ
     *  return      検索マークのインデックスno
     */
    fun getMarkNum(sp: PointD, mapData: MapData): Int {
        for (i in 0..mMarkList.size - 1) {
            var listPos = mapData.baseMap2Screen(mMarkList[i].mLocation)
            if (listPos.distance(sp) < mTolerance)
                return i
        }
        return -1
    }

    /**
     *  タイトルからマークデータを取得する
     *  title   検索するタイトル名
     *  return  MarkData
     */
    fun getMark(title: String): MarkData? {
        for (mark in mMarkList) {
            if (mark.mTitle.compareTo(title) == 0)
                return mark
        }
        return null
    }

    /**
     *  タイトルとグループからマークデータを取得する
     *  title   検索タイトル名
     *  group   検索対象グループ名
     *  return  MarkData
     */
    fun getMark(title: String, group: String): MarkData {
        for (mark in mMarkList) {
            if (mark.mTitle.compareTo(title) == 0) {
                if (0 == group.length)
                    return mark
                var groups = mark.mGroup.split(",").map { it.trim() }
                for (i in groups.indices)
                    if (groups[i].compareTo(group) == 0)
                        return mark
            }
        }
        return MarkData()
    }

    /**
     *  タイトルからマークデータのインデックスを返す
     *  title   検索タイトル名
     *  return  マークデータのインデックスno
     */
    fun getMarkNum(title: String): Int {
        for (i in 0..mMarkList.size - 1) {
            if (mMarkList[i].mTitle.compareTo(title) == 0)
                return i
        }
        return -1
    }

    /**
     *  タイトルとグループからマークデータのインデックスを返す
     *  title   検索タイトル名
     *  group   検索対象グループ名
     *  return  マークデータのインデックスNo
     */
    fun getMarkNum(title: String, group: String): Int {
        for (i in 0..mMarkList.size - 1) {
            if (mMarkList[i].mTitle.compareTo(title) == 0) {
                if (0 == group.length)
                    return i
                var groups = mMarkList[i].mGroup.split(",").map { it.trim() }
                for (j in groups.indices)
                    if (groups[j].compareTo(group) == 0)
                        return i
            }
        }
        return -1
    }

    /**
     *  マークリストのマークを表示
     *  canvas      描画Canvas
     *  mapData     MapData
     */
    fun draw(canvas: Canvas, mapData: MapData) {
        if (mMarkDisp) {
            for (mark in mMarkList) {
                if (mark.mVisible) {
                    var groups = mark.mGroup.split(",").map { it.trim() }
                    for (i in groups.indices) {
                        if (mGroupDispList.size == 0 || mGroupDispList.getValue(groups[i])) {
                            mark.mScale = mMarkScale
                            mark.draw(canvas, mapData)
                            break
                        }
                    }
                }
            }
        }
    }

    /**
     * マークリストからグループの表示Mapを作成
     */
    fun setGroupDispList() {
        mGroupDispList.clear()
        for (markData in mMarkList) {
            var groups = markData.mGroup.split(",").map { it.trim() }
            for (i in groups.indices) {
                if (!mGroupDispList.containsKey(groups[i]))
                    mGroupDispList.put(groups[i], true)
            }
        }
    }

    /**
     * グループの表示Mapリストからグルーブ名配列を取得
     */
    fun getMarkGroupArray(): Array<String> {
        var groupList = mutableListOf<String>()
        for (key in mGroupDispList.keys) {
            groupList.add(key)
        }
        return groupList.toTypedArray()
    }

    /**
     * グループの表示Mapリストから表示状態配列を取得
     */
    fun getMarkGroupBooleanArray(): BooleanArray {
        var groupList = mutableListOf<Boolean>()
        for (value in mGroupDispList.values)
            groupList.add(value)
        return groupList.toBooleanArray()
    }

    /**
     *  マークリストをファイルに保存
     */
    fun saveMarkFile(context: Context) {
        saveMarkFile(mSaveFilePath)
        saveParameter(context)
    }

    /**
     *  マークリストをファイルから取得
     */
    fun loadMarkFile(context: Context) {
        loadMarkFile(mSaveFilePath, false)
        loadParameter(context)
    }

    /**
     *
     *  マークリストを指定パスでファイルに保存
     *  path    保存ファイルパス
     */
    fun saveMarkFile(path: String) {
        if (mMarkList.size == 0)
            return
        var dataList = mutableListOf<List<String>>()
        for (mark in mMarkList) {
            dataList.add(mark.getStringData())
        }
        klib.saveCsvData(path, MarkData.mDataFormat, dataList)
    }

    /**
     *  マークリストを指定ファイルから取得
     *  path    ファイルパス
     *  add     追加読込フラグ
     */
    fun loadMarkFile(path: String, add: Boolean) {
        var file = File(path)
        if (file.exists()) {
            var dataList = klib.loadCsvData(path, MarkData.mDataFormat)
            if (!add)
                mMarkList.clear()
            for (data in dataList) {
                var markData = MarkData()
                markData.setStringData(data)
                if (0 == getMark(markData.mTitle, markData.mGroup).mTitle.length)
                    mMarkList.add(markData)
            }
        }
        setGroupDispList()
    }

    /**
     *  プリファレンスに表示パラメータを保存
     */
    fun saveParameter(context: Context) {
        klib.setBoolPreferences(mMarkDisp, "MarkDisp", context)
        klib.setFloatPreferences(mMarkScale, "MarkScale", context)
        klib.setStrPreferences(mListSort.toString(), "MarkSort", context)
    }

    /**
     *  プリファレンスから表示パラメータを取得
     */
    fun loadParameter(context: Context) {
        mMarkDisp = klib.getBoolPreferences("MarkDisp", context)
        mMarkScale = klib.getFloatPreferences("MarkScale", mMarkScale, context)
        mListSort = SORTTYPE.valueOf(klib.getStrPreferences("MarkSort", mListSort.toString(), context)?:"Non")
    }


    /**
     *  マークデータ登録編集画面
     *  C           コンテキスト
     *  title       ダイヤログのタイトル
     *  message     マークデータをカンマ接続した文字列8タイトル,グループ､マークタイプ､サイズ､コメント､リンク9
     *  operation   結果を処理する関数インタフェース
     */
    fun setMarkInputDialog(c: Context, title: String, message: String, operation: Consumer<String>) {
        val linearLayout = LinearLayout(c)
        val titleLabel = TextView(c)
        val etTitle = EditText(c)
        val groupLabel = TextView(c)
        var etGroup = EditText(c)
        var spGroup = Spinner(c)
        val typeLabel = TextView(c)
        val spMarkType = Spinner(c)
        val sizeLabel = TextView(c)
        val spMarkSize = Spinner(c)
        val commanetLabel = TextView(c)
        val etComment = EditText(c)
        val cooordinateLabel = TextView(c)
        val etCoordinate = EditText(c)
        val linkLabel = TextView(c)
        val etLink = EditText(c)
        var groupFirst = true

        linearLayout.orientation = LinearLayout.VERTICAL
        titleLabel.setText("タイトル")
        groupLabel.setText("グループ")
        typeLabel.setText("マークタイプ")
        sizeLabel.setText("サイズ")
        commanetLabel.setText("コメント")
        cooordinateLabel.setText("座標")
        linkLabel.setText("リンク")

        var groupList = getGroupList("")
        var groupAdapter = ArrayAdapter(c, android.R.layout.simple_spinner_dropdown_item, groupList)
        spGroup.adapter = groupAdapter
        spGroup.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!groupFirst || 0 == etGroup.text.length)
                    etGroup.setText(groupList[position])
                groupFirst = false
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }
        }
        var markTypeAdapter = ArrayAdapter(c, android.R.layout.simple_spinner_dropdown_item, MarkData.mMarkName)
        spMarkType.adapter = markTypeAdapter
        var markSizeAdapter =
            ArrayAdapter(c, android.R.layout.simple_spinner_dropdown_item, MarkData.mSizeName)
        spMarkSize.adapter = markSizeAdapter
        spMarkSize.setSelection(3)

//        var data = message.split(',')
        var data = klib.splitCsvString(message)     //  カンマ区切り文字列をリストに変換
        if (5 < data.size) {
            //  編集時のデータ設定
            etTitle.setText(data[0])
            etGroup.setText(data[1])
            spGroup.setSelection(groupList.indexOf(data[1]))
            var markType = data[2].toInt()
            markType = Math.min(Math.max(0, markType), MarkData.mMarkName.size - 1)
            spMarkType.setSelection(markType)
            var markSize = MarkData.mSizeName.indexOf(data[3])
            if (0 <= markSize)
                spMarkSize.setSelection(markSize)
            etComment.setText(data[4])
            etLink.setText(data[5])
            if (9 < data.size && (0 < data[8].length && 0 < data[9].length)) {
                var bp = PointD(data[8].toDouble(), data[9].toDouble())
                var cp = klib.baseMap2Coordinates(bp)
                etCoordinate.setText(cp.y.toString()+ "," + cp.x.toString())
            }
        }

        linearLayout.addView(titleLabel)
        linearLayout.addView(etTitle)
        linearLayout.addView(groupLabel)
        linearLayout.addView(etGroup)
        linearLayout.addView(spGroup)
        linearLayout.addView(typeLabel)
        linearLayout.addView(spMarkType)
        linearLayout.addView(sizeLabel)
        linearLayout.addView(spMarkSize)
        linearLayout.addView(commanetLabel)
        linearLayout.addView(etComment)
        linearLayout.addView(cooordinateLabel)
        linearLayout.addView(etCoordinate)
        linearLayout.addView(linkLabel)
        linearLayout.addView(etLink)

        val dialog = AlertDialog.Builder(c)
        dialog.setTitle(title)
        dialog.setView(linearLayout)
        dialog.setPositiveButton("OK", DialogInterface.OnClickListener { dialog, which ->
            operation.accept("\"" + etTitle.text.toString() + "\",\"" +
                    etGroup.text.toString() + "\",\"" +
                    spMarkType.selectedItemPosition.toString() + "\",\"" +
                    spMarkSize.selectedItem.toString() + "\",\"" +
                    etComment.text.toString() + "\",\"" +
                    etLink.text.toString() + "\",\"" +
                    etCoordinate.text.toString() + "\"")
        })
        dialog.setNegativeButton("Cancel", null)
        dialog.show()
    }

    //  マーク登録の関数インターフェース
    var iMarkSetOperation = Consumer<String> { s ->
        var data = klib.splitCsvString(s)       //  カンマ区切りで分解
        //  新規
        var markData = MarkData()
        markData.mTitle = data[0]
        markData.mGroup = data[1]
        markData.mMarkType = data[2].toInt()
        markData.mSize = data[3].toInt()
        markData.mComment = data[4]
        markData.mLink = data[5]
        if (7 < data.count()) {
            var cp = PointD(data[7].toDouble(), data[6].toDouble())
            markData.mLocation = klib.coordinates2BaseMap(cp)
        } else {
            var coord = data[6].split(',')
            var cp = PointD(coord[1].toDouble(), coord[0].toDouble())
            markData.mLocation = klib.coordinates2BaseMap(cp)
            Log.d(TAG, "iMarkSetOperation: " + markData.mLocation.toString())
        }
        mMarkList.add(markData)
        setGroupDispList()
    }

}