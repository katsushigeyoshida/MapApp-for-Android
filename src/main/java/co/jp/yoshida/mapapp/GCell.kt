package co.jp.yoshida.mapapp

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.util.SizeF


enum class GCELLTYPE { RECT, CIRCLE }
enum class TEXTHORIZONTALALIGNMENT { LEFT, CENTER, RIGHT}
enum class TEXTVERTICALALIGNMENT { TOP, CENTER, BOTTOM }

/**
 * 地図データのセル単位での表示処理
 */
class GCell {
    var mId = 0                                 //  ＩＤ
    var mGCellType = GCELLTYPE.RECT             //  セルの種類
    var mPosition = PointF(0f, 0f)          //  配置位置(左上)
    var mSize = SizeF(0f, 0f)           //  大きさ
    var mBackColor = Color.WHITE                //  背景色
    var mBackTransparent = true                 //  透過
    var mBorderColor = Color.BLACK              //  境界線色
    var mBorderWidth = 1f                       //  境界線の幅
    var mTitle = ""                             //  タイトル文字
    var mTitleHorizontalAlignment = TEXTHORIZONTALALIGNMENT.CENTER
    var mTitleVerticalAlignment = TEXTVERTICALALIGNMENT.CENTER
    var mTitleSize = 20f                        //  文字サイズ
    var mTitleColor = Color.BLACK               //  文字色
    var mImageFile = ""                         //  イメージファイルのパス
    var mImageResource: Int = -1                //  リソース
    var mResources: Resources? = null           //  Resourceクラスのオブジェクト(リソース元)
    var mTrimmingRect = RectF(0f, 0f, 1f, 1f)   //  画像トリミングサイズ
    var mCellOn = true                          //  セルのoN/oFF
    var mVisible = true                         //  セルの表示/非表示

    /**
     * 図形を描画する
     */
    fun draw(canvas: Canvas) {
        if (mGCellType == GCELLTYPE.CIRCLE) {
            drawCircle(canvas)
        } else {
            drawRect(canvas)
        }
        drawImage(canvas)
        if (0 <= mTitle.length)
            drawText(canvas)
    }

    /**
     * 四角を描画する
     */
    private fun drawRect(canvas: Canvas) {
        val paint = Paint()
        paint.color = mBorderColor
        paint.style = Paint.Style.STROKE
        if (!mBackTransparent) {
            paint.style = Paint.Style.FILL
            paint.color = mBackColor
        }
        paint.strokeWidth = mBorderWidth
        val rect = RectF(mPosition.x, mPosition.y, mPosition.x + mSize.width, mPosition.y + mSize.height)
        canvas?.drawRect(rect, paint)
    }

    /**
     * 円を描画する
     */
    private fun drawCircle(canvas: Canvas) {
        val paint = Paint()
        paint.color = mBorderColor
        paint.style = Paint.Style.STROKE
        if (!mBackTransparent) {
            paint.style = Paint.Style.FILL
            paint.color = mBackColor
        }
        paint.strokeWidth = mBorderWidth
        val rect = RectF(mPosition.x, mPosition.y, mPosition.x + mSize.width, mPosition.y + mSize.height)
        canvas?.drawOval(rect, paint)
    }

    /**
     * Bitmapの取得
     */
    fun getBitmapImage(): Bitmap? {
        if (0 <= mImageResource) {
            //  リソースからBitmapを作成
            return BitmapFactory.decodeResource( mResources, mImageResource)
        } else if (0 < mImageFile.length) {
            //  画像ファイルからBitmapを作成
            return BitmapFactory.decodeFile(mImageFile)
        } else {
            return null
        }
    }

    /**
     * リソースまたはイメージファイルの表示
     */
    private fun drawImage(canvas: Canvas) {
        val paint = Paint()
        var bmp: Bitmap? = null
        if (0 <= mImageResource) {
            //  リソースからBitmapを作成
            bmp = BitmapFactory.decodeResource( mResources, mImageResource)
        } else if (0 < mImageFile.length) {
            //  画像ファイルからBitmapを作成
            bmp = BitmapFactory.decodeFile(mImageFile)
        } else {
            return
        }
        //  BitMapの大きさ設定(比率で設定)
        //  トリミングサイズ(start.x,start.y, width,height)
        val rectIn = Rect(
            (bmp.width * mTrimmingRect.left).toInt(),       //  左端
            (bmp.height * mTrimmingRect.top).toInt(),       //  上端
            (bmp.width * mTrimmingRect.right).toInt(),      //  右端
            (bmp.height * mTrimmingRect.bottom).toInt()
        )    //  下端
        //  貼り付け位置(起点と終点)
        val rectOut = RectF(mPosition.x, mPosition.y,
            mPosition.x + mSize.width, mPosition.y + mSize.height)
        //  drawBitmap(ビットマップ,表示で切り出す領域,実際に表示するサイズ,Paint属性
        canvas.drawBitmap(bmp, rectIn, rectOut, paint)
    }

    /**
     * セル内の文字表示
     */
    private fun drawText(canvas: Canvas) {
        if (mTitle.length <= 0)
            return
        val paint = Paint()
        paint.color = mTitleColor
        paint.style = Paint.Style.FILL
        paint.textSize = mTitleSize
        paint.textAlign = when (mTitleHorizontalAlignment) {
            TEXTHORIZONTALALIGNMENT.LEFT -> Paint.Align.LEFT
            TEXTHORIZONTALALIGNMENT.CENTER -> Paint.Align.CENTER
            TEXTHORIZONTALALIGNMENT.RIGHT -> Paint.Align.RIGHT
        }
        val offset = when (mTitleVerticalAlignment) {
            TEXTVERTICALALIGNMENT.TOP -> 0f
            TEXTVERTICALALIGNMENT.CENTER -> mTitleSize / 2f
            TEXTVERTICALALIGNMENT.BOTTOM -> mTitleSize
        } + mTitleSize * 0.1f
        canvas?.drawText(mTitle, mPosition.x + mSize.width / 2,
            mPosition.y + mSize.height - offset, paint)
    }

    /**
     * 指定座標がセル内あるかを確認
     */
    fun getCellIn(pos: PointF): Boolean {
        if (mGCellType == GCELLTYPE.RECT)
            return inRect(pos)
        else if (mGCellType == GCELLTYPE.CIRCLE)
            return inCircle(pos)
        else
            return false
    }

    /**
     * 指定座標が四角内にあるかを確認
     */
    private fun inRect(pos: PointF): Boolean {
        if (mPosition.x <= pos.x && pos.x <= mPosition.x + mSize.width &&
            mPosition.y <= pos.y && pos.y <= mPosition.y + mSize.height)
            return true
        else
            return false
    }

    /**
     * 指定座標が円内にあるかを確認
     */
    private fun inCircle(pos: PointF): Boolean {
        var dx = mPosition.x + mSize.width / 2f - pos.x
        var dy = mPosition.y + mSize.height / 2f - pos.y
        var r = mSize.width / 2
        dy = dy * mSize.width / mSize.height
        if (r * r <= (dx * dx + dy * dy))
            return false
        else
            return true
    }

}