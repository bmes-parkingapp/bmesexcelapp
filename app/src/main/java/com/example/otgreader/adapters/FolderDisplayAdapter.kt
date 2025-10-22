package com.example.otgreader.adapters

import android.content.Context
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.example.otgreader.R
import com.example.otgreader.models.FileItem

class FolderDisplayAdapter(context: Context, private val items: List<FileItem>) :
    ArrayAdapter<FileItem>(context, 0, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val fileItem = items[position]

        val fontBold = ResourcesCompat.getFont(context, R.font.ancizar_serif_bold)
        val fontNormal = ResourcesCompat.getFont(context, R.font.ancizar_serif_regular)

        val view =
            convertView ?: LayoutInflater.from(context).inflate(R.layout.item_file, parent, false)
        val textView = view.findViewById<TextView>(R.id.textFileName)
        val imageView = view.findViewById<ImageView>(R.id.imageFileIcon)

        when {
            fileItem.name == "<-- Back" -> {
                val underlined = SpannableString("Go Back").apply {
                    setSpan(UnderlineSpan(), 0, length, 0)
                }
                textView.text = underlined
                imageView.setImageResource(R.drawable.back_arrow_icon)
                textView.typeface = fontNormal
            }

            fileItem.isDirectory -> {
                textView.text = fileItem.name
                imageView.setImageResource(R.drawable.folder_icon)
                textView.typeface = fontBold
            }

            else -> {
                textView.text = fileItem.name
                imageView.setImageResource(R.drawable.file_icon)
                textView.typeface = fontNormal
            }
        }

        if (fileItem.isSectionHeader) {
            imageView.visibility = View.GONE
        } else {
            imageView.visibility = View.VISIBLE
        }
        return view
    }
}
