package com.studiotaraneh.app

import android.app.Application
import com.studiotaraneh.app.data.AppDatabase

class StudioTaranehApplication : Application() {
    val database by lazy { AppDatabase.get(this) }
}
