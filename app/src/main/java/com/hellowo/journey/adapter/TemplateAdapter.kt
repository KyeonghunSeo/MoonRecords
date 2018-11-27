package com.hellowo.journey.adapter

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import com.hellowo.journey.AppTheme
import com.hellowo.journey.R
import com.hellowo.journey.dpToPx
import com.hellowo.journey.model.Template
import com.hellowo.journey.model.TimeObject
import com.hellowo.journey.ui.activity.MainActivity
import com.hellowo.journey.ui.view.TimeObjectView
import kotlinx.android.synthetic.main.list_item_template.view.*
import java.util.*

class TemplateAdapter(val context: Context, val items: ArrayList<Template>, val adapterInterface: (template: Template) -> Unit)
    : RecyclerView.Adapter<TemplateAdapter.ViewHolder>() {

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(container: View) : RecyclerView.ViewHolder(container) {
        init {
            container.layoutParams.width = dpToPx(100)
            container.layoutParams.height = dpToPx(100)
            itemView.contentLy.setCardBackgroundColor(AppTheme.backgroundColor)
            itemView.titleText.setTextColor(AppTheme.primaryText)
            itemView.tagText.setTextColor(AppTheme.primaryText)
            itemView.pinImg.setColorFilter(AppTheme.primaryText)
            itemView.typeImg.setColorFilter(AppTheme.primaryText)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, position: Int)
            = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item_template, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val v = holder.itemView
        val template = items[position]
        v.titleText.text = template.title
        v.typeImg.setImageResource(TimeObject.Type.values()[template.type].iconId)
        v.colorImg.setBackgroundColor(AppTheme.getColor(template.colorKey))
        //v.typeImg.setColorFilter(AppTheme.getColor(template.colorKey))

        if(template.tags.isNotEmpty()) {
            v.tagText.visibility = View.VISIBLE
            v.tagText.text = template.tags.joinToString("") { "#${it.id}" }
        }else {
            v.tagText.visibility = View.GONE
            v.tagText.text = context.getString(R.string.no_tag)
        }

        if(template.inCalendar) {
            v.pinLy.alpha = 1f
        }else {
            v.pinLy.alpha = 0.1f
        }

        v.setOnClickListener { adapterInterface.invoke(template) }
    }
}