package com.hellowo.journey.ui.dialog

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import com.hellowo.journey.R
import com.hellowo.journey.model.Alarm
import com.hellowo.journey.model.Record
import com.hellowo.journey.setGlobalTheme
import kotlinx.android.synthetic.main.dialog_alarm_picker.*


@SuppressLint("ValidFragment")
class AlarmPickerDialog(activity: Activity, private val record: Record, private val alarm: Alarm,
                        private val onResult: (Boolean, Long) -> Unit) : Dialog(activity) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.attributes.windowAnimations = R.style.DialogAnimation
        setContentView(R.layout.dialog_alarm_picker)
        setGlobalTheme(rootLy)
        setLayout()
        setOnShowListener {}
    }

    private fun setLayout() {
        rootLy.layoutParams.width = WRAP_CONTENT
        rootLy.requestLayout()

        alarmPicker.onSelected = { offset ->
            onResult.invoke(true, offset)
            dismiss()
        }
        deleteBtn.setOnClickListener {
            onResult.invoke(false, 0)
            dismiss()
        }
    }
}
