package com.example.otgreader.fragments

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AutoCompleteTextView
import android.widget.ListView
import androidx.fragment.app.Fragment
import com.example.otgreader.R
import com.example.otgreader.adapters.CustomDropdownAdapter
import com.example.otgreader.adapters.FilterResultDisplay
import com.example.otgreader.models.ColumnValue
import com.example.otgreader.models.RowData
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.BufferedReader
import java.io.InputStreamReader

class FileContentFilterViewFragment : Fragment(), FilterResultDisplay.OnRowClickListener {

    companion object {
        private const val ARG_CONTENT = "file_content"
        private const val ARG_FILE_TYPE = "file_type"
        private const val ARG_FILE_URI = "file_uri"
        private const val ALL = "All"

        fun newInstance(
            content: String,
            fileType: String,
            fileUri: String?
        ): FileContentFilterViewFragment {
            return FileContentFilterViewFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CONTENT, content)
                    putString(ARG_FILE_TYPE, fileType)
                    putString(ARG_FILE_URI, fileUri)
                }
            }
        }
    }

    private val rows = mutableListOf<RowData>()
    private val filteredRows = mutableListOf<RowData>()
    private lateinit var cityListWithAll: List<String>
    private lateinit var allNames: List<String>
    private var cityAdapter: CustomDropdownAdapter? = null
    private var nameAdapter: CustomDropdownAdapter? = null
    private val csvDelimiter = ","
    var selectedCity: String? = null
    var selectedName: String? = null
    private var fileUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_file_content_filter_view, container, false)
        val autoCities = view.findViewById<AutoCompleteTextView>(R.id.auto_cities)
        val autoNames = view.findViewById<AutoCompleteTextView>(R.id.auto_names)
        val listView = view.findViewById<ListView>(R.id.list_view_content)

        val fileType = arguments?.getString(ARG_FILE_TYPE) ?: "csv"
        fileUri = arguments?.getString(ARG_FILE_URI)?.let { Uri.parse(it) }

        when (fileType.lowercase()) {
            "csv" -> fileUri?.let { parseCsvFile(it) }
            "excel" -> fileUri?.let { parseExcelFile(it) }
        }

        val cities = rows.map { it.city }.distinct().sorted()
        cityListWithAll = listOf(ALL) + cities
        allNames = rows.map { it.name }.distinct().sorted()

        cityAdapter = CustomDropdownAdapter(requireContext(), cityListWithAll.toList())
        autoCities?.setAdapter(cityAdapter)
        autoCities?.threshold = 0
        autoCities?.dropDownHeight = ViewGroup.LayoutParams.WRAP_CONTENT

        nameAdapter = CustomDropdownAdapter(requireContext(), listOf(ALL) + allNames)
        autoNames.setAdapter(nameAdapter)
        autoNames.threshold = 0
        autoNames.dropDownHeight = ViewGroup.LayoutParams.WRAP_CONTENT

        val adapterItems = mutableListOf<RowData>()
        val rowAdapter = FilterResultDisplay(requireContext(), adapterItems, this)
        listView.adapter = rowAdapter

        fun updateCityAdapter(query: String?) {
            val filteredCities = if (query.isNullOrBlank()) {
                cityListWithAll.toList()
            } else {
                cityListWithAll.filter { it == ALL || it.contains(query, ignoreCase = true) }
            }
            cityAdapter?.clear()
            cityAdapter?.addAll(filteredCities)
            cityAdapter?.notifyDataSetChanged()
            autoCities?.showDropDown()
        }

        fun updateNameAdapter(city: String?, query: String?, showDropdown: Boolean = false) {
            val filteredNames = if (city == null || city == ALL) {
                allNames.filter { query.isNullOrBlank() || it.contains(query, ignoreCase = true) }
            } else {
                rows.filter { it.city == city }
                    .map { it.name }
                    .distinct()
                    .sorted()
                    .filter { query.isNullOrBlank() || it.contains(query, ignoreCase = true) }
            }
            val nameListWithAll = listOf(ALL) + filteredNames
            nameAdapter?.clear()
            nameAdapter?.addAll(nameListWithAll)
            nameAdapter?.notifyDataSetChanged()
            if (showDropdown) {
                autoNames.showDropDown()
            }
        }

        fun updateContentDisplay() {
            filteredRows.clear()
            val distinctRows = rows.filter { row ->
                (selectedCity == null || selectedCity == ALL || row.city == selectedCity) &&
                        (selectedName == null || selectedName == ALL || row.name == selectedName)
            }.distinctBy { it.name }

            filteredRows.addAll(distinctRows)

            adapterItems.clear()
            adapterItems.addAll(filteredRows)
            rowAdapter.notifyDataSetChanged()
        }


        autoCities?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                selectedName = null
                selectedCity = null
                updateCityAdapter(s.toString())
                if (selectedName != null) {
                    updateNameAdapter(null, null)
                }
                if (s.isNullOrBlank()) {
                    autoNames.setText("")
                    updateContentDisplay()
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        autoCities?.setOnItemClickListener { _, _, _, _ ->
            val selectedItem = autoCities.text.toString()
            selectedCity = selectedItem.ifBlank { ALL }
            selectedName = null
            autoNames.setText("")
            updateNameAdapter(selectedCity, null, false)
            updateContentDisplay()
        }

        autoCities?.setOnClickListener {
            val currentText = autoCities.text?.toString() ?: ""
            if (currentText.isEmpty()) {
                autoCities.showDropDown()
            } else {
                updateCityAdapter(currentText)
            }
        }

        autoNames.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateNameAdapter(selectedCity, s.toString())
                if (s.isNullOrBlank()) {
                    selectedName = null
                    updateContentDisplay()
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        autoNames.setOnItemClickListener { _, _, _, _ ->
            val selectedItem = autoNames.text.toString()
            selectedName = selectedItem.ifBlank { ALL }
            updateContentDisplay()
        }

        autoNames.setOnClickListener {
            autoNames.showDropDown()
        }
        return view
    }

    private fun parseCsvFile(uri: Uri) {
        try {
            requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    val lines = reader.readLines()
                    if (lines.isEmpty()) return
                    val firstColumn = lines[0].split(csvDelimiter).map { it.trim() }

                    for (i in firstColumn.indices) {
                        if (firstColumn[i] != "") {
                            displayValues.add(
                                ColumnValue(
                                    cellIndex = i, columnName = firstColumn[i]
                                )
                            )
                        }
                    }
                    val secondColumn = lines[1].split(csvDelimiter).map { it.trim() }
                    for (i in secondColumn.indices) {
                        if (secondColumn[i] != "") {
                            val existingIndex = displayValues.indexOfFirst { it.cellIndex == i }
                            if (existingIndex == -1) {
                                val checkingIndex = i - 1
                                val newIndex =
                                    displayValues.indexOfFirst { it.cellIndex == checkingIndex }
                                val child = ColumnValue(cellIndex = i, columnName = secondColumn[i])
                                displayValues[newIndex].children.add(child)
                            } else {
                                val child = ColumnValue(cellIndex = i, columnName = secondColumn[i])
                                displayValues[existingIndex].children.add(child)
                            }
                        }
                    }

                    val nameIndex = firstColumn.indexOfFirst { it.equals("Customer Name", true) }
                    val cityIndex = firstColumn.indexOfFirst { it.equals("Town", true) }

                    if (nameIndex == -1 || cityIndex == -1) return

                    lines.drop(1).forEach { line ->
                        val cols = line.split(csvDelimiter).map { it.trim() }
                        if (cols.size > maxOf(nameIndex, cityIndex)) {
                            val name = cols[nameIndex]
                            val city = cols[cityIndex]
                            if (name.isNotEmpty() && city.isNotEmpty()) {
                                rows.add(RowData(name, city, cols))
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
        }
    }

    private val displayValues = mutableListOf<ColumnValue>()

    private fun parseExcelFile(uri: Uri) {
        try {
            requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                val workbook = WorkbookFactory.create(inputStream)
                val sheet = workbook.getSheetAt(0)
                val headerRow = sheet.getRow(0) ?: return

                val headers = mutableListOf<String>()
                headerRow.forEachIndexed { _, cell -> headers.add(cell.toString().trim()) }

                for (i in 0 until headerRow.lastCellNum) {
                    val value =
                        headerRow.getCell(i)?.toString()?.trim().takeIf { !it.isNullOrEmpty() }
                            ?: ""

                    if (value.isNotEmpty()) {
                        displayValues.add(ColumnValue(cellIndex = i, columnName = value))
                    }
                }

                val secondRow = sheet.getRow(1)
                if (secondRow != null) {
                    for (i in 0 until secondRow.lastCellNum) {
                        val value =
                            secondRow.getCell(i)?.toString()?.trim().takeIf { !it.isNullOrEmpty() }
                                ?: ""
                        if (value.isNotEmpty()) {
                            val existingIndex = displayValues.indexOfFirst { it.cellIndex == i }
                            if (existingIndex == -1) {
                                val checkingIndex = i - 1
                                val newIndex =
                                    displayValues.indexOfFirst { it.cellIndex == checkingIndex }
                                val child = ColumnValue(cellIndex = i, columnName = value)
                                displayValues[newIndex].children.add(child)
                            } else {
                                val child = ColumnValue(cellIndex = i, columnName = value)
                                displayValues[existingIndex].children.add(child)
                            }
                        }
                    }
                }

                var nameIndex = -1
                var cityIndex = -1

                headers.forEachIndexed { i, header ->
                    when (header.lowercase()) {
                        "customer name" -> nameIndex = i
                        "town" -> cityIndex = i
                    }
                }

                if (nameIndex == -1 || cityIndex == -1) return

                for (i in 1..sheet.lastRowNum) {
                    val row = sheet.getRow(i) ?: continue
                    val allCols = mutableListOf<String>()
                    for (cellIndex in 0 until row.lastCellNum) {
                        allCols.add(row.getCell(cellIndex)?.toString()?.trim() ?: "")
                    }

                    val name = allCols.getOrNull(nameIndex) ?: ""
                    val city = allCols.getOrNull(cityIndex) ?: ""

                    if (name.isNotEmpty() && city.isNotEmpty()) {
                        rows.add(RowData(name, city, allCols))
                    }
                }

                workbook.close()
            }
        } catch (e: Exception) {
        }
    }

    override fun onItemClick(position: Int) {
        hideKeyboard(requireContext())
        val selectedRow = filteredRows[position]

        navigateToDisplayFragment("", "excel", fileUri.toString(), selectedRow.name)
    }

    private fun hideKeyboard(mContext: Context) {
        val imm = mContext
            .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        try {
            (mContext as Activity?)?.window.let {
                imm.hideSoftInputFromWindow(
                    it?.currentFocus?.windowToken, 0
                )
            }
        } catch (ex: Exception) {
        }
    }

    private fun navigateToDisplayFragment(
        content: String,
        fileType: String,
        fileUri: String?,
        selectedCity: String
    ) {
        val fragment = SummaryDisplayFragment.newInstance(content, fileType, fileUri, selectedCity)
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}




