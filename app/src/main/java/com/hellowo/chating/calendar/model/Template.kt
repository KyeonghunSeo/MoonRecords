package com.hellowo.chating.calendar.model

import android.graphics.Color
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.Required
import java.util.*

open class Template(@PrimaryKey var id: Int = -1,
                    var title: String? = null,
                    var type: Int = 0,
                    var color: Int = Color.BLACK,
                    var style: Int = 0,
                    var fomular: Int = 0,
                    var order: Int = 0): RealmObject()