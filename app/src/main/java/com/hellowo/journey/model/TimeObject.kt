package com.hellowo.journey.model

import android.graphics.Color
import com.hellowo.journey.R
import com.hellowo.journey.getCalendarTime0
import com.hellowo.journey.getCalendarTime23
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.util.*

open class TimeObject(@PrimaryKey var id: String? = null,
                      var type: Int = 0,
                      var style: Int = 0,
                      var title: String? = null,
                      var color: Int = Color.BLACK,
                      var fontColor: Int = Color.WHITE,
                      var location: String? = null,
                      var description: String? = null,
                      var repeat: String? = null,
                      var count: Int = 0,
                      var dtUntil: Long = Long.MIN_VALUE,
                      var allday: Boolean = true,
                      var dtStart: Long = Long.MIN_VALUE,
                      var dtEnd: Long = Long.MIN_VALUE,
                      var dtDone: Long = Long.MIN_VALUE,
                      var dtCreated: Long = Long.MIN_VALUE,
                      var dtUpdated: Long = Long.MIN_VALUE,
                      var timeZone: String? = null,
                      var exDates: RealmList<String> = RealmList(),
                      var tags: RealmList<Tag> = RealmList(),
                      var alarms: RealmList<Alarm> = RealmList(),
                      var links: RealmList<Link> = RealmList(),
                      var latitude: Double = Double.MIN_VALUE,
                      var longitude: Double = Double.MIN_VALUE): RealmObject() {

    enum class Type(val titleId: Int, val iconId: Int) {
        NOTE(R.string.note, R.drawable.ic_baseline_description_24px),
        EVENT(R.string.event, R.drawable.ic_baseline_calendar_today_24px),
        TASK(R.string.task, R.drawable.ic_baseline_done_24px),
        STAMP(R.string.stamp, R.drawable.ic_outline_class),
        MONEY(R.string.money, R.drawable.ic_outline_monetization_on),
        DECORATION(R.string.decoration, R.drawable.ic_baseline_favorite_24px)
    }

    enum class Style {
        DEFAULT, SHORT, LONG, INDICATOR
    }

    enum class Formula {
        BACKGROUND, TOPSTACK, LINEAR, BOTTOMSTACK, OVERLAY
    }

    enum class ViewLevel(val priority: Int) {
        IMPORTANT(0), NORMAL(1), PROJECT(2), STAMP(3), JOURNAL(4), ROUGH(5), BACKGROUND(-1)
    }


    fun getViewLevelPriority(): Int = when(Type.values()[type]) {
        Type.EVENT -> {
            when(Style.values()[style]) {
                Style.LONG -> ViewLevel.ROUGH.priority
                else -> ViewLevel.NORMAL.priority
            }
        }
        Type.TASK -> {
            when(Style.values()[style]) {
                Style.LONG -> ViewLevel.PROJECT.priority
                else -> ViewLevel.NORMAL.priority
            }
        }
        Type.MONEY -> {
            when(Style.values()[style]) {
                Style.SHORT -> ViewLevel.JOURNAL.priority
                Style.LONG -> ViewLevel.JOURNAL.priority
                else -> ViewLevel.NORMAL.priority
            }
        }
        Type.STAMP -> ViewLevel.STAMP.priority
        Type.DECORATION -> ViewLevel.JOURNAL.priority
        else -> ViewLevel.NORMAL.priority
    }

    fun getFormula(): Formula = when(Type.values()[type]) {
        Type.NOTE -> Formula.LINEAR
        Type.EVENT -> {
            when(Style.values()[style]) {
                Style.SHORT -> Formula.LINEAR
                else -> Formula.TOPSTACK
            }
        }
        Type.TASK -> {
            when(Style.values()[style]) {
                Style.LONG -> Formula.TOPSTACK
                else -> Formula.LINEAR
            }
        }
        Type.MONEY -> {
            when(Style.values()[style]) {
                Style.SHORT -> Formula.TOPSTACK
                Style.LONG -> Formula.TOPSTACK
                else -> Formula.LINEAR
            }
        }
        Type.STAMP -> Formula.TOPSTACK
        Type.DECORATION -> Formula.TOPSTACK
        else -> Formula.TOPSTACK
    }

    fun setDateTime(a: Boolean, s: Calendar, e: Calendar) {
        if(allday) {
            setDateTime(a, getCalendarTime0(s), getCalendarTime0(e))
        }else {
            setDateTime(a, s.timeInMillis, e.timeInMillis)
        }
    }

    fun setDateTime(a: Boolean, s: Long, e: Long) {
        allday = a
        dtStart = s
        dtEnd = e
        alarms.forEach {
            it.dtAlarm = dtStart + it.offset
        }
    }

    fun copy(data: TimeObject) {
        id = data.id
        type = data.type
        style = data.style
        title = data.title
        color = data.color
        fontColor = data.fontColor
        location = data.location
        description = data.description
        repeat = data.repeat
        count = data.count
        dtUntil = data.dtUntil
        allday = data.allday
        dtStart = data.dtStart
        dtEnd = data.dtEnd
        dtDone = data.dtDone
        dtCreated = data.dtCreated
        dtUpdated = data.dtUpdated
        timeZone = data.timeZone

        exDates.clear()
        exDates.addAll(data.exDates)

        tags.clear()
        data.tags.forEach {

        }

        alarms.clear()
        data.alarms.forEach {
            alarms.add(Alarm(it.id, it.dtAlarm, it.offset, it.action))
        }

        links.clear()
        data.links.forEach {

        }

        latitude = data.latitude
        longitude = data.longitude
    }

    fun clearRepeat() {
        repeat = null
        dtUntil = Long.MIN_VALUE
        exDates.clear()
    }

    override fun toString(): String {
        return "TimeObject(id=$id, type=$type, style=$style, title=$title, color=$color, fontColor=$fontColor, location=$location, description=$description, repeat=$repeat, count=$count, dtUntil=$dtUntil, allday=$allday, dtStart=$dtStart, dtEnd=$dtEnd, dtDone=$dtDone, dtCreated=$dtCreated, dtUpdated=$dtUpdated, timeZone=$timeZone, exDates=$exDates, tags=$tags, alarms=$alarms, links=$links, latitude=$latitude, longitude=$longitude)"
    }

    fun isDone(): Boolean = dtDone != Long.MIN_VALUE


}