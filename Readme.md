## いろんな地図(MapApp) - 地図表示

旧版のMapAppを新しい開発環境(Android Studio Narwhal)で再作成。  
大きな変更はGPSデータファイルなどを共通ストレージ(/strage/emulated/0/DCIM/mapapp)に変更、あと細かいところを修正。  

### 機能
・国土地理院の地図データなどをいろいろな地図を表示する  
・GPSデータを取得して異動した経路を表示する  
・GPSのトレースデータを一覧で管理する  
・GPSのトレースデータをタイトルやコメントなど情報をつけてかんりする  
・GPSトレースデータを速度/距離、標高/距離などのグラフ表示をする  
・GPSのトレースデータをGPXファイルとして出力できる  
・他のGPXファイルのデータを地図上に表示する  


### 起動画面
<img src="image/MapApp_Main.png" width="80%">

**上部選択リスト** : グループ名と記録中のGPXファイル名が表示される  
**下部ボタン** : 目的地をリスト表示、選択した目的地の方向と距離を表示  

#### オプションメニュー
**地図情報** : 現在地をグーグルマップで開く  
**GPSトレースリスト** :  
**マーク操作** : 目的地をグーグルマップで開く  
**写真の位置** : 設定画面を開く  
**地図データ一括取込み** : 
**地図データ編集** : 
**アプリ情報** :    

### 目的地リスト選択
<img src="image/MapApp_Main_MapMenu.png" width="80%">

### 標準地図
<img src="image/MapApp_Main_標準地図.png" width="80%">

### 色別標高図
<img src="image/MapApp_Main_色別標高図.png" width="80%">

### 航空写真地図
<img src="image/MapApp_Main_航空写真.png" width="80%">

### 気象庁の雨雲の動きと白地図の重ね合わせ
<img src="image/MapApp_Main_雨雲の動き.png" width="80%">

### 20万分の1日本シームレス地質図
凡例ファイル(data/legend_seamless_v2.csv)を入れると地質名が表示できる  
<img src="image/MapApp_Main_日本シームレス地質図.png" width="80%">

### 地図データの設定編集
<img src="image/MapApp_MapEdit.png" width="80%">
  
### GPSのトレース表示
<img src="image/MapApp_Main_GPSTrace.png" width="80%">

### GPSトレースリスト
<img src="image/MapApp_GPSTraceList.png" width="80%">

### GPSトレースリストの操作メニュー
<img src="image/MapApp_GPSTraceListMenu.png" width="80%">

### GPSトレースファイルの登録編集
<img src="image/MapApp_GPSTraceEdit.png" width="80%">

### GPSトレースデータのグラフ表示
<img src="image/MapApp_GpsTraceGraph.png" width="80%">

### マークデータの編集画面
<img src="image/MapApp_MarkEdit.png" width="80%">

#### インストール方法
実行ファイルのダウンロードは[mapapp-debug.apk](mapapp-debug.apk)をダウンロードする。  
<img src="image/download.png" width="80%">

スマホを開発者オプションの設定にする(Android12/13)
1.	スマホの設定アプリを開く
2.	デバイス情報 (端末情報)をタップ
3.	下のほうに「ビルド番号」という項目があるので、10回程度タップする
4.	設定アプリの最初に戻って「システム」をタップ
5.	「開発者向けオプション」が表示されるのでタップ
6.	一番上の「開発者向けオプションの使用」をタップして開発者向けオプションを有効化する

Filesアプリでダウンロードしたファイルをタップするかファイルを選択してから右側の点をタップするとメニューが表示されるのでインストールを選択するとインストールが開始される。  

履歴  
2025/10/15 PSトレース開始時にBeep音追加、GPSトレースグラフに歩数を追加  
2025/09/26 Android Studio Norwhal で再作成  


### 開発環境  
Android Studio Narwhal 3 Feature Drop | 2025.1.3  
Build #AI-251.26094.121.2513.14007798, built on August 28, 2025  
Windows 11.0  
Java  
