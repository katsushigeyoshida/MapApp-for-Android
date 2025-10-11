package co.jp.yoshida.mapapp

import android.util.Log
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.URL

//  Web上のファイルのダウ化ロード(非同期処理)
//  使い方
//      var downLoad = DownLoadWebFile(mUrl, mOutPath)
//      downLoad.start()                //  ダウンロード開始
//      //  ダウンロード終了待ち
//      while (downLoad.isAlive()) {
//          Thread.sleep(100L)
//      }

/**
 *  Web上のファイルのダウ化ロード(非同期処理)
 *  url         ダウンロード先URLあどれす
 *  outPath     保存先ファイル名
 */
class DownLoadWebFile(val url: String, val outPath: String): Thread() {
    val TAG = "DownLoadWebFile"

    override fun run() {
        try {
            if (makeDir(outPath)) {
//                Log.d(TAG, "make dir: $url $outPath")
                val url = URL(url)
//                val inputStream = url.openStream()
                //  サーバーによって要求ヘッダーに acceptや user-agentなどを追加しないと403(Forbidden)を返しデータが取得できないことがある
                //  サーバーの種類によって内容が異なる
                //  要求ヘッダーを設定するためにopenStream()の代わりにopenConnection()を使用して設定する
                var urlCon = url.openConnection()
                urlCon.setRequestProperty("accept", "text/html;q=0.9,image/webp,image/apng,*/*")
                urlCon.setRequestProperty("user-agent", "Mozilla Chrome Mobile Safari Edg")
                //  Microsoft Edgeが出している要求ヘッダー
//                urlCon.setRequestProperty("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
//                urlCon.setRequestProperty("user-agent", "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.131 Mobile Safari/537.36 Edg/92.0.902.67");
                val inputStream = urlCon.getInputStream()
                val outputStream = FileOutputStream(outPath)
                fileCopy(inputStream, outputStream)
                outputStream.flush()
                outputStream.close()
                inputStream.close()
            } else {
                Log.d(TAG, "unmake dir: $url $outPath")
            }
        } catch (e: FileNotFoundException) {
            //  acceptの内容によっても発生する
            Log.d(TAG, "FileNotFoundException Error: " + e.message)
        } catch (e: IOException) {
            Log.d(TAG, "IOException Error: " + e.message)
        } catch (e: Exception) {
            Log.d(TAG, "Exception Error: " + e.message)
            e.printStackTrace()
        }
    }

    /**
     *  ファイル名を含むパスから複数階層のディレクトリ作成
     *  path        ファイルパス
     *  return      作成の可否
     */
    private fun makeDir(path: String): Boolean {
        val file = File(path)
        val dir = File(file.parent)
        if (!dir.exists()) {
            return dir.mkdirs()
        }
        return true
    }

    /**
     *  Stream間ファイルコピー
     *  inputStream     入力ストリーム
     *  outStream       出力ストリーム
     */
    private fun fileCopy(inputStream: InputStream, outputStream: OutputStream) {
        var size = 1024 * 4
        val buf = ByteArray(size)
        try {
            while (-1 != inputStream.read(buf).also { size = it }) {
                outputStream.write(buf, 0, size)
            }
            outputStream.flush()
            inputStream.close()
            outputStream.close()
        } catch (e: java.lang.Exception) {
            Log.d(TAG, "fileCopy Exception: " + e.message)
        }
    }
}