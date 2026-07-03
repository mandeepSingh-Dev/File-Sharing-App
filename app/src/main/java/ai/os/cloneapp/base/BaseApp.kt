package ai.os.cloneapp.base

import android.app.Application

class BaseApp : Application(){


    companion object{
        lateinit var instance : BaseApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

}