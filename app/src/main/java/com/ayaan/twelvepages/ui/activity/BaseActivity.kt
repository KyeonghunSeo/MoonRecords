package com.ayaan.twelvepages.ui.activity

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.ayaan.twelvepages.AppTheme
import com.ayaan.twelvepages.R
import com.ayaan.twelvepages.setGlobalTheme
import com.ayaan.twelvepages.ui.dialog.LoadingDialog

@SuppressLint("Registered")
open class BaseActivity : AppCompatActivity() {
    var progressDialog: LoadingDialog? = null

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    fun initTheme(rootLy: View) {
        setGlobalTheme(rootLy)
        rootLy.setBackgroundColor(AppTheme.background)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            var flags = window.peekDecorView().systemUiVisibility
            flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            }
            window.peekDecorView().systemUiVisibility = flags
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = AppTheme.backgroundDark
        }
    }

    fun showProgressDialog(msg: String? = null, sub: String? = null) {
        hideProgressDialog()
        progressDialog = LoadingDialog(this, if(msg.isNullOrEmpty()) getString(R.string.plz_wait) else msg, sub).apply {
            com.ayaan.twelvepages.showDialog(this, false, true, true, false)
        }
    }

    fun hideProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    fun setFullScreen() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = Color.parseColor("#00000000")
        }
    }
}