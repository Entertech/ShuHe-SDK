package cn.entertech.shuhedemo

import android.os.Bundle
import android.view.View
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import cn.entertech.affective.sdk.api.Callback
import cn.entertech.affective.sdk.api.IAffectiveDataAnalysisService
import cn.entertech.affective.sdk.api.IConnectionServiceListener
import cn.entertech.affective.sdk.api.IFinishAffectiveServiceListener
import cn.entertech.affective.sdk.api.IGetReportListener
import cn.entertech.affective.sdk.api.IStartAffectiveServiceLister
import cn.entertech.affective.sdk.bean.AffectiveServiceWay
import cn.entertech.affective.sdk.bean.EnterAffectiveConfigProxy
import cn.entertech.affective.sdk.bean.Error
import cn.entertech.affective.sdk.bean.UploadReportEntity
import cn.entertech.affectivesdk.utils.SingleChannelEEGUtil
import cn.entertech.affectivesdk.utils.SingleChannelEEGUtil.SINGLE_EGG_PCK_CHECK
import cn.entertech.affectivesdk.utils.SingleChannelEEGUtil.SINGLE_EGG_PCK_END
import cn.entertech.affectivesdk.utils.SingleChannelEEGUtil.SINGLE_EGG_PCK_START
import cn.entertech.shuhedemo.databinding.ActivityMainBinding
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.util.Random
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"
    private lateinit var binding: ActivityMainBinding
    private val affectiveDataAnalysisService by lazy {
        IAffectiveDataAnalysisService.getService(AffectiveServiceWay.AffectiveLocalService)
    }
    private var screenText: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
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
        startAffectiveService {
            thread {
                var inputStream = resources.openRawResource(R.raw.pepr1)
                it?.apply {
                    readFileAnalysisData(inputStream, { singleData ->
                        false
                    }, { allData ->
                        if (allData.isNotEmpty()) {
                            appendPEPRData(allData.toByteArray())
                        }
                    }, {
                        it.toInt().toByte()
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
        affectiveDataAnalysisService?.finishAffectiveService(object :
            IFinishAffectiveServiceListener {
            override fun finishAffectiveFail(error: Error?) {
                appendLog("算法初始化失败：${error}")
            }

            override fun finishBioFail(error: Error?) {
                appendLog("算法初始化失败：${error}")
            }

            override fun finishError(error: Error?) {
                appendLog("算法初始化失败：${error}")
            }

            override fun finishSuccess() {
                appendLog("结束服务成功")
            }
        })
        affectiveDataAnalysisService?.closeAffectiveServiceConnection()
        super.onDestroy()
    }

}