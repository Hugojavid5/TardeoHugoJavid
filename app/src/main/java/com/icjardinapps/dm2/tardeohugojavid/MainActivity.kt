package com.icjardinapps.dm2.tardeohugojavid

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import android.Manifest
import android.media.MediaPlayer
import android.text.Editable
import android.text.TextWatcher
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

        // Verificar permisos para notificaciones
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }

        val urlField = findViewById<EditText>(R.id.URL)
        val wordField = findViewById<EditText>(R.id.FINDTEXT)
        val btnPlay = findViewById<Button>(R.id.play)
        val btnStop = findViewById<Button>(R.id.parar)

        // Inicializar el MediaPlayer para el sonido de error
        val incorrectSound = MediaPlayer.create(this, R.raw.error)

        // Hacer el botón de "Play" inicialmente deshabilitado
        btnPlay.isEnabled = false

        // Función para comprobar que los campos no estén vacíos
        fun checkFields() {
            val urlText = urlField.text.toString().trim()
            val wordText = wordField.text.toString().trim()
            btnPlay.isEnabled = urlText.isNotEmpty() && wordText.isNotEmpty()
        }

        // TextWatcher para los campos de URL y palabra clave
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                checkFields()
            }

            override fun afterTextChanged(s: Editable?) {}
        }

        // Agregar el TextWatcher a los campos de texto
        urlField.addTextChangedListener(textWatcher)
        wordField.addTextChangedListener(textWatcher)

        // Acciones al pulsar el botón "Play"
        btnPlay.setOnClickListener {
            val url = urlField.text.toString().trim()
            val word = wordField.text.toString().trim()

            if (url.isEmpty() || word.isEmpty()) {
                // Reproducir sonido de error
                incorrectSound.start()

                // Mostrar mensaje de error
                Toast.makeText(this, "URL o palabra clave vacías. No se inicia el servicio.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Guardar valores en SharedPreferences
            val sharedPreferences = getSharedPreferences("WebCheckerPrefs", MODE_PRIVATE)
            sharedPreferences.edit()
                .putString("url", url)
                .putString("word", word)
                .putString("semaforo", semaforo)
                .apply()

            // Cancelar trabajos anteriores
            WorkManager.getInstance(this).cancelAllWorkByTag(this.workTag)

            // Crear y ejecutar el trabajo
            val workRequest = PeriodicWorkRequestBuilder<WebCheckerWorker>(
                15,
                TimeUnit.MINUTES
            ).addTag(this.workTag).build()

            WorkManager.getInstance(this).enqueue(workRequest)
            this.workId = workRequest.id
        }

        // Acciones al pulsar el botón "Stop"
        btnStop.setOnClickListener {
            println("Pulso boton stop")
            this.semaforo = "R"
            val sharedPreferences = getSharedPreferences("WebCheckerPrefs", MODE_PRIVATE)
            sharedPreferences.edit().putString("semaforo", semaforo).apply()

            stopWork()
        }
    }

    // Función para detener los trabajos
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
