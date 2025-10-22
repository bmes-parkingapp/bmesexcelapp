package com.example.otgreader.fragments

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.example.otgreader.R
import com.example.otgreader.models.DisplayValue
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SummaryDisplayFragment : Fragment() {

    companion object {
        private const val ARG_CONTENT = "file_content"
        private const val ARG_FILE_TYPE = "file_type"
        private const val ARG_FILE_URI = "file_uri"
        private const val ARG_SELECTED_NAME = "selected_name"

        fun newInstance(
            content: String,
            fileType: String,
            fileUri: String?,
            name: String?
        ): SummaryDisplayFragment {
            return SummaryDisplayFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CONTENT, content)
                    putString(ARG_FILE_TYPE, fileType)
                    putString(ARG_FILE_URI, fileUri)
                    putString(ARG_SELECTED_NAME, name)
                }
            }
        }
    }

    private var fileUri: Uri? = null
    private var displayItems: MutableList<DisplayValue> = mutableListOf()
    private var selectedName: String = ""

    private var customerText: TextView? = null
    private var areaTopText: TextView? = null
    private var areaBelowText: TextView? = null
    private var closingWeightText: TextView? = null
    private var closingValueText: TextView? = null
    private var lastVisitedDateText: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_summary_display, container, false)

        customerText = view?.findViewById(R.id.customer)
        areaTopText = view?.findViewById(R.id.areaTop)
        areaBelowText = view?.findViewById(R.id.areaBelow)
        closingWeightText = view?.findViewById(R.id.closingWeight)
        closingValueText = view?.findViewById(R.id.closingValue)
        lastVisitedDateText = view?.findViewById(R.id.lastVisitedDate)

        val detailButton = view?.findViewById<Button>(R.id.view_detail_button)
        detailButton?.setOnClickListener {
            navigateToDisplayFragment()
        }

        val fileType = arguments?.getString(ARG_FILE_TYPE) ?: "csv"
        fileUri = arguments?.getString(ARG_FILE_URI)?.let { Uri.parse(it) }
        selectedName = arguments?.getString(ARG_SELECTED_NAME) ?: ""

        when (fileType.lowercase()) {
            "excel" -> fileUri?.let { parseExcelFile(it) }
        }
        return view
    }

    private fun parseExcelFile(uri: Uri) {
        requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
            val workbook = WorkbookFactory.create(inputStream)
            val sheet = workbook.getSheetAt(0)
            val headerRow = sheet.getRow(0) ?: return

            val headers = mutableListOf<String>()
            headerRow.forEachIndexed { _, cell -> headers.add(cell.toString().trim()) }

            for (i in 0 until headerRow.lastCellNum) {
                displayItems.add(DisplayValue(cellIndex = i))
            }

            for (i in 2..sheet.lastRowNum) {
                val row = sheet.getRow(i) ?: continue
                val matchCell = row.getCell(2)?.toString()?.trim() ?: ""

                if (matchCell.equals(selectedName, ignoreCase = true)) {
                    for (j in 0 until headerRow.lastCellNum) {
                        val cell = row.getCell(j, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK)
                        val value = cell.toString().trim()
                        if (j < displayItems.size) {
                            displayItems[j].rowValues.add(value)
                        }
                    }
                }

            }
            workbook.close()
        }

        if (displayItems.size > 1) {
            customerText?.text = displayItems[2].rowValues.last()
            areaTopText?.text = displayItems[3].rowValues.last()
            areaBelowText?.text = displayItems[3].rowValues.last()
            lastVisitedDateText?.text = getCurrentDateFormatted()
            closingWeightText?.text = displayItems[24].rowValues.last()
            closingValueText?.text = displayItems[25].rowValues.last()
        }
    }

    private fun navigateToDisplayFragment() {
        val fragment = DetailDisplayFragment.newInstance(
            arguments?.getString(ARG_CONTENT) ?: "",
            arguments?.getString(ARG_FILE_TYPE) ?: "",
            arguments?.getString(ARG_FILE_URI),
            arguments?.getString(ARG_SELECTED_NAME)
        )
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun getCurrentDateFormatted(): String {
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        return dateFormat.format(Date())
    }
}