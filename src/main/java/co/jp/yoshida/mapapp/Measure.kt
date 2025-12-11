package co.jp.yoshida.mapapp

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log


/**
 * 地図上の距離を測定する
 */
class Measure {
    val TAG = "Measure"

    var mPositionList = mutableListOf<PointD>()
    var mMeasureMode = false

    val klib = KLib()

    /**
     *  測定点を追加する
     *  bp      追加する測定点(BaseMao座標9
     */
    fun add(bp: PointD) {
        mPositionList.add(bp)
    }

    /**
     *  測定を開始する
     */
    fun start() {
        mPositionList.clear()
        mMeasureMode = true
    }

    /**
     * 測定を終了
      */
    fun end() {
        mPositionList.clear()
        mMeasureMode = false
    }

    /**
     *  測定点を一つ戻す
     */
    fun decriment() {
        if (0 < mPositionList.size)
            mPositionList.removeAt(mPositionList.size - 1)
    }

    /**
     *  測定点間を結ぶ線を表示する
     *  canvas      Canvas
     *  mapData     地図パラメータ
     */
    fun draw(canvas: Canvas, mapData: MapData) {
        if (1 < mPositionList.size) {
            var paint = Paint()
            paint.color = Color.GREEN
            paint.strokeWidth = 3f
            var sp = mapData.baseMap2Screen(mPositionList[0])
            for (i in 1..mPositionList.size - 1) {
                var ep = mapData.baseMap2Screen(mPositionList[i])
                canvas.drawLine(sp.x.toFloat(), sp.y.toFloat(), ep.x.toFloat(), ep.y.toFloat(), paint)
                sp = ep
            }
        }
    }

    /**
     *  登録した測定点間の距離の合計を求める
     *  mapData     地図パラメータ
     *  return      距離(km)
     */
    fun measure(mapData: MapData): Double {
        var dis = 0.0
        for (i in 1..mPositionList.size - 1) {
            dis += distance(klib.baseMap2Coordinates(mPositionList[i - 1]),
                klib.baseMap2Coordinates(mPositionList[i]))
        }
        return dis
    }

    /**
     *  緯度経度座標で2点間の距離を求める(km)
     *  ps      開始座標
     *  pe      終了座標
     */
    fun distance(ps: PointD, pe: PointD):Double {
        return klib.cordinateDistance(ps.x, ps.y, pe.x, pe.y)
    }

}