package co.jp.yoshida.mapapp

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.Log
import androidx.core.util.Consumer
import java.net.URLDecoder

/**
 *  マークデータクラス
 */
class MarkData {
    val TAG = "MarkData"

    var mTitle = ""                         //  マークのタイトル
    var mGroup = ""                         //  マークのグループ名
    var mLocation = PointD(0.0, 0.0)    //  マークのの配置位置(BaseMap座標)
    var mMarkType = 0                       //  マークの種類
    var mComment = ""                       //  コメント
    var mLink = ""                          //  リンクデータ
    var mSize = 10                          //  マークのサイズ
    var mVisible = true                     //  表示可否
    var mTitleVisible = true                //  タイトルの表示か費
    var mPaint = Paint()
    var mScale = 2.0f                       //  マークシンボルのスケール設定

    //  マーク形状名、サイズ名、ファイル保存時のタイトル名(外部からstaticアクセスを可にする)
    companion object {
        var mMarkName = listOf("クロス", "クロス円", "三角形", "家", "ビル", "工場", "橋", "公園")
        var mSizeName = listOf("3", "5", "10", "15", "20", "25", "30", "35", "40")
        val mDataFormat = listOf(
            "Title", "Group", "MarkType", "Size", "Comment", "Link", "Visible", "TitleVisible", "XLocation", "YLocation"
        )
    }
    //  マークの表示コマンド
    var mMarkPath = listOf<List<String>>(                        //  記号パターン
        listOf<String>("C=Black", "L=-1,0,1,0", "L=0,-1,0,1"),                              //  クロス
        listOf<String>("C=Black", "L=-1,0,1,0", "L=0,-1,0,1", "C=Red", "A=0,0,0.7,0,360"),  //  クロスに〇
        listOf<String>("C=Black", "L=-1,1,1,1", "L=1,1,0,-1", "L=0,-1,-1,1"),               //  △三角
        listOf<String>("C=Black", "L=-1,1,1,1", "L=1,1,1,0", "L=1,0,0,-1", "L=0,-1,-1,0", "L=-1,0,-1,1", "L=-1,0,1,0"),    //  家
        listOf<String>("C=Black", "R=-0.6, -1,0.6,1", "R=-0.4,-0.7,0.4,-0.4", "R=-0.4,-0.2,0.4,0.2", "R=-0.4,0.4,0.4,0.7"),    //  ビル
        listOf<String>("C=Black", "R=-1,0.0, 1,1", "R=-0.7,-1,-0.3,0.0"),                   //  工場
        listOf<String>("C=Black", "L=-1.5,0,1.5,0", "A=0,0,1,180,180"),                     //  橋
        listOf<String>( "C=Black", "L=0,-1.5,-1,-0.3", "L=0,-1.5,1,-0.3", "L=1,-0.3,-1,-0.3",
            "L=0,-0.4,-1,0.8", "L=0,-0.4,1,0.8", "L=1,0.8,-1,0.8", "L=0,0.8,0,1.5")         //  公園(木)
    )

    var klib = KLib()

    /**
     *  コマンドを使ってマークの形状表示
     */
    fun draw(canvas: Canvas, mapData: MapData) {
        for ( i in 0..mMarkPath[mMarkType].size - 1) {
            mPaint.strokeWidth = 3f
            mPaint.style = Paint.Style.STROKE
            when (mMarkPath[mMarkType][i][0]) {
                'C' -> {
                    setColor(getParameter(mMarkPath[mMarkType][i]))
                }
                'L' -> {
                    drawLine(canvas,mapData, getParameter(mMarkPath[mMarkType][i]))
                }
                'R' -> {
                    drawRect(canvas,mapData, getParameter(mMarkPath[mMarkType][i]))
                }
                'P' -> {
                    drawPolyLine(canvas,mapData, getParameter(mMarkPath[mMarkType][i]))
                }
                'A' -> {
                    drawArc(canvas,mapData, getParameter(mMarkPath[mMarkType][i]))
                }
            }
        }
        //  タイトル表示
        if(mTitleVisible)
            drawTitle(canvas, mapData, mTitle)
    }


    /**
     *  コマンドのパラメータを取得
     */
    fun getParameter(command: String): String {
        return command.substring(command.indexOf('=') + 1).trim()
    }

    /**
     *  カラーの設定
     */
    fun setColor(color: String) {
        mPaint.color = if (klib.mColorMap[color]==null) Color.BLACK else klib.mColorMap[color]!!
    }

    /**
     *  線分の表示
     */
    fun drawLine(canvas: Canvas, mapData: MapData, command: String) {
        var data = command.split(',')
        var pos = mapData.baseMap2Screen(mLocation)
        var sx = data[0].toFloat() * mSize.toFloat() * mScale + pos.x.toFloat()
        var sy = data[1].toFloat() * mSize.toFloat() * mScale + pos.y.toFloat()
        var ex = data[2].toFloat() * mSize.toFloat() * mScale + pos.x.toFloat()
        var ey = data[3].toFloat() * mSize.toFloat() * mScale + pos.y.toFloat()
        canvas.drawLine(sx, sy, ex, ey, mPaint)
    }

    /**
     *  四角形の表示
     */
    fun drawRect(canvas: Canvas, mapData: MapData, command: String) {
        var data = command.split(',')
        var pos = mapData.baseMap2Screen(mLocation)
        var sx = data[0].toFloat() * mSize.toFloat() * mScale + pos.x.toFloat()
        var sy = data[1].toFloat() * mSize.toFloat() * mScale + pos.y.toFloat()
        var ex = data[2].toFloat() * mSize.toFloat() * mScale + pos.x.toFloat()
        var ey = data[3].toFloat() * mSize.toFloat() * mScale + pos.y.toFloat()
        canvas.drawLine(sx, sy, sx, ey, mPaint)
        canvas.drawLine(ex, sy, ex, ey, mPaint)
        canvas.drawLine(sx, sy, ex, sy, mPaint)
        canvas.drawLine(sx, ey, ex, ey, mPaint)
    }

    /**
     *  連続線分の表示
     */
    fun drawPolyLine(canvas: Canvas, mapData: MapData, command: String) {
        var data = command.split(',')
        var pos = mapData.baseMap2Screen(mLocation)
        var sx = data[0].toFloat() * mSize.toFloat() * mScale + pos.x.toFloat()
        var sy = data[1].toFloat() * mSize.toFloat() * mScale + pos.y.toFloat()
        for (i in 1..data.size - 1 step 2) {
            var ex = data[i].toFloat() * mSize.toFloat() * mScale + pos.x.toFloat()
            var ey = data[i+1].toFloat() * mSize.toFloat() * mScale + pos.y.toFloat()
            canvas.drawLine(sx, sy, ex, ey, mPaint)
            sx = ex
            sy = ey
        }
    }

    /**
     *  円弧の表示
     */
    fun drawArc(canvas: Canvas, mapData: MapData, command: String) {
        mPaint.style = Paint.Style.STROKE;
        var data = command.split(',')
        var pos = mapData.baseMap2Screen(mLocation)
        var cx = data[0].toFloat() * mSize.toFloat() * mScale + pos.x.toFloat()
        var cy = data[1].toFloat() * mSize.toFloat() * mScale + pos.y.toFloat()
        var r  = data[2].toFloat() * mSize.toFloat() * mScale
        var sa = data[3].toFloat()      //  開始角度(deg)
        var ea = data[4].toFloat()      //  展開角度(deg)
        var oval = RectF(cx - r, cy - r, cx + r, cy + r)
        canvas.drawArc(oval, sa, ea, true, mPaint)
    }

    /**
     *  タイトルを表示
     */
    fun drawTitle(canvas: Canvas, mapData: MapData, title: String){
        var pos = mapData.baseMap2Screen(mLocation)
        mPaint.style = Paint.Style.FILL;
        mPaint.color = Color.BLACK
        mPaint.textSize = mSize.toFloat() * 2 * mScale
        canvas.drawText(title, pos.x.toFloat(), (pos.y + mSize * mScale * 3).toFloat(), mPaint)
    }

    /**
     *  パラメータをカンマ区切りの文字列で取得
     */
    fun getDataString(): String {
        var dataList = getStringData()
        return klib.getCsvData(dataList)    //  ダブルクオーテーションでくくってカンマ区切り文字列にする
    }

    /**
     *  指定点(緯度経度座標)までの距離(km)
     *  p   緯度経度座標
     */
    fun distance(p: PointD): Double{
        return klib.cordinateDistance(klib.baseMap2Coordinates(mLocation), p)
    }

    /**
     *  パラメータを文字配列データで取得
     */
    fun getStringData(): List<String> {
        var dataList = mutableListOf<String>()
        dataList.add(mTitle)
        dataList.add(mGroup)
        dataList.add(mMarkType.toString())
        dataList.add(mSize.toString())
        dataList.add(mComment)
        dataList.add(URLDecoder.decode(mLink))
        dataList.add(mVisible.toString())
        dataList.add(mTitleVisible.toString())
        dataList.add(mLocation.x.toString())
        dataList.add(mLocation.y.toString())
        return dataList
    }

    /**
     *  文字データ配列でパラメータを設定
     */
    fun setStringData(data: List<String>) {
        if (data.size <= 6)
            return
        mTitle    = data[0]
        mGroup    = data[1]
        mMarkType = data[2].toDouble().toInt()
        mSize     = data[3].toDouble().toInt()
        mComment  = data[4]
        mLink     = URLDecoder.decode(data[5])
        if (7 < data.size) {
            mVisible  = data[6].toBoolean()
            mTitleVisible = data[7].toBoolean()
        }
        if (9 < data.size) {
            mLocation.x = data[8].toDouble()
            mLocation.y = data[9].toDouble()
        }
    }

    //  マーク登録の関数インターフェース
    var iMarkEditOperation = Consumer<String> { s ->
        var data = klib.splitCsvString(s)       //  カンマ区切りで分解
        Log.d(TAG, "iMarkSetOperation: " + data.count() + " " + data[6])
        //  新規
        mTitle = data[0]
        mGroup = data[1]
        mMarkType = data[2].toInt()
        mSize = data[3].toInt()
        mComment = data[4]
        mLink = data[5]
        if (7 < data.count()) {
            var cp = PointD(data[7].toDouble(), data[6].toDouble())
            mLocation = klib.coordinates2BaseMap(cp)
        } else {
            var coord = data[6].split(',')
            var cp = PointD(coord[1].toDouble(), coord[0].toDouble())
            mLocation = klib.coordinates2BaseMap(cp)
            Log.d(TAG, "iMarkEditOperation: " + mLocation.toString())
        }
    }
}