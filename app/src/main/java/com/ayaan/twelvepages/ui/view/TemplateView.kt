package com.ayaan.twelvepages.ui.view

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionManager
import com.ayaan.twelvepages.*
import com.ayaan.twelvepages.adapter.RecordCalendarAdapter
import com.ayaan.twelvepages.adapter.TemplateAdapter
import com.ayaan.twelvepages.adapter.util.TemplateDiffCallback
import com.ayaan.twelvepages.manager.RecordManager
import com.ayaan.twelvepages.model.Folder
import com.ayaan.twelvepages.model.Record
import com.ayaan.twelvepages.model.Template
import com.ayaan.twelvepages.ui.activity.MainActivity
import com.ayaan.twelvepages.ui.activity.TemplateActivity
import com.ayaan.twelvepages.ui.dialog.StickerPickerDialog
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.view_template.view.*
import java.util.*
import kotlin.collections.ArrayList

class TemplateView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {
    private val startCal = Calendar.getInstance()
    private val endCal = Calendar.getInstance()
    private val panelElevation = dpToPx(30f)
    private var behavior: BottomSheetBehavior<View>

    init {
        LayoutInflater.from(context).inflate(R.layout.view_template, this, true)

        bottomSheet.setOnClickListener { collapse() }
        templatePanel.setOnClickListener {}

        setOnTouchListener { _, motionEvent ->
            if(motionEvent.action == MotionEvent.ACTION_DOWN) {
                if(MainActivity.isProfileOpened()) {
                    MainActivity.closeProfileView()
                    return@setOnTouchListener true
                }else if(isExpanded()) {
                    collapse()
                    return@setOnTouchListener true
                }
            }
            return@setOnTouchListener super.onTouchEvent(motionEvent)
        }

        behavior = BottomSheetBehavior.from(bottomSheet)
        behavior.isHideable = true
        behavior.state = BottomSheetBehavior.STATE_HIDDEN
        behavior.skipCollapsed = true
        behavior.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback(){
            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if(newState == BottomSheetBehavior.STATE_HIDDEN) {
                    templatePanel.elevation = 0f
                    hiddened()
                }else {
                    templatePanel.elevation = panelElevation
                }
            }
        })
    }

    fun clip(record: Record?) {
        vibrate(context)
        if(record == null) {
            collapse()
        }else {
            if(record.id.isNullOrEmpty()) {
                clipTypeText.text = str(R.string.copy)
            }else {
                clipTypeText.text = str(R.string.cut)
            }
            clipText.text = record.getTitleInCalendar()
            clipIconImg.setColorFilter(record.getColor())
            clipPasteBtn.setOnClickListener {
                MainActivity.getTargetFolder().let { record.folder = Folder(it) }
                record.setDate(MainActivity.getTargetTime() ?: Long.MIN_VALUE)
                if(record.id.isNullOrEmpty()) {
                    if(record.isRepeat()) {
                        record.clearRepeat()
                    }
                    RecordManager.save(record)
                    toast(R.string.copied, R.drawable.copy)
                }else {
                    if(record.isRepeat()) {
                        RecordManager.deleteOnly(record)
                        record.clearRepeat()
                        record.id = null
                        RecordManager.save(record)
                    }else {
                        RecordManager.delete(record)
                        record.id = null
                        RecordManager.save(record)
                    }
                    toast(R.string.moved, R.drawable.change)
                }
                MainActivity.getViewModel()?.clipRecord?.value = null
            }
            clipLy.visibility = View.VISIBLE
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    fun collapse() {
        ObjectAnimator.ofFloat(backgroundLy, "alpha", backgroundLy.alpha, 0f).start()
        MainActivity.instance?.window?.let { removeDimStatusBar(it) }
        MainActivity.instance?.clearCalendarHighlight()
        behavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

    private fun hiddened() {
        ObjectAnimator.ofFloat(backgroundLy, "alpha", backgroundLy.alpha, 0f).start()
        MainActivity.instance?.window?.let { removeDimStatusBar(it) }
        MainActivity.instance?.clearCalendarHighlight()
    }

    fun notifyListChanged() {}

    fun isExpanded() = behavior.state != BottomSheetBehavior.STATE_HIDDEN
}