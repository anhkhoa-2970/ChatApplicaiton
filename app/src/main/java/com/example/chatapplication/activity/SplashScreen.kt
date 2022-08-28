package com.example.chatapplication.activity

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.chatapplication.R
import com.example.chatapplication.databinding.ActivitySplashScreenBinding
import java.io.File
import java.io.IOException

class SplashScreen : AppCompatActivity() {
    private lateinit var binding: ActivitySplashScreenBinding
    private lateinit var mediaRecorder: MediaRecorder
    private lateinit var mediaPlayer: MediaPlayer
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mediaRecorder = MediaRecorder()
        mediaPlayer = MediaPlayer()
        binding.btnStop.isEnabled = false
        var path = Environment.getExternalStorageDirectory().toString()+ System.currentTimeMillis() + "/myrec111.3gp"
        requestPermission()


//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1) {
//            mPath= getActivity().getExternalFilesDir(Environment.DIRECTORY_DCIM) + "/" + now + ".jpeg";
//        }
//        else
//        {
//            mPath= Environment.getExternalStorageDirectory().toString() + "/" + now + ".jpeg";
//        }


        binding.btnStart.setOnClickListener {


            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            mediaRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB)
            mediaRecorder.setOutputFile(path)
            try {
                mediaRecorder.prepare()
                mediaRecorder.start()

            } catch (e: IOException) {
                e.printStackTrace()
            }

            binding.btnStop.isEnabled = true
            binding.btnStart.isEnabled = false
        }
        binding.btnStop.setOnClickListener {
            try {
                mediaRecorder.stop()
                mediaRecorder.release()
            } catch (stopException : RuntimeException ) {
                stopException.printStackTrace()
            }
            binding.btnStart.isEnabled = true
        }

        binding.btnPlay.setOnClickListener {
            val file = File(path)
            if (file.exists()){
                Toast.makeText(this,"File exitst", Toast.LENGTH_SHORT).show()
            }else{
                Toast.makeText(this,"File not exist", Toast.LENGTH_SHORT).show()
            }

            try {
                mediaPlayer.setDataSource(path)
                mediaPlayer.prepare()
                mediaPlayer.start()
            }catch ( e: IOException){
                e.printStackTrace()
            }
        }
    }


    private fun hasRecordAudio() =
        ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

    private fun hasWriteExternalStorage() =
        ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

    private fun requestPermission() {
        val permissionToRequest = mutableListOf<String>()

        if (!hasRecordAudio()) {
            permissionToRequest.add(Manifest.permission.RECORD_AUDIO)
        }
        if (!hasWriteExternalStorage()) {
            permissionToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (permissionToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionToRequest.toTypedArray(), 0)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 0 && grantResults.isNotEmpty()) {
            for (i in grantResults.indices) {
                grantResults[i] == PackageManager.PERMISSION_GRANTED
            }
        }
        binding.btnStart.isEnabled = true
    }
}