package com.mt.rotarycontrol.view

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.add
import androidx.fragment.app.commit
import com.mt.rotarycontrol.R

class MainActivity : AppCompatActivity(R.layout.activity_main)  {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Adding fragment
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            add<MainFragment>(R.id.fragment_container_view)
        }
    }
}