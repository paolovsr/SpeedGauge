package com.omsi.speedgaugedemo

import android.animation.ValueAnimator
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.pab.speedgauge.SpeedGauge

class MainActivity : AppCompatActivity() {

   private lateinit var speedGauge:SpeedGauge

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

         speedGauge = findViewById<SpeedGauge>(R.id.main_speed_gauge);

        animateGaugeUp()

    }



    fun animateGaugeUp(){
        val valueAnimator = ValueAnimator.ofInt(0, 4000)
        valueAnimator.duration = 5000
        valueAnimator.repeatMode = ValueAnimator.REVERSE
        valueAnimator.repeatCount = ValueAnimator.INFINITE

        valueAnimator.addUpdateListener { valueAnimator ->
            speedGauge.setValue((valueAnimator.animatedFraction*4000).toInt())
        /*    textview.setText(
                getString(
                    formatter,
                    initialValue + valueAnimator.animatedFraction * (finalValue - initialValue)
                )
            )*/
        }
        valueAnimator.start()
    }

    fun animateGaugeDown(){
        val valueAnimator = ValueAnimator.ofInt(4000, 0)
        valueAnimator.duration = 5000

        valueAnimator.addUpdateListener { valueAnimator ->
            speedGauge.setValue((valueAnimator.animatedFraction*4000).toInt())
            /*    textview.setText(
                    getString(
                        formatter,
                        initialValue + valueAnimator.animatedFraction * (finalValue - initialValue)
                    )
                )*/
            if(valueAnimator.animatedFraction<=100f){
                animateGaugeUp()
            }
        }
        valueAnimator.start()
    }
}