package co.jp.yoshida.mapapp

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/// 国土地理院の地図データに関するデータ
/// 参照: https://maps.gsi.go.jp/development/ichiran.html
/// 参考: インタフェース 2019年4月号 (CQ出版)
/// 　　　第1部国土地理院地図＆基礎知識 29P
class MapInfoData {
    val TAG = "MapInfoData"

    val mHelpUrl = "https://maps.gsi.go.jp/development/ichiran.html"    //  地理院タイルのヘルプ
    val mGsiUrl = "https://cyberjapandata.gsi.go.jp/xyz/"   //  国土地理院データ参照先
    val mHelpOsmUrl = "https://openstreetmap.jp/"           // オープンストリートマップの参照先

    val mMapDataFormat = listOf(
        "タイトル", "データＩＤ", "ファイル拡張子", "タイル名", "有効ズームレベル",
        "整備範囲", "概要", "地図データURL", "地図データ提供先名", "地図データ提供先URL",
        "標高データID","BaseMapID", "透過色", "BaseMap上位"
    )

    //  20万分の1日本シームレス地質図V2 Web API 凡例取得サービス
    //  https://gbank.gsj.jp/seamless/v2/api/1.2.1/     2020年6月9日
    var mLegendSeamless_V2 = "https://gbank.gsj.jp/seamless/v2/api/1.2.1/legend.csv"

    //  標高データ(https://maps.gsi.go.jp/development/ichiran.html#dem)
    //  256x256ピクセルの標高タイル画像に相当するデータが256行x256個の標高値を表すカンマ区切りの
    //  数値データとして格納されている。標高値が存在しない画素に相当するところはeが入っている
    val mMapElevatorData = listOf(
        listOf(
            "標高タイルデータ",         //  [0]タイトル
            "dem5a",                    //  [1]データID(DEM5A:Z15,5x5m,精度0.3m)
            "txt",                      //  [2]ファイル拡張子
            "標高数値データ",           //  [3]地図の種類
            "1-15",                     //  [4]有効な最大ズームレベル
            "",                         //  [5]データの整備範囲
            "データソース:基板地図情報数値標高モデル、測量方法:航空レーザー測量、標高点格子間隔:約5m四方", //  [6]データの概要
            "https://cyberjapandata.gsi.go.jp/xyz/dem5a/{z}/{x}/{y}.txt",   //  [7]タイル座標URL
            "国土地理院",               //  [8]参照先名
            "https://maps.gsi.go.jp/development/ichiran.html#dem",  //  [9]参照先URL
        ),
        listOf(
            "標高タイルデータ(都市周辺など)",//  [0]タイトル
            "dem5b",                    //  [1]データID(DEM5B:Z15,5x5m,精度0.7m)
            "txt",                      //  [2]ファイル拡張子
            "標高数値データ",           //  [3]地図の種類
            "1-15",                     //  [4]有効な最大ズームレベル
            "",                         //  [5]データの整備範囲
            "データソース:基板地図情報数値標高モデル(都市周辺など)、測量方法:写真測量(地上画素寸法20cm)、標高点格子間隔:約5m四方", //  [6]データの概要
            "https://cyberjapandata.gsi.go.jp/xyz/dem5b/{z}/{x}/{y}.txt",   //  [7]
            "国土地理院",               //  [8]参照先名
            "https://maps.gsi.go.jp/development/ichiran.html#dem",  //  [9]参照先URL
        ),
        listOf(
            "標高タイルデータ(一部の島嶼などなど)",//  タイトル
            "dem5c",                    //  データID(DEM5C:Z15,5x5m,精度1.4m)
            "txt",                      //  ファイル拡張子
            "標高数値データ",           //  地図の種類
            "1-15",                     //  有効な最大ズームレベル
            "",                         //  データの整備範囲
            "データソース:基板地図情報数値標高モデル(一部の島嶼など)、測量方法:写真測量(地上画素寸法40cm)、標高点格子間隔:約5m四方", //  データの概要
            "https://cyberjapandata.gsi.go.jp/xyz/dem5c/{z}/{x}/{y}.txt",
            "国土地理院",               //  [8]参照先名
            "https://maps.gsi.go.jp/development/ichiran.html#dem",  //  [9]参照先URL
        ),
        listOf(
            "標高タイルデータ(1/2.5万地形等高線)", //  タイトル
            "dem",                      //  データID(DEM10B:Z14,10x10m,精度5m)
            "txt",                      //  ファイル拡張子
            "標高数値データ(DEM10B)",   //  地図の種類
            "1-14",                     //  有効な最大ズームレベル
            "",                         //  データの整備範囲
            "データソース:基板地図情報数値標高モデル、測量方法:1/2.5万地形等高線、標高点格子間隔:約10m四方", //  データの概要
            "https://cyberjapandata.gsi.go.jp/xyz/dem/{z}/{x}/{y}.txt",
            "国土地理院",               //  [8]参照先名
            "https://maps.gsi.go.jp/development/ichiran.html#dem",  //  [9]参照先URL
        ),
        listOf(
            "標高タイルデータ地球地図全球版標高第2版)", //  [0]タイトル
            "demgm",                    //  [1]データID(DEM10B:Z14,10x10m,精度5m)
            "txt",                      //  [2]ファイル拡張子
            "標高数値データ(DEMGM)",    //  [3]地図の種類
            "0-8",                      //  [4]有効な最大ズームレベル
            "地球地図全球版",           //  [5]データの整備範囲
            "データソース:地球地図全球版標高第2版を線形的に平滑化することによって得られた値", //  [6]データの概要
            "https://cyberjapandata.gsi.go.jp/xyz/demgm/{z}/{x}/{y}.txt",   //  [7]
            "国土地理院",               //  [8]参照先名
            "https://maps.gsi.go.jp/development/ichiran.html#dem",  //  [9]参照先URL
        ),
    )
    //  地図データ(各種の国土地理院地図とOpenStreetMap)
    var mMapData = mutableListOf(
        listOf(
            "標準地図",                             //  [0] タイトル
            "std",                                  //  [1] データID(フォルダ名も兼ねる)
            "png",                                  //  [2] タイル画像のファイル拡張子
            "1.標準地図(std)",                      //  [3] 地図の種類
            "0-18",                                 //  [4] 利用できるズーム範囲
            "本全国および全世界",                   //  [5] 利用できる範囲
            "道路、建物などの電子地図上の位置の基準である項目と植生、崖、岩、構造物などの土地の状況を" +  //  [6] 地図の概要
                    "表す項目を一つにまとめたデータをもとに作られた。国土地理院の提供している一般的な地図",
            "https://cyberjapandata.gsi.go.jp/xyz/std/{z}/{x}/{y}.png", //  [7]参照先URL(地理院地図はデフォルトを使用)タイル座標順を含む(指定のない時は{z}/{x}/{y})
            "国土地理院",                           //  [8]参照先名
            "https://maps.gsi.go.jp/development/ichiran.html",  //  [9]参照先URL
            "dem",                                  //  [10]標高データID
            "",                                     //  [11]BaseMapID
            "",                                     //  [12]重ねるデータの透過色
            "",                                     //  [13]BaseMapが上の場合 (true)
        ),
        listOf(
            "淡色地図",                             //  [0] タイトル
            "pale",                                 //  [1] データID(フォルダ名も兼ねる)
            "png",                                  //  [2] タイル画像のファイル拡張子
            "2.淡色地図(pale)",                     //  [3] 地図の種類
            "2-18",                                 //  [4] 利用できるズーム範囲
            "日本全国",                             //  [5] 利用できる範囲
            "標準地図を淡い色調で表したもの",       //  [6] 地図の概要
            "https://cyberjapandata.gsi.go.jp/xyz/pale/{z}/{x}/{y}.png",    //  [7] 参照先URL
            "国土地理院",                           //  [8]参照先名
            "https://maps.gsi.go.jp/development/ichiran.html",  //  [9]参照先URL
            "dem",                                  //  [10]標高データID
            "",                                     //  [11]BaseMapID
            "",                                     //  [12]重ねるデータの透過色
            "",                                     //  [13]BaseMapが上の場合 (true)
        ),
        listOf(
            "数値地図25000",                        //  [0]タイトル
            "lcm25k_2012",                          //  [1]データID
            "png",                                  //  [2]ファイル拡張子
            "3.数値地図2500(土地条件)(lcm25k)",     //  [3]地図の種類
            "4-16",                                 //  [4]有効なズームレベル
            "一部地域",                             //  [5]データの整備範囲
            "防災対策や土地利用/土地保全/地域開発などの計画の策定に必要な土地の自然条件などに関する" +
                    "基礎資料提供する目的で、昭和30年代から実施している土地条件調査の成果を基に地形分類" +
                    "(山地、台地・段丘、低地、水部、人口地形など)について可視化したもの",
            "https://cyberjapandata.gsi.go.jp/xyz/lcm25k_2012/{z}/{x}/{y}.png",
            "国土地理院",                           //  [8]参照先名
            "https://maps.gsi.go.jp/development/ichiran.html",  //  [9]参照先URL
            "dem",                                  //  [10]標高データID
            "",                                     //  [11]BaseMapID
            "",                                     //  [12]重ねるデータの透過色
            "",                                     //  [13]BaseMapが上の場合 (true)
        ),
        listOf(
            "沿岸海域土地条件図",                   //  [0]タイトル
            "ccm1",                                 //  [1]データID
            "png",                                  //  [2]ファイル拡張子
            "4.沿岸海域土地条件図(ccm1)",           //  [3]地図の種類
            "14-16",                                //  [4]有効なズームレベル
            "一部地域(瀬戸内海)",                   //  [5]データの整備範囲
            "陸部、解部の地形条件、標高、水深、底質、堆積層、沿岸関連施設、機関、区域などを可視化したもの",
            "https://cyberjapandata.gsi.go.jp/xyz/ccm1/{z}/{x}/{y}.png",
            "国土地理院",                           //  [8]参照先名
            "https://maps.gsi.go.jp/development/ichiran.html",  //  [9]参照先URL
            "dem",                                  //  [10]標高データID
            "",                                     //  [11]BaseMapID
            "",                                     //  [12]重ねるデータの透過色
            "",                                     //  [13]BaseMapが上の場合 (true)
        ),
        listOf(
            "火山基本図",                           //  [0]タイトル
            "vbm",                                  //  [1]データID
            "png",                                  //  [2]ファイル拡張子
            "5.火山基本図(vbm)",                    //  [3]地図の種類
            "16-18",                                //  [4]有効なズームレベル
            "一部地域(雄阿寒/雌阿寒岳、十勝岳、有珠山、樽前山、北海道駒ヶ岳、" +
                    "八幡平、秋田駒ヶ岳、岩手山、鳥海山、栗駒山、磐梯山、安達太良山、妙高、草津白根山、" +
                    "御嶽山、富士山、大島、三宅島、久住山、阿蘇山、普賢岳、韓国岳、桜島、鬼界ヶ島、竹島)",
            "噴火の防災計画、緊急対策用のほか、火山の研究や火山噴火予知などの基礎資料として整備した" +
                    "火山の地形を精密に表す等高線や火山防災施設などを示した縮尺 1/2500-1/10000の地形図",
            "https://cyberjapandata.gsi.go.jp/xyz/vbm/{z}/{x}/{y}.png",
            "国土地理院",                           //  [8]参照先名
            "https://maps.gsi.go.jp/development/ichiran.html",  //  [9]参照先URL
            "dem",                                  //  [10]標高データID
            "",                                     //  [11]BaseMapID
            "",                                     //  [12]重ねるデータの透過色
            "",                                     //  [13]BaseMapが上の場合 (true)
        ),
        listOf(
            "火山土地条件図",                       //  [0]タイトル
            "vlcd",                                 //  [1]データID
            "png",                                  //  [2]ファイル拡張子
            "6.火山土地条件図(vlcd)",               //  [3]地図の種類
            "13-16",                                //  [4]有効なズームレベル
            "一部地域(雄阿寒/雌阿寒岳、十勝岳、有珠山、樽前山、北海道駒ヶ岳、" +
                    "八幡平、秋田駒ヶ岳、岩手山、鳥海山、栗駒山、磐梯山、安達太良山、妙高、草津白根山、" +
                    "御嶽山、富士山、大島、三宅島、久住山、阿蘇山、普賢岳、韓国岳、桜島、鬼界ヶ島、竹島)",
            "火山災害の予測や防災対策立案に利用されている他、地震災害対策、土地保全/利用計画立案や" +
                    "各種の調査/研究、教育のための基礎資料としてあるいは地域や強度の理解を深めるための資料としても" +
                    "活用することを目的として整備した。火山の地形分類を示した縮尺 1/10000-1/50000の地図。" +
                    "過去の火山活動によって形成された地形や噴出物の分布(溶岩流、火砕流、スコリア丘、岩屑なだれなど)" +
                    "防災関連施設・機関、救護保安施設、河川工作物、観光施設などをわかりやすく表示したもの",
            "https://cyberjapandata.gsi.go.jp/xyz/vlcd/{z}/{x}/{y}.png",
            "国土地理院",                           //  [8]参照先名
            "https://maps.gsi.go.jp/development/ichiran.html",  //  [9]参照先URL
            "dem",                                  //  [10]標高データID
            "",                                     //  [11]BaseMapID
            "",                                     //  [12]重ねるデータの透過色
            "",                                     //  [13]BaseMapが上の場合 (true)
        ),
        listOf(
            "白地図",                               //  [0]タイトル
            "blank",                                //  [1]データID
            "png",                                  //  [2]ファイル拡張子
            "9.白地図(blank)",                      //  [3]地図の種類
            "5-14",                                 //  [4]有効なズームレベル
            "日本全国",                             //  [5]データの整備範囲
            "全国の白地図",                         //  [6]データの概要
            "https://cyberjapandata.gsi.go.jp/xyz/blank/{z}/{x}/{y}.png",
            "国土地理院",                           //  [8]参照先名
            "https://maps.gsi.go.jp/development/ichiran.html",  //  [9]参照先URL
            "dem",                                  //  [10]標高データID
            "",                                     //  [11]BaseMapID
            "",                                     //  [12]重ねるデータの透過色
            "",                                     //  [13]BaseMapが上の場合 (true)
        ),
        listOf(
            "湖沼図",                               //  [0]タイトル
            "lake1",                                //  [1]データID
            "png",                                  //  [2]ファイル拡張子
            "10.湖沼図(lake1)",                     //  [3]地図の種類
            "11-17",                                //  [4]有効なズームレベル
            "主要な湖および沼(不明)",               //  [5]データの整備範囲
            "湖及び沼とその周辺における、道路、主要施設、底質、推進、地形などを示したもの",
            "https://cyberjapandata.gsi.go.jp/xyz/lake1/{z}/{x}/{y}.png",
            "国土地理院",                           //  [8]参照先名
            "https://maps.gsi.go.jp/development/ichiran.html",  //  [9]参照先URL
            "dem",                                  //  [10]標高データID
            "",                                     //  [11]BaseMapID
            "",                                     //  [12]重ねるデータの透過色
            "",                                     //  [13]BaseMapが上の場合 (true)
        ),
        listOf(
            "航空写真(全国最新撮影)",               //  [0]タイトル
            "seamlessphoto",                        //  [1]データID
            "jpg",                                  //  [2]ファイル拡張子
            "11.航空写真全国最新写真(シームレス)(seamlessphoto)", //  [3]地図の種類
            "2-18",                                 //  [4]有効なズームレベル
            "日本全国",                             //  [5]データの整備範囲
            "電子国土基本図(オルソ画像)、東日本大震災後正射画像、森林(国有林)の空中写真、" +
                    "簡易空中写真、国土画像情報を組み合わせ、全国をシームレスに閲覧でるようにしたもの",
            "https://cyberjapandata.gsi.go.jp/xyz/seamlessphoto/{z}/{x}/{y}.jpg",
            "国土地理院",                           //  [8]参照先名
            "https://maps.gsi.go.jp/development/ichiran.html",  //  [9]参照先URL
            "dem",                                  //  [10]標高データID
            "",                                     //  [11]BaseMapID
            "",                                     //  [12]重ねるデータの透過色
            "",                                     //  [13]BaseMapが上の場合 (true)
        ),
        listOf(
            "色別標高図",                          //  [0]タイトル
            "relief",                              //  [1]データID
            "png",                                 //  [2]ファイル拡張子
            "12.色別標高図(relief)",               //  [3]地図の種類
            "6-15",                                //  [4]有効なズームレベル
            "日本全国",                            //  [5]データの整備範囲
            "基礎地図情報(数値標高モデル)および日本海洋データ・センタが提供する500mメッシュ" +
                    "海底地形データをもとに作成。標高の変化を色の変化を用いて視覚的に表現したもの",
            "https://cyberjapandata.gsi.go.jp/xyz/relief/{z}/{x}/{y}.png",
            "国土地理院",                           //  [8]参照先名
            "https://maps.gsi.go.jp/development/ichiran.html",  //  [9]参照先URL
            "dem",                                  //  [10]標高データID
            "",                                     //  [11]BaseMapID
            "",                                     //  [12]重ねるデータの透過色
            "",                                     //  [13]BaseMapが上の場合 (true)
        ),
        listOf(
            "活断層図(都市圏活断層図)",             //  [0]タイトル
            "afm",                                  //  [1]データID
            "png",                                  //  [2]ファイル拡張子
            "13.活断層図(都市圏活断層)(afm)",       //  [3]地図の種類
            "7-16",                                 //  [4]有効なズームレベル
            "日本全国の活断層",                     //  [5]データの整備範囲
            "地震被害の軽減に向けて整備された。地形図、活断層とその状態、地形分類を可視化したもの",
            "https://cyberjapandata.gsi.go.jp/xyz/afm/{z}/{x}/{y}.png",
            "国土地理院",                           //  [8]参照先名
            "https://maps.gsi.go.jp/development/ichiran.html",  //  [9]参照先URL
            "dem",                                  //  [10]標高データID
            "",                                     //  [11]BaseMapID
            "",                                     //  [12]重ねるデータの透過色
            "",                                     //  [13]BaseMapが上の場合 (true)
        ),
        listOf(
            "宅地利用動向調査成果",                 //  [0]タイトル
            "lum4bl_capital1994",                   //  [1]データID
            "png",                                  //  [2]ファイル拡張子
            "14.宅地利用動向調査成果(lum4bl_capital1994)",//  [3]地図の種類
            "6-16",                                 //  [4]有効なズームレベル
            "首都圏、(中部圏、近畿圏)",             //  [5]データの整備範囲
            "宅地利用動向調査の結果(山林・荒地、田、畑・その他の農地、造成中地、空地、工業用地" +
                    "一般低層住宅地、密集低層住宅地、中・高層住宅、商業・業務用地、道路用地、公園・緑地など、" +
                    "その他の公共施設用地、河川・湖沼など、その他、海、対象地域外)を可視化したもの" +
                    "首都圏は1994年、中部圏は1997年、近畿圏は1996年のデータが最新である",
            "https://cyberjapandata.gsi.go.jp/xyz/lum4bl_capital1994/{z}/{x}/{y}.png",
            "国土地理院",                           //  [8]参照先名
            "https://maps.gsi.go.jp/development/ichiran.html",  //  [9]参照先URL
            "dem",                                  //  [10]標高データID
            "",                                     //  [11]BaseMapID
            "",                                     //  [12]重ねるデータの透過色
            "",                                     //  [13]BaseMapが上の場合 (true)
        ),
        listOf(
            "全国植生指標データ",                   //  [0]タイトル
            "ndvi_250m_2010_10",                    //  [1]データID
            "png",                                  //  [2]ファイル拡張子
            "15.全国植生指標データ(ndvi_250m_2010_10)",//  [3]地図の種類
            "6-10",                                 //  [4]有効なズームレベル
            "日本とその周辺",                       //  [5]データの整備範囲
            "植生指標とは植物による光の反射の特徴を生かし衛星データを使って簡易な計算式で" +
                    "植生の状況を把握することを目的として考案された指標で植物の量や活力を表している",
            "https://cyberjapandata.gsi.go.jp/xyz/ndvi_250m_2010_10/{z}/{x}/{y}.png",
            "国土地理院",                           //  [8]参照先名
            "https://maps.gsi.go.jp/development/ichiran.html",  //  [9]参照先URL
            "dem",                                  //  [10]標高データID
            "",                                     //  [11]BaseMapID
            "",                                     //  [12]重ねるデータの透過色
            "",                                     //  [13]BaseMapが上の場合 (true)
        ),
        listOf(
            "磁気図(水平分力)(2015.0年値)",         //  [0]タイトル
            "jikizu2015_chijiki_h",                 //  [1]データID
            "png",                                  //  [2]ファイル拡張子
            "17.磁気図(jikizu2015_chijiki_h)",      //  [3]地図の種類
            "6-8",                                  //  [4]有効なズームレベル
            "日本全国",                             //  [5]データの整備範囲
            "時期の偏角、伏角、全磁力、水平分力、鉛直分力を示したもの",
            "https://cyberjapandata.gsi.go.jp/xyz/jikizu2015_chijiki_h/{z}/{x}/{y}.png",
            "国土地理院",                           //  [8]参照先名
            "https://maps.gsi.go.jp/development/ichiran.html",  //  [9]参照先URL
            "dem",                                  //  [10]標高データID
            "",                                     //  [11]BaseMapID
            "",                                     //  [12]重ねるデータの透過色
            "",                                     //  [13]BaseMapが上の場合 (true)
        ),
        listOf(
            "オープンストリートマップ",             //  [0]タイトル
            "osm",                                  //  [1]データID
            "png",                                  //  [2]ファイル拡張子
            "世界地図",                             //  [3]地図の種類
            "0-18",                                 //  [4]有効なズームレベル
            "世界各国",                             //  [5]データの整備範囲
            "オープンストリートマップ（OpenStreetMap、OSM）は自由に利用でき、" +
                    "なおかつ編集機能のある世界地図を作る共同作業プロジェクトである",  //  [6]データの概要
            "https://tile.openstreetmap.org/{z}/{x}/{y}.png",   //  [7]タイルデータ参照先URL
            "オープンストリートマップ",             //  [8]参照先名
            "https://tile.openstreetmap.org/",      //  [9]参照先URL
            "demgm",                                //  [10]標高データID
            "",                                     //  [11]BaseMapID
            "",                                     //  [12]重ねるデータの透過色
            "",                                     //  [13]BaseMapが上の場合 (true)
        ),
        listOf(
            "20万分の1日本シームレス地質図V2",      //  [0]タイトル
            //  Web API  https://gbank.gsj.jp/seamless/v2/api/1.2.1/
            "seamless_v2",                          //  [1]データID
            "png",                                  //  [2]ファイル拡張子
            "日本地図",                             //  [3]地図の種類
            "3-13",                                 //  [4]有効なズームレベル
            "日本全国",                             //  [5]データの整備範囲
            "産業技術総合研究所地質調査総合センターが提供する日本全国統一の凡例を用いた地質図をタイル化したものです。" +
                    "産総研地質調査総合センターウェブサイト利用規約に従って利用できる", //  [6]データの概要
            "https://gbank.gsj.jp/seamless/v2/api/1.2.1/tiles/{z}/{y}/{x}.png", //  [7]タイルデータ参照先URL
            "地質調査総合センター",                 //  [8]参照先名
            "https://www.gsj.jp/HomePageJP.html",   //  [9]ヘルプ参照先URL
            "dem",                                  //  [10]標高データID
            "",                                     //  [11]BaseMapID
            "",                                     //  [12]重ねるデータの透過色
            "",                                     //  [13]BaseMapが上の場合 (true)
        ),
        listOf(
            "赤色立体地図",                         //  [0]タイトル
            "sekishoku",                            //  [1]データID
            "png",                                  //  [2]ファイル拡張子
            "標高・土地の凹凸 10ｍメッシュ（標高）DEM10B",    //  [3]地図の種類
            "2-14",                                 //  [4]有効なズームレベル
            "日本地図",                             //  [5]データの整備範囲
            "赤色立体地図はアジア航測株式会社の特許（第3670274号等）を使用して作成したものです。",    //  [6]データの概要
            "https://cyberjapandata.gsi.go.jp/xyz/sekishoku/{z}/{x}/{y}.png",   //  [7]タイルデータ参照先URL
            "国土地理院",                           //  [8]参照先名
            "https://maps.gsi.go.jp/development/ichiran.html",  //  [9]参照先URL
            "demgm",                                //  [10]標高データID
            "",                                     //  [11]BaseMapID
            "",                                     //  [12]重ねるデータの透過色
            "",                                     //  [13]BaseMapが上の場合 (true)
        ),
        listOf(
            "地すべり地形分布図日本全国版",         //  [0]タイトル
            "landslide",                            //  [1]データID
            "png",                                  //  [2]ファイル拡張子
            "地すべり地形分布図日本全国版",         //  [3]地図の種類
            "5-15",                                 //  [4]有効なズームレベル
            "日本全国（関東地方中央部、東京都及び鹿児島県の島嶼部並びに沖縄県を除く）", //  [5]データの整備範囲
            "防災科学技術研究所地震ハザードステーション（J-SHIS）が提供する地すべり地形分布図日本全国版を表示します。" +
                    "地すべり地形分布図は、地すべり変動によって形成された地形的痕跡である「地すべり地形」の外形と" +
                    "基本構造（滑落崖・移動体）を空中写真判読によりマッピングし、1:50,000地形図にその分布を示した図面です。", //  [6]データの概要
            "https://jmapweb3v.bosai.go.jp/map/xyz/landslide/{z}/{x}/{y}.png", //  [7]タイルデータ参照先URL
            "地震ハザードステーション",             //  [8]参照先名
            "https://www.j-shis.bosai.go.jp/landslidemap",   //  [9]ヘルプ参照先URL
            "dem",                                  //  [10]標高データID
            "pale",                                 //  [11]BaseMapID
            "",                                     //  [12]重ねるデータの透過色
            "",                                     //  [13]BaseMapが上の場合 (true)
        ),
        listOf(
            "洪水浸水想定区域（想定最大規模）",     //  [0]タイトル
            "01_flood_l2_shinsuishin_data",         //  [1]データID
            "png",                                  //  [2]ファイル拡張子
            "ハザードマップ",                       //  [3]地図の種類
            "2-17",                                 //  [4]有効なズームレベル
            "都道府県",                             //  [5]データの整備範囲
            "本データは、洪水浸水想定区域（想定最大規模）_国管理河川と洪水浸水想定区域（想定最大規模）_都道府県管理河川のデータを統合したものです。", //  [6]データの概要
            "https://disaportaldata.gsi.go.jp/raster/01_flood_l2_shinsuishin_data/{z}/{x}/{y}.png", //  [7]タイルデータ参照先URL
            "ハザードマップポータルサイト",         //  [8]参照先名
            "https://disaportal.gsi.go.jp/hazardmap/copyright/opendata.html",   //  [9]ヘルプ参照先URL
            "dem",                                  //  [10]標高データID
            "pale",                                 //  [11]BaseMapID
            "",                                     //  [12]重ねるデータの透過色
            "",                                     //  [13]BaseMapが上の場合 (true)
        ),
        listOf(
            "気象庁 雨雲の動き",                    //  [0]タイトル
            "jmatile_hrpns",                        //  [1]データID
            "png",                                  //  [2]ファイル拡張子
            "今後の雨(降水短時間予報)"  ,           //  [3]地図の種類
            "3-10",                                 //  [4]有効なズームレベル
            "全国",                                 //  [5]データの整備範囲
            "気象庁が提供する「雨雲の動き」を表示、レーダー観測に基づく5分ごとの降水強度分布を表示する。" +
                    "1時間先までの予報を表示できる。高解像度降水ナウキャストは、気象レーダーの観測データを利用して、" +
                    "250m解像度で降水の短時間予報を提供している。 予報時間は0～60分で5分おき", //  [6]データの概要
            "https://www.jma.go.jp/bosai/jmatile/data/nowc/{yyyyMMddHHmmss_UTC_10_10}/none/{yyyyMMddHHmmss_UTC0_10_10}/surf/hrpns/{z}/{x}/{y}.png", //  [7]タイルデータ参照先URL
            "気象庁高解像度降水ナウキャスト",       //  [8]参照先名
            "https://www.jma.go.jp/jma///kishou/know/kurashi/highres_nowcast.html",   //  [9]ヘルプ参照先URL
            "dem",                                  //  [10]標高データID
            "pale",                                 //  [11]BaseMapID
            "",                                     //  [12]重ねるデータの透過色
            "",                                     //  [13]BaseMapが上の場合 (true)
        ),
        listOf(
            "気象庁 今後の雨",                      //  [0]タイトル
            "jmatile_rastf",                        //  [1]データID
            "png",                                  //  [2]ファイル拡張子
            "今後の雨(降水短時間予報)"  ,           //  [3]地図の種類
            "3-10",                                 //  [4]有効なズームレベル
            "日本全国",                             //  [5]データの整備範囲
            "レーダーとアメダスなどの降水量観測地から作成した降水量分布、15時間先までの1時間ごとの降水量分布を予測したもの。" +
                    "このURL゛ては15時間先までの降水量予測は1時間ごとに更新しているが、7時間以降で粗くなる。予報時間は0～6(15)時間まで1時間おき", //  [6]データの概要
            "https://www.jma.go.jp/bosai/jmatile/data/rasrf/{yyyyMMddHHmmss_UTC_60}/immed/{yyyyMMddHHmmss_UTC0_60}/surf/rasrf/{z}/{x}/{y}.png", //  [7]タイルデータ参照先URL
            "気象庁 降水短時間予報",                //  [8]参照先名
            "https://www.jma.go.jp/jma/kishou/know/kurashi/kotan_nowcast.html",   //  [9]ヘルプ参照先URL
            "dem",                                  //  [10]標高データID
            "pale",                                 //  [11]BaseMapID
            "",                                     //  [12]重ねるデータの透過色
            "",                                     //  [13]BaseMapが上の場合 (true)
        ),
        listOf(
            "気象庁 今後の雪",                      //  [0]タイトル
            "jmatile_snow",                         //  [1]データID
            "png",                                  //  [2]ファイル拡張子
            "今後の雪雨(降水短時間予報)"  ,         //  [3]地図の種類
            "3-10",                                 //  [4]有効なズームレベル
            "日本全国",                             //  [5]データの整備範囲
            "レーダーとアメダスなどの降水量観測地から作成した降水量分布、15時間先までの1時間ごとの降水量分布を予測したもの。" +
                    "このURL゛ては15時間先までの降水量予測は1時間ごとに更新しているが、7時間以降で粗くなる。", //  [6]データの概要
            "https://www.jma.go.jp/bosai/jmatile/data/snow/{yyyyMMddHHmmss_UTC_60}/none/{yyyyMMddHHmmss_UTC0_60}/surf/snowd/{z}/{x}/{y}.png", //  [7]タイルデータ参照先URL
            "気象庁 降水短時間予報",       //  [8]参照先名
            "https://www.jma.go.jp/jma/kishou/know/kurashi/kotan_nowcast.html",   //  [9]ヘルプ参照先URL
            "dem",                                  //  [10]標高データID
            "pale",                                 //  [11]BaseMapID
            "",                                     //  [12]重ねるデータの透過色
            "",                                     //  [13]BaseMapが上の場合 (true)
        ),
        listOf(
            "天気分布予報(気温)",                   //  [0]タイトル
            "jmatile_wdist_temp",                   //  [1]データID
            "png",                                  //  [2]ファイル拡張子
            "気温の分布予報",                       //  [3]地図の種類
            "3-10",                                 //  [4]有効なズームレベル
            "全国",                                 //  [5]データの整備範囲
            "日本全国を5km四方のメッシュに分け、そのそれぞれについて以下の要素の明日24時までの予報を掲載、" +
                    "色別で表示しているため、全国または各地域の天気、気温、降水量、降雪量の分布と変化傾向がひと目でわかります。" +
                    "毎日5時、11時、17時に発表で3時間おきの予報値をだす。予報時間は0～36時間まで3時間おき", //  [6]データの概要
            "https://www.jma.go.jp/bosai/jmatile/data/wdist/{yyyyMMddHHmmss_UTC1}/none/{yyyyMMddHHmmss_UTC2}/surf/temp/{z}/{x}/{y}.png", //  [7]タイルデータ参照先URL
            "気象庁 天気分布予報",                  //  [8]参照先名
            "https://www.jma.go.jp/jma/kishou/know/kurashi/bunpu.html",   //  [9]ヘルプ参照先URL
            "dem",                                  //  [10]標高データID
            "blank",                                //  [11]BaseMapID
            "FFFFFF",                               //  [12]重ねるデータの透過色
            "true",                                 //  [13]BaseMapが上の場合 (true)
        ),
        listOf(
            "天気分布予報(天気)",                   //  [0]タイトル
            "jmatile_wdist_wm",                     //  [1]データID
            "png",                                  //  [2]ファイル拡張子
            "天気の分布予報",                       //  [3]地図の種類
            "3-10",                                 //  [4]有効なズームレベル
            "日本全国",                             //  [5]データの整備範囲
            "日本全国を5km四方のメッシュに分け、そのそれぞれについて以下の要素の明日24時までの予報を掲載、" +
                    "色別で表示しているため、全国または各地域の天気、気温、降水量、降雪量の分布と変化傾向がひと目でわかります。" +
                    "毎日5時、11時、17時に発表で3時間おきの予報値をだす。予報時間は0～36時間まで3時間おき(過去データはなし)", //  [6]データの概要
            "https://www.jma.go.jp/bosai/jmatile/data/wdist/{yyyyMMddHHmmss_UTC1}/none/{yyyyMMddHHmmss_UTC2}/surf/wm/{z}/{x}/{y}.png", //  [7]タイルデータ参照先URL
            "気象庁 天気分布予報",                  //  [8]参照先名
            "https://www.jma.go.jp/jma/kishou/know/kurashi/bunpu.html",   //  [9]ヘルプ参照先URL
            "dem",                                  //  [10]標高データID
            "blank",                                //  [11]BaseMapID
            "FFFFFF",                               //  [12]重ねるデータの透過色
            "true",                                 //  [13]BaseMapが上の場合 (true)
        ),
        listOf(
            "天気分布予報(雲)",                     //  [0]タイトル
            "jmatile_wdist_r3",                     //  [1]データID
            "png",                                  //  [2]ファイル拡張子
            "雲の分布予報",                         //  [3]地図の種類
            "3-10",                                 //  [4]有効なズームレベル
            "日本全国",                             //  [5]データの整備範囲
            "本全国を5km四方のメッシュに分け、そのそれぞれについて以下の要素の明日24時までの予報を掲載、" +
                    "色別で表示しているため、全国または各地域の天気、気温、降水量、降雪量の分布と変化傾向がひと目でわかります。" +
                    "毎日5時、11時、17時に発表で3時間おきの予報値をだす。預保時間は0～36時間まで3時間おき(過去データはなし)", //  [6]データの概要
            "https://www.jma.go.jp/bosai/jmatile/data/wdist/{yyyyMMddHHmmss_UTC1}/none/{yyyyMMddHHmmss_UTC2}/surf/r3/{z}/{x}/{y}.png", //  [7]タイルデータ参照先URL
            "気象庁 天気分布予報",                  //  [8]参照先名
            "https://www.jma.go.jp/jma/kishou/know/kurashi/bunpu.html",   //  [9]ヘルプ参照先URL
            "dem",                                  //  [10]標高データID
            "blank",                                //  [11]BaseMapID
            "FFFFFF",                               //  [12]重ねるデータの透過色
            "true",                                 //  [13]BaseMapが上の場合 (true)
        ),
    )

    //  時間指定URLのフォーマット　{format_type_interval_deley}
    //  {yyyyMMddHHmmss}            日本時間表示 5分単位
    //  {yyyyMMddHHmmss_UTC_n_m}    世界時間表示 n分単位、m分遅延 データ取得(n省略５分単位、m省略０分)
    //  {yyyyMMddHHmmss_UTC0_n_m}   世界時間表示 n分単位、m分遅延 予想データ取得(n省略1５分単位、m省略０分)
    //  {yyyyMMddHHmmss_UTC1_m}     世界時間表示 日本時間の5,11,17時にデータ取得
    //  {yyyyMMddHHmmss_UTC2_m}     世界時間表示 ３時間おきにデータ取得
    val mDateTimeForm = listOf<String>(
        "yyyyMMddHHmmss",
        "yyyyMMddHHmmss_UTC", "yyyyMMddHHmmss_UTC0",//  雨雲レーダー用
        "yyyyMMddHHmmss_UTC1", "yyyyMMddHHmmss_UTC2"//  天気分布予報用
    )
    var mDateTimeInc: Long = 0                      //  表示時間の増加数
    var mDateTimeInterval: Long = 0                 //  日時追加のインターバル時間(分)
    var mDispDateTime = mutableListOf<LocalDateTime>()  //  画面表示時間(日本時間)
    var mDateTimeFolder = ""                        //  日時データ用のフォルダ名
    var mMapTitleNum = 0
    var mElevatorDataNo = 0
    var mMapUrl = ""
    @RequiresApi(Build.VERSION_CODES.O)
    var mPreDateTime = LocalDateTime.now()

    val klib = KLib()

    fun setMapTitleNum(mapTitleNum: Int) {
        mMapTitleNum = mapTitleNum
        mElevatorDataNo = getElevatorDataNo(mMapData[mMapTitleNum][10])
        mMapUrl = mMapData[mMapTitleNum][7]
    }

    /**
     * 日時形式の変換文字列の入った地図URLで日時データの置き換えをする
     * mapUrl       日時置換えURL
     * return       日時データ置き換えURL
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun replaceDateTime(mapUrl: String): String {
        var transData = mutableListOf<List<String>>()
        var dateJpn = LocalDateTime.now(ZoneId.of("Asia/Tokyo"))
        var dateUtc = LocalDateTime.now(ZoneId.of("UTC"))
        var convForm = klib.extractBrackets(mapUrl)
        mPreDateTime = LocalDateTime.now(ZoneId.of("UTC"))
        mDispDateTime.clear()
        for (i in convForm.indices) {
            if (0 <= convForm[i].indexOf("yyyy")) {
                var dateBuf = convForm[i].split('_')
                var convData = transDate(dateBuf, dateJpn, dateUtc)
                transData.add(listOf("{" + convForm[i] + "}", convData))
                //  日時データの追加フォルダ名
                mDateTimeFolder = "/" + convData
            }
        }
        var mapUrl2 = mapUrl
        for (data in transData)
            mapUrl2 = mapUrl2.replace(data[0], data[1])
        return mapUrl2
    }

    /**
     *  日時形式から日時データを設定する
     *  form        日時形式リスト
     *  dateJpn     日本時間
     *  dateUtc     世界時間
     *  return      日時データ文字列
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun transDate(form: List<String>, dateJpn: LocalDateTime, dateUtc: LocalDateTime): String {
        var dateTime = LocalDateTime.now()
        if (form.size == 1) {
            //  yyyyMMddHHmmss
            dateTime = LocalDateTime.of(dateJpn.year, dateJpn.month, dateJpn.dayOfMonth, dateJpn.hour, dateJpn.minute / 5 * 5, 0)
        } else if (form.size == 2) {
            //  yyyyMMddHHmmss_UTCx
            if (form[1].compareTo("UTC") == 0) {
                //  10分単位の時間
                dateTime = klib.roundDateTimeMin(dateUtc, 10)
            } else if (form[1].compareTo("UTC0") == 0) {
                //  15分単位で時間をかえる
                mDateTimeInterval = 15
                var dt = if (mPreDateTime.isAfter(dateUtc)) mPreDateTime else dateUtc
                dt = dt.plusMinutes(mDateTimeInterval * mDateTimeInc)
                dateTime = klib.roundDateTimeMin(dt, mDateTimeInterval)
            } else if (form[1].compareTo("UTC1") == 0) {
                //  2時、8時、20時に丸める
                var dt = klib.roundDateTimeMin(dateUtc.minusHours(2), 6 * 60)
                dateTime = dt.plusHours(if (dt.hour == 12) -4 else 2)
            } else if (form[1].compareTo("UTC2") == 0) {
                //  3時間単位で時間を変える
                mDateTimeInterval = 3 * 60
                var dt = if (mPreDateTime.isAfter(dateUtc)) mPreDateTime else dateUtc
                dt = dt.plusMinutes(mDateTimeInterval * (mDateTimeInc + 1))
                dateTime = klib.roundDateTimeMin(dt, mDateTimeInterval)
            } else if (klib.isNumber(form[1])) {
                //  {yyyyMMddHHmmss_n}
                mDateTimeInterval = form[1].toLong()
                dateTime = klib.roundDateTimeMin(dateJpn, mDateTimeInterval)
            }
        } else if (form.size == 3) {
            //  yyyyMMddHHmmss_UTCx_Interval(delay)
            mDateTimeInterval = klib.str2Integer(form[2]).toLong()
            if (form[1].compareTo("UTC") == 0) {
                dateTime = klib.roundDateTimeMin(dateUtc, mDateTimeInterval)
            } else if (form[1].compareTo("UTC0") == 0) {
                var dt = if (mPreDateTime.isAfter(dateUtc)) mPreDateTime else dateUtc
                dt = dt.plusMinutes(mDateTimeInterval * mDateTimeInc)
                dateTime = klib.roundDateTimeMin(dt, mDateTimeInterval)
            } else if (form[1].compareTo("UTC1") == 1) {
                //  2時、8時、20時に丸める
                var dt = dateUtc.plusMinutes(-mDateTimeInterval) //  遅延時間追加
                dt = klib.roundDateTimeMin(dt.minusHours(2), 6 * 60)
                dateTime = dt.plusHours(if (dt.hour == 12) -4 else 2)
            } else if (form[1].compareTo("UTC2") == 2) {
                //  3時間単位で時間を変える
                mDateTimeInterval = 3 * 60
                var dt = if (mPreDateTime.isAfter(dateUtc)) mPreDateTime else dateUtc
                dt = dt.plusMinutes(-mDateTimeInterval) //  遅延時間追加
                dt = dt.plusMinutes(mDateTimeInterval * mDateTimeInc)
                dateTime = klib.roundDateTimeMin(dt, mDateTimeInterval)
            }
        } else if (form.size == 4) {
            //  yyyyMMddHHmmss_UTCx_Interval_Delay
            mDateTimeInterval = klib.str2Integer(form[2]).toLong()
            val delay =  klib.str2Integer(form[3]).toLong()
            if (form[1].compareTo("UTC") == 0) {
                var dt = dateUtc.plusMinutes(-delay)    //  遅延時間追加
                dateTime = klib.roundDateTimeMin(dt, mDateTimeInterval)
            } else if (form[1].compareTo("UTC0") == 0) {
                var dt = if (mPreDateTime.isAfter(dateUtc)) mPreDateTime else dateUtc
                dt = dt.plusMinutes(-delay)    //  遅延時間追加
                dt = dt.plusMinutes(mDateTimeInterval * mDateTimeInc)
                dateTime = klib.roundDateTimeMin(dt, mDateTimeInterval)
            }
        } else {
            //  10分単位の時間
            mDateTimeInterval = 10;
            var dt = dateJpn.plusMinutes(mDateTimeInterval * mDateTimeInc)
            dateTime = klib.roundDateTimeMin(dt, mDateTimeInterval)
        }
        mPreDateTime = dateTime     //  前回値保存

        //  地図に表示する日時データ(日本時間に変換)
        val dateTimeJpn = if (0 <= form[1].indexOf("UTC")) dateTime.plusHours(9)
        else dateTime
        mDispDateTime.add(dateTimeJpn)

        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern(form[0])
        val dateStr = dateTime.format(formatter)
        if (0 < form.size) {
            Log.d(TAG, "transDate: " + form.size + " " + form[1] + " " + dateTime + "  " + dateStr)
        } else {
            Log.d(TAG, "transDate: " + form.size + " " + dateTime + "  " + dateStr)
        }
        return dateStr
    }

    /**
     * 日時データを使う地図かの確認
     * url          地図データのURL
     */
    fun isDateTimeData(mapUrl: String = ""): Boolean {
        var url = mMapData[mMapTitleNum][7]
        if (0 < mapUrl.length)
            url = mapUrl
        for (i in mDateTimeForm.indices)
            if (0 <= url.indexOf(mDateTimeForm[i]))
                return true
        return false
    }

    /**
     * 地図データのIDからデータNoを取得
     * id       地図データのID名
     * return   地図データNo(該当なしは -1)
     */
    fun getMapDataNo(id: String): Int {
        return mMapData.indexOfFirst { p -> p[1].compareTo(id) == 0 }
    }

    /**
     * 地図データのWebアドレスを取得(日時データ変換)
     * mapUrl       Webアドレス
     * return       日時変換したWebアドレス
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun setMapWebAddress(mapUrl: String = ""): String {
        var webUrl = mapUrl
        if (webUrl.length <= 1)
            webUrl =  mMapData[mMapTitleNum][7]
        if (isDateTimeData(webUrl)) {
            webUrl = replaceDateTime(webUrl)
        }
        mMapUrl = webUrl
        return webUrl
    }

    /**
     * 地図データのWebアドレスを求める
     * zoom         ズーム値
     * x            X座標
     * y            Y座標
     * mapTitleNum  地図データのNo
     * return       Webアドレス
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun getMapWebAddress(zoom:Int, x: Int, y: Int, mapTitleNum: Int = -1): String {
        var webUrl = ""
        if (0 <= mapTitleNum) {
            webUrl = mMapData[mapTitleNum][7]
            if (isDateTimeData(webUrl))
                webUrl = replaceDateTime(webUrl)
        } else
            webUrl = mMapUrl
        webUrl = webUrl.replace("{z}", zoom.toString())
        webUrl = webUrl.replace("{x}", x.toString())
        webUrl = webUrl.replace("{y}", y.toString())
        return webUrl
    }

    /**
     * 地図データのデータタイトルを求める
     * mapTitleNum  地図データのNo
     * return       データタイトル
     */
    fun getMapDataTitle(mapTitleNum: Int = -1): String {
        if (0 <= mapTitleNum)
            return mMapData[mapTitleNum][0]
        else
            return mMapData[mMapTitleNum][0]
    }

    /**
     * 地図データのデータIDを求める
     * mapTitleNum  地図データのNo
     * return       データID
     */
    fun getMapDataId(mapTitleNum: Int = -1): String {
        if (0 <= mapTitleNum)
            return mMapData[mapTitleNum][1]
        else
            return mMapData[mMapTitleNum][1]
    }

    /**
     * 地図データの拡張子を求める
     * mapTitleNum  地図データのNo
     * return       拡張子
     */
    fun getMapDataExt(mapTitleNum: Int = -1): String {
        if (0 <= mapTitleNum)
            return mMapData[mapTitleNum][2]
        else
            return mMapData[mMapTitleNum][2]
    }

    /**
     * 地図データの最大ズーム値を求める
     * mapTitleNum  地図データのNo
     * return       最大ズーム値
     */
    fun getMapDataMaxZoom(mapTitleNum: Int = -1): Int {
        if (0 <= mapTitleNum)
            return getMaxZoom(mMapData[mapTitleNum][4])
        else
            return getMaxZoom(mMapData[mMapTitleNum][4])
    }

    /**
     * 地図データの重ね合わせ地図データのデータIDを求める
     * mapTitleNum  地図データのNo
     * return       データID
     */
    fun getMapMergeDataId(mapTitleNum: Int = -1): String {
        if (0 <= mapTitleNum)
            return mMapData[mapTitleNum][11]
        else
            return mMapData[mMapTitleNum][11]
    }

    /**
     *  重ね合わせるデータの透過色を取得
     * mapTitleNum  地図データのNo
     * return       透過色(RGB 0xRRGGBB)
     */
    fun getMapOverlapTransparent(mapTitleNum: Int = -1): Int {
        if (0 <= mapTitleNum)
            return klib.str2Integer("0x" + mMapData[mapTitleNum][12])
        else
            return klib.str2Integer("0x" + mMapData[mMapTitleNum][12])
    }

    /**
     * 重ね合わせ地図データが上になるかを取得
     * mapTitleNum  地図データのNo
     * return       上に重ねる(true)
     */
    fun getMapMergeOverlap(mapTitleNum: Int = -1): Boolean {
        if (0 <= mapTitleNum)
            return mMapData[mapTitleNum][13].compareTo("true") == 0
        else
            return mMapData[mMapTitleNum][13].compareTo("true") == 0
    }

    /**
     * 標高データのIDからデータNoを取得
     * id       標高データのID名
     * return   標高データNo
     */
    fun getElevatorDataNo(id: String): Int {
        val no = mMapElevatorData.indexOfFirst { p -> p[1].compareTo(id) == 0 }
        return if (no < 0) 0 else no
    }

    /**
     *  標高データの最ID名を取得
     * elevatorDataNo   標高データのNo
     * return           標高データのID名
     */
    fun getElevatorDataId(elevatorDataNo: Int = -1): String {
        if (0 <= elevatorDataNo)
            return mMapElevatorData[elevatorDataNo][1]
        else
            return mMapElevatorData[mElevatorDataNo][1]
    }

    /**
     *  標高データの拡張子を取得
     * elevatorDataNo   標高データのNo
     * return           拡張子
     */
    fun getElevatorDataExt(elevatorDataNo: Int = -1): String {
        if (0 <= elevatorDataNo)
            return mMapElevatorData[elevatorDataNo][2]
        else
            return mMapElevatorData[mElevatorDataNo][2]
    }

    /**
     *  標高データのWebアドレスを取得
     * elevatorDataNo   標高データのNo
     * x                X座標
     * y                Y座標
     * return           Webアドレス
     */
    fun getElevatorWebAddress(zoom:Int, x: Int, y: Int, elevatorDataNo: Int = -1): String {
        var webUrl = ""
        if (0 <= elevatorDataNo)
            webUrl = mMapElevatorData[elevatorDataNo][7]
        else
            webUrl = mMapElevatorData[mElevatorDataNo][7]
        webUrl = webUrl.replace("{z}", zoom.toString())
        webUrl = webUrl.replace("{x}", x.toString())
        webUrl = webUrl.replace("{y}", y.toString())
        return webUrl
    }

    /**
     * 標高データの最大ズーム値を取得
     * elevatorDataNo   標高データのNo
     * return           最大ズーム値
     */
    fun getElevatorMaxZoom(elevatorDataNo: Int = -1): Int {
        if (0 <= elevatorDataNo)
            return getMaxZoom(mMapElevatorData[elevatorDataNo][4], 14)
        else
            return getMaxZoom(mMapElevatorData[mElevatorDataNo][4], 14)
    }

    /**
     * ズーム値の文字列から最大ズーム値(最後の数値)を求める
     * zoom         ズーム値の文字列
     * default      数値がない時の値
     * return       ズーム値
     */
    fun getMaxZoom(zoom: String, default: Int = 18): Int {
        var buf = ""
        var buf2 = ""
        for (i in zoom.indices) {
            if (zoom[i].isDigit())
                buf += zoom[i]
            else {
                buf2 = buf
                buf = ""
            }
        }
        if (0 < buf.length)
            return buf.toInt()
        else if (0 < buf2.length)
            return buf2.toInt()
        else
            return default
    }

    /**
     * ファイルからMapDataを読み込む
     * path     ファイル名
     */
    fun loadMapInfoData(path: String) {
        if (klib.existsFile(path)) {
            val mapData = klib.loadCsvData(path, mMapDataFormat)
            if (0 < mapData.size) {
                mMapData.clear()
                mMapData.addAll(mapData)
            }
        }
    }

    /**
     * MapDataをファイルに保存
     * path     ファイル名
     */
    fun saveMaoInfoData(path: String) {
        if (0 < mMapData.size)
            klib.saveCsvData(path, mMapDataFormat, mMapData)
    }
}