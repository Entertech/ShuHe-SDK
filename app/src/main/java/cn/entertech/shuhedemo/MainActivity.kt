package cn.entertech.shuhedemo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ScrollView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import cn.entertech.affective.sdk.api.Callback
import cn.entertech.affective.sdk.api.IAffectiveDataAnalysisService
import cn.entertech.affective.sdk.api.IConnectionServiceListener
import cn.entertech.affective.sdk.api.IFinishAffectiveServiceListener
import cn.entertech.affective.sdk.api.IGetReportListener
import cn.entertech.affective.sdk.api.IStartAffectiveServiceLister
import cn.entertech.affective.sdk.bean.AffectiveServiceWay
import cn.entertech.affective.sdk.bean.EnterAffectiveConfigProxy
import cn.entertech.affective.sdk.bean.RealtimeAffectiveData
import cn.entertech.affective.sdk.bean.RealtimeBioData
import cn.entertech.affective.sdk.bean.UploadReportEntity
import cn.entertech.affectivesdk.authentication.AuthenticationHelper
import cn.entertech.affectivesdk.utils.SingleChannelEEGUtil.SINGLE_EGG_PCK_CHECK
import cn.entertech.affectivesdk.utils.SingleChannelEEGUtil.SINGLE_EGG_PCK_END
import cn.entertech.affectivesdk.utils.SingleChannelEEGUtil.SINGLE_EGG_PCK_START
import cn.entertech.ble.cushion.CushionBleManager
import cn.entertech.ble.single.BiomoduleBleManager
import cn.entertech.shuhedemo.databinding.ActivityMainBinding
import java.io.BufferedWriter
import cn.entertech.affective.sdk.bean.Error
import java.io.File
import java.io.FileWriter
import java.util.Random
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"
    private lateinit var biomoduleBleManager: BiomoduleBleManager
    private lateinit var cushionBleManager: CushionBleManager
    lateinit var binding: ActivityMainBinding
    private val affectiveDataAnalysisService by lazy {
        IAffectiveDataAnalysisService.getService(AffectiveServiceWay.AffectiveLocalService)
    }
    private var screenText: String = ""

    private val bdListener by lazy {
        val bdListener: ((RealtimeBioData?) -> Unit) = {

        }
        bdListener
    }

    private val listener by lazy {
        val bdListener: ((RealtimeAffectiveData?) -> Unit) = {

        }
        bdListener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initManager()
        initPermission()
    }

    fun initManager() {
        biomoduleBleManager = BiomoduleBleManager.getInstance(App.getInstance().applicationContext)
        cushionBleManager = CushionBleManager.getInstance(App.getInstance().applicationContext)
        biomoduleBleManager.addRawDataListener(rawDataListener)
        cushionBleManager.addRawDataListener(rawCushionDataListener)
        biomoduleBleManager.addHeartRateListener(hrDataListener)
        affectiveDataAnalysisService?.subscribeData(bdListener, listener)
    }

    /**
     * 创建随机数
     * */
    fun onCreateData(view: View) {
        thread {
            val random = Random()
            val pckNum = random.nextInt(10)
            val isPck = true
            val sb = java.lang.StringBuilder()
            if (isPck) {
                appendLog("正常的包 pckNum： $pckNum")
                for (j in 0 until pckNum) {
                    for (i in 0..23) {
                        if (i in 0..2) {
                            sb.append(SINGLE_EGG_PCK_START)
                        } else if (i == 20) {
                            sb.append(SINGLE_EGG_PCK_CHECK)
                        } else if (i in 21..23) {
                            sb.append(SINGLE_EGG_PCK_END)
                        } else {
                            val num = random.nextInt(255)
                            if (num < 16) {
                                sb.append(0)
                            }
                            sb.append(Integer.toHexString(num))
                        }
                    }
                }
                appendLog("sb: $sb")
                val file = File(cacheDir.absolutePath + "filename.txt")
                // if file doesnt exists, then create it
                if (file.exists()) {
                    file.delete()
                }
                file.createNewFile()
                val fw = FileWriter(file.absoluteFile)
                val bw = BufferedWriter(fw)
                bw.write(sb.toString())
                bw.close()
                appendLog("成功生成文件")
            } else {
                appendLog("非正常的包")
            }
        }

    }


    fun onUseData(view: View) {
        /*thread {
            affectiveDataAnalysisService
                .readFileAndAppendData(cacheDir.absolutePath + "filename.txt") {
                    it?.apply {
                        enterAffectiveSDKManager.appendSingleEEG(this)
                    }
                }

        }*/
    }

    fun byteArrayOfInts(vararg ints: Int) = ByteArray(ints.size) { pos -> ints[pos].toByte() }

    fun onConnect(view: View) {
        appendLog("正在扫描蓝牙...")
        biomoduleBleManager.scanNearDeviceAndConnect(fun() {
            appendLog("蓝牙扫描成功，正在连接...")
            Log.d(TAG, "sacn success")
        }, fun(e: Exception) {
            appendLog("蓝牙扫描失败：$${e}")
            Log.d(TAG, "sacn failed：$e")
        }, fun(mac: String) {
            appendLog("蓝牙连接成功$mac")
            Log.d(TAG, "connect success$mac")
            runOnUiThread {
                Toast.makeText(this@MainActivity, "connect to device success", Toast.LENGTH_SHORT)
                    .show()
            }
        }) { msg ->
            Log.d(TAG, "connect failed")
            appendLog("蓝牙连接失败")
            runOnUiThread {
                Toast.makeText(
                    this@MainActivity,
                    "failed to connect to device：${msg}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun onConnectCushion(view: View) {
        appendLog("正在扫描蓝牙...")
        cushionBleManager.scanNearDeviceAndConnect(fun() {
            appendLog("蓝牙扫描成功，正在连接...")
            Log.d(TAG, "sacn success")
        }, fun(e: Exception) {
            appendLog("蓝牙扫描失败：$${e}")
            Log.d(TAG, "sacn failed：$e")
        }, fun(mac: String) {
            appendLog("蓝牙连接成功$mac")
            Log.d(TAG, "connect success$mac")
            runOnUiThread {
                Toast.makeText(this@MainActivity, "connect to device success", Toast.LENGTH_SHORT)
                    .show()
            }
        }) { msg ->
            Log.d(TAG, "connect failed")
            appendLog("蓝牙连接失败")
            runOnUiThread {
                Toast.makeText(
                    this@MainActivity,
                    "failed to connect to device：${msg}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun onDisconnect(view: View) {
        biomoduleBleManager.disConnect()
        cushionBleManager.disconnect()
    }

    /**
     * 从硬件中读取到的脑波原始数据，通过appendEEG方法传入至情感算法SDK中
     */
    var rawDataListener = fun(eeg: ByteArray) {
        affectiveDataAnalysisService?.apply {
            if (hasStartAffectiveService()) {
                appendEEGData(eeg)
            }
        }

    }


    /**
     * 从硬件中读取到的脑波原始数据，通过appendEEG方法传入至情感算法SDK中
     */
    var rawCushionDataListener = fun(eeg: ByteArray) {
        affectiveDataAnalysisService?.apply {
            if (hasStartAffectiveService()) {
                appendPEPRData(eeg)
            }
        }
    }

    /**
     * 从硬件中读取到的心率原始数据，通过appendHR方法传入至情感算法SDK中
     */
    var hrDataListener = fun(hr: Int) {
        affectiveDataAnalysisService?.apply {
            if (hasStartAffectiveService()) {
                appendHeartRateData(hr)
            }
        }
    }

    fun onStart(view: View) {
        startAffectiveService {
            biomoduleBleManager.startHeartAndBrainCollection()
        }
    }

    fun onStartCushionData(view: View) {
        startAffectiveService {
            cushionBleManager.startCollection()
        }
    }

    private fun startAffectiveService(startCollection: (IAffectiveDataAnalysisService?) -> Unit) {
        affectiveDataAnalysisService?.apply {
            closeAffectiveServiceConnection()
            connectAffectiveServiceConnection(object : IConnectionServiceListener {
                override fun connectionError(error: Error?) {
                    appendLog("服务连接失败...")
                }

                override fun connectionSuccess(sessionId: String?) {
                    if (hasStartAffectiveService()) {
                        startCollection(this@apply)
                        appendLog("开始采集头环数据...")
                    } else {
                        startAffectiveService(
                            object : IStartAffectiveServiceLister {
                                override fun startAffectionFail(error: Error?) {
                                    appendLog("Affection算法初始化失败：${error}")
                                }

                                override fun startBioFail(error: Error?) {
                                    appendLog("Bio算法初始化失败：${error}")
                                }

                                override fun startFail(error: Error?) {
                                    appendLog("算法初始化失败：${error}")
                                }

                                override fun startSuccess() {
                                    appendLog("算法初始化成功")
                                    startCollection(this@apply)
                                    appendLog("开始采集数据...")
                                }
                            })
                    }
                }
            }, EnterAffectiveConfigProxy())

        }
    }

    fun onEnd(view: View) {
        biomoduleBleManager.stopHeartAndBrainCollection()
        affectiveDataAnalysisService?.getReport(object : IGetReportListener {
            override fun getAffectiveReportError(error: Error?) {
                appendLog("获取Affective报表数据失败：${error}")
            }

            override fun getBioReportError(error: Error?) {
                appendLog("获取Bio报表数据失败：${error}")
            }

            override fun onError(error: Error?) {
                appendLog("生成报表数据失败：${error}")
            }

            override fun onSuccess(entity: UploadReportEntity?) {
                appendLog("生成报表数据：${entity}")
            }
        }, true)

    }

    fun onAnalysisSceegData(view: View) {
        startAffectiveService {
            thread {
                var inputStream = resources.openRawResource(R.raw.sceeg)
                it?.apply {
                    readFileAnalysisData(inputStream, { singleData ->
                        appendSCEEGData(singleData)
                        true
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
                            it?.getReport(object : IGetReportListener {
                                override fun getAffectiveReportError(error: Error?) {

                                }

                                override fun getBioReportError(error: Error?) {
                                }

                                override fun onError(error: Error?) {
                                    appendLog("生成报表数据失败：${error}")
                                }

                                override fun onSuccess(entity: UploadReportEntity?) {
                                    appendLog("生成报表数据：${entity}")
                                }
                            }, true)
                        }
                    })
                }
            }
        }
    }

    fun onAnalysisCushionData(view: View) {
        /*   enterAffectiveSDKManager.init(R.raw.algorithm_auth, fun() {
               appendLog("算法初始化成功")
               cushionBleManager.startCollection()
               appendLog("开始采集数据...")

               thread {
                   var inputStream = resources.openRawResource(R.raw.pepr1)
                   enterAffectiveSDKManager.readFileAndAppendData(inputStream) {
                       it?.apply {
                           enterAffectiveSDKManager.appendPepr(this)
                       }
                   }
                   cushionBleManager.stopCollection()
                   var report = enterAffectiveSDKManager.finish()
                   appendLog("生成报表数据：${report}")
               }
           }, fun(error: String) {
               appendLog("算法初始化失败：${error}")
           })*/

    }

    fun onAnalysisEegData(view: View) {
        /*   enterAffectiveSDKManager.setSaveRawData(true)
           enterAffectiveSDKManager.init(R.raw.algorithm_auth, fun() {
               appendLog("算法初始化成功")
               biomoduleBleManager.startHeartAndBrainCollection()
               appendLog("开始采集数据...")

               thread {
                   var inputStream = resources.openRawResource(R.raw.eeg_test)
                   enterAffectiveSDKManager.readFileAndAppendData(inputStream) {
                       it?.apply {
                           enterAffectiveSDKManager.appendEEG(this)
                       }
                   }
                   biomoduleBleManager.stopHeartAndBrainCollection()
                   var report = enterAffectiveSDKManager.finish()
                   appendLog("生成报表数据：${report}")
               }
           }, fun(error: String) {
               appendLog("算法初始化失败：${error}")
           })*/
    }


    /**
     * Android6.0 auth
     */
    fun initPermission() {
        val needPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
        val needRequestPermissions = ArrayList<String>()
        for (i in needPermission.indices) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    needPermission[i]
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                needRequestPermissions.add(needPermission[i])
            }
        }
        if (needRequestPermissions.size != 0) {
            val permissions = arrayOfNulls<String>(needRequestPermissions.size)
            for (i in needRequestPermissions.indices) {
                permissions[i] = needRequestPermissions[i]
            }
            ActivityCompat.requestPermissions(this@MainActivity, permissions, 1)
        }
    }

    fun appendLog(text: String) {
        runOnUiThread {
            screenText += "->:$text\n"
            if (screenText.split("\n").size >= 20) {
                var startIndex = screenText.indexOfFirst {
                    it == '\n'
                }
                screenText = screenText.substring(startIndex + 1, screenText.length)
            }
            binding.tvLogs.text = screenText
            binding.scrollViewLogs.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }

    override fun onDestroy() {
        biomoduleBleManager.removeRawDataListener(rawDataListener)
        cushionBleManager.removeRawDataListener(rawCushionDataListener)
        biomoduleBleManager.removeHeartRateListener(hrDataListener)
        affectiveDataAnalysisService?.finishAffectiveService(object :
            IFinishAffectiveServiceListener {
            override fun finishAffectiveFail(error: Error?) {
                TODO("Not yet implemented")
            }

            override fun finishBioFail(error: Error?) {
                TODO("Not yet implemented")
            }

            override fun finishError(error: Error?) {
                TODO("Not yet implemented")
            }

            override fun finishSuccess() {
                TODO("Not yet implemented")
            }
        })
        super.onDestroy()
    }

}