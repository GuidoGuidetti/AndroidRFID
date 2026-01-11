package com.rfid.reader

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.rfid.reader.databinding.ActivityNewInventoryBinding
import com.rfid.reader.network.CreateInventoryRequest
import com.rfid.reader.network.RetrofitClient
import com.rfid.reader.utils.SessionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Activity per la creazione di un nuovo inventario
 */
class NewInventoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNewInventoryBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewInventoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        // Precarica campi del form
        setupForm()

        // Click listeners
        binding.btnBack.setOnClickListener { finish() }
        binding.btnCancel.setOnClickListener { finish() }
        binding.btnCreate.setOnClickListener { createInventory() }
    }

    /**
     * Precarica i campi del form con dati utente
     */
    private fun setupForm() {
        // Place non editabile - usa usr_def_place dell'utente
        val userPlace = sessionManager.getUserPlace() ?: "N/A"
        val placeName = sessionManager.getPlaceName() ?: ""
        binding.tvPlace.text = if (placeName.isNotEmpty()) {
            "$userPlace ($placeName)"
        } else {
            userPlace
        }

        // Nome inventario precaricato con pattern: "userName place data-ora"
        val userName = sessionManager.getUserName() ?: "User"
        val dateTime = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        binding.etInventoryName.setText("$userName $userPlace $dateTime")
    }

    /**
     * Crea un nuovo inventario tramite API
     */
    private fun createInventory() {
        val name = binding.etInventoryName.text.toString().trim()
        val note = binding.etNotes.text.toString().trim()

        // Validazione nome inventario
        if (name.isEmpty()) {
            Toast.makeText(this, "Nome inventario richiesto", Toast.LENGTH_SHORT).show()
            return
        }

        val placeId = sessionManager.getUserPlace()

        if (placeId == null) {
            Toast.makeText(this, "Errore: luogo utente non trovato", Toast.LENGTH_LONG).show()
            return
        }

        // Disabilita pulsante durante creazione
        binding.btnCreate.isEnabled = false
        binding.btnCreate.text = "Creazione..."

        lifecycleScope.launch {
            try {
                val request = CreateInventoryRequest(
                    name = name,
                    note = note.ifEmpty { null },
                    placeId = placeId
                )

                val response = RetrofitClient.apiService.createInventory(request)

                if (response.success) {
                    // Torna alla lista con risultato OK
                    setResult(RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(
                        this@NewInventoryActivity,
                        "Errore: ${response.error}",
                        Toast.LENGTH_LONG
                    ).show()
                    resetCreateButton()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@NewInventoryActivity,
                    "Errore di rete: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                resetCreateButton()
            }
        }
    }

    /**
     * Riabilita il pulsante "Crea Inventario"
     */
    private fun resetCreateButton() {
        binding.btnCreate.isEnabled = true
        binding.btnCreate.text = "Crea Inventario"
    }
}
