package com.example.otgreader.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import com.example.otgreader.R

class CustomDropdownAdapter(context: Context, private val items: List<String>) :
    ArrayAdapter<String>(context, R.layout.item_dropdown_custom, items), Filterable {

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): String? = items.getOrNull(position)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_dropdown_custom, parent, false)
        val textView = view.findViewById<TextView>(R.id.textDropdownItem)
        textView.text = getItem(position) ?: ""
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getView(position, convertView, parent)
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()
                results.values = items
                results.count = items.size
                return results
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                notifyDataSetChanged()
            }
        }
    }
}
