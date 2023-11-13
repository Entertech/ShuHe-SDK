# 集成

### Gradle自动集成

在module的build.gradle文件下添加以下依赖

```groovy
implementation 'cn.entertech.android:affective-offline-sdk-shuhe:1.0.7'
```

在项目根目录的build.gradle文件下添加以下依赖地址

```groovy
allprojects {
    repositories {
        mavenCentral()
    }
}
```

app/src/main/jniLibs目录下对应平台的so文件拷入自己的工程中

### 本地依赖

将Demo中app/libs目录下的affective-offline-sdk-shuhe-1.0.7.aar文件和app/src/main/jniLibs目录下对应平台的so文件拷入自己的工程中

# 快速接入

SDK提供了快速接入情感云的管理类，使用该类只需要几步就可以完成客户端与情感云平台的数据交互。

## 1.初始化

```kotlin
 private val affectiveDataAnalysisService by lazy {
        IAffectiveDataAnalysisService.getService(AffectiveServiceWay.AffectiveLocalService)
}
```



## 2.连接情感云服务

```kotlin
fun connectAffectiveServiceConnection(
        listener: IConnectionServiceListener,
        configProxy: EnterAffectiveConfigProxy
)

interface IConnectionServiceListener {
    /**
     * 连接成功
     * @param sessionId sessionId
     * */
    fun connectionSuccess(sessionId:String?)

    /**
     * 连接失败
     * */
    fun connectionError(error: Error?)
}
```

**参数说明**

|      参数     |             类型             |           说明           |
| :---------: | :------------------------: | :--------------------: |
|   listener  | IConnectionServiceListener | 连接结果回调，连接成功返回sessionId |
| configProxy |  EnterAffectiveConfigProxy |   配置信息，使用**默认构造函数**即可  |

## 3.启动情感云服务

```kotlin
 fun startAffectiveService(
        initListener: IStartAffectiveServiceLister
)
```

**参数说明**

|      参数      |              类型              |  说明 |
| :----------: | :--------------------------: | :-: |
| initListener | IStartAffectiveServiceLister |     |

## 4.解析脑电数据文件

```kotlin
 fun <R> readFileAnalysisData(inputStream: InputStream,
                                 appSingleData: ((R) -> Boolean)? = null,
                                 appendAllData: (List<R>) -> Unit,
                                 case: (String) -> R,
                                 callback: Callback,
 )

```

**参数说明**

|       参数      |         类型         |                        说明                       |
| :-----------: | :----------------: | :---------------------------------------------: |
|  inputStream  |     inputStream    |                     脑电数据文件流                     |
| appSingleData |  ((R) -> Boolean)? | 处理单个数据，true 表示消耗，则表示消耗该数据，不添加到appendAllData数据里面 |
| appendAllData | (List\<R>) -> Unit |        数据流读取出来的所有数据，除了appSingleData消耗了的数据       |
|      case     |    (String) -> R   |               数据流读取出来的字符串转成需要的类型R               |
|    callback   |      Callback      |                      解析结果回调                     |

脑波单通道数据文件流的解析分两种，一种是有包头包尾的完整数据文件流，一种是只有有效数据的有效数据文件流

### 解析完整的单通道数据文件流

    readFileAnalysisData(inputStream, { singleData ->
                            SingleChannelEEGUtil.process(singleData) { allData ->
                                appendSCEEGData(allData)
                            }
                            true
                        }, { allData ->
                           //不做任何处理
                        }, {
                            it.toInt()
                        }, object : Callback {
                            override fun onError(error: Error?) {
                                appendLog("解析文件失败：${error}")
                            }

                            override fun onSuccess() {
                                appendLog("解析文件成功")
                            }
                        })

### 解析有效的单通道数据文件流

    readFileAnalysisData(inputStream, { singleData ->
    						//也可以单独发送，这里就得返回true
                            false

                        }, { allData ->
                            if (allData.isNotEmpty()) {
                                appendSCEEGData(allData)
                            }
                        }, {
                            it.toInt()
                        }, object : Callback {
                            override fun onError(error: Error?) {
                                appendLog("解析文件失败：${error}")
                            }

                            override fun onSuccess() {
                                appendLog("解析文件成功")
                            }
                        })

## 5.获取报表

```kotlin
    fun getReport(listener: IGetReportListener, needFinishService: Boolean)

/**
 * 获取报表接口
 * */
interface IGetReportListener {

    /**
     * 获取报表出错
     * */
    fun onError(error: Error?)


    /**
     * 获取报表成功
     * */
    fun onSuccess(entity: UploadReportEntity?)

    /**
     * 获取生物基础数据报表出错
     * */
    fun getBioReportError(error: Error?)


    /**
     * 获取生理状态分析数据报表出错
     * */
    fun getAffectiveReportError(error: Error?)
}


data class UploadReportEntity(
        val code: Int,

        val `data`: Data? = null,
        val msg: String,


        val reportVersion: String = "3",

        var sessionId: String,
        var start: String,

        var timePoints: TimePoints? = null,
        val user_id: Int,
        /**
         * 算法版本
         * */
        val version: Version,
        )

data class Data(
        val affective: Affective,
        val biodata: Biodata
)

data class TimePoints(
        val affective: AffectiveTimePoints,
        val biodata: BiodataTimePoints
)

data class Version(
        val affective: AffectiveVersion,
        val biodata: BiodataVersion
)

data class Affective(
        val arousal: Arousal,
        val attention: Attention,
        val coherence: Coherence,
        val pleasure: Pleasure,
        val pressure: Pressure,
        val relaxation: Relaxation,
        val meditation: Meditation
)

data class Biodata(
        val eeg: Eeg,

        val hr: HrV2,
        val pepr: PEPR?
)

data class PEPR(
        val hrAvg: Int,
        val hrMax: Int,
        val hrMin: Int,
        val hrRec: List<Int>,

        val hrvAvg: Double,

        val hrvRec: List<Double>,

        val rrAvg: Double,

        val rrRec: List<Double>,

        val bcgQualityRec: List<Int>,

        val rwQualityRec: List<Int>
)


data class Arousal(
        /**
         * 全程激活度有效值（除去无效值0）的均值
         * */
        val arousal_avg: Int,
        /**
         * 全程激活度记录
         * */
        val arousal_rec: Any
)

data class Attention(
        /**
         * 全程注意力有效值（除去无效值0）的均值
         * */

        val attentionAvg: Double,
        /**
         * 全程注意力记录
         * */

        val attentionRec: List<Double>
)

data class Coherence(
        /**
         * 全程和谐度有效值（除去无效值0）的均值
         * */

        val coherenceAvg: Double,

        val coherenceDuration: Int?,

        val coherenceFlag: List<Int>?,
        /**
         * 全程和谐度记录
         * */

        val coherenceRec: List<Double>
)

data class Pleasure(
        /**
         * 全程愉悦度有效值（除去无效值0）的均值
         * */

        val pleasureAvg: Double,
        /**
         * 全程压力水平记录
         * */

        val pleasureRec: List<Double>
)

data class Pressure(

        val pressureAvg: Double,

        val pressureRec: List<Double>
)

data class Relaxation(
        /**
         * 全程放松度有效值（除去无效值0）的均值
         * */

        val relaxationAvg: Double,
        /**
         * 全程放松度记录
         * */

        val relaxationRec: List<Double>
)

data class Meditation(

        val meditationAvg: Double,

        val meditationRec: List<Double>,

        val meditationTipsRec: List<Int>,

        val flowPercent: Double,

        val flowDuration: Int,

        val flowLatency: Int,

        val flowCombo: Int,

        val flowDepth: Double,

        val flowBackNum: Int,

        val flowLossNum: Int,
        )

data class Eeg(

        val eegAlphaCurve: List<Double>,

        val eegBetaCurve: List<Double>,

        val eegDeltaCurve: List<Double>,

        val eegGammaCurve: List<Double>,

        val eegThetaCurve: List<Double>,

        val eegQualityRec: List<Int>
)

data class HrV2(

        val hrAvg: Double?,

        val hrMax: Int?,

        val hrMin: Int?,

        val hrRec: List<Int>,

        val hrvAvg: Double?,

        val hrvRec: List<Double>
)

data class AffectiveTimePoints(
        val arousal: List<TimePoint>,
        val attention: List<TimePoint>,
        val coherence: List<TimePoint>,
        val pleasure: List<TimePoint>,
        val pressure: List<TimePoint>,
        val relaxation: List<TimePoint>,
        val meditation: List<TimePoint>
)

data class BiodataTimePoints(
        val eeg: List<TimePoint>,

        val hr: List<TimePoint>,
        val pepr: List<TimePoint>
)

data class AffectiveVersion(
        val arousal: String,
        val attention: String,
        val coherence: String,
        val pleasure: String,
        val pressure: String,
        val relaxation: String
)

data class BiodataVersion(
        val eeg: String,

        val hr: String,
        val pepr: String
)

/**
 * 持续的时间段，可能中途会断开
 * */
data class TimePoint(
        var start: String,
        var stop: String
)

```

**参数说明**

|         参数        |         类型         |           说明           |
| :---------------: | :----------------: | :--------------------: |
|      listener     | IGetReportListener |         获取报表回调         |
| needFinishService |       Boolean      | 是否需要自动结束情感服务 true 自动结束 |

## 6.资源释放

注意，每次使用完情感云服务都需调用如下finishAffectiveService方法来释放资源

```kotlin
    fun finishAffectiveService(listener: IFinishAffectiveServiceListener)
```

## 7.断开情感云服务

    fun closeAffectiveServiceConnection()

# 详细API功能说明

如果你需要根据不同场景灵活使用情感云服务，可以使用IAffectiveDataAnalysisService来调用相应API，该类封装了所有情感云服务对外的接口。更加详情的情感云API可以查看[情感云详细API功能说明](https://github.com/Entertech/Enter-Affective-Offline-SDK#%E6%83%85%E6%84%9F%E7%A6%BB%E7%BA%BF%E7%AE%97%E6%B3%95sdk)
