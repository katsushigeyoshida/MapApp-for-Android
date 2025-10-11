package co.jp.yoshida.mapapp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

//  GPSにgoogle play service()FusedLocationProviderClient)を使用
//  google play service インストール
//      File　→　Setting → Apperrance & Behavior
//      → System Settings → Android SDK　→ SDK Tools → google play sevice)
//  Location関連ライブラリの追加
//      File → Project Structure → Dependencies → Modules → [Module名]
//      → All Dependences → [+] → 1.Library Dependency選択
//      → [検索] com.google.android.gms → play-service-locationとversionをインストール

//  permission ACCESS_FINE_LOCATION
//  permission FOREGROUND_SERVICE

//  SensorMangerを使うときは SensorEventListener をインプリメントする
//	StepDetctorとStepCounterとの違い
//	StepDetectorは起動から実際に歩くまでコールバックは行われない
//	StepCounterは起動直後に歩いていなくてもコールバックする
//  StepCounterの設定
//	StepDetctorとStepCounterとの違い
//	StepDetectorは起動から実際に歩くまでコールバックは行われない
//	StepCounterは起動直後に歩いていなくてもコールバックする
//  StepCounterの設定

class GpsService : Service(), SensorEventListener {

    companion object {
        val TAG = "GpsService"
        const val CHANNEL_ID = "GpsService_notification_channel"
        val serviceName = "GPS通知"
        val serviceStartTitle = "GPS開始"
        val serviceStartMsg = "GPSの取得を開始しました"
        val serviceFinishTitle = "GPS終了"
        val serviceFinishMsg = "GPSの取得を終了しました"
        val mGpsFileName = "GpsData.csv"
    }

    //  GPS情報処理
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var mLocationRequest: LocationRequest
    private lateinit var mOnUpdateLocation: GpsService.OnUpdateLocation //  GPSコールバックオブジェクト
    private lateinit var  mSensorManager: SensorManager
    private val mIntervalTime = 5000L - 1000L       //  GPS取得インターバル(ms)
    private var mGpsFilePath = ""                   //  GPSデータ保存パス
    private var mGpsCount = 0

    //  歩数カウンタ
    private var mCurrentStepNum = 0     //	現在の歩数
    private var mStepCount = 0          //  歩数
    private var mStepFlag = true        //  歩数計の可否

    val klib = KLib()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        Log.d(TAG,"onCreate")
//        super.onCreate()
        //  通知設定(HIGH:sound,heads-up,vib DEFAULT:,vib LOW:no sound,no vib MIN:no sound,no icon, no vib)
        notificationSet(serviceName, NotificationManager.IMPORTANCE_DEFAULT, CHANNEL_ID)

        //  GPS(FusedLocationProviderClient)の取得
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this@GpsService)
        mLocationRequest = LocationRequest.create()
        mLocationRequest.let {
            it.interval = mIntervalTime                             //  更新間隔
            it.fastestInterval = mIntervalTime                      //  最短更新間隔
            it.priority = LocationRequest.PRIORITY_HIGH_ACCURACY    //  位置取得精度
        }
        mOnUpdateLocation = OnUpdateLocation()                      //  位置情報コールバックオブジェクトの生成

        //  GPSデータ保存パス
        mGpsFilePath = klib.getDCIMPackageName(this@GpsService) + "/" + mGpsFileName

        //  歩数センサー
        mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        if (!supportedStepSensor(this)) {   //	歩数センサーの確認
            mStepFlag = false
            Toast.makeText(this, "歩数センサーが対応していません", Toast.LENGTH_SHORT).show()
        }
        val sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        val supportBatch = mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)

    }

    override fun onBind(intent: Intent): IBinder? {
//        TODO("Return the communication channel to the service.")
        return  null
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        Log.d(TAG,"onStartCommand")
        //  通知設定
        notificationStart(this@GpsService, CHANNEL_ID, serviceStartTitle, serviceStartMsg)
        //  GPS追跡開始
        if (!GpsLocationStart()) {
            Toast.makeText(this, "位置情報の権限が設定されていません", Toast.LENGTH_LONG).show()
        }
        return START_NOT_STICKY     //  強制終了した場合、自動再起動しない
//        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        Log.d(TAG,"onDestroy")
        GpsLocationEnd()
        mSensorManager.unregisterListener(this)
        super.onDestroy()
    }

    /**
     * センサーレンジ変更イベント
     */
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
//        TODO("Not yet implemented")
    }

    /**
     * センサー情報イベント
     */
    override fun onSensorChanged(sensorEvent: SensorEvent?) {
        //	歩数検知(端末が起動してからの累計歩数)
        if (sensorEvent == null) return
        if (sensorEvent.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            //  歩数センサーによる歩数検知
            mCurrentStepNum = sensorEvent.values[0].toInt()
            mStepCount++
        }
    }

    /**
     * 位置情報の追跡開始
     */
    fun GpsLocationStart(): Boolean {
        Log.d(TAG,"GpsLocationStart")
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            return false
        }
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mOnUpdateLocation,mainLooper)
        return true
    }

    /**
     * 位置情報追跡の停止
     */
    private fun GpsLocationEnd() {
        Log.d(TAG,"GpsLocationEnd")
        mFusedLocationClient.removeLocationUpdates(mOnUpdateLocation)
    }


    /**
     * 位置所法の取得
     */
    private inner class OnUpdateLocation: LocationCallback() {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onLocationResult(locationResult: LocationResult) {
//            super.onLocationResult
            Log.d(TAG,"OnUpdateLocation: "+mGpsCount)
            if (0 < mGpsCount++) {
                locationResult.let {
                    val location =  it.lastLocation
                    location.let {
                        var buffer = LocalDateTime
                            .ofEpochSecond(it!!.time / 1000 + 9*3600, 0, ZoneOffset.UTC)
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        buffer += "," + it.time.toString()              //  時間(UTC Time)(ms)
                        buffer += "," + it.latitude.toString()          //  緯度(度)
                        buffer += "," + it.longitude.toString()         //  経度(度)
                        buffer += "," + it.altitude.toString()          //  高度(m)
                        buffer += "," + it.speed.toString()             //  速度(m/s)
                        buffer += "," + it.bearing.toString()           //  方位(度)
                        buffer += "," + it.accuracy.toString()          //  精度(m半径)
                        buffer += "," + mCurrentStepNum                 //  歩数
                        buffer += "," + mStepCount                      //  歩数の出力回数(onSensorChangedの回数)
                        appendSaveData(mGpsFilePath, buffer)
                    }
                }
            }
        }
    }


    /**
     * 位置情報の追加保存
     * ファイルが存在しない場合にはタイトルを追加語にデータを追加
     * path     保存ファイル名
     * buffer   保存データ
     */
    fun appendSaveData(path: String, buffer: String) {
        Log.d(TAG,"appendSaveData: "+path+" "+buffer)
        if (!klib.existsFile(path)) {
            //  初回書き込み
            var title = "DateTime,Time,Latitude,Longtude,Altitude,Speed,Bearing,Accuracy,StepCount"
            klib.writeFileData(path, title)
        }
        klib.writeFileDataAppend(path, "\n" + buffer)
    }

    /**
     * Notificationの設定
     * bame         通知名名
     * importance   通知の重要度(IMPORTANCE_NON/MIN/LOW/DEFAULT/HIGH)
     * channelId    チャンネル名
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun notificationSet(name: String, importance:Int, channelId: String) {
        val channel = NotificationChannel(channelId, name, importance)  //  通知チャンネル
        val manager = getSystemService(NotificationManager::class.java) //  NotificationManagerの取得
        manager.createNotificationChannel(channel)                      //  通知チャンネルの設定
    }

    /**
     * サービス開始通知(「Androidアプリ開発の教科書Kotliin対応」635p参照)
     * startForeground()を有効にするには Manifest に以下が必要
     *   サービスのタイプ		        android:foregroundServiceType = location
     *   マニフェストでの権限		    FOREGROUND_SERVICE_LOCATION
     *   startForeground() の定数	FOREGROUND_SERVICE_TYPE_LOCATION
     *   ランタイムの前提条件		    ACCESS_COARSE_LOCATION , ACCESS_FINE_LOCATION
     *
     * context      コンテキスト
     * channelId    チャンネル名
     * title        通知タイトル
     * message      通知メッセージ
     */
    fun notificationStart(context: Context, channelId: String, title: String, message: String) {
        Log.d(TAG,"notificationStart")
        val builder = NotificationCompat.Builder(context, channelId)
        builder.setSmallIcon(android.R.drawable.ic_dialog_info) //  アイコン設定
        builder.setContentTitle(title)                          //  表示タイトルの設定
        builder.setContentText(message)                         //  表示メッセージの設定
        Log.d(TAG,"notificationStart 1")

        val intent = Intent(this@GpsService, MainActivity::class.java)   //  起動先Activity
        intent.putExtra("fromNptification", true)   //  起動先atcivityの設定
        //  Target SDK 31(Android 12)から PendingIntent のmutability(可変性)を指定する必要
        val stopServiceIntent = PendingIntent.getActivity(context, 0,
            intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        Log.d(TAG,"notificationStart 2")
        builder.setContentIntent(stopServiceIntent)             //  PendingIntentの設定
        builder.setAutoCancel(true)                             //  通知メッセージの自動消去
        val notification = builder.build()                      //  オブジェクトの生成
        startForeground(200, notification)                  //  ServiceのForGround化
        Log.d(TAG,"notificationStart end")
    }

    /**
     * サービス終了通知(不要)
     * 終了通知が残る
     * context      コンテキスト
     * channelId    チャンネル名
     * title        通知タイトル
     * message      通知メッセージ
     */
    fun notificationEnd(context: Context, channelId: String, title: String, message: String) {
        Log.d(TAG,"notificationEnd")
        val builder = NotificationCompat.Builder(context, channelId)
        builder.setSmallIcon(android.R.drawable.ic_dialog_info) //  アイコン設定
        builder.setContentTitle(title)
        builder.setContentText(message)
        val notification = builder.build()                      //  Notificatioのオブジェクトの生成
        val manager = NotificationManagerCompat.from(context)
        manager.notify(100, notification)                   //  通知
        stopSelf()                                              //  自分を終了させる
    }

    /**
     * 歩数カウントに対応している場合はtrue
     * countext     コンテキスト
     * return       使用の可否
     */
    private fun supportedStepSensor(context: Context): Boolean {
        val packageManager = context.packageManager
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT // APIレベルチェック
                && packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_COUNTER)  // ステップカウンタチェック
                && packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_DETECTOR))// ステップディテクタチェック
    }
}