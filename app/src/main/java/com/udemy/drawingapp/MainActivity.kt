package com.udemy.drawingapp

import android.app.AlertDialog
import android.app.Dialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.get
import com.github.dhaval2404.colorpicker.ColorPickerDialog
import com.github.dhaval2404.colorpicker.model.ColorShape
import com.udemy.drawingapp.databinding.ActivityMainBinding
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: ActivityMainBinding
    private var brushDialog: Dialog? = null
    private var currentSelectedColor: ImageButton? = null
    private val requestPermission: ActivityResultLauncher<Array<String>> = activityResultLauncher()
    private val openGalleryLauncher: ActivityResultLauncher<Intent> = openGalleryResultLauncher()
    private var customDialog: Dialog? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initDialogBrush()
        initUI()
        binding.drwnView.setSizeFromBrush(5f)
    }

    private fun initDialogBrush() {
        brushDialog = Dialog(this)
        brushDialog?.setContentView(R.layout.dialog_brush_size)
    }

    private fun initUI() {
        currentSelectedColor = binding.lnrOptionsColors[0] as ImageButton
        currentSelectedColor!!.setImageDrawable(
            ContextCompat.getDrawable(
                this,
                R.drawable.pallet_selected
            )
        )

        val btnSmall = brushDialog?.findViewById<ImageButton>(R.id.bsSmall)
        val btnMedium = brushDialog?.findViewById<ImageButton>(R.id.bsMedium)
        val btnLarge = brushDialog?.findViewById<ImageButton>(R.id.bsLarge)

        btnSmall?.setOnClickListener(this)
        btnMedium?.setOnClickListener(this)
        btnLarge?.setOnClickListener(this)
        binding.bsSelect.setOnClickListener(this)
        binding.clrBlack.setOnClickListener(this)
        binding.clrSkin.setOnClickListener(this)
        binding.clrRed.setOnClickListener(this)
        binding.clrGreen.setOnClickListener(this)
        binding.clrBlue.setOnClickListener(this)
        binding.clrYellow.setOnClickListener(this)
        binding.clrLollipop.setOnClickListener(this)
        binding.clrRandom.setOnClickListener(this)
        binding.clrWhite.setOnClickListener(this)
        binding.ibUndo.setOnClickListener(this)
        binding.ibGallery.setOnClickListener(this)
        binding.ibRemoveImage.setOnClickListener(this)
        binding.ibSaveImage.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.bsSmall -> {
                binding.drwnView.setSizeFromBrush(5f)
                brushDialog?.dismiss()
            }
            R.id.bsMedium -> {
                binding.drwnView.setSizeFromBrush(10f)
                brushDialog?.dismiss()
            }
            R.id.bsLarge -> {
                binding.drwnView.setSizeFromBrush(20f)
                brushDialog?.dismiss()
            }
            binding.bsSelect.id -> brushDialog?.show()
            binding.clrBlack.id -> setColorByTag(v)
            binding.clrSkin.id -> setColorByTag(v)
            binding.clrRed.id -> setColorByTag(v)
            binding.clrGreen.id -> setColorByTag(v)
            binding.clrBlue.id -> setColorByTag(v)
            binding.clrYellow.id -> setColorByTag(v)
            binding.clrLollipop.id -> setColorByTag(v)
            binding.clrRandom.id -> showColorPicker(v)
            binding.clrWhite.id -> setColorByTag(v)
            binding.ibUndo.id -> binding.drwnView.undoLastPath()
            binding.ibGallery.id -> requestStoragePermission()
            binding.ibRemoveImage.id -> binding.ivBackground.setImageDrawable(null)
            binding.ibSaveImage.id -> startProcessForSaving()

        }
    }

    private fun startProcessForSaving() {
        if (isReadStorageAllowed()) {
            showCustomDialog()
            lifecycleScope.launch {
                saveImage(convertViewToBitmap(binding.frameDrawingContainer))
            }
        }
    }

    private fun setColorByTag(v: View) {
        val ib = v as ImageButton
        val tag = ib.tag.toString()
        binding.drwnView.setColorByString(tag)

        currentSelectedColor!!.setImageDrawable(
            ContextCompat.getDrawable(
                this,
                R.drawable.pallet_normal
            )
        )
        ib.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pallet_selected))


        currentSelectedColor = ib
    }

    private fun showColorPicker(v: View) {
        val ib = v as ImageButton
        ColorPickerDialog
            .Builder(this)       // Pass Activity Instance
            .setTitle("Pick Theme")    // Default "Choose Color"
            .setColorShape(ColorShape.SQAURE)  // Default ColorShape.CIRCLE
            .setDefaultColor(R.color.random)  // Pass Default Color
            .setColorListener { color, _ ->
                binding.drwnView.setColorByInt(color)
                currentSelectedColor!!.setImageDrawable(
                    ContextCompat.getDrawable(
                        this,
                        R.drawable.pallet_normal
                    )
                )
                ib.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pallet_selected))
                currentSelectedColor = ib
            }
            .show()
    }

    private fun activityResultLauncher() =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach { p ->
                val permissionName = p.key
                val granted = p.value
                if (granted) {
                    //access to gallery
                    if (permissionName == Manifest.permission.READ_EXTERNAL_STORAGE) {
                        val picker =
                            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        openGalleryLauncher.launch(picker)
                    }
                } else {
                    if (permissionName == Manifest.permission.READ_EXTERNAL_STORAGE) {
                        //rechazo los permisos anteriormente
                        Toast.makeText(this, "You denied the permission", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }


    private fun showRationaleDialog(title: String, message: String) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
        builder.create().show()

    }

    private fun isReadStorageAllowed(): Boolean {
        val result =
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        ) {
            showRationaleDialog(
                "Drawing App",
                "Drawing App needs to access to your external storage"
            )
        } else {
            requestPermission.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }

    private fun openGalleryResultLauncher() =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                binding.ivBackground.setImageURI(result.data?.data)
            }
        }

    private fun convertViewToBitmap(view: View): Bitmap {
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if (bgDrawable != null) {
            bgDrawable.draw(canvas)
        } else {
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)

        return returnedBitmap
    }

    private suspend fun saveImage(bitmap: Bitmap?): String {
        var result = ""
        withContext(Dispatchers.IO) {
            if (bitmap != null) {
                try {
                    val bytes = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)
                    val file = File(
                        externalCacheDir?.absoluteFile.toString()
                                + "${File.separator}DrawingApp${System.currentTimeMillis() / 1000}.png"
                    )

                    val fileOutput = FileOutputStream(file)
                    fileOutput.write(bytes.toByteArray())
                    fileOutput.close()

                    result = file.absolutePath
                    runOnUiThread {
                        cancelCustomDialog()
                        if (result.isNotEmpty()) {
                            Toast.makeText(
                                this@MainActivity,
                                "File saved successfully :$result",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                "An error has ocurred when trying to save the file",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        shareImage(result)
                    }
                } catch (e: IOException) {
                    result = "Error: ${e.printStackTrace()}"
                }
            }

        }
        return result
    }

    private fun showCustomDialog() {
        customDialog = Dialog(this)
        customDialog?.setContentView(R.layout.custom_progress_dialog)
        customDialog?.show()
    }

    private fun cancelCustomDialog() {
        if (customDialog != null) {
            customDialog?.dismiss()
            customDialog = null
        }
    }

    private fun shareImage(result: String) {
        MediaScannerConnection.scanFile(this, arrayOf(result), null) { _, uri ->
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            shareIntent.type = "image/png"
            startActivity(Intent.createChooser(shareIntent, "Share"))
        }
    }
}
