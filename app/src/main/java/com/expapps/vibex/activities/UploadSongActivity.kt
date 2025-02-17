package com.expapps.vibex.activities

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.expapps.vibex.FirebaseSource
import com.expapps.vibex.Utils
import com.expapps.vibex.Utils.showToast
import com.expapps.vibex.databinding.ActivityUploadSongBinding
import com.expapps.vibex.models.SongsModel
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import java.io.File
import kotlin.math.round


class UploadSongActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUploadSongBinding
    private var selectedSongUri: Uri? = null
    private lateinit var progressDialog: ProgressDialog
    private lateinit var firebaseSource: FirebaseSource

    companion object {
        const val SONG_SELECTION_CODE = 100
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUploadSongBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initProgress()
        firebaseSource = FirebaseSource()

        binding.backIv.setOnClickListener {
            finish()
        }
        binding.uploadSongBtn.setOnClickListener {
            chooseSong()
        }
        binding.uploadedSongBtn.setOnClickListener {
            hideRemoveSongButton()
            selectedSongUri = null
        }
        binding.submitBtn.setOnClickListener {
            selectedSongUri?.let { it1 -> initiateUpload(it1) }
        }
    }

    private fun initiateUpload(uri: Uri) {
        val songTitle = binding.songTitleEt.text.toString().trim()
        val songArtists = binding.artistsNameEt.text.toString().trim()
        val songGenre = binding.genreTagsEt.text.toString().trim()

        if (!Utils.checkEmptyOrNullString(songTitle, songArtists, songGenre)) {
            showToast("All fields are mandatory")
            return
        }
        if (selectedSongUri == null) {
            showToast("Please select a song")
            return
        }
        val songModel = SongsModel(
            songTitle = songTitle,
            songArtists = songArtists,
            songGenre = songGenre
        )
        uploadSong(uri, songModel)
    }

    private fun initProgress() {
        progressDialog = ProgressDialog(this)
        progressDialog.setCancelable(false)
        progressDialog.setCanceledOnTouchOutside(false)
        progressDialog.setTitle("VibeX")
        progressDialog.setMessage("Uploading song... 0%")
    }

    private fun chooseSong() {
        val intentUpload = Intent()
        intentUpload.type = "audio/*"
        intentUpload.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(intentUpload, SONG_SELECTION_CODE)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SONG_SELECTION_CODE) {
            if (resultCode == RESULT_OK) {
                val uri = data?.data
                if (uri != null) {
                    selectedSongUri = uri
                    binding.uploadedTv.text = getFileName(uri)
                    showRemoveSongButton()
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun uploadSong(uri: Uri, songsModel: SongsModel) {
        progressDialog.show()
        val storage = FirebaseStorage.getInstance()
        val storageRef = storage.reference

        val imageRef = storageRef.child("Songs/${getFileName(uri)}")

        val uploadTask = imageRef.putFile(uri)
        uploadTask.addOnProgressListener { taskSnapshot: UploadTask.TaskSnapshot ->

            val progress = 100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount
            progressDialog.setMessage("Uploading song... ${round(progress)}%")

        }.addOnSuccessListener { taskSnapshot: UploadTask.TaskSnapshot? ->
            imageRef.downloadUrl
                .addOnSuccessListener { uri: Uri ->
                    val songUrl = uri.toString()
                    songsModel.songUrl = songUrl
                    firebaseSource.uploadSong(songsModel).observe(this) {
                        progressDialog.dismiss()
                        finish()
                    }
                }
        }.addOnFailureListener { exception: Exception? ->
            progressDialog.dismiss()
        }
    }

    @SuppressLint("Range")
    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(if (cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME) >= 0) cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME) else 0)
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result!!.lastIndexOf('/')
            if (cut != -1) {
                result = result.substring(cut + 1)
            }
        }
        return result
    }
    private fun showRemoveSongButton() {
        binding.uploadedSongBtn.visibility = View.VISIBLE
        binding.uploadSongBtn.visibility = View.GONE
    }

    private fun hideRemoveSongButton() {
        binding.uploadedSongBtn.visibility = View.GONE
        binding.uploadSongBtn.visibility = View.VISIBLE
    }
}