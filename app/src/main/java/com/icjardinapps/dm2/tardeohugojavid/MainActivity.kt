package com.icjardinapps.dm2.tardeohugojavid

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import android.Manifest
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private val workTag = "WebCheckerWork"

    private var workId = UUID.randomUUID()

    private var semaforo = "R"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }
        val urlField = findViewById<EditText>(R.id.URL)
        val wordField = findViewById<EditText>(R.id.FINDTEXT)
        val btnPlay = findViewById<Button>(R.id.play)
        val btnStop = findViewById<Button>(R.id.parar)
        btnPlay.isEnabled = false
        fun checkFields() {
            val urlText = urlField.text.toString().trim()
            val wordText = wordField.text.toString().trim()
            btnPlay.isEnabled = urlText.isNotEmpty() && wordText.isNotEmpty()
        }
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                checkFields()
            }

            override fun afterTextChanged(s: Editable?) {}
        }
        urlField.addTextChangedListener(textWatcher)
        wordField.addTextChangedListener(textWatcher)

        btnPlay.setOnClickListener {
            this.semaforo = "V"
            val sharedPreferences = getSharedPreferences("WebCheckerPrefs", MODE_PRIVATE)

            val url = urlField.text.toString().trim()
            val word = wordField.text.toString().trim()

            if (url.isEmpty() || word.isEmpty()) {
                println("Error: URL o palabra clave vacías. No se inicia el servicio.")
                return@setOnClickListener
            }

            sharedPreferences.edit()
                .putString("url", url)
                .putString("word", word)
                .putString("semaforo", semaforo)
                .apply()

            WorkManager.getInstance(this).cancelAllWorkByTag(this.workTag)

            val workRequest = PeriodicWorkRequestBuilder<WebCheckerWorker>(
                15,
                TimeUnit.MINUTES
            ).addTag(this.workTag).build()

            WorkManager.getInstance(this).enqueue(workRequest)
            this.workId = workRequest.id
        }


        btnStop.setOnClickListener {
            println("Pulso boton stop")
            this.semaforo = "R"
            val sharedPreferences = getSharedPreferences("WebCheckerPrefs", MODE_PRIVATE)
            sharedPreferences.edit().putString("semaforo", semaforo).apply()

            stopWork()
        }
    }

    private fun stopWork() {
        WorkManager.getInstance(this).cancelWorkById(this.workId)

        WorkManager.getInstance(this).pruneWork()

        WorkManager.getInstance(this).getWorkInfosByTag(workTag).get().forEach { workInfo ->
            println("Trabajo ID: ${workInfo.id}, Estado: ${workInfo.state}")
            if (workInfo.state == WorkInfo.State.ENQUEUED || workInfo.state == WorkInfo.State.RUNNING) {
                WorkManager.getInstance(this).cancelWorkById(workInfo.id)
                println("Trabajo con ID ${workInfo.id} cancelado")
            }
        }
    }
}
