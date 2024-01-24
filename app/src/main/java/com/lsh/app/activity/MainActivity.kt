package com.lsh.app.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.lsh.app.R
import com.lsh.app.app.MyApplication

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.leo_ta_leo_ta_activity_main)
        val text = MyApplication.TEXT
        val logo = R.mipmap.leo_ta_leo_ta_ic_launcher_round
    }
}
