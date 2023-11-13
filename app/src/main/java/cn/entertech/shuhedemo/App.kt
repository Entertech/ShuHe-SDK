package cn.entertech.shuhedemo

import android.app.Application
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security




class App: Application() {

    companion object {
        var application: Application? = null
        fun getInstance(): Application {
            return application!!
        }
    }

    override fun onCreate() {
        super.onCreate()
        application = this
        Security.addProvider(BouncyCastleProvider())
    }
}