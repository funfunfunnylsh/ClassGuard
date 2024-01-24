package com.lsh.app.app

import android.app.Application


class MyApplication  : Application(){

    companion object{
        const val TEXT = "text"
    }

    override fun onCreate() {
        super.onCreate()
    }
}
