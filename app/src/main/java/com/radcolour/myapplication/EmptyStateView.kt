package com.radcolour.myapplication

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class EmptyStateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val emptyIcon: ImageView
    private val emptyTitle: TextView
    private val emptySubtitle: TextView

    init {
        LayoutInflater.from(context).inflate(R.layout.view_empty_state, this, true)
        emptyIcon = findViewById(R.id.emptyIcon)
        emptyTitle = findViewById(R.id.emptyTitle)
        emptySubtitle = findViewById(R.id.emptySubtitle)
    }

    fun setup(
        iconRes: Int,
        title: String,
        subtitle: String,
        accentColour: Int
    ) {
        emptyIcon.setImageResource(iconRes)
        emptyTitle.text = title
        emptyTitle.setTextColor(accentColour)
        emptySubtitle.text = subtitle
        emptySubtitle.setTextColor(0xFF8A8A8A.toInt())
    }
}