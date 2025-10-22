package com.example.otgreader.activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import com.example.otgreader.fragments.FileContentFilterViewFragment
import com.example.otgreader.R
import com.example.otgreader.adapters.FolderDisplayAdapter
import com.example.otgreader.models.FileItem
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {

    private lateinit var usbManager: UsbManager
    private lateinit var tvUsbStatus: TextView
    private lateinit var openUsb: Button
    private lateinit var lvFolders: ListView
    private val folderItems = mutableListOf<FileItem>()
    private lateinit var folderAdapter: FolderDisplayAdapter
    private var currentFolder: DocumentFile? = null
    private val folderBackStack = ArrayDeque<DocumentFile>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvUsbStatus = findViewById(R.id.tv_usb_status)
        lvFolders = findViewById(R.id.lv_folders)
        openUsb = findViewById(R.id.btn_open_usb)
        folderAdapter = FolderDisplayAdapter(this, folderItems)
        lvFolders.adapter = folderAdapter
        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager

        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        registerReceiver(usbReceiver, filter)

        checkConnectedDevices()
        listFolders(null)

        openUsb.setOnClickListener {
            if (isUsbDeviceConnected()) {
                folderBackStack.clear()
                currentFolder = null
                folderItems.clear()
                folderAdapter.notifyDataSetChanged()
                listFolders(null)
                accessUsbStorage()
            }
        }

        lvFolders.setOnItemClickListener { _, _, position, _ ->
            val selectedItem = folderItems[position]

            if (selectedItem.isSectionHeader) return@setOnItemClickListener

            if (selectedItem.name == "<-- Back") {
                if (folderBackStack.isNotEmpty()) {
                    val previousFolder = folderBackStack.removeLast()
                    listFolders(previousFolder, pushToStack = false)
                }
            } else if (selectedItem.uri != null) {
                if (selectedItem.isLocalFile) {
                    // Handle local file
                    val file = File(selectedItem.uri)
                    if (file.exists()) {
                        readLocalFileContent(file)
                    } else {
                        Toast.makeText(this, "Recent file not found", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val file = DocumentFile.fromSingleUri(this, Uri.parse(selectedItem.uri))
                    if (file != null) {
                        readFileContent(file)
                    } else {
                        Toast.makeText(this, "Cannot open recent file", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                currentFolder?.listFiles()?.firstOrNull { it.name == selectedItem.name }
                    ?.let { file ->
                        if (file.isDirectory) {
                            listFolders(file)
                        } else {
                            readFileContent(file)
                        }
                    }
            }
        }
    }

    private fun readLocalFileContent(file: File) {
        try {
            val fileName = file.name.lowercase()
            when {
                fileName.endsWith(".csv") -> readLocalCsvFile(file)
                fileName.endsWith(".xls") || fileName.endsWith(".xlsx") -> readLocalExcelFile(file)
                else -> Toast.makeText(this, "Unsupported file format", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error reading file: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun readLocalCsvFile(file: File) {
        try {
            val csvContent = StringBuilder()
            file.bufferedReader().use { reader ->
                reader.lineSequence().forEach { line ->
                    csvContent.append(line).append("\n")
                }
            }
            navigateToFileContentFragment(
                csvContent.toString(),
                "csv",
                Uri.fromFile(file).toString()
            )
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to open CSV file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun readLocalExcelFile(file: File) {
        try {
            FileInputStream(file).use { inputStream ->
                val workbook = WorkbookFactory.create(inputStream)
                val sheet = workbook.getSheetAt(0)
                val excelContent = StringBuilder()

                sheet.forEach { row ->
                    row.forEach { cell ->
                        excelContent.append(cell.toString()).append("\t")
                    }
                    excelContent.append("\n")
                }
                workbook.close()
                navigateToFileContentFragment(
                    excelContent.toString(),
                    "excel",
                    Uri.fromFile(file).toString()
                )
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to open Excel file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkConnectedDevices() {
        val deviceList = usbManager.deviceList
        if (deviceList.isNotEmpty()) {
            tvUsbStatus.text = "USB Status: Device Attached"
            openUsb.visibility = View.VISIBLE
        } else {
            tvUsbStatus.text = "USB Status: No Device Connected"
            openUsb.visibility = View.GONE
        }
    }

    private fun isUsbDeviceConnected(): Boolean {
        return usbManager.deviceList.isNotEmpty()
    }

    private fun accessUsbStorage() {
        openDocumentTreeLauncher.launch(null)
    }

    private val openDocumentTreeLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            uri?.let {
                contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                val documentFile = DocumentFile.fromTreeUri(this, it)
                folderBackStack.clear()
                listFolders(documentFile)
            }
        }

    private fun listFolders(folder: DocumentFile?, pushToStack: Boolean = true) {
        folder?.let {
            if (pushToStack && currentFolder != null) {
                folderBackStack.addLast(currentFolder!!)
            }
        }

        folderItems.clear()
        currentFolder = folder

        val recent = getRecentsFromInternal()
        folderItems.add(FileItem("Recent Files", isDirectory = true, isSectionHeader = true))

        recent.forEach { file ->
            folderItems.add(
                FileItem(
                    file.name,
                    isDirectory = false,
                    uri = file.absolutePath,
                    isLocalFile = true
                )
            )
        }

        folderItems.add(FileItem("Folder Contents", isDirectory = true, isSectionHeader = true))

        if (folderBackStack.isNotEmpty()) {
            folderItems.add(FileItem("<-- Back", true))
        }

        folder?.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                folderItems.add(FileItem(file.name ?: "Unnamed", true))
            } else {
                val name = file.name?.lowercase() ?: ""
                if (name.endsWith(".csv") || name.endsWith(".xls") || name.endsWith(".xlsx")) {
                    folderItems.add(
                        FileItem(
                            file.name ?: "Unnamed File",
                            false,
                            uri = file.uri.toString()
                        )
                    )
                }
            }
        }
        folderAdapter.notifyDataSetChanged()
    }

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    folderBackStack.clear()
                    currentFolder = null
                    folderItems.clear()
                    folderAdapter.notifyDataSetChanged()
                    listFolders(null)
                }

                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    folderBackStack.clear()
                    currentFolder = null
                    folderItems.clear()
                    folderAdapter.notifyDataSetChanged()
                    listFolders(null)
                    tvUsbStatus.text = "USB Status: No Device Connected"
                }
            }
        }
    }

    private fun readFileContent(file: DocumentFile) {
        try {
            val fileName = file.name?.lowercase() ?: ""
            when {
                fileName.endsWith(".csv") -> readCsvFile(file)
                fileName.endsWith(".xls") || fileName.endsWith(".xlsx") -> readExcelFile(file)
                else -> Toast.makeText(this, "Unsupported file format", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error reading file: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun readCsvFile(file: DocumentFile) {
        copyFileToLocal(file)
        contentResolver.openInputStream(file.uri)?.use { inputStream ->
            val reader = BufferedReader(InputStreamReader(inputStream))
            val csvContent = StringBuilder()
            reader.lineSequence().forEach { line ->
                csvContent.append(line).append("\n")
            }
            navigateToFileContentFragment(csvContent.toString(), "csv", file.uri.toString())
        } ?: Toast.makeText(this, "Failed to open CSV file", Toast.LENGTH_SHORT).show()
    }

    private fun readExcelFile(file: DocumentFile) {
        copyFileToLocal(file)
        contentResolver.openInputStream(file.uri)?.use { inputStream ->
            val workbook = WorkbookFactory.create(inputStream)
            val sheet = workbook.getSheetAt(0) // Read first sheet
            val excelContent = StringBuilder()

            sheet.forEach { row ->
                row.forEach { cell ->
                    excelContent.append(cell.toString()).append("\t")
                }
                excelContent.append("\n")
            }
            workbook.close()
            navigateToFileContentFragment(excelContent.toString(), "excel", file.uri.toString())
        } ?: Toast.makeText(this, "Failed to open Excel file", Toast.LENGTH_SHORT).show()
    }

    private fun navigateToFileContentFragment(content: String, fileType: String, fileUri: String?) {
        val fragment = FileContentFilterViewFragment.newInstance(content, fileType, fileUri)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun copyFileToLocal(file: DocumentFile): File? {
        return try {
            // Public Documents folder
            val documentsDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)

            // Create bmesfiles folder inside Documents
            val folder = File(documentsDir, "bmesfiles")
            if (!folder.exists() && !folder.mkdirs()) {
                return null
            }

            // Destination file
            val destFile = File(folder, file.name ?: "unnamed_${System.currentTimeMillis()}")

            // Copy contents
            contentResolver.openInputStream(file.uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            destFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getRecentsFromInternal(): List<File> {
        val folder = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            "bmesfiles"
        )
        if (!folder.exists()) return emptyList()

        return folder.listFiles()
            ?.filter { it.isFile }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(usbReceiver)
    }
}


