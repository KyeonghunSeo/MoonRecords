package com.hellowo.chating.calendar

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.transition.TransitionManager
import com.hellowo.chating.*
import io.realm.RealmResults
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class TimeObjectAdapter(private var items : RealmResults<TimeObject>, private val calendarView: CalendarView) {
    private val viewHolderList = ArrayList<TimeObjectViewHolder>()
    private val viewLevelStatusMap = HashMap<Int, ViewLevelStatus>()
    private val context = calendarView.context
    private val columns = CalendarView.columns
    private var rows = 0
    private var maxCellNum = 0
    private var calStartTime = 0L
    private var withAnimtion = false
    private val cellBottomArray = Array(42){ _ -> CalendarView.dateArea}
    private val rowHeightArray = Array(6){ _ -> CalendarView.dateArea}

    fun draw() {
        setCalendarData()
        computePosition()
        setTimeObjectViews()
        refreshCalendarView()
    }

    fun refresh(result: RealmResults<TimeObject>, anim: Boolean) {
        items = result
        withAnimtion = anim
        viewHolderList.clear()
        viewLevelStatusMap.clear()
        cellBottomArray.fill(CalendarView.dateArea)
        rowHeightArray.fill(CalendarView.dateArea)
        draw()
    }

    private fun setCalendarData() {
        rows = calendarView.rows
        maxCellNum = rows * columns
        calStartTime = calendarView.calendarStartTime
    }

    private fun computePosition() {
        items.forEach{
            try{
                val info = TimeObjectViewHolder(it)
                info.startCellNum = ((it.dtStart - calStartTime) / DAY_MILL).toInt()
                info.endCellNum = ((it.dtEnd - calStartTime) / DAY_MILL).toInt()

                var lOpen = false
                var rOpen = false

                if(info.startCellNum < 0) {
                    info.startCellNum = 0
                    lOpen = true
                }
                if(info.endCellNum >= maxCellNum) {
                    info.endCellNum = maxCellNum - 1
                    rOpen = true
                }

                var currentCell = info.startCellNum
                var length = info.endCellNum - info.startCellNum + 1
                var margin = columns - currentCell % columns
                val size = 1 + (info.endCellNum / columns - info.startCellNum / columns)

                info.timeObjectViewList = Array(size) { index ->
                    TimeObjectView(context, it, currentCell, if(length <= margin) length else margin).apply {
                        currentCell += margin
                        length -= margin
                        margin = 7
                        when(index) {
                            0 -> {
                                leftOpen = lOpen
                                rightOpen = size > 1
                            }
                            size - 2 -> {
                                leftOpen = true
                                rightOpen = true
                            }
                            else -> {
                                leftOpen = size > 1
                                rightOpen = rOpen
                            }
                        }
                    }
                }
                viewHolderList.add(info)
            }catch (e: Exception){ e.printStackTrace() }
        }
        viewHolderList.sortWith(CalendarComparator())
    }

    private fun setTimeObjectViews() {
        var currentViewLevel = -1
        viewHolderList.forEach {
            try{
                val timeObject = it.timeObject
                val viewLevel = timeObject.getViewLevelPriority()
                val formula = timeObject.getFormula()
                val status = viewLevelStatusMap[viewLevel]?: ViewLevelStatus().apply { viewLevelStatusMap[viewLevel] = this }

                if(currentViewLevel == -1) { currentViewLevel = viewLevel }

                if(viewLevel != currentViewLevel) {
                    currentViewLevel = viewLevel
                    computeRowHeight()
                }
                it.timeObjectViewList?.forEach {
                    it.mLeft = (calendarView.minWidth * (it.cellNum % columns)).toInt()
                    it.mRight = it.mLeft + (calendarView.minWidth * it.Length).toInt()
                    when(formula) {
                        TimeObject.Formula.FILL -> {
                            it.mTop = computeOrder(it, status) * it.getViewHeight() /*블럭수에 따른 높이*/ + rowHeightArray[it.cellNum / columns] /* + 기본 높이*/
                        }
                        TimeObject.Formula.BOTTOM -> {
                            //l("viewLevel : ${viewLevel}, ${it.cellNum} : ${cellBottomArray[it.cellNum]}")
                            it.mTop = cellBottomArray[it.cellNum]
                        }
                        else -> {}
                    }
                    it.mBottom = it.mTop + it.getViewHeight()
                    it.setLayout()
                    (it.cellNum until it.cellNum + it.Length).forEach{ index ->
                        l("${it.cellNum} : ${index}")
                        cellBottomArray[index] = Math.max(cellBottomArray[index], it.mBottom)
                    }
                }
            }catch (e: Exception){ e.printStackTrace() }
        }
        computeRowHeight()
    }

    private fun computeRowHeight() {
        (0..5).forEach{ index ->
            rowHeightArray[index] = cellBottomArray.sliceArray(index*7..index*7+6).max() ?: 0 + TimeObjectView.levelMargin
        }
    }

    private fun refreshCalendarView() {
        if(withAnimtion) {
            TransitionManager.beginDelayedTransition(calendarView.calendarLy, makeChangeBounceTransition())
            withAnimtion = false
        }
        var calendarHeight = 0
        val minHeight = calendarView.minHeight.toInt()
        val bottomPadding = CalendarView.weekLyBottomPadding
        calendarView.weekLys.forEachIndexed { index, ly ->
            if(ly.childCount > columns) {
                ly.removeViews(columns, ly.childCount - columns)
            }
            if(index < rows) {
                val newHeight = rowHeightArray[index] + bottomPadding
                val finalHeight = Math.max(minHeight, newHeight)
                calendarHeight += finalHeight
                ly.layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, finalHeight)
            }
        }
        calendarView.calendarLy.layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, calendarHeight)
        viewHolderList.forEach {
            try{
                it.timeObjectViewList?.forEach {
                    calendarView.weekLys[it.cellNum / columns].addView(it)
                    if(TimeObjectManager.lastUpdatedItem == it.timeObject) {
                        showInsertAnimation(it)
                    }
                }
            }catch (e: Exception){ e.printStackTrace() }
        }
    }

    private fun showInsertAnimation(view: TimeObjectView) {
        TimeObjectManager.lastUpdatedItem = null
        val animSet = AnimatorSet()
        animSet.playTogether(ObjectAnimator.ofFloat(view, "scaleX", 2f, 1f).setDuration(ANIM_DUR),
                ObjectAnimator.ofFloat(view, "scaleY", 2f, 1f).setDuration(ANIM_DUR),
                ObjectAnimator.ofFloat(view, "alpha", 0f, 1f).setDuration(ANIM_DUR))
        animSet.interpolator = FastOutSlowInInterpolator()
        animSet.start()
    }

    private fun computeOrder(view: TimeObjectView, status: ViewLevelStatus): Int {
        var order = 0
        for (i in view.cellNum until view.cellNum + view.Length) {
            val s = StringBuilder(status.status[i])
            if(i == view.cellNum) {
                order = s.indexOf("0")
                if(order == -1) order = s.length
            }

            if(order >= s.length) {
                s.append(CharArray(order - s.length + 1, { _-> '0'}))
            }
            status.status[i] = s.replaceRange(order, order + 1, "1").toString()
        }
        return order
    }

    fun getViews(cellNum: Int) : List<TimeObjectView> {
        val result = ArrayList<TimeObjectView>()
        viewHolderList.filter { it.startCellNum == cellNum }.forEach {
            it.timeObjectViewList?.let { result.addAll(it) }
        }
        return result
    }

    inner class TimeObjectViewHolder(val timeObject: TimeObject) {
        var startCellNum = 0
        var endCellNum = 0
        var timeObjectViewList : Array<TimeObjectView>? = null
        override fun toString(): String {
            return "TimeObjectViewHolder(timeObject=$timeObject, startCellNum=$startCellNum, endCellNum=$endCellNum, timeObjectViewList=${Arrays.toString(timeObjectViewList)})"
        }
    }

    inner class ViewLevelStatus {
        var status = Array(maxCellNum){ _ -> "0" }
    }
}