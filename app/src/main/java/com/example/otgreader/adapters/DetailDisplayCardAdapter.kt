package com.example.otgreader.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.otgreader.R
import com.example.otgreader.models.DisplayValue

class DetailDisplayCardAdapter(private val items: MutableList<DisplayValue>) :
    RecyclerView.Adapter<DetailDisplayCardAdapter.CardViewHolder>() {

    private val rowCount: Int = items.firstOrNull()?.rowValues?.size ?: 0

    inner class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val refNo = itemView.findViewById<TextView>(R.id.refNoId)
        val dateId = itemView.findViewById<TextView>(R.id.dateId)
        val tdayRtId = itemView.findViewById<TextView>(R.id.tdayRtId)
        val opDtId = itemView.findViewById<TextView>(R.id.opDtId)
        val opWtId = itemView.findViewById<TextView>(R.id.opWtId)
        val opValueId = itemView.findViewById<TextView>(R.id.opValueId)
        val tIssueId = itemView.findViewById<TextView>(R.id.tIssueId)
        val tReturnId = itemView.findViewById<TextView>(R.id.tReturnId)
        val rWtId = itemView.findViewById<TextView>(R.id.rWtId)
        val addId = itemView.findViewById<TextView>(R.id.addId)
        val lessId = itemView.findViewById<TextView>(R.id.lessId)
        val xrAmtId = itemView.findViewById<TextView>(R.id.xrAmtId)
        val mcId = itemView.findViewById<TextView>(R.id.mcId)
        val bbcId = itemView.findViewById<TextView>(R.id.bbcId)
        val waraId = itemView.findViewById<TextView>(R.id.waraId)
        val sterlingId = itemView.findViewById<TextView>(R.id.sterlingId)
        val krId = itemView.findViewById<TextView>(R.id.krId)
        val cbId = itemView.findViewById<TextView>(R.id.cbId)
        val cbAmt = itemView.findViewById<TextView>(R.id.cbAmt)
        val remarkId = itemView.findViewById<TextView>(R.id.remarkId)
        val tAmtId = itemView.findViewById<TextView>(R.id.tAmtId)
        val aReceivedId = itemView.findViewById<TextView>(R.id.aReceivedId)
        val aPaidId = itemView.findViewById<TextView>(R.id.aPaidId)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_display_card, parent, false)
        return CardViewHolder(view)
    }

    override fun getItemCount(): Int = rowCount

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
//        holder.refNo.text = items[0].rowValues[position]
        holder.dateId.text = items[1].rowValues[position]
        holder.tdayRtId.text = items[33].rowValues[position]

        holder.opDtId.text = items[26].rowValues[position]
        holder.opWtId.text = items[4].rowValues[position]
        holder.opValueId.text = items[5].rowValues[position]
        holder.tIssueId.text = items[6].rowValues[position]
        holder.tReturnId.text = items[8].rowValues[position]

        val val1 = items[12].rowValues[position].toIntOrNull()
        val val2 = items[13].rowValues[position].toIntOrNull()

        holder.rWtId.text = when {
            val1 != null && val2 != null -> (val1 * val2).toString()
            val1 != null -> val1.toString()
            val2 != null -> val2.toString()
            else -> ""
        }

        holder.addId.text = items[26].rowValues[position]
        holder.lessId.text = items[15].rowValues[position]
        holder.xrAmtId.text = ""
        holder.mcId.text = items[7].rowValues[position]
        holder.bbcId.text = items[18].rowValues[position]
        holder.waraId.text = items[9].rowValues[position]
        holder.sterlingId.text = items[16].rowValues[position]
        holder.krId.text = items[20].rowValues[position]
        holder.cbId.text = items[24].rowValues[position]
        holder.remarkId.text = items[34].rowValues[position]
//        holder.tAmtId.text = items[0].rowValues[position]
//        holder.aReceivedId.text = items[0].rowValues[position]
//        holder.aPaidId.text = items[0].rowValues[position]
    }
}
