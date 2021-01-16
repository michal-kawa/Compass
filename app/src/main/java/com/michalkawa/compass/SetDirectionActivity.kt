package com.michalkawa.compass

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class SetDirectionActivity : AppCompatActivity() {

    var setLatitude: EditText? = null
    var setLongitude: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_direction)

        setLatitude = findViewById(R.id.et_set_latitude)
        setLongitude = findViewById(R.id.et_set_longitude)
    }

    fun setDirectionAndClose(view: View) {
        val resultIntent = Intent()
        resultIntent.putExtra(NavigationActivity.DIRECTION_EXTRA_VALUES_LATITUDE, setLatitude!!.text.toString())
        resultIntent.putExtra(NavigationActivity.DIRECTION_EXTRA_VALUES_LONGITUDE, setLongitude!!.text.toString())

        setResult(RESULT_OK, resultIntent)
        finish()
    }
}