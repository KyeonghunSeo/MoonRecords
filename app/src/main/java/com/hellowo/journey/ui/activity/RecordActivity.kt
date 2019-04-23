package com.hellowo.journey.ui.activity

import android.Manifest
import android.animation.AnimatorSet
import android.animation.LayoutTransition
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.widget.NestedScrollView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.google.android.gms.location.places.ui.PlacePicker
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.hellowo.journey.*
import com.hellowo.journey.alarm.AlarmManager
import com.hellowo.journey.manager.RecordManager
import com.hellowo.journey.manager.RepeatManager
import com.hellowo.journey.model.Alarm
import com.hellowo.journey.model.Link
import com.hellowo.journey.model.Record
import com.hellowo.journey.model.Tag
import com.hellowo.journey.ui.dialog.*
import kotlinx.android.synthetic.main.activity_record.*
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import net.yslibrary.android.keyboardvisibilityevent.Unregistrar
import java.io.ByteArrayOutputStream
import java.util.*

class RecordActivity : BaseActivity() {
    private var originalData: Record? = null
    private val record: Record = Record()
    private var googleMap: GoogleMap? = null
    private var keypadListener : Unregistrar? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record)
        MainActivity.getViewModel()?.targetTimeObject?.value?.let {
            initTheme(rootLy)
            initLayout()
            initInput()
            initMap()
            setData(it)
            updateUI()
            return
        }
        finish()
    }

    private fun initLayout() {
        folderLy.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
        btnsLy.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
        mainScrollView.setOnScrollChangeListener { _: NestedScrollView?, _: Int, scrollY: Int, _: Int, _: Int ->
            if(scrollY > 0) topShadow.visibility = View.VISIBLE
            else topShadow.visibility = View.GONE
        }
        editorTimeBtn.setOnClickListener { editorAction("time") }
        editorQuoteBtn.setOnClickListener { editorAction("quote") }
        editorQuotesBtn.setOnClickListener { editorAction("quotes") }
        editorDotBtn.setOnClickListener { editorAction("dot") }
        editorUpBtn.setOnClickListener { editorAction("up") }
        editorDownBtn.setOnClickListener { editorAction("down") }
        editorLeftBtn.setOnClickListener { editorAction("left") }
        editorRightBtn.setOnClickListener { editorAction("right") }
    }

    private fun initInput() {
        keypadListener = KeyboardVisibilityEvent.registerEventListener(this) { isOpen ->
            l("키보드 상태 $isOpen")
            if(isOpen) {

            }else {

            }
        }
        
        titleInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if(titleInput.text.isNotEmpty()) {
                    record.title = titleInput.text.toString()
                }else {
                    record.title = null
                }
                if(record.inCalendar) updateStyleUI()
            }
            override fun afterTextChanged(p0: Editable?) {}
        })
        titleInput.onScaleChanged = { isNormalScale ->
            val scale = if(isNormalScale) 1f else 0.7f
            val animSet = AnimatorSet()
            animSet.playTogether(ObjectAnimator.ofFloat(checkBox, "scaleX", checkBox.scaleX, scale),
                    ObjectAnimator.ofFloat(checkBox, "scaleY", checkBox.scaleY, scale))
            animSet.duration = 200L
            animSet.start()
        }

        callAfterViewDrawed(titleInput, Runnable{
            updateTitleUI()
            if(!record.id.isNullOrEmpty()) {
                l("!!!!!!!!!!")
                showKeyPad(titleInput)
            }
        })

        memoInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if(memoInput.text.isNotEmpty()) {
                    record.description = memoInput.text.toString()
                }else {
                    record.description = null
                }
            }
            override fun afterTextChanged(p0: Editable?) {}
        })
    }

    private fun initMap() {
        (supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment).getMapAsync { map ->
            googleMap = map
            mapTouchView.setOnTouchListener { v, event -> event.action == MotionEvent.ACTION_MOVE }
            updateLocationUI()
        }
    }

    private fun updateUI() {
        updateHeaderUI()
        updateTagUI()
        updateCheckBoxUI()
        updateCheckListUI()
        updateDeadLineUI()
        updatePercentageUI()
        updateDateUI()
        updateDdayUI()
        updateRepeatUI()
        updateAlarmUI()
        updateMemoUI()
        updateStyleUI()
        updateLinkUI()
    }

    private fun updateHeaderUI() {
        addOptionBtn.setOnClickListener {
            showDialog(MoreOptionDialog(this, record, this),
                    true, true, true, false)
        }

        colorBtn.setOnClickListener {
            showDialog(ColorPickerDialog(this, record.getColor()) { colorKey ->
                record.colorKey = colorKey
                updateUI()
            }, true, true, true, false)
        }

        pinBtn.setOnClickListener {
            record.inCalendar = !record.inCalendar
            if(record.inCalendar) toast(R.string.show_in_calendar)
            else toast(R.string.hide_in_calendar)
            updateHeaderUI()
            updateStyleUI()
        }

        styleBtn.setOnClickListener {}

        deleteBtn.setOnClickListener {
            showDialog(CustomDialog(this, getString(R.string.delete),
                    getString(R.string.delete_sub), null) { result, _, _ ->
                if(result) delete()
            }, true, true, true, false)
        }

        moreBtn.setOnClickListener {
            moreBtn.visibility = View.GONE
            updateHeaderUI()
        }

        if(moreBtn.visibility == View.GONE) {
            colorBtn.visibility = View.VISIBLE
            deleteBtn.visibility = View.VISIBLE
            if(record.folder == null) {
                pinBtn.visibility = View.VISIBLE
                if(record.inCalendar) {
                    pinBtn.alpha = 1f
                    styleBtn.visibility = View.VISIBLE
                }else {
                    pinBtn.alpha = 0.3f
                    styleBtn.visibility = View.GONE
                }
            }else {
                pinBtn.visibility = View.GONE
                styleBtn.visibility = View.GONE
            }
        }

        folderImg.setColorFilter(record.getColor())
        folderText.setTextColor(record.getColor())
        addOptionBtn.setColorFilter(record.getColor())
        colorBtn.setColorFilter(record.getColor())
        pinBtn.setColorFilter(record.getColor())
        styleBtn.setColorFilter(record.getColor())
        deleteBtn.setColorFilter(record.getColor())
        moreBtn.setColorFilter(record.getColor())

        updateFolderUI()
    }

    private fun updateFolderUI() {
        if(record.folder != null) {
            folderImg.setImageResource(R.drawable.folder)
            folderText.text = record.folder?.name
            folderText.setOnClickListener { showFolderPickerDialog() }
        }else {
            folderImg.setImageResource(R.drawable.calendar_empty)
            folderText.text = AppDateFormat.simpleYmdDate.format(Date(record.dtStart))
            folderText.setOnClickListener { showDatePickerDialog() }
        }
        folderImg.setOnClickListener {
            showDialog(CustomListDialog(this,
                    getString(R.string.record_position),
                    getString(R.string.record_position_sub),
                    null,
                    false,
                    listOf(getString(R.string.calendar), getString(R.string.folder))) { index ->
                if(index == 0) showDatePickerDialog()
                else showFolderPickerDialog()
            }, true, true, true, false)
        }
    }

    private fun showDatePickerDialog() {
        val time = if(record.dtStart == Long.MIN_VALUE) System.currentTimeMillis()
        else record.dtStart

        showDialog(DatePickerDialog(this, time) {
            record.folder = null
            record.setDate(it)
            updateHeaderUI()
            updateDateUI()
        }, true, true, true, false)
    }

    private fun showFolderPickerDialog() {
        MainActivity.getViewModel()?.folderList?.value?.let { folderList ->
            showDialog(CustomListDialog(this,
                    getString(R.string.folder),
                    null,
                    null,
                    false,
                    folderList.map { if(it.name.isNullOrBlank()) getString(R.string.untitle) else it.name!! }) { index ->
                record.folder = folderList[index]
                updateHeaderUI()
            }, true, true, true, false)
        }
    }

    private fun updateTagUI() {
        if(record.tags.isNotEmpty()) {
            tagText.visibility = View.VISIBLE
            tagText.text = record.tags.joinToString("") { "#${it.id}" }
            tagText.setOnClickListener { showTagDialog() }
        }else {
            tagText.visibility = View.GONE
            tagText.setOnClickListener(null)
        }
    }

    fun showTagDialog() {
        val items = ArrayList<Tag>().apply { addAll(record.tags) }
        showDialog(TagDialog(this, items) {
            record.tags.clear()
            record.tags.addAll(it)
            updateTagUI()
        }, true, true, true, false)
    }

    private fun updateTitleUI() {
        titleInput.setText(record.title)
        titleInput.setSelection(record.title?.length ?: 0)
    }

    fun updateCheckBoxUI() {
        if(record.isSetCheckBox()) {
            checkBox.visibility = View.VISIBLE
            if(record.isDone()) {
                checkBox.setImageResource(R.drawable.check)
            }else {
                checkBox.setImageResource(R.drawable.uncheck)
            }
            checkBox.setOnClickListener {
                if(record.isDone()) record.undone()
                else record.done()
                updateCheckBoxUI()
            }
            checkBox.setOnLongClickListener {
                showDialog(CustomDialog(this, getString(R.string.checkbox),
                        getString(R.string.delete_checkbox_sub), null) { result, _, _ ->
                    if(result) {
                        record.clearCheckBox()
                        updateCheckBoxUI()
                    }
                }, true, true, true, false)
                return@setOnLongClickListener true
            }
        }else {
            checkBox.visibility = View.GONE
        }
    }

    fun updateCheckListUI() {
        if(record.isSetCheckList()) {
            checkListLy.visibility = View.VISIBLE
            checkListView.setCheckList(record)
        }else {
            checkListLy.visibility = View.GONE
        }
    }

    fun updateDeadLineUI() {
        if(record.isSetDeadLine()) {
            deadlineLy.visibility = View.VISIBLE
            deadlineText.text = record.getDdayText(System.currentTimeMillis())
            deadlineLy.setOnClickListener {
                showDialog(CustomDialog(this, getString(R.string.deadline),
                        getString(R.string.delete_deadline_sub), null) { result, _, _ ->
                    if(result) {
                        record.clearDeadLine()
                        updateDeadLineUI()
                    }
                }, true, true, true, false)
            }
        }else {
            deadlineLy.visibility = View.GONE
        }
    }

    fun updatePercentageUI() {
        if(record.isSetPercentage()) {
            percentageLy.visibility = View.VISIBLE
            percentageLy.setOnClickListener {
                showDialog(CustomDialog(this, getString(R.string.percentage),
                        getString(R.string.delete_percentage_sub), null) { result, _, _ ->
                    if(result) {
                        record.clearPercentage()
                        updatePercentageUI()
                    }
                }, true, true, true, false)
            }
        }else {
            percentageLy.visibility = View.GONE
        }
    }

    private val startCal = Calendar.getInstance()
    private val endCal = Calendar.getInstance()

    @SuppressLint("SetTextI18n")
    private fun updateDateUI() {
        if(record.isScheduled()) {
            timeLy.visibility = View.VISIBLE
            startCal.timeInMillis = record.dtStart
            endCal.timeInMillis = record.dtEnd

            timeLy.setOnClickListener { showStartEndDialog() }
            timeLy.setOnLongClickListener {
                showDialog(CustomDialog(this, getString(R.string.shedule),
                        getString(R.string.delete_shedule_sub), null) { result, _, _ ->
                    if(result) {
                        record.clearSchdule()
                        updateDateUI()
                    }
                }, true, true, true, false)
                return@setOnLongClickListener true
            }

            if(record.allday) {
                smallStartText.text = AppDateFormat.ymDate.format(startCal.time)
                bigStartText.text = "${AppDateFormat.date.format(startCal.time)} ${AppDateFormat.simpleDow.format(startCal.time)}"
                smallEndText.text = AppDateFormat.ymDate.format(endCal.time)
                bigEndText.text = "${AppDateFormat.date.format(endCal.time)} ${AppDateFormat.simpleDow.format(endCal.time)}"
            }else {
                smallStartText.text = AppDateFormat.ymdDate.format(startCal.time)
                bigStartText.text = AppDateFormat.time.format(startCal.time)
                smallEndText.text = AppDateFormat.ymdDate.format(endCal.time)
                bigEndText.text = AppDateFormat.time.format(endCal.time)
            }

            if(startCal == endCal) {
                startEndDivider.visibility = View.GONE
                endLy.visibility = View.GONE
            }else {
                startEndDivider.visibility = View.VISIBLE
                endLy.visibility = View.VISIBLE
                durationText.text = getDurationText(startCal.timeInMillis, endCal.timeInMillis, record.allday)
            }
        }else {
            timeLy.visibility = View.GONE
        }
    }

    fun showStartEndDialog() {
        showDialog(StartEndPickerDialog(this, record) { sCal, eCal, allday ->
            record.setSchedule()
            record.setDateTime(allday, sCal, eCal)
            updateUI()
        }, true, true, true, false)
    }

    fun updateDdayUI() {
        if(record.isSetDday()) {
            ddayLy.visibility = View.VISIBLE
            ddayText.text = record.getDdayText(System.currentTimeMillis())
            ddayLy.setOnClickListener {
                showDialog(CustomDialog(this, getString(R.string.dday),
                        getString(R.string.delete_dday_sub), null) { result, _, _ ->
                    if(result) {
                        record.clearDday()
                        updateDdayUI()
                    }
                }, true, true, true, false)
            }
        }else {
            ddayLy.visibility = View.GONE
        }
    }

    private fun updateRepeatUI() {
        if(record.isRepeat()) {
            repeatLy.visibility = View.VISIBLE
            repeatText.text = RepeatManager.makeRepeatText(record)
            if(record.isLunarRepeat()) {
                //repeatIcon.setImageResource(R.drawable.lunar)
                repeatLy.setOnClickListener { showLunarRepeatDialog() }
            }else {
                //repeatIcon.setImageResource(R.drawable.sharp_repeat_black_48dp)
                repeatLy.setOnClickListener { showRepeatDialog() }
            }
        }else {
            repeatLy.visibility = View.GONE
        }
    }

    fun showRepeatDialog() {
        showDialog(RepeatDialog(this, record) { repeat, dtUntil ->
            record.repeat = repeat
            record.dtUntil = dtUntil
            updateRepeatUI()
        }, true, true, true, false)
    }

    fun showLunarRepeatDialog() {
        showDialog(LunarRepeatDialog(this, record) { repeat, dtStart ->
            record.setDate(dtStart)
            record.repeat = repeat
            record.dtUntil = Long.MIN_VALUE
            updateRepeatUI()
        }, true, true, true, false)
    }

    private fun updateAlarmUI() {
        if(record.alarms.isNotEmpty()) {
            alarmLy.visibility = View.VISIBLE
            record.alarms[0]?.let { alarm ->
                alarmText.text = AlarmManager.getTimeObjectAlarmText(this, alarm)
                alarmLy.setOnClickListener { showAlarmDialog(alarm) }
            }
        }else {
            alarmLy.visibility = View.GONE
        }
    }

    private fun updateLocationUI() {
        if(record.location.isNullOrBlank()) {
            locationLy.visibility = View.GONE
        }else {
            locationLy.visibility = View.VISIBLE
            locationText.text = record.location

            val latLng = LatLng(record.latitude, record.longitude)
            googleMap?.clear()
            googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
            googleMap?.addMarker(MarkerOptions().position(latLng))

            locationText.setOnClickListener {
                showDialog(CustomDialog(this, getString(R.string.location),
                        null, arrayOf(getString(R.string.delete), getString(R.string.edit)))
                { result, index, _ ->
                    if(result) {
                        if(index == 0) {
                            record.location = null
                            updateLocationUI()
                        }else {
                            showPlacePicker()
                        }
                    }
                }, true, true, true, false)
            }
            mapTouchView.setOnClickListener{ openMapActivity() }
        }
    }

    private fun updateMemoUI() {
        if(record.description.isNullOrEmpty()) {
            memoInput.setText("")
            memoLy.visibility = View.GONE
        }else {
            memoInput.setText(record.description)
            memoLy.visibility = View.VISIBLE
        }
    }

    private fun updateStyleUI() {}

    fun showMemoUI() {
        memoLy.visibility = View.VISIBLE
        showKeyPad(memoInput)
    }

    fun updateLinkUI() {
        if(record.isSetLink()) {
            linkLy.visibility = View.VISIBLE
            linkListView.setList(record)
        }else {
            linkLy.visibility = View.GONE
        }
    }

    private fun setData(data: Record) {
        originalData = data
        record.copy(data)
    }

    override fun onBackPressed() {
        confirm()
    }

    fun confirm() {
        if(originalData != record) {
            if(originalData?.repeat.isNullOrEmpty()) {
                RecordManager.save(record)
                finish()
            }else {
                RepeatManager.save(this, record, Runnable { finish() })
            }
        }else {
            finish()
        }
    }

    fun delete() {
        if(originalData?.repeat.isNullOrEmpty()) {
            RecordManager.delete(record)
            finish()
        }else {
            RepeatManager.delete(this, record, Runnable { finish() })
        }
    }

    fun showPlacePicker() {
        val builder = PlacePicker.IntentBuilder()
        startActivityForResult(builder.build(this), RC_LOCATION)
    }

    private fun openMapActivity() {
        val intent = Intent(this, MapActivity::class.java)
        intent.putExtra("location", record.location)
        intent.putExtra("lat", record.latitude)
        intent.putExtra("lng", record.longitude)
        startActivity(intent)
    }

    fun addNewAlarm() {
        val alarm = Alarm(UUID.randomUUID().toString(), record.dtStart, 0, 0)
        record.alarms.add(alarm)
        showAlarmDialog(alarm)
    }

    private fun showAlarmDialog(alarm: Alarm) {
        showDialog(AlarmPickerDialog(this, record, alarm) { result, offset ->
            if (result) {
                alarm.offset = offset
                if (offset != Long.MIN_VALUE) {
                    alarm.dtAlarm = record.dtStart + offset
                } else {
                    alarm.dtAlarm = record.dtStart
                    showTimePicker(alarm.dtAlarm) {
                        alarm.dtAlarm = it
                        updateAlarmUI()
                    }
                }
            } else {
                record.alarms.remove(alarm)
            }
            updateAlarmUI()
        }, true, true, true, false)
    }

    private fun showTimePicker(time: Long, onResult: (Long) -> (Unit)) {
        showDialog(TimePickerDialog(this, time, onResult),
                true, true, true, false)
    }

    fun showEditWebsiteDialog() {
        showDialog(AddWebLinkDialog(this) { link ->
            record.links.add(link)

        }, true, true, true, false)
    }

    fun showImagePicker() {
        checkExternalStoragePermission(RC_IMAGE_ATTACHMENT)
    }

    fun checkExternalStoragePermission(requestCode: Int) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), requestCode)
        } else {
            showPhotoPicker(requestCode)
        }
    }

    private fun showPhotoPicker(requestCode: Int) {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        try {
            startActivityForResult(Intent.createChooser(intent, "Select a File to Upload"), requestCode)
        } catch (ex: android.content.ActivityNotFoundException) { ex.printStackTrace() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RC_LOCATION && resultCode == Activity.RESULT_OK) {
            val place = PlacePicker.getPlace(this, data)
            record.location = "${place.name}\n${place.address}"
            record.latitude = place.latLng.latitude
            record.longitude = place.latLng.longitude
            updateLocationUI()
        }else if (requestCode == RC_IMAGE_ATTACHMENT && resultCode == AppCompatActivity.RESULT_OK) {
            if (data != null) {
                val uri = data.data
                try{
                    showProgressDialog(null)
                    Glide.with(this).asBitmap().load(uri)
                            .into(object : SimpleTarget<Bitmap>(){
                                override fun onResourceReady(resource: Bitmap,
                                                             transition: com.bumptech.glide.request.transition.Transition<in Bitmap>?) {
                                    l("사진 크기 : ${resource.rowBytes} 바이트")
                                    val imageId = UUID.randomUUID().toString()
                                    val ref = FirebaseStorage.getInstance().reference
                                            .child("${FirebaseAuth.getInstance().uid}/$imageId.jpg")
                                    val baos = ByteArrayOutputStream()
                                    resource.compress(Bitmap.CompressFormat.JPEG, 25, baos)
                                    val uploadTask = ref.putBytes(baos.toByteArray())
                                    uploadTask.addOnFailureListener {
                                        hideProgressDialog()
                                    }.addOnSuccessListener { _ ->
                                        ref.downloadUrl.addOnCompleteListener {
                                            l("다운로드 url : ${it.result.toString()}")
                                            record.links.add(Link(imageId, Link.Type.IMAGE.ordinal,
                                                    null, it.result.toString()))
                                            hideProgressDialog()
                                            updateLinkUI()
                                        }
                                    }
                                }
                                override fun onLoadFailed(errorDrawable: Drawable?) {
                                    super.onLoadFailed(errorDrawable)
                                    hideProgressDialog()
                                }
                            })
                }catch (e: Exception){}
            }
        }
    }

    fun setKeyboardLy(isOpen: Boolean) {
        if(isOpen) {
            textEditorLy.visibility = View.GONE
        }else {
            textEditorLy.visibility = View.GONE
        }
    }

    private fun editorAction(action: String) {
        val v = if(titleInput.isFocused) titleInput else if(memoInput.isFocused) memoInput else null
        v?.let {
            when(action) {
                "time" -> {
                    val text = AppDateFormat.time.format(Date())
                    val start = Math.max(v.selectionStart, 0)
                    val end = Math.max(v.selectionEnd, 0)
                    v.text.replace(Math.min(start, end), Math.max(start, end), text, 0, text.length)
                    return@let
                }
                "quote" -> {
                    val text = "“”"
                    val start = Math.max(v.selectionStart, 0)
                    val end = Math.max(v.selectionEnd, 0)
                    v.text.replace(Math.min(start, end), Math.max(start, end), text, 0, text.length)
                    v.setSelection(v.selectionStart - 1)
                    return@let
                }
                "quotes" -> {
                    val text = "‘’"
                    val start = Math.max(v.selectionStart, 0)
                    val end = Math.max(v.selectionEnd, 0)
                    v.text.replace(Math.min(start, end), Math.max(start, end), text, 0, text.length)
                    v.setSelection(v.selectionStart - 1)
                    return@let
                }
                "dot" -> {
                    val text = "\n ㆍ "
                    val start = Math.max(v.selectionStart, 0)
                    val end = Math.max(v.selectionEnd, 0)
                    v.text.replace(Math.min(start, end), Math.max(start, end), text, 0, text.length)
                    return@let
                }
                "left" -> {
                    if(v.selectionStart > 0) v.setSelection(v.selectionStart - 1)
                }
                "up" -> {
                    val start = Math.max(v.selectionStart, 0)
                    val layout = v.layout
                    val currentLine = layout.getLineForOffset(start)
                    if(currentLine > 0) {
                        val offset = start - layout.getLineStart(currentLine)
                        val s = layout.getLineStart(currentLine - 1)
                        val e = layout.getLineEnd(currentLine - 1)
                        v.setSelection(if(s + offset <= e) s + offset else e)
                    }
                }
                "right" -> {
                    if(v.selectionStart < v.text.length) v.setSelection(v.selectionStart + 1)
                }
                "down" -> {
                    val start = Math.max(v.selectionStart, 0)
                    val layout = v.layout
                    val currentLine = layout.getLineForOffset(start)
                    if(currentLine < v.lineCount - 1) {
                        val offset = start - layout.getLineStart(currentLine)
                        val s = layout.getLineStart(currentLine + 1)
                        val e = layout.getLineEnd(currentLine + 1)
                        v.setSelection(if(s + offset <= e) s + offset else e)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        keypadListener?.unregister()
    }
}