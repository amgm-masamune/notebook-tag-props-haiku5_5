package com.amgm.notebook

import android.app.Application
import com.amgm.notebook.di.AppContainer
import com.amgm.notebook.di.DefaultAppContainer

class NotebookApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer.create(this)
    }
}
