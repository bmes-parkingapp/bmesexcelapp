package com.example.otgreader.models

import java.io.Serializable

data class DisplayValue(
    val cellIndex: Int,
    val rowValues: MutableList<String> = mutableListOf()
) : Serializable

data class FileItem(
    val name: String,
    val isDirectory: Boolean,
    val uri: String? = null,
    val isSectionHeader: Boolean = false,
    val isLocalFile: Boolean = false // flag to indicate local file
)

data class ColumnValue(
    val cellIndex: Int,
    val columnName: String,
    val children: MutableList<ColumnValue> = mutableListOf(),
    var rowValue: String = ""
) : Serializable

data class RowData(
    val name: String,
    val city: String,
    val columns: List<String>
)