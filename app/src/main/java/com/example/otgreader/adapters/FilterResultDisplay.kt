package com.example.otgreader.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import com.example.otgreader.R
import com.example.otgreader.models.RowData

class FilterResultDisplay(
    context: Context,
    private val items: List<RowData>,
    private val listener: OnRowClickListener
) : ArrayAdapter<RowData>(context, 0, items) {

    interface OnRowClickListener {
        fun onItemClick(position: Int)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val row = items[position]
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_filter_result_display, parent, false)

        view.findViewById<TextView>(R.id.tv_name).text = row.name
        view.findViewById<TextView>(R.id.tv_city).text = row.city
        view.findViewById<LinearLayout>(R.id.parentView).setOnClickListener {
            listener.onItemClick(position)
        }
        return view
    }
}
