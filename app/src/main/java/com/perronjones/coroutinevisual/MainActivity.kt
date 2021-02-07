
package com.perronjones.coroutinevisual

import android.animation.*
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import com.perronjones.coroutinevisual.databinding.ActivityMainBinding
import kotlinx.coroutines.*

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    private var binding: ActivityMainBinding? = null
    private var blueActivityScope = CoroutineScope(Dispatchers.IO + Job())
    private val blueExceptionHandler = CoroutineExceptionHandler { _, exception ->
        Log.e(TAG, "Exception (${exception::class.qualifiedName}) occurred.")
        blueActivityScope.cancel("Cancelling due to caught exception")
        // Reinitialize Scope
        blueActivityScope = initializeBlueCoroutineScope()
        stopBouncingFromException(binding?.blueCircle)
    }
    private var orangeJob = Job()
    private var orangeActivityScope = CoroutineScope(Dispatchers.IO + orangeJob)
    private val orangeExceptionHandler = CoroutineExceptionHandler { _, exception ->
        Log.e(TAG, "Exception (${exception::class.qualifiedName}) occurred.")
        // Cancel Scope and explicitly cancel job and children
        orangeActivityScope.cancel("Cancelling due to caught exception")
        orangeJob.cancelChildren()
        orangeJob.cancel()
        stopBouncingFromException(binding?.orangeCircle)
    }
    private var grayJob = Job()
    private var grayActivityScope = CoroutineScope(Dispatchers.IO + grayJob)
    private val grayExceptionHandler = CoroutineExceptionHandler { _, exception ->
        Log.e(TAG, "Exception (${exception::class.qualifiedName}) occurred.")
        // Cancel scope only
        grayActivityScope.cancel("Cancelling due to caught exception")
        stopBouncingFromException(binding?.grayCircle)
    }
    private val animationScope = CoroutineScope(Dispatchers.Main)

    private val animatorMap = mutableMapOf<View, Animator>()

    private fun initializeBlueCoroutineScope() = CoroutineScope(Dispatchers.IO + Job())
    private fun initializeOrangeCoroutineScope() = CoroutineScope(Dispatchers.IO + orangeJob)
    private fun initializeGrayCoroutineScope() = CoroutineScope(Dispatchers.IO + grayJob)

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        binding!!.blueCircle.drawable.colorFilter = BlendModeColorFilter(Color.BLUE, BlendMode.SRC)
        binding!!.orangeCircle.drawable.colorFilter = BlendModeColorFilter(Color.parseColor("#FFA500"), BlendMode.SRC)
        binding!!.grayCircle.drawable.colorFilter = BlendModeColorFilter(Color.parseColor("#808080"), BlendMode.SRC)

        binding!!.launchBtn.setOnClickListener {
            onBounceButtonClicked()
        }
        binding!!.exceptionBtn.setOnClickListener {
            onBlowUpButtonClicked()
        }

        binding!!.resetBtn.setOnClickListener {
            resetCoroutineScopes()
        }
    }

    private fun onBounceButtonClicked() {
        Log.i(TAG, "test button clicked")
        binding?.let {
            blueActivityScope.launch(blueExceptionHandler) {
                bounceBall(it.blueCircle, true)
            }
            orangeActivityScope.launch(orangeExceptionHandler) {
                bounceBall(it.orangeCircle, true)
            }
            grayActivityScope.launch(grayExceptionHandler) {
                bounceBall(it.grayCircle, true)
            }
        }
    }

    private fun onBlowUpButtonClicked() {
        throwException(blueActivityScope, blueExceptionHandler)
        throwException(orangeActivityScope, orangeExceptionHandler)
        throwException(grayActivityScope, grayExceptionHandler)
    }

    private fun throwException(scope: CoroutineScope, exceptionHandler: CoroutineExceptionHandler) {
        scope.launch(exceptionHandler) {
            throw RuntimeException()
        }
    }

    private fun stopBouncingFromException(view: View?) {
        view?.let {
            animationScope.launch {
                bounceBall(it, false)
            }
        }
    }

    private suspend fun bounceBall(view: View, start: Boolean) {
        var animator = animatorMap[view]
        if(start && animator?.isRunning != true) {
            animator?.let {
                withContext(Dispatchers.Main) {
                    it.start()
                }
            } ?: run {
                animator = AnimatorInflater.loadAnimator(this, R.animator.anim_bounce)
                animator!!.setTarget(view)
                animator!!.addListener(getAnimationListener(view))
                withContext(Dispatchers.Main) {
                    animator!!.start()
                }
            }
            animatorMap[view] = animator!!
        } else if(!start){
            animator?.end()
        }
    }

    private fun getAnimationListener(view: View) = object : Animator.AnimatorListener {
        override fun onAnimationStart(animation: Animator?) {
            // no op
        }

        override fun onAnimationEnd(animation: Animator?) {
            resetViewPosition(view)
        }

        override fun onAnimationCancel(animation: Animator?) {
            resetViewPosition(view)
        }

        override fun onAnimationRepeat(animation: Animator?) {
            // no op
        }
    }

    private fun resetViewPosition(view: View) {
        val objAnimator = ObjectAnimator.ofFloat(view, "translationY", 0f)
        objAnimator.repeatMode = ValueAnimator.REVERSE
        objAnimator.start()
    }

    private fun resetCoroutineScopes() {
        blueActivityScope = initializeBlueCoroutineScope()
        orangeJob = Job()
        orangeActivityScope = initializeOrangeCoroutineScope()
        grayJob = Job()
        grayActivityScope = initializeGrayCoroutineScope()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}