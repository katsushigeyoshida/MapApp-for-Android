package co.jp.yoshida.mapapp

import android.Manifest
import android.app.ActivityManager
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.location.Location
import android.media.AudioManager
import android.media.ExifInterface
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.preference.PreferenceManager
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.AdapterView
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.getExternalFilesDirs
import androidx.core.content.FileProvider
import androidx.core.util.Consumer
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.IOException
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.TimeZone
//import java.util.function.Consumer
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sign


class KLib {
    val TAG = "KLib"

    //  --- グラフィック関連  ---

    /**
     *  色配列マップ
     *  Color = klib.mColorMap["Black"]
     */
    val mColorMap = mapOf(
        "Black" to Color.BLACK, "Red" to Color.RED, "Blue" to Color.BLUE, "Green" to Color.GREEN,
        "Yellow" to Color.YELLOW, "White" to Color.WHITE, "Cyan" to Color.CYAN, "Gray" to Color.GRAY,
        "LightGray" to Color.LTGRAY, "Magenta" to Color.MAGENTA, "DarkGray" to Color.DKGRAY, "Transparent" to Color.TRANSPARENT
    )

    /**
     * RasterColorからRGBに変換
     * c        RasterColor(getPixelの値)
     * return   RGB値(0x00RRGGBB)
     */
    fun color2rgb(c: Int): Int {
        val rgb =  Color.red(c) * 0x10000 + Color.green(c) * 0x100 + Color.blue(c)
        return rgb
    }

    /**
     * 画像の重ね合わせ(画像合成)
     * bitmap1      ベースになる画像
     * bitmap2      重ねる画像
     * retutn       合成画像
     */
    fun imageComposite(bitmap1: Bitmap, bitmap2: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(bitmap1.width, bitmap1.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawBitmap(bitmap1, 0f, 0f, null)
        canvas.drawBitmap(bitmap2, 0f, 0f, null)
        return output
    }

    /**
     * 画像ファイルを重ね合わせ(画像合成)てPNGファイル保存
     * src1ImagePath    ベースになる画像ファイル名
     * src2ImagePath    上に重ねる画像ファイル名
     * outPath          合成画像ファイル名
     * transparent      透過色(RGB)
     * return           出力画像ファイル名(失敗した時は空名)
     */
    fun imageComposite(src1ImagePath: String, src2ImagePath: String, outPath: String, transparent: Int = 0): String {
        var bitmap1: Bitmap? = null
        var bitmap2: Bitmap? = null
        var output: Bitmap? = null
        if (existsFile(src1ImagePath))
            bitmap1 = BitmapFactory.decodeFile(src1ImagePath)
        if (existsFile(src2ImagePath))
            bitmap2 = BitmapFactory.decodeFile(src2ImagePath)
        if (bitmap1 != null && bitmap2 !=null) {
            if (transparent != 0)
                bitmap2 = makeTransparent(bitmap2,  transparent)
            output = imageComposite(bitmap1, bitmap2)
        } else if (bitmap1 != null && bitmap2 == null)
            output = bitmap1
        else if (bitmap1 == null && bitmap2 != null)
            output = bitmap2
        else
            return ""
        return saveImageFile(output, outPath)
    }


    /**
     * Bitmapデータのファイル保存
     * bitmap       Bitmapデータ
     * path         保存ファイル名
     * compType     保存形式(PNG/JPEG)デフォルトはPNG
     */
    fun saveImageFile(bitmap:Bitmap, path: String, compType: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG): String {
        Log.d(TAG,"saveImageFile: " + path)
        try {
            val fos = FileOutputStream(File(path))
            bitmap.compress(compType, 100, fos)
            fos.close()
            return path
        } catch (e: Exception) {
            Log.d(TAG, "saveImageFile: " + e.message)
            return ""
        }
    }

    /**
     * 画像の透過処理(指定色を透過にする)
     * bitmap       画像データ
     * c            透過にする色(RGB 0xRRGGBB)
     * return       透過処理をした画像データ
     */
    fun makeTransparent(bitmap: Bitmap, c: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        var pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (color2rgb(pixels[x + y * width]) == c) {
                    pixels[x + y * width] = 0
                }
            }
        }
        output.eraseColor(Color.argb(0, 0, 0, 0))
        output.setPixels(pixels, 0, width, 0, 0, width, height)
        return output
    }

    //  ---  座標処理  ---

    /**	地球上の２地点の緯度・経度を指定して最短距離とその方位角を計算
     *	地球を赤道半径r=6378.137kmを半径とする球体として計算しています。
     *	方位角は北:0度、東:90度、南:180度、西:270度。
     *	地点A(経度x1, 緯度y1)、地点B(経度x2, 緯度y2)
     *	ABの距離(km) d = r*acos(sin(y1)*sin(y2)+cos(y1)*cos(y2)*cos(x2-x1))
     *	方位角　φ = 90 - atan2(sin(x2-x1), cos(y1)*tan(y2) - sin(y1)*cos(x2-x1))
     *	http://keisan.casio.jp/has10/SpecExec.cgi
     *
     *  球面上の2点間座標の距離
     *  longi : 経度 latii : 緯度
     *  return : 距離(km)
     */
    fun cordinateDistance(longi1: Double, lati1: Double, longi2: Double, lati2: Double): Double {
        val r = 6378.137
        val x1 = longi1 / 180.0 * Math.PI
        val y1 = lati1 / 180.0 * Math.PI
        val x2 = longi2 / 180.0 * Math.PI
        val y2 = lati2 / 180.0 * Math.PI
        val dis = r * Math.acos(Math.sin(y1) * Math.sin(y2) + Math.cos(y1) * Math.cos(y2) * Math.cos(x2 - x1))
        return if (dis.isNaN()) 0.0 else dis
    }

    /**
     * 地球上の２地点の緯度・経度を指定して最短距離を求める
     * ps       開始座標
     * pe       終了座標
     * return   距離(km)
     */
    fun cordinateDistance(ps: PointD, pe: PointD): Double {
        return cordinateDistance(ps.x, ps.y, pe.x, pe.y)
    }

    /** 球面上の2点間座標の方位
     *  longi   : 経度
     *  latii   : 緯度
     *  return  : 方位角
     */
    fun cordinateAzimuth(longi1: Double, lati1: Double, longi2: Double, lati2: Double): Double {
        val x1 = longi1 / 180.0 * Math.PI
        val y1 = lati1 / 180.0 * Math.PI
        val x2 = longi2 / 180.0 * Math.PI
        val y2 = lati2 / 180.0 * Math.PI
        return 90 - Math.atan2(Math.sin(x2 - x1), Math.cos(y1) * Math.tan(y2) - Math.sin(y1) * Math.cos(x2 - x1)) * 180 / Math.PI
    }

    /**  緯度経度座標(度)からメルカトル図法での距離を求める
     *  cp      cp.x : 経度、 cp.y : 緯度
     *  return  BaseMap座標
     */
    fun coordinates2BaseMap(cp: PointD): PointD {
        //  座標変換
        return PointD(
            cp.x / 360.0 + 0.5,
            0.5 - 0.5 / Math.PI * Math.log(Math.tan(Math.PI * (1 / 4.0 + cp.y / 360.0))))
    }

    /** メルカトル図法での距離から緯度経度座標(度)を求める
     *  bp.X : 経度方向の距離、 bp.Y : 緯度方向の距離
     *  return : BaseMap上の位置
     */
    fun baseMap2Coordinates(bp: PointD): PointD {
        val cp = PointD(Math.PI * (2.0 * bp.x - 1),
            2.0 * Math.atan(Math.exp((0.5 - bp.y) * 2.0 * Math.PI)) - Math.PI / 2.0)
        //  rad → deg
        cp.x *= 180.0 / Math.PI
        cp.y *= 180.0 / Math.PI
        return cp
    }

    private val coordinatePatterns = listOf(
        "北緯(.*?)度(.*?)分(.*?)秒東経(.*?)度(.*?)分(.*?)秒",
        "北緯(.*?)度(.*?)分東経(.*?)度(.*?)分",
        "北緯(.*?)度(.*?)東経(.*?)度(.*?)"
    )

    /**
     * 日本語の座標を座標値に変換
     * coordinate       日本語の座標文字列
     * return           座標値(PointD(経度,緯度)
     */
    fun string2Coordinate2(coordinate: String): PointD {
        var lati = 0.0          //  緯度
        var longi = 0.0         //  経度
        for (i in coordinatePatterns.indices) {
            Log.d(TAG, "string2Coordinate: " + i + " " + coordinatePatterns[i])
            val regex = Regex(coordinatePatterns[i])
            val match = regex.find(coordinate)
            if (match != null) {
                when (i) {
                    0 -> {
                        lati = match.groupValues.elementAt(1).toDouble()
                        lati += match.groupValues.elementAt(2).toDouble() / 60.0
                        lati += match.groupValues.elementAt(3).toDouble() / 3600.0
                        longi = match.groupValues.elementAt(4).toDouble()
                        longi += match.groupValues.elementAt(5).toDouble() / 60.0
                        longi += match.groupValues.elementAt(6).toDouble() / 3600.0
                        break
                    }
                    1 -> {
                        lati = match.groupValues.elementAt(1).toDouble()
                        lati += match.groupValues.elementAt(2).toDouble() / 60.0
                        longi = match.groupValues.elementAt(3).toDouble()
                        longi += match.groupValues.elementAt(4).toDouble() / 60.0
                        break
                    }
                    2 -> {
                        lati = match.groupValues.elementAt(1).toDouble()
                        longi = match.groupValues.elementAt(3).toDouble()
                        break
                    }
                }
            }
        }
        return PointD(longi, lati)
    }


    /**
     * 日本語の座標を座標値に変換
     * 例: S北緯35度12分34秒東経145度56分01秒
     * coordinate       日本語の座標文字列
     * return           座標値(PointD(経度,緯度)
     */
    fun string2Coordinate(coordinate: String): PointD {
        var lati = 0.0          //  緯度
        var longi = 0.0         //  経度
        val a1 = coordinate.indexOf("北緯")
        val b1 = coordinate.indexOf("東経")
        if (a1 < 0 || b1 < 9)
            return PointD(0.0,0.0)
        try {
            val a2 = coordinate.indexOf("度", a1)
            if (0 <= a2 && a2 < b1) {
                var buf = coordinate.substring(a1 + 2, a2)
                lati = if (buf.length == 0) 0.0 else buf.trim('.',' ').toDouble()
                val a3 = coordinate.indexOf("分", a1)
                if (0 <= a3 && a3 < b1) {
                    buf = coordinate.substring(a2 + 1, a3)
                    lati += if (buf.length == 0) 0.0 else buf.trim('.',' ').toDouble() / 60.0
                    val a4 = coordinate.indexOf("秒", a1)
                    if (0 <= a4 && a4 < b1) {
                        buf = coordinate.substring(a3 + 1, a4)
                        lati += if (buf.length == 0) 0.0 else buf.trim('.',' ').toDouble() / 3600.0
                    }
                }
            }

            val b2 = coordinate.indexOf("度", b1)
            if (0 <= b2) {
                var buf = coordinate.substring(b1 + 2, b2)
                longi = if (buf.length == 0) 0.0 else buf.trim('.',' ').toDouble()
                val b3 = coordinate.indexOf("分", b1)
                if (0 <= b3) {
                    buf = coordinate.substring(b2 + 1, b3)
                    longi += if (buf.length == 0) 0.0 else buf.trim('.',' ').toDouble() / 60.0
                    val b4 = coordinate.indexOf("秒", b1)
                    if (0 <= b4) {
                        buf = coordinate.substring(b3 + 1, b4)
                        longi += if (buf.length == 0) 0.0 else buf.trim('.',' ').toDouble() / 3600.0
                    }
                }
            }
        } catch (e: Exception) {
            return PointD(0.0,0.0)
        }

        return PointD(longi, lati)
    }

    /**
     * 画像データのEXIF情報から座標データを抽出
     * path         画像ファイルのパス
     * return       座標
     */
    fun getExifCoordinate(path: String): PointD {
        val exif = ExifInterface(path)
        var coord = PointD()
        coord.x = dmsStr2Double(exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE).toString())
        coord.y = dmsStr2Double(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE).toString())
        return coord
    }

    /**
     * DMS形式を度に変換
     * [135/1,37308/1000,0/1]  →  141.216667
     * dms      DMS文字列
     * retunn   度
     */
    fun dmsStr2Double(dms: String): Double {
        val dmsArray = dms.split(',')
        if (2 < dmsArray.size) {
            var value = simpleExpression(dmsArray[0])
            value += simpleExpression(dmsArray[1]) / 60
            value += simpleExpression(dmsArray[2]) / 3600
            return value
        }
        return 0.0
    }

    //  ---  数値処理  ---

    /**
     * 最上位桁が1,2,5になるように数値を丸める
     * v        数値
     * return   丸めた数値
     */
    fun floorStepSize(v: Double): Double {
        val mag = floor(log10(abs(v)))
        val base = 10.0
        val magPow = floor(abs(v) / base.pow(mag))
        var magMsd = 0.0
        if (magPow >= 5.0) {
            magMsd = 5 * base.pow(mag)
        } else if (magPow >= 2.0) {
            magMsd = 2 * base.pow(mag)
        } else {
            magMsd = base.pow(mag)
        }
        return magMsd * sign(v)
    }

    /**
     *  グラフ作成時の補助線間隔を求める
     *  補助線の間隔は1,2,5の倍数
     *  range        補助線の範囲
     *  targetSteps  補助線の数
     *  returns      補助線の間隔
     */
    fun graphStepSize(range: Double, targetSteps: Double, base: Double = 10.0): Double {
        // calculate an initial guess at step size
        val tempStep = range / targetSteps
        // get the magnitude of the step size
        val mag = floor(log(tempStep, base))
        val magPow = base.pow(mag)
        // calculate most significant digit of the new step size
        var magMsd = floor(tempStep / magPow + 0.5)

        // promote the MSD to either 1, 2, or 5
        if (magMsd > base / 2.0)
            magMsd = base
        else if (magMsd > base / 5.0)
            magMsd = floor(base / 2.0)
        else if (magMsd > base / 10.0)
            magMsd = floor(base / 5.0)

        return magMsd * magPow
    }

    /**
     *  グラフの最大値を求める
     *  height      データの最大値
     *  stepSize    目盛間隔
     *  returns     最大値
     */
    fun graphHeightSize(height: Double, stepSize: Double): Double {
        //  グラフ高さの調整
        if (height < 0)
            return ((height / stepSize).toInt() - 1) * stepSize
        else
            return ((height / stepSize).toInt() + 1) * stepSize
    }


    /**
     *  簡単な四則演算(剰余の優先順位なし、括弧なし)
     */
    fun simpleExpression(express: String): Double {
        val expList = expressionList(express)
        var result: Double = 0.0
        var index: Int = 0
        var ope: String = ""
        while (index < expList.size) {
            Log.d(TAG,"expression "+expList[index]+" "+result)
            val num = expList[index].toDoubleOrNull()
            if (num == null ) {
                ope = expList[index]
            } else  if (index == 1 && ope == "-") {
                result = num * -1
            } else {
                if (ope == "+") {
                    result += num
                } else if (ope == "-") {
                    result -= num
                } else if (ope == "*") {
                    result *= num
                } else if (ope == "/") {
                    result /= num
                } else {
                    result = num
                }
            }
            index++
        }
        return result
    }

    /**
     * 文字列を数値と演算子のリストに分解する
     */
    fun expressionList(express: String): List<String> {
        val expList = mutableListOf<String>()
        val number = listOf<Char>( '0','1','2' ,'3','4','5','6','7','8','9','.')
        val operationCode = listOf( '+', '-', '*', '/')
        var buf = ""
        for(c in express ) {
            if (0 <= number.indexOf(c)) {
                buf += c
            } else if (0 <= operationCode.indexOf(c)) {
                expList.add(buf)
                buf = ""
                expList.add(c.toString())
            }
        }
        if (0 < buf.length)
            expList.add(buf)
        return expList
    }

    //  ---  文字列処理  ---


    /**
     * 文字列を整数に変換(後についている数値以外の文字は無視する
     * 先頭に0xがある場合は16進変換、文字列が数値でない場合は0を返す
     * @param str   文字列
     * @return      整数値
     */
    fun str2Integer(str: String): Int {
        try {
            if (str.trim().substring(0,2).compareTo("0x") == 0) {
                return Integer.decode(str.trim())
            } else {
                var numStr = stripUnNumberChar(str.trim())
                return if (isNumber(numStr)) numStr.toInt() else 0
            }
        } catch (e: Exception) {
            return 0
        }
    }

    /**
     * 数値以外の文字を除去する
     * str      文字列
     * return   数値文字列
     */
    fun stripUnNumberChar(str: String): String {
        var buf = ""
        for (i in str.indices) {
            val n = HanNumCode.indexOf(str[i])
            if (0 <= n) {
                buf += str[i]
            } else if ((str[i] == 'E' || str[i] == 'e') &&
                (i + 1 < str.length && 0 <= HanNumCode.indexOf(str[i + 1])) &&
                (0 < i && 0 <= HanNumCode.indexOf(str[i - 1]))) {
                buf += str[i]
            } else {
                break
            }
        }
        return buf
    }

    /**
     * 文字列が整数かの判定
     * @param v     文字列
     * @return      判定結果
     */
    fun isNumber(v: String): Boolean {
        return try {
            v.toInt()
            true
        } catch (e: NumberFormatException) {
            false
        }
    }


    val ZenCode =
        "　！”＃＄％＆’（）＊＋，－．／" +
                "０１２３４５６７８９：；＜＝＞？" +
                "＠ＡＢＣＤＥＦＧＨＩＪＫＬＭＮＯ" +
                "ＰＱＲＳＴＵＶＷＸＹＺ［￥］＾＿" +
                "‘ａｂｃｄｅｆｇｈｉｊｋｌｍｎｏ" +
                "ｐｑｒｓｔｕｖｗｘｙｚ｛｜｝～"
    val HanCode =
        " !\"#$%&'()*+,-./" +
                "0123456789:;<=>?" +
                "@ABCDEFGHIJKLMNO" +
                "PQRSTUVWXYZ[\\]^_" +
                "`abcdefghijklmno" +
                "pqrstuvwxyz{|}~"
    val ZenNumCode = "０１２３４５６７８９．＋－"
    val HanNumCode = "0123456789.+-"

    /**
     * 文字列内の全角英数字を半角に変換する
     * zenStr       文字列
     * return       変換後の文字列
     */
    fun strZne2Han(zenStr: String):String {
        var buf = ""
        for (i in zenStr.indices) {
            val n = ZenCode.indexOf(zenStr[i])
            if (0 <= n && n < HanCode.length) {
                buf += HanCode[n]
            } else {
                buf += zenStr[i]
            }
        }
        return buf
    }

    /**
     * 文字列内の全角数値を半角に変換する
     * zenStr       文字列
     * return       変換後の文字列
     */
    fun strNumZne2Han(zenStr: String): String {
        var buf = ""
        for (i in zenStr.indices) {
            val n = ZenNumCode.indexOf(zenStr[i])
            if (0 <= n && n < HanCode.length) {
                buf += HanNumCode[n]
            } else {
                buf += zenStr[i]
            }
        }
        return buf
    }

    /**
     * 弧で囲まれた文字列を抽出する(ディフォルトは'{','}')
     *  text        文字列
     *  sb          開始括弧
     *  eb          終了括弧
     *  withBacket  抽出した文字列に括弧を含む
     *  return      抽出したカッコ内文字列リスト
     */
    fun extractBrackets(text: String, sb: Char = '{', eb: Char ='}', withBacket: Boolean = false): List<String> {
        var extractText = mutableListOf<String>()
        var bOffset = if (withBacket) 1 else 0
        var pos = 0
        var sp = text.indexOf(sb)
        var ep = text.indexOf(eb)
        if ((0 <= sp && 0 <= ep && ep < sp) || (sp < 0 && 0 <= ep)) {
            var data = text.substring(0, ep + bOffset)
            if (0 < data.length)
                extractText.add(data)
            pos = ep + 1
        }
        while (pos < text.length) {
            var st = text.indexOf(sb, pos)
            var data = ""
            if (pos <= st) {
                var ct = text.indexOf(eb, st)
                if (0 <= ct) {
                    data = text.substring(st + 1 - bOffset, ct + bOffset)
                    pos = ct + 1
                } else {
                    data = text.substring(st + 1 - bOffset)
                    pos = text.length
                }
            } else {
                pos = text.length
            }
            if (0 < data.length)
                extractText.add(data)
        }
        return extractText
    }

    /**
     * カンマセパレータで文字列を分解する
     * "で囲まれている場合は"内の文字列を抽出する
     */
    fun splitCsvString(str: String): List<String> {
        val data = mutableListOf<String>()
        var buf = ""
        var i = 0
        while (i < str.length) {
            if (str[i] == '"' && i < str.length - 1) {
                //  ["]で囲まれた文字列の抽出して登録
                i++
                while (str[i] != '"' && str[i] != '\n') {
                    buf += str[i++]
                    if (i == str.length) break
                }
                data.add(buf)
                i++
            } else if (str[i] == ',' && i < str.length - 1) {
                //  [,]区切りで登録
                if ((0 < i && str[i - 1] == ',') || (i == 0 && str[i] == ','))
                    data.add("")
                i++
            } else {
//                if (str.charAt(i) == ',' && i < str.length() -1)
//                    i++;
                //  [,]または[\n]までの単語を登録
                while (str[i] != ',' && str[i] != '\n') {
                    buf += str[i++]
                    if (i == str.length) break
                }
                data.add(buf)
                i++
            }
            buf = ""
        }
        return data
    }


    /**
     * ファイル名をワイルドカードでマッチングする
     *  (*.csv, a??de.dat・・・)
     * @param srcstr  マッチングファイル名
     * @param findstr ワイルドカード
     * @return
     */
    fun wcMatch(srcstr: String, findstr: String): Boolean {
        if (findstr.isEmpty())
            return true
        var si = 0
        var si2 = -1
        var fi = 0
        var fi2 = -1
        val ss = srcstr.uppercase()
        val fs = findstr.uppercase()
        do {
            if (fs[fi] == '*') {
                fi2 = fi
                fi++
                if (fs.length <= fi) return true
                while (si < ss.length && ss[si] != fs[fi]) {
                    si++
                }
                si2 = si
                if (ss.length <= si) return false
            }
            if (fs[fi] != '?') {
                if (ss.length <= si) return false
                if (ss[si] != fs[fi]) {
                    if (si2 < 0) return false
                    si = si2 + 1
                    fi = fi2
                    continue
                }
            }
            si++
            fi++
            if (fs.length <= fi && si < ss.length)
                return false
            if (ss.length <= si && fi < fs.length && fs[fi] != '*' && fs[fi] != '?')
                return false
        } while (si < ss.length && fi < fs.length)

        return true
    }

    /**
     * 文字列を前から検索する
     * text         検索される文字列
     * v            検索する文字列
     * count        検索回数
     * return       検索位置
     */
    fun indexOf(text: String, v: String, count: Int = 1): Int {
        var n = 0
        for (i in 0..count - 1) {
            n = text.indexOf(v, n + 1)
        }
        return n
    }

    /**
     * 文字列を後から検索する
     * text         検索される文字列
     * v            検索する文字列
     * count        検索回数
     * return       検索位置
     */
    fun lastIndexOf(text: String, v: String, count: Int = 1): Int {
        var n = text.length
        for (i in 0..count - 1) {
            n = text.lastIndexOf(v, n - 1)
        }
        return n
    }

    //  ---  時間・日付処理  ---

    /**
     * 現在の年を取得
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun getNowDate(form:String = "YYYYMMdd_HHmmss"): String {
        val ldt = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern(form)
        return ldt.format(formatter)
    }

    /**
     * Locationデータから日時文字列を作成
     * Format : "yyyy-MM-dd'T'HH:mm:ss'Z'"
     * location     Locationデータ(GPSなど)
     * return       日時文字列
     */
    fun getLocationTime(location: Location): String? {
        val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        val tz = TimeZone.getTimeZone("UTC")
        df.timeZone = tz
        return df.format(location.time)
    }

    /**
     * Location(位置情報)データからFormatを指定して日時文字列を取得(日本時間)
     *  location    Locationデータ(GPSなど)
     *  format      日時文字列のフォーマット(yyyy-MM-dd HH:mm:ss)
     *  return      日時文字列
     */
    fun getLocationTime(location: Location, format: String?): String? {
        val df = SimpleDateFormat(format)
        val tz = TimeZone.getTimeZone("UTC")
        //		TimeZone tz = TimeZone.getTimeZone("JST");
        df.timeZone = tz
        return df.format(location.time + 9 * 3600 * 1000)   //  日本時間補正
    }

    /**
     *  日付文字列に日付型に変換
     *  対応形式
     *  "yyyy-MM-dd'T'HH:mm:ss'Z'"
     *  "yyyy-MM-dd HH:mm:ss"
     *  date              日付文字列
     *  timeZoneOffset   タイムゾーンオフセット値(日本 9)
     */
    fun string2Date(date: String, timeZoneOffset: Int = 9): Date {
        var df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        if (19 < date.length && date[19] == 'Z') {
            //  標準時間
            df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
            val dateTime = df.parse(date)
            return Date(dateTime.getTime() + (timeZoneOffset * 60 * 60 * 1000))
        } else if (23 < date.length && date[23] == 'Z') {
            //  「山と高原地図対応
            df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss'Z'")
            val dateTime = df.parse(date)
            return Date(dateTime.getTime() + (timeZoneOffset * 60 * 60 * 1000))
        } else {
            return df.parse(date)
        }
    }

    /**
     * Dateをフォーマットにしたがって文字列に変換する
     * 例: date2String(date, "yyyy/MM/dd HH:mm:ss")
     *  date             Date()時間
     *  format           変換フォーマット
     *  timeZoneOffset   タイムゾーンオフセット値(日本 9)
     */
    fun date2String(date: Date, format: String, timeZoneOffset: Int = 9): String {
        val dateTime = Date(date.getTime() + (timeZoneOffset * 60 * 60 * 1000))
        val df = SimpleDateFormat(format)
        return df.format(dateTime)
    }

    /**
     * 経過時間を文字列に変換
     *  var lap = Date(date.getTime() - date2.getTime())
     *  println(lap2String(lap.getTime()))
     *  lapTime     経過時間(ms)
     *  return      "HH:mm:ss"または"d HH:mm:ss"
     */
    fun lap2String(lapTime: Long): String {
        val lap = lapTime / 1000
        val ss = lap % 60
        val mm = (lap / 60) % 60
        val hh = (lap / 60 / 60) % 24
        val dd = (lap / 60 / 60 / 24)
        if (0 < dd)
            return String.format("%d %02d:%02d:%02d", dd, hh, mm, ss)
        else
            return String.format("%02d:%02d:%02d", hh, mm, ss)
    }

    /**
     * 日時を分単位で丸める
     * et           日時
     * min          丸める分
     * return       真似メタ日時
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun roundDateTimeMin(et: LocalDateTime, min: Long): LocalDateTime {
        val st = LocalDateTime.of(2000, 1, 1, 0, 0, 0)
        val dt = ChronoUnit.MINUTES.between(st, et)
        val rt = st.plusMinutes(floor(dt.toFloat() / min.toFloat()).toLong() * min)
        return rt
    }

    //  ---  ファイル処理  ---

    /**
     * CSV形式でListデータを保存する
     * path     ファイルパス
     * format   CSVファイルのタイトル行
     * data     リストデータ
     */
    fun saveCsvData(path: String, format: List<String>, data: List<List<String>>) {
        var buffer: String = getCsvData(format) + "\n"
        for (i in data.indices) {
            if (0 < data[i].size) {
                buffer += getCsvData(data[i])
                buffer += "\n"
            }
        }
        if (buffer.isNotEmpty()) {
            writeFileData(path, buffer)
        }
    }

    /**
     * ListデータをCSV形式で保存する
     * path     ファイルパス
     * data     Listデータ
     */
    fun saveCsvData(path: String, data: List<List<String>>) {
        var buffer = ""
        for (i in data.indices) {
            if (0 < data[i].size) {
                buffer += getCsvData(data[i])
                buffer += "\n"
            }
        }
        if (buffer.isNotEmpty()) {
            writeFileData(path, buffer)
        }
    }

    /**
     * Listデータを"で挟んだカンマセパレータ形式の文字列にする
     * data     Listデータ
     * return   CDSV形式の文字列
     */
    fun getCsvData(datas: List<String>): String {
        var buffer = ""
        //  配列データをCSV形式になるように["]でくくって[,]で区切った文字列にする
        for (j in datas.indices) {
            buffer += "\"" + datas[j] + "\""
            if (j < datas.size - 1)
                buffer += ","
        }
        return buffer
    }

    /**
     * CSV形式のファイルを読み込んでList<List<String>>形式にする
     *
     * title[]が指定されて一行目にタイトルが入っていればタイトルの順番に合わせて取り込む
     * titleがnullであればそのまま配列に置き換える
     * @param path      ファイルパス
     * @param title     タイトルの配列
     * @return          List<String>のLitsデータ
     */
    fun loadCsvData(path: String, title: List<String>): List<List<String>> {
        val listData = mutableListOf<List<String>>()
        if (!existsFile(path))
            return listData

        //	ファイルデータの取り込み
        val fileData = mutableListOf<String>()
        fileData.clear()
        readTextFile(path, fileData)

        //	フォーマットの確認(タイトル行の展開)
        var start = 1
        val titleNo = Array(title.size) {0}
        if (0 < fileData.size) {
            val fileTitle: List<String> = splitCsvString(fileData[0])
            if (0 < title.size && fileTitle[0].compareTo(title[0]) == 0) {
                //	データの順番を求める
                for (n in title.indices) {
                    titleNo[n] = -1
                    for (m in fileTitle.indices) {
                        if (title[n].compareTo(fileTitle[m]) == 0) {
                            titleNo[n] = m
                            break
                        }
                    }
                }
                start = 1
            } else {
                //  タイトルがない場合そのまま順で追加
                for (n in title.indices)
                    titleNo[n] = n
                start = 0
            }
        } else {
            return listData
        }

        //  CSVデータを配列にしてListに登録
        for (i in start until fileData.size) {
            val text: List<String> = splitCsvString(fileData[i])
            //  指定のタイトル順に並べ替えて追加
            val buffer = mutableListOf<String>()
            for (n in title.indices) {
                if (0 <= titleNo[n] && titleNo[n] < text.size) {
                    buffer.add(text[titleNo[n]])
                } else {
                    buffer.add("")
                }
            }
            listData.add(buffer)
        }
        return listData
    }

    /**
     * CSV形式のファイルを読み込んでList<List<String>>形式にする
     * titleがnullであればそのまま配列に置き換える
     * @param path      ファイルパス
     * @return          List<List<String>>のデータ
     */
    fun loadCsvData(path: String): List<List<String>> {
        val listData = mutableListOf<List<String>>()
        if (!existsFile(path))
            return listData
        //	ファイルデータの取り込み
        val fileData = mutableListOf<String>()
        fileData.clear()
        readTextFile(path, fileData)

        //  CSVデータを配列にしてListに登録
        for (i in fileData.indices) {
            val text: List<String> = splitCsvString(fileData[i])
            listData.add(text)
        }
        return listData
    }

    /**
     * Listテキストデータをファイルに保存
     * @param path      ファイルパス
     * @param data      List型のテキストデータ
     */
    fun saveTextData(path: String, data: List<String>) {
        var buffer = ""
        for (text in data)
            buffer += text + "\n"
        if (buffer.isNotEmpty()) {
            writeFileData(path, buffer)
        }
    }

    /**
     * ファイルのテキストデータをListデータに取り込む
     * @param path      ファイルパス
     * @return          Listデータ
     */
    fun loadTextData(path: String): List<String> {
        val fileData = mutableListOf<String>()
        fileData.clear()
        if (!existsFile(path)) {
            return fileData
        }
        readTextFile(path, fileData)
        return fileData
    }

    //  ファイル読込時のEncode設定
    val mCharSet = listOf("UTF_8", "SJIS", "EUC-JP")
    var mFileEncode = 0

    /**
     * テキストファイルを1行単位読み込んでListに格納する
     * path     ファイルパス
     * fileText Listファイル(出力)
     */
    fun readTextFile(path: String, fileText: MutableList<String>) {
        //Log.d(TAG, "readTextFile ");
        try {
            val file = File(path)
//            val br = BufferedReader(FileReader(file))
            val br = file.bufferedReader(Charset.forName(mCharSet[mFileEncode]))
            var str = br.readLine()
            //  BOM付きの場合、BOM削除
            if (str.startsWith("\uFEFF"))
                str = str.substring(1)
            while (str != null && 0 <= str.length) {
//                Log.d(TAG, "readTextFile " + str);
                fileText.add(str)
                str = br.readLine()
            }
            br.close()
        } catch (e: FileNotFoundException) {
            Log.d(TAG, "readTextFile " + e.message)
        } catch (e: IOException) {
            Log.d(TAG, "readTextFile " + e.message)
        } catch (e: Exception) {
            Log.d(TAG, "readTextFile " + e.message)
        }
    }

    /**
     * テキストデータをファイルに書き込む
     * path     ファイルパス
     * buffer   テキストデータ
     */
    fun writeFileData(path: String, buffer: String) {
        //Log.d(TAG, "writeFileData: "+ path + " " + buffer);
        try {
            val fw = FileWriter(path)
            fw.write(buffer)
            fw.close()
        } catch (e: IOException) {
            Log.d(TAG, "writeFileData " + e.message)
        }
    }

    /**
     * テキストデータを追加でファイルに書き込む
     * path     ファイルパス
     * buffer   テキストデータ
     */
    fun writeFileDataAppend(path: String, buffer: String) {
        //Log.d(TAG, "writeFileData: "+ path + " " + buffer);
        try {
            val fw = FileWriter(path, true)
            fw.write(buffer)
            fw.close()
        } catch (e: IOException) {
            Log.d(TAG, "writeFileData " + e.message)
        }
    }

    /**
     * ディレクトリかどうかの確認
     * @param       path
     * @return      true/false
     */
    fun isDirectory(path: String): Boolean {
        val file = File(path)
        return file.isDirectory
    }

    /**
     * ディレクトリの作成
     * @param       path
     * @return      true/false
     */
    fun mkdir(path: String): Boolean {
        val file = File(path)
        return if (!file.exists()) file.mkdirs() else true
    }

    /**
     * ファイルの存在チェック
     * path     ファイルパス
     * return    存在の有無
     */
    fun existsFile(path: String): Boolean {
        val file = File(path)
        return file.exists()
    }

    /**
     * ファイル削除
     * path     ファイルパス
     * return   削除成否
     */
    fun removeFile(path: String): Boolean {
        val file = File(path)
        if (file.exists())
            return file.delete()
        return false
    }

    /**
     * 再帰的にディレクトリを削除
     * path     ディレクトリ名
     */
    fun deleteDirectory(path: String) {
        val file = File(path)
        deleteDirectory(file)
    }

    /**
     * 再帰的にディレクトリを削除
     * file     Fileデータ
     */
    fun deleteDirectory(file: File) {
        if (file.isDirectory) {
            val contents = file.listFiles()
            for (f in contents) {
                deleteDirectory(f)
            }
        }
        file.delete()
    }

    /**
     * ファイルのコピー
     * @param srcPath コピー元のパス名
     * @param destDir コピー先フォルダー名
     * @return
     */
    fun copyFile(srcPath: String, destDir: String): Boolean {
//        Log.d(TAG, "copyFile: " + srcPath + " ," + destDir);
        val sfile = File(srcPath)
        return copyfile(srcPath!!, destDir + "/" + sfile.name)
    }

    /**
     * ファイルのコピー(コピー先ファイル名の変更も可能)
     * @param srFile コピー元のパス名
     * @param dtFile コピー先のパス名
     * @return
     */
    fun copyfile(srFile: String, dtFile: String): Boolean {
        try {
            val fs = File(srFile)
            val fd = File(dtFile)
            fs.copyTo(fd, true)
            return true
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * ファイルの移動(ディレクトリ指定)
     * @param orgFilePath   元のファイルパス
     * @param destDir       移動先ディレクトリ
     * @return              実行結果
     */
    fun moveFile(orgFilePath: String, destDir: String): Boolean {
        // 移動もとなるファイルパス
        val file = File(orgFilePath)
        val dir = File(destDir)
        return file.renameTo(File(dir, file.name))
    }

    /**
     * ファイル名変更
     * @param orgFilePath   元のファイルパス
     * @param destFilePath  移動(変更)先ファイルパス
     * @return              実行結果
     */
    fun renameFile(srFile: String, dtFile: String): Boolean {
        try {
            val fs = File(srFile)
            val fd = File(dtFile)
            fs.renameTo(fd)
            return true
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * ファイルのフルパスを取得
     * @param path  パス
     * @return フルパス
     */
    fun getFullPath(path: String): String {
        val file = File(path)
        return try {
            file.canonicalPath
        } catch (e: java.lang.Exception) {
            ""
        }
    }

    /**
     * ファイルの親ディレクトリを取得
     * path         ファイルパス
     * return       親ディレクトリ
     */
    fun getFolder(path: String):String {
        val file = File(path)
        return file.parent
    }

    /**
     * 内部メモリのディレクトリの取得
     * @return          ディレクトリ
     */
    fun getInternalStrage(): String {
        return Environment.getExternalStorageDirectory().absolutePath
    }

    /**
     * SDカードのディレクトリの取得
     * @param context   コンテキスト
     * @return          ディレクトリ
     */
    fun getExternalStrage(context: Context): String {
        val dirList = getSdCardFilesDirs(context)
        if (0 < dirList.size) {
            var externalDir = dirList[0]
            externalDir = externalDir.substring(0, indexOf(externalDir, "/", 2))
            return externalDir
        }
        return ""
    }

    /**
     *  SDカード（外部ストレージ）のパスを取得する
     */
    fun getSdCardFilesDirs(context: Context): ArrayList<String> {
        val sdCardDirs: ArrayList<String> = ArrayList()
        val externalStoragePaths = getExternalFilesDirs(context,null)
        externalStoragePaths.forEach { path ->
            path?.let {
                if (Environment.isExternalStorageRemovable(it)) {
                    sdCardDirs.add(it.absolutePath)
                    Log.d(TAG,"SDカードのパス: $it.absolutePath")
                }
            }
        }
        return sdCardDirs
    }


    /**
     * ファイル名の抽出
     * path         パス名
     * @return      ファイル名
     */
    fun getName(path: String): String {
        val file = File(path)
        return file.name
    }

    /**
     * ファイル名の取得拡張子なし
     */
    fun getFileNameWithoutExtension(path: String):String {
        val file = File(path)
        return file.nameWithoutExtension
    }

    /**
     * ファイル名の拡張子を取得(ディレクトリを除く)
     *
     * @param path
     * @return
     */
    fun getNameExt(path: String): String {
        val name: String = getName(path)
        val n = name.lastIndexOf('.')
        return if (n < 0) "" else name.substring(n + 1)
    }

    /**
     * 拡張しなしのパッケージ名の取得
     * @return  パッケージ名
     */
    fun getPackageNameWithoutExt(context: Context): String {
        return context.getPackageName().substring(context.getPackageName().lastIndexOf('.') + 1)
    }

    /**
     * データファイルの保存用にパッケージ名のディレクトリの作成
     * @return      ファイルアクセスディレクトリパス
     */
    fun getPackageNameDirectory(context: Context): String {
        return setSaveDirectory(context.getPackageName().substring(context.getPackageName().lastIndexOf('.') + 1))
    }

    /**
     * データ保存ディレクトリの設定(ファイルのアクセスにはpermissionの設定が必要)
     * WRITE_EXTERNAL_STORAGE / READ_EXTERNAL_STORAGE
     * @param subName パッケージ名(getPackageName().substring(getPackageName().lastIndexOf('.')+1))
     * @return 保存ディレクトリ
     */
    fun setSaveDirectory(subName: String): String {
        //	データ保存ディレクトリ
        var saveDirectory = Environment.getExternalStorageDirectory().toString() + "/" + subName
        if (!existsFile(saveDirectory) && !isDirectory(saveDirectory)) {
            if (!mkdir(saveDirectory)) {
                Log.d(TAG, "setSaveDirectory: $saveDirectory")
                saveDirectory = Environment.getExternalStorageDirectory().toString()
            }
        }
        return saveDirectory
    }

    /**
     * 共通ストレージ(DCIM)にパッケージ名を追加したディレクトリの作成とパスの取得
     */
    fun setDCIMPackageNameDirectory(context: Context): String {
        var saveDirectory = getDCIMPackageName(context)
        if (!existsFile(saveDirectory) && !isDirectory(saveDirectory)) {
            if (!mkdir(saveDirectory)) {
                Log.d(TAG, "setSaveDirectory: $saveDirectory")
                saveDirectory = Environment.getExternalStorageDirectory().toString()
            }
        }
        return saveDirectory
    }

    /**
     * 共通ストレージ(DCIM)にパッケージ名を追加したディレクトリパスの取得
     */
    fun getDCIMPackageName(context: Context): String {
        return  getDCIMDirectory() + "/" + context.getPackageName().substring(context.getPackageName().lastIndexOf('.') + 1)
    }

    /**
     * 共通ストレージ(DCIM)のパスの取得
     */
    fun getDCIMDirectory():String {
        val extStrageDir = Environment.getExternalStorageDirectory()    //  /strage/emulated/0
        return extStrageDir.absolutePath + "/" + Environment.DIRECTORY_DCIM
    }

    /**
     * ファイル検索してリスト化
     * path     検索ディレクトリ
     * subdir   再帰検索の有無(default: true)
     * filter   フィルタ(ワイルドカード) (default: フィルタなし)
     * return   ファイルリスト(File形式)
     */
    fun getFileList(path: String, subdir: Boolean = true, filter: String = ""): List<File> {
        val fileList = mutableListOf<File>()
        val dir = File(path)
        val files = dir.listFiles()
        if (null != files) {
            for (i in files.indices) {
                if (files[i].isFile) {
                    if (filter.isEmpty() || wcMatch(files[i].name, filter))
                        fileList.add(files[i])
                } else if (subdir) {
                    fileList.addAll(getFileList(files[i].path, subdir, filter))
                }
            }
        }
        return fileList
    }

    /**
     * ディレクトリ検索(再帰取得なし)
     * @param path      検索パス
     * @return          検索ディレクトリリスト
     */
    fun getDirList(path: String): List<String> {
        val fileList = mutableListOf<String>()
        val dir = File(path)
        val files = dir.listFiles()
        if (null != files) {
            val fpath = getFullPath(path)
            if (fpath.compareTo("/") != 0 && fpath.compareTo("/storage") != 0) fileList.add("..")
            //  ディレクトリの取得
            for (i in files.indices) {
                if (!files[i].isFile) {
                    fileList.add(files[i].name)
                }
            }
        } else {
            fileList.add("..")
            fileList.add("0")
        }
        return fileList
    }

    /**
     * ディレクトリとファイル検索(再帰取得なし)
     * 取得したディレクトリはファイルと区別するため、[dir]と括弧で囲む
     *
     * @param path      検索ディレクトリパス
     * @param filter    ファイルフィルタ(ワイルドカード)
     * @param getDir    ディレクトリ取得の可否
     * @return          ファイル・ディレクトリリスト
     */
    fun getDirFileList(path: String, filter: String, getDir: Boolean): List<String> {
        Log.d(TAG, "getDirFileList: " + path + " " + filter + " " + getDir)
        val fileList = mutableListOf<String>()
        val dir = File(path)
        val files = dir.listFiles()
        if (null != files) {
            Log.d(TAG, "getDirFileList: " + files.size)
            val fpath: String = getFullPath(path)
            if (getDir) {
                if (fpath.compareTo("/") != 0 && fpath.compareTo("/storage") != 0)
                    fileList.add("[..]")
                //  ディレクトリの取得
                for (i in files.indices) {
                    if (!files[i].isFile) {
                        fileList.add("[" + files[i].name + "]")
                    }
                }
            }
            //  ファイルの取得
            for (i in files.indices) {
                if (files[i].isFile) {
                    val fileName = files[i].name
                    if (filter.isEmpty() || wcMatch(fileName, filter))
                        fileList.add(files[i].name)
                }
            }
        } else {
            fileList.add("[..]")
            fileList.add("[0]")
        }
        return fileList
    }

    /***
     * ファイルのMIME(Multipurpose Internet Mail Extension)タイプを求める
     * 例えば pdfファイルであれば application/pdf を返す
     * @param path      ファイルのパス
     * @return MIMEタイプ
     */
    fun getMimeType(path: String): String {
        //  拡張子を求めるが全角文字だとエラーになるようなので自前を使う
        var extention = MimeTypeMap.getFileExtensionFromUrl(path)
        if (extention == null || extention.length < 1) extention = getNameExt(path)
        extention = extention!!.lowercase()
        var mimetype = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extention)
        if (mimetype == null) {
            mimetype = "application/" + getNameExt(path)
        }
        return mimetype
    }

    /**
     * URIからパスを取得する
     * uri      ファイルのURI
     * return   フルパス
     */
    fun getUriPath(c: Context, uri: Uri): String {
        val uriPath = uri.path.toString()
        var path = getInternalStrage()
        var n = uriPath.indexOf(":") + 1
        if (n < 0)
            n = indexOf(uriPath, "/",1) + 1
        path += "/" + uriPath.substring(n)
        if (!existsFile(path))
            path = getExternalStrage(c) + "/" + uriPath.substring(n)
        return path
    }


    //  ---  HTMLファイル  ---

    //  === ダイヤログ関数 ===

    /**
     * メッセージダイヤログ
     * @param c         コンテキスト
     * @param title     ダイヤログのタイトル
     * @param message   デフォルトメッセージ
     */
    fun messageDialog(c: Context, title: String, message: String) {
        AlertDialog.Builder(c)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    /**
     * メッセージをダイヤログ表示し、OKの時に指定の関数にメッセージの内容を渡して処理するを処理する
     * 関数インターフェースの例
     *  var iDelListOperation = new Consumer<String> { s ->
     *      mDataMap.remove(s)      //  ダイヤログで指定された文字列をリストから削除
     *  }
     * 関数の呼び出し方法
     *      ylib.messageDialog(mC, "計算式の削除",mTitleBuf, iDelListOperation);
     * c            コンテキスト
     * title        ダイヤログのタイトル
     * message      メッセージ
     * operation    処理する関数インタフェース
     */
    fun messageDialog(c: Context, title: String, message: String, operation: Consumer<String>) {
        AlertDialog.Builder(c)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton( "OK") {
                    dialog, which -> operation.accept("OK")
            }
            .setNegativeButton( "Cancel") {
                    dialog, which ->
            }
            .show()
    }

    /**
     * メッセージをダイヤログ表示し、OK/No/Cancelのどれかを指定の関数にメッセージを渡す
     * 関数インターフェースの例
     *  var iResultOperation = Consumer<String> { s ->
     *      if (s.compareTo("OK") == 0) {
     *      } else if (s.compareTo("No") == 0) {
     *      } else {
     *      }
     *  }
     * 関数の呼び出し方法
     *      ylib.messageDialog(mC, "計算式の削除",mTitleBuf, iDelListOperation);
     * c            コンテキスト
     * title        ダイヤログのタイトル
     * message      メッセージ
     * operation    処理する関数インタフェース
     */
    fun messageDialog2(c: Context, title: String, message: String, operation: Consumer<String>) {
        AlertDialog.Builder(c)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton( "OK") {
                    dialog, which -> operation.accept("OK")
            }
            .setNeutralButton("No") {
                    dialog, which -> operation.accept("No")
            }
            .setNegativeButton( "Cancel") {
                    dialog, which ->
            }
            .show()
    }

    /**
     * 文字入力ダイヤログ
     * 入力した文字は関数インタフェースを使って取得
     *     var iInputOperation = Consumer<String> { s ->
     *         Toast.makeText(this, s, Toast.LENGTH_LONG).show()
     *         Log.d(TAG,"inputDialog: " + s)
     *     }
     * @param c         コンテキスト
     * @param title     ダイヤログのタイトル
     * @param message   デフォルトメッセージ
     * @param operation 処理する関数(関数インターフェース)
     */
    fun setInputDialog(c: Context, title: String, message: String, operation: Consumer<String>) {
        val editText = EditText(c)
        editText.setText(message)
        val dialog = AlertDialog.Builder(c)
        dialog.setTitle(title)
        dialog.setView(editText)
        dialog.setPositiveButton("OK", DialogInterface.OnClickListener {
                dialog, which -> operation.accept(editText.text.toString())
        })
        dialog.setNegativeButton("Cancel", null)
        dialog.show()
    }

    /**
     * メニュー選択ダイヤログ
     * 選択したメニューは関数インタフェースを使って取得
     *    var iPostionSelectOperation = Consumer<String> { s ->
     *        Toast.makeText(this, s, Toast.LENGTH_LONG).show()
     *        Log.d(TAG,"inputDialog: " + s)
     *    }
     * @param title     ダイヤログのタイトル
     * @param menu      メニューデータ(配列)
     * @param operation 処理する関数(関数インターフェース)
     */
    fun setMenuDialog(c: Context, title: String, menu: List<String>, operation: Consumer<String>) {
        AlertDialog.Builder(c)
            .setTitle(title)
            .setItems(menu.toTypedArray(), DialogInterface.OnClickListener { dialog, which ->
//                    Toast.makeText(c, which.toString() + " " + menu[which] + " が選択", Toast.LENGTH_LONG).show()
                operation.accept(menu[which])
            })
            .create()
            .show()
    }


    /**
     * チェックボックス付きリスト選択ダイヤログ
     *  例
     *    klib.setChkMenuDialog(this, "表示設定", itemList, chkList, iGpsVisibleOperation)
     *
     *    var iGpsVisibleOperation = Consumer<BooleanArray> { s ->
     *      var dataList = mGpsDataList.getDataList(mGpsDataList.mPreSelectGroup)
     *      for (i in 0..s.size - 1) {
     *          Log.d(TAG,"iGpsVisibleOperation: "+i+" "+s[i])
     *          dataList[i].mVisible = s[i]
     *      }
     *    }
     *
     * @param c         コンテキスト
     * @param title     タイトル
     * @param menu      リスト文字列  (Array)
     * @param chkItem   チェックボックスの設定 (BoolearnArray)
     * @param operation 関数インタフェース
     */
    fun setChkMenuDialog(c: Context, title: String, menu: Array<String>, chkItems: BooleanArray, operation: Consumer<BooleanArray>) {
        AlertDialog.Builder(c)
            .setTitle(title)
            .setMultiChoiceItems(menu, chkItems, { dialog, which, isChecked ->
//                    operation.accept(menu[which])
            })
            .setPositiveButton("OK") { dialog, which ->
                operation.accept(chkItems)
//                    for (i in chkItems) {
//
//                    }
            }
            .setNegativeButton("Cancel") { dialog, which -> }
            .create()
            .show()
    }

    /**
     * ファイル選択ダイヤログ
     * 使用例
     *  fun markImport() {
     *      klib.fileSelectDialog(this, klib.getPackageNameDirectory(this), "*.csv", true, iFilePath)
     *  }
     *  var  iFilePath = Consumer<String>() { s ->
     *      if (0 < s.length) {
     *          mMarkList.loadMarkFile(s, true)
     *      }
     *  }
     * @param   dir                     初期ディレクトリ
     * @param   filter                  ファイル名(ワイルドカード)
     * @param   getDir(directryの有無)   サブディレクトリ検索有無
     * @param   path                    実行関数インターフェース
     */
    fun fileSelectDialog(c: Context, dir: String, filter: String, getDir: Boolean, path: Consumer<String>) {
        //  ストレージを考慮したディレクトリリスト
        val folder = replaceStrageFileFolder(c, dir)
        val fileList = getStrageFileList(c, folder, filter, getDir)
        Log.d(TAG,"fileSelectDialog: "+folder+"  "+dir)
//        val fileList = getDirFileList(dir, filter, getDir) //  ファイルリストの取得
        //  ファイルリストのソート
        val sortList = fileList.sortedWith{ t, t2 -> fileCompare(t, t2)}
        //  ファイルリストのソート(ラムダ式)
//        var sortList = fileList.sortedWith{ t, t2 ->
//            when {
//                0 < t.compareTo(t2) -> {
//                    if (t[0] == '[' && t2[0] != '[') -1
//                    else if (t2[0] == '[' && t[0] != '[') 1
//                    else 1
//                }
//                else -> {
//                    if (t[0] == '[' && t2[0] != '[') -1
//                    else if (t2[0] == '[' && t[0] != '[') 1
//                    else -1
//                }
//            }
//        }
        AlertDialog.Builder(c)
            .setTitle("ファイル選択 [" + getFullPath(folder) + "]")
            .setItems(sortList.toTypedArray()) { dialog, which ->
                val fname = sortList[which]
                Toast.makeText(c, which.toString() + " " + fname + " が選択", Toast.LENGTH_LONG).show()
                if (fname[0] == '[') {         //  ディレクトリの時はファイル選択のダイヤログを開きなおす
                    val directory: String = getFullPath(folder + "/" + fname.substring(1, fname.length - 1))
                    fileSelectDialog(c, directory, filter, getDir, path)
                } else {                            //  ファイルを選択したときは与えられた関数を実行
                    path.accept("$dir/$fname")
                }
            }
            .create()
            .show()
    }

    /***
     *  保存ファイル名選択ダイヤログ
     *  ファイル一覧でファイルまたはディレクトリを選択した後にファイル名変更ダイヤログを表示する
     *  ディレクトリだけで選択を終えるときは[OK]ボタンを押して進む
     *  使用例
     *  fun markExport() {
     *      klib.saveFileSelectDialog(this, klib.getPackageNameDirectory(this), "*.csv", true, iDirPath)
     *  }
     *  var  iDirPath = Consumer<String>() { s ->
     *      if (0 < s.length) {
     *          mMarkList.saveMarkFile(s)
     *      }
     *  }
     * @param   dir     初期ディレクトリ
     * @param   filter  ファイル名のワイルドカード (ex. "*.csv")
     * @param   getDir(directryの有無) ディレクトリ表示の有無
     * @param   path    選択結果の関数インターフェース
     */
    fun saveFileSelectDialog(c: Context, dir: String, filter: String, getDir: Boolean, path: Consumer<String>) {
        //  ストレージを考慮したディレクトリリスト
        val folder = replaceStrageFileFolder(c, dir)
        val fileList = getStrageFileList(c, folder, filter, getDir)
        Log.d(TAG,"saveFileSelectDialog: "+folder+"  "+dir)
//        val fileList = getDirFileList(dir, filter, getDir) //  ファイルリストの取得
        //  ファイルリストのソート
        val sortList = fileList.sortedWith{ t, t2 -> fileCompare(t, t2)}
        AlertDialog.Builder(c)
            .setTitle("ファイル選択 [" + getFullPath(folder) + "]")
            .setItems(sortList.toTypedArray()) { dialog, which ->
                val fname = sortList[which]
                Toast.makeText(c, which.toString() + " " + fname + " が選択", Toast.LENGTH_LONG).show()
                if (fname[0] == '[') {         //  ディレクトリの時はファイル選択のダイヤログを開きなおす
                    val directory: String = getFullPath(folder + "/" + fname.substring(1, fname.length - 1))
                    saveFileSelectDialog(c, directory, filter, getDir, path)
                } else {                            //  ファイルを選択したときは与えられた関数を実行
                    setInputDialog(c, "ファイル名", "$folder/$fname", path)
                }
            }
            .setPositiveButton("OK") { dialog, which ->
                setInputDialog(c, "ファイル名編集", folder, path)
            }
            .setNegativeButton("Cancel") { dialog, which -> }
            .create()
            .show()
    }

    /**
     * アクセス不可のディレクトリはストレージ名に置換える
     * c            コンテキスト
     * dir          ディレクトリ名
     * return       ストレージ名に置換えたディレクトリ名
     */
    fun getStrageFileList(c: Context, folder: String, filter: String, getDir: Boolean): List<String> {
        var dirList: List<String>
        if ((folder.count{it.compareTo('/')==0} == 2 && 0 < folder.indexOf("emulated"))
            || folder.count{it.compareTo('/')==0} == 1) {
            if (0 < getExternalStrage(c).length)
                dirList = listOf("[内部ストレージ]", "[SDカード]")
            else
                dirList = listOf("[内部ストレージ]")
        } else {
            dirList = getDirFileList(folder, filter, getDir)
        }
        return dirList
    }


    /**
     * フォルダ選択ダイヤログ
     * 選択結果(パス)は関数インターフェースで処理する
     * 関数インターフェース例
     *  fun markExport() {
     *      klib.folderSelectDialog(this, klib.getPackageNameDirectory(this),iDirPath)
     *  }
     *  var  iDirPath = Consumer<String>() { s ->
     *      if (0 < s.length) {
     *          mMarkList.loadMarkFile(s, true)
     *      }
     *  }
     * @param c         コンテキスト
     * @param dir       初期ディレクトリ
     * @param operation 選択したファイルのフルパスで実行する関数インターフェース
     */
    fun folderSelectDialog(c: Context, dir: String, operation: Consumer<String>) {
        //  ストレージを考慮したディレクトリリスト
        val folder = replaceStrageFolder(c, dir)
        val dirList = getStrageDirList(c, folder)
        Log.d(TAG,"folderSelectDialog: "+dir+"  "+folder)
        //  ファイルソート
        val directries = dirList.sortedWith{ t, t2 -> fileCompare(t, t2)}
        AlertDialog.Builder(c)
            .setTitle("フォルダ選択 [" + getFullPath(folder) + "]")
            .setItems(directries.toTypedArray()) { dialog, which ->
                val fname = directries[which]
                Toast.makeText(c, which.toString() + " " + fname + " が選択", Toast.LENGTH_LONG).show()
                val directory = getFullPath("$folder/$fname")
                folderSelectDialog(c, directory, operation)
            }
            .setPositiveButton("OK") { dialog, which -> operation.accept(dir) }
            .setNegativeButton("Cancel") { dialog, which -> }
            .create()
            .show()
    }

    /**
     * アクセス不可のディレクトリはストレージ名に置換える
     * c            コンテキスト
     * folder       ディレクトリ名
     * return       ストレージ名に置換えたディレクトリリスト
     */
    fun getStrageDirList(c: Context, folder: String): List<String> {
        var dirList: List<String>
        if ((folder.count{it.compareTo('/')==0} == 2 && 0 < folder.indexOf("emulated"))
            || folder.count{it.compareTo('/')==0} == 1) {
            if (0 < getExternalStrage(c).length)
                dirList = listOf("[内部ストレージ]", "[SDカード]")
            else
                dirList = listOf("[内部ストレージ]")
        } else {
            dirList = getDirList(folder)
        }
        return dirList
    }

    /**
     * ディレクトリにストレージ名が含まれていた時に実際のディレクトリ名に置換える
     * c            コンテキスト
     * dir          ストレージ名を含むディレクトリ名
     * return       実際のディレクトリ名
     */
    fun replaceStrageFileFolder(c: Context, dir: String): String {
        var folder = dir
        if (folder.count{it.compareTo('/')==0} < 4) {
            if (0 < folder.indexOf("内部ストレージ"))
                folder = getInternalStrage()
            else if (0 < folder.indexOf("SDカード"))
                folder = getExternalStrage(c)
        }
        return folder
    }

    /**
     * ディレクトリにストレージ名が含まれていた時に実際のディレクトリ名に置換える
     * c            コンテキスト
     * dir          ストレージ名を含むディレクトリ名
     * return       実際のディレクトリ名
     */
    fun replaceStrageFolder(c: Context, dir: String): String {
        var folder = dir
        if (folder.count{it.compareTo('/')==0} < 4) {
            if (0 < folder.indexOf("[内部ストレージ]"))
                folder = getInternalStrage()
            else if (0 < folder.indexOf("[SDカード]"))
                folder = getExternalStrage(c)
        }
        return folder
    }

    /**
     * ファイル選択ダイヤログのファイルソートの比較関数
     * []を優先して昇順(a→z)で並べ替える
     */
    private fun fileCompare(t: String, t2: String):Int {
        return when {
            0 < t.compareTo(t2) -> {
                if (t[0] == '[' && t2[0] != '[') -1
                else if (t2[0] == '[' && t[0] != '[') 1
                else 1
            }
            else -> {
                if (t[0] == '[' && t2[0] != '[') -1
                else if (t2[0] == '[' && t[0] != '[') 1
                else -1
            }
        }
    }


    //  ====  行列計算  ====
    //  https://qiita.com/sekky0816/items/8c73a7ec32fd9b040127
    //  マトリックスの初期化
    //    var c = arrayOf(
    //        arrayOf(0.0, 1.0, 2.0),
    //        arrayOf(1.0, 2.0, 3.0)
    //    )

    /**
     * 単位行列の作成(正方行列のみ)
     * unit         行列のサイズ
     * return       単位行列
     */
    fun unitMatrix(unit: Int): Array<Array<Double>> {
        var matrix = Array(unit, {Array<Double>(unit){0.0}})
        for (i in 0 until unit) {
            for (j in 0 until unit) {
                if (i == j)
                    matrix[i][j] = 1.0
                else
                    matrix[i][j] = 0.0
            }
        }
        return matrix
    }

    /**
     * row x col の行列作成
     * row      行数
     * col      列数
     * retrun   0 行列
     */
    fun createMatrix(row: Int, col: Int): Array<Array<Double>> {
        var matrix = Array(row, {Array<Double>(col){0.0}})
        for (i in matrix.indices) {
            for (j in matrix[i].indices)
                matrix[i][j] = 0.0
        }
        return matrix
    }

    /**
     * マトリックスを複製する
     * A            マトリックス
     * retrun       複製したマトリックス
     */
    fun copyMatrix(A: Array<Array<Double>>): Array<Array<Double>> {
        var copyA = Array(A.count(), {Array<Double>(A[0].count()){0.0}})
        for (i in A.indices) {
            for (j in A[i].indices) {
                copyA[i][j] = A[i][j]
            }
        }
        return copyA
    }

    /**
     * 転置行列  行列Aの転置A^T
     * A            行列
     * return       転置した行列
     */
    fun matrixTranspos(A: Array<Array<Double>>): Array<Array<Double>> {
        var AT = createMatrix(A[0].count(), A.count())
        for (i in A[0].indices) {
            for (j in A.indices) {
                AT[i][j] = A[j][i]
            }
        }
        return AT
    }

    /**
     * 行列の積  AxB
     * 行列の積では
     *      結合の法則  (AxB)xC = Ax(BxC)
     *      分配の法則 (A+B)xC = AxC+BxC , Cx(A+B) = CxA + CxB
     *  A       行列 A
     *  B       行列 B
     *  return  積の結果行列
     */
    fun matrixMulti(A: Array<Array<Double>>, B: Array<Array<Double>>): Array<Array<Double>> {
        var product = createMatrix(A.count(), B[0].count())
        for (i in A.indices) {
            for (j in B[i].indices) {
                for (k in A[i].indices) {
                    product[i][j] += A[i][k] * B[k][j]
                }
            }
        }
        return product
    }

    /**
     * 行列の和 A+B
     * 異なるサイズの行列はゼロ行列にする
     *  A       行列 A
     *  B       行列 B
     *  return  和の結果行列
     */
    fun matrixAdd(A: Array<Array<Double>>, B: Array<Array<Double>>): Array<Array<Double>> {
        var sum = createMatrix(A.count(), A[0].count())
        if (A.count() == B.count() && A[0].count() == B[0].count()) {
            for (i in A.indices) {
                for (j in B[i].indices) {
                    sum[i][j] = A[i][j] + B[i][j]
                }
            }
        }
        return sum
    }

    /**
     * 逆行列 A^-1 (ある行列で線形変換した空間を元に戻す行列)
     *  A       行列 A
     *  return  逆行列の結果
     */
    fun matrixInverse(A: Array<Array<Double>>): Array<Array<Double>> {
        var n = A.count()
        var m = A[0].count()
        var invA = Array(n, {Array<Double>(m){0.0}})
        var AA = copyMatrix(A)
        if (n == m) {
            //	正方行列
            var max = 0
            var tmp = 0.0
            for (j in 0 until n) {
                for (i in 0 until n) {
                    invA[j][i] = if (i == j) 1.0 else 0.0
                }
            }
            for (k in 0 until n) {
                max = k
                for (j in k + 1 until n) {
                    if (abs(AA[j][k]) > abs(AA[max][k])) {
                        max = j
                    }
                }
                if (max != k) {
                    for (i in 0 until n) {
                        //	入力行列側
                        tmp = AA[max][i]
                        AA[max][i] = AA[k][i]
                        AA[k][i] = tmp
                        //	単位行列側
                        tmp = invA[max][i]
                        invA[max][i] = invA[k][i]
                        invA[k][i] = tmp
                    }
                }
                tmp = AA[k][k]
                for (i in 0 until n) {
                    AA[k][i] /= tmp
                    invA[k][i] /=tmp
                }
                for (j in 0 until n) {
                    if (j != k) {
                        tmp = AA[j][k] / AA[k][k]
                        for (i in 0 until n) {
                            AA[j][i] = AA[j][i] - AA[k][i] * tmp
                            invA[j][i] = invA[j][i] - invA[k][i] * tmp
                        }
                    }
                }
            }
            //	逆行列が計算できなかった場合の確認
            for (j in 0 until n) {
                for (i in 0 until n) {
                    if (invA[j][i].isNaN()) {
//                        println("Error: Unable to compute inverse matrix")
                        invA[j][i] = 0.0	//	とりあえず0に置き換える
                    }
                }
            }
            return invA
        } else {
//            println("Error: It is not a squre matrix")
            return invA
        }
    }


    //  === システム関連 ===

    /**
     * ビープ音を鳴らす
     *
     * @param dura 音出力時間  (ms)
     */
    fun beep(dura: Int) {
        //Toast.makeText(TimerActivity.this, "BEEP音", Toast.LENGTH_SHORT).show();
        val toneGenerator = ToneGenerator(AudioManager.STREAM_SYSTEM, ToneGenerator.MAX_VOLUME)
        //toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP);
        toneGenerator.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, dura)
    }

    /**
     * クリップボードにテキストをコピーする
     * @param text      コピーするテキスト
     */
    fun setTextClipBoard(c: Context, text: String) {
        //クリップボードに格納するItemを作成
        val item = ClipData.Item(text)
        //MIMETYPEの作成
        val mimeType = arrayOfNulls<String>(1)
        mimeType[0] = ClipDescription.MIMETYPE_TEXT_PLAIN
        //クリップボードに格納するClipDataオブジェクトの作成
        val cd = ClipData(ClipDescription("text_data", mimeType), item)
        //クリップボードにデータを格納
        val cm = c.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(cd)
    }

    /**
     * クリップボードからテキストを取り出す
     * @return      クリップボードのテキスト
     */
    fun getTextClipBoard(c: Context): String {
        val cm = c.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager //システムのクリップボードを取得
        val cd: ClipData? = cm.getPrimaryClip() //クリップボードからClipDataを取得
        if (cd != null) {
            val item = cd.getItemAt(0) //クリップデータからItemを取得
            return item.text.toString()
        }
        return ""
    }

    /**
     *  URLのWeb表示をする
     *  c       コンテキスト
     *  url     表示するURLアドレス
     */
    fun webDisp(c: Context, url: String?) {
        val uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        c.startActivity(intent)
    }

    /***
     * ローカルファイルを関連付けで開く
     * FileProvider を使う(参考: https://www.petitmonte.com/java/android_fileprovider.html)
     *    <provider
     *      android:name="androidx.core.content.FileProvider"
     *      android:authorities="${applicationId}.provider"
     *      android:exported="false"
     *      android:grantUriPermissions="true">
     *      <meta-data
     *          android:name="android.support.FILE_PROVIDER_PATHS"
     *          android:resource="@xml/my_provider" />
     *    </provider>
     * FileProvider を使うためには manifest.xml に「プロバイダ」を定義する
     * my_provider.xml に「プロバイダで使用するpath」を定義する
     *    <?xml version="1.0" encoding="utf-8"?>
     *    <paths xmlns:android="http://schemas.android.com/apk/res/android">
     *    <external-path name="share" path="." />
     *    <root-path name="sdcard" path="." />
     *    </paths>     *
     * @param c         コンテキスト
     * @param path      開くファイルのパス
     */
    fun executeFile(c: Context, path: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            val uri = FileProvider.getUriForFile(
                c,
                c.applicationContext.packageName + ".provider",
                File(path)
            )
            intent.setDataAndType(uri, getMimeType(path))
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            c.startActivity(intent)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
//            Log.d(YLib.TAG, "executeFile: " + e.message + " " + path)
        }
    }

    /**
     * GPS permissionの確認
     */
    fun checkGpsPermission(c: Context?): Boolean {
        // 既に許可している
        return if (ActivityCompat.checkSelfPermission(c!!,
                Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED) {
            true
        } else {
            // 拒否していた場合
            messageDialog(c, "アクセス権",
                "位置情報のアクセス権限が与えられていません\n" +
                        "[設定]-[アプリと通知]-[アプリ名]-[権限]で位置情報をONにしてください")
            false
        }
    }

    /**
     * サービスの起動状態の確認
     * c            コンテキスト
     * cls          サービスクラス
     * return
     */
    fun isServiceRunning(c: Context, cls: Class<*>): Boolean {
        val am = c.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningService = am.getRunningServices(Int.MAX_VALUE)
        for (serviceInfo in runningService) {
            Log.d(TAG, "service: " + serviceInfo.service.className + " : " + serviceInfo.started)
            //  クラス名の比較
            if (cls.name == serviceInfo.service.className) {
                Log.d(TAG, "running")
                return true
            }
        }
        return false
    }

    //  androidのPreferenceManagerはAPI29で廃止されるのでandroixに変更する
    //  build.gradleに以下を追加し、importもandroid.prefernce... から
    //  androidx.preference...に変更するが後は変わらない
    //  dependencies {
    //      implementation 'androidx.preference:preference:1.0.0'
    //  }
    /**
     * プリファレンスから論理値を取得
     * @param key
     * @param context
     * @return
     */
    fun getBoolPreferences(key: String, context: Context): Boolean {
        val prefs: SharedPreferences
        prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(key, false)
    }

    /**
     * プリファレンスから論理値を取得
     * @param key
     * @param default
     * @param context
     * @return
     */
    fun getBoolPreferences(key: String, default: Boolean, context: Context): Boolean {
        val prefs: SharedPreferences
        prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(key, default)
    }

    /**
     * プリファレンスに論理値を設定
     * @param value
     * @param key
     * @param context
     */
    fun setBoolPreferences(value: Boolean?, key: String?, context: Context?) {
        val prefs: SharedPreferences
        prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = prefs.edit()
        editor.putBoolean(key, value!!)
        editor.commit()
    }

    /**
     * プリファレンスから文字列の値を取得
     * データがない時は"###"を返す
     * @param key     データのキーワード
     * @param context コンテキスト
     * @return 取得したデータ
     */
    fun getStrPreferences(key: String?, context: Context?, defaultValue: String = "###"): String? {
        val prefs: SharedPreferences
        prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString(key, defaultValue)
    }

    /**
     * プリファレンスから文字列の値を取得
     * データがない時は"###"を返す
     * @param key     データのキーワード
     * @param default ディフォルト値
     * @param context コンテキスト
     * @return 取得したデータ
     */
    fun getStrPreferences(key: String?, default: String, context: Context?): String? {
        val prefs: SharedPreferences
        prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString(key, default)
    }

    /**
     * プリファレンスに文字列を設定
     * @param value   設定するデータ
     * @param key     データのキーワード
     * @param context コンテキスト
     */
    fun setStrPreferences(value: String?, key: String?, context: Context?) {
        val prefs: SharedPreferences
        prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = prefs.edit()
        editor.putString(key, value)
        editor.commit()
    }

    /**
     * プリファレンスから数値(int)を取得
     * @param key
     * @param context
     * @return
     */
    fun getIntPreferences(key: String?, context: Context?): Int {
        val prefs: SharedPreferences
        prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getInt(key, 0)
    }

    /**
     * プリファレンスから数値(int)を取得
     * @param key
     * @param default
     * @param context
     * @return
     */
    fun getIntPreferences(key: String?, default: Int, context: Context?): Int {
        val prefs: SharedPreferences
        prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getInt(key, default)
    }

    /**
     * プリファレンスに数値(int)を設定
     * @param value
     * @param key
     * @param context
     */
    fun setIntPreferences(value: Int, key: String?, context: Context?) {
        val prefs: SharedPreferences
        prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = prefs.edit()
        editor.putInt(key, value)
        editor.commit()
    }

    /**
     * プリファレンスから数値(long)を取得
     * @param key
     * @param context
     * @return
     */
    fun getLongPreferences(key: String?, context: Context?): Long {
        val prefs: SharedPreferences
        prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getLong(key, 0)
    }

    /**
     * プリファレンスから数値(long)を取得
     * @param key
     * @param default
     * @param context
     * @return
     */
    fun getLongPreferences(key: String?, default: Long, context: Context?): Long {
        val prefs: SharedPreferences
        prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getLong(key, default)
    }

    /**
     * プリファレンスに数値(long)を設定
     * @param value
     * @param key
     * @param context
     */
    fun setLongPreferences(value: Long, key: String?, context: Context?) {
        val prefs: SharedPreferences
        prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = prefs.edit()
        editor.putLong(key, value)
        editor.commit()
    }

    /**
     * プリファレンスから数値(float)を取得
     * @param key
     * @param context
     * @return
     */
    fun getFloatPreferences(key: String?, context: Context?): Float {
        val prefs: SharedPreferences
        prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getFloat(key, 0f)
    }

    /**
     * プリファレンスから数値(float)を取得
     * @param key
     * @param default
     * @param context
     * @return
     */
    fun getFloatPreferences(key: String?, default: Float, context: Context?): Float {
        val prefs: SharedPreferences
        prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getFloat(key, default)
    }

    /**
     * プリファレンスに数値(float)を設定
     * @param value
     * @param key
     * @param context
     */
    fun setFloatPreferences(value: Float, key: String?, context: Context?) {
        val prefs: SharedPreferences
        prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = prefs.edit()
        editor.putFloat(key, value)
        editor.commit()
    }

}