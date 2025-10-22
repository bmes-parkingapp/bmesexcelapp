package com.example.otgreader.fragments

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.otgreader.R
import com.example.otgreader.adapters.DetailDisplayCardAdapter
import com.example.otgreader.models.DisplayValue
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.WorkbookFactory

class DetailDisplayFragment : Fragment() {

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
        ): DetailDisplayFragment {
            return DetailDisplayFragment().apply {
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
    private var selectedName: String = ""
    private var displayItems: MutableList<DisplayValue> = mutableListOf()
    private var recyclerView: RecyclerView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_detail_display, container, false)

        recyclerView = view?.findViewById(R.id.recycler_cards)

        val fileType = arguments?.getString(ARG_FILE_TYPE) ?: "csv"
        fileUri = arguments?.getString(ARG_FILE_URI)?.let { Uri.parse(it) }
        selectedName = arguments?.getString(ARG_SELECTED_NAME) ?: ""

        when (fileType.lowercase()) {
            "csv" -> fileUri?.let { parseCsvFile(it) }
            "excel" -> fileUri?.let { parseExcelFile(it) }
        }

        return view
    }

    private fun parseCsvFile(uri: Uri) {}

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
            setupRecyclerView()
        }
    }

    private fun setupRecyclerView() {
        recyclerView?.layoutManager = LinearLayoutManager(requireContext())
        val adapter = DetailDisplayCardAdapter(displayItems)
        recyclerView?.adapter = adapter
    }
}