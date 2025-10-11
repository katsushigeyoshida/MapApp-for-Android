package co.jp.yoshida.mapapp

import android.util.Log


//  地図画面の状態をリストにし登録・呼び出し、ファイル保存を行う

class AreaData {
    val TAG = "AreaData"

    var mAreaDataListPath = "AreaDataList.csv"
    val mAreaDataList = mutableMapOf<String,List<String>>()

    var klib = KLib()

    //  保存ファイルのパス設定
    fun setSavePath(path: String ) {
        mAreaDataListPath = path
        Log.d(TAG,"setSavePath: "+mAreaDataListPath)
    }

    //  ファイルから読み込む
    fun loadAreaDataList() {
        loadAreaDataList(mAreaDataListPath)
    }

    //  ファイルに保存
    fun saveAreaDataList() {
        saveAreaDataList(mAreaDataListPath)
    }

    //  保存データのタイトルをMapDataから作成
    fun getTitleList(): List<String>{
        var titleList = mutableListOf<String>("タイトル")
        titleList.addAll(MapData.mMapDataFormat)
        return titleList
    }

    //  登録位置画像情報を保存する
    fun saveAreaDataList(path: String) {
        var titleList = getTitleList()
        if (0 < mAreaDataList.count()) {
            var mapPositionlist = mutableListOf<List<String>>()
            for (keyValue in mAreaDataList) {
                var dataList = mutableListOf<String>()
                dataList.add(keyValue.key)
                for (data in keyValue.value) {
                    dataList.add(data)
                }
                mapPositionlist.add(dataList)
            }
            klib.saveCsvData(path, titleList, mapPositionlist)
        }
    }

    //  登録位置画像情報を取り込む
    fun loadAreaDataList(path: String) {
        var titleList = getTitleList()
        var mapAreaDatalist = klib.loadCsvData(path, titleList)
        mAreaDataList.clear()
        for (data in mapAreaDatalist) {
            var parameter = mutableListOf<String>()
            for (i in 1..data.size - 1)
                parameter.add(data[i])

            mAreaDataList.put(data[0], parameter)
        }
    }
}