package com.rfid.reader.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.rfid.reader.network.InventoryItemDetail
import com.rfid.reader.network.RetrofitClient
import com.rfid.reader.rfid.RFIDManager
import com.rfid.reader.utils.BeepHelper
import com.rfid.reader.utils.SettingsManager
import kotlinx.coroutines.launch

class TagInfoViewModel(application: Application) : AndroidViewModel(application) {
    private val rfidManager = RFIDManager.getInstance(application)
    private val apiService = RetrofitClient.apiService
    private val settingsManager = SettingsManager(application)
    private val beepHelper = BeepHelper.getInstance(application)

    private val _foundTags = MutableLiveData<List<InventoryItemDetail>>(emptyList())
    val foundTags: LiveData<List<InventoryItemDetail>> = _foundTags

    private val _isScanning = MutableLiveData<Boolean>(false)
    val isScanning: LiveData<Boolean> = _isScanning

    private val _isConnected = MutableLiveData<Boolean>(false)
    val isConnected: LiveData<Boolean> = _isConnected

    private val _readerStatus = MutableLiveData<String>("Disconnected")
    val readerStatus: LiveData<String> = _readerStatus

    private val _connectionProgress = MutableLiveData<Boolean>(false)
    val connectionProgress: LiveData<Boolean> = _connectionProgress

    private val checkedTagsCache = mutableMapOf<String, InventoryItemDetail?>()

    init {
        observeRFIDManager()
    }

    private fun observeRFIDManager() {
        viewModelScope.launch {
            rfidManager.connectionState.collect { state ->
                _isConnected.value = (state == RFIDManager.ConnectionState.CONNECTED)
                _connectionProgress.value = (state == RFIDManager.ConnectionState.CONNECTING)
                
                _readerStatus.value = when (state) {
                    RFIDManager.ConnectionState.CONNECTED -> "Reader Connected"
                    RFIDManager.ConnectionState.CONNECTING -> "Connecting..."
                    RFIDManager.ConnectionState.DISCONNECTED -> "Reader Disconnected"
                    RFIDManager.ConnectionState.ERROR -> "Connection Error"
                }
            }
        }

        // Osserva trigger per start/stop
        viewModelScope.launch {
            var lastTriggerState = false
            rfidManager.triggerPressed.collect { pressed ->
                if (lastTriggerState && !pressed) {
                    toggleScan()
                }
                lastTriggerState = pressed
            }
        }

        viewModelScope.launch {
            rfidManager.tagReadFlow.collect { tagList ->
                tagList.forEach { tag ->
                    processTag(tag.tagID)
                }
            }
        }
    }

    private fun processTag(epc: String) {
        if (checkedTagsCache.containsKey(epc)) {
            val item = checkedTagsCache[epc]
            // ✅ APPLICARE FILTRO ANCHE PER TAG IN CACHE
            applyModeFilterAndAdd(item, epc)
            return
        }

        viewModelScope.launch {
            try {
                val response = apiService.getItemByEpc(epc)
                if (response.isSuccessful) {
                    val itemResp = response.body()
                    if (itemResp != null) {
                        // Create detail object
                        val detail = InventoryItemDetail(
                            epc = itemResp.item_id,
                            product_id = itemResp.item_product_id,
                            fld01 = null,
                            fld02 = null,
                            fld03 = null,
                            fldd01 = null
                        )
                        
                        // Fetch product details to fill fields
                        val prodId = itemResp.item_product_id
                        if (prodId != null) {
                            val prodResp = apiService.getProductById(prodId)
                            if (prodResp.isSuccessful) {
                                val p = prodResp.body()
                                val fullDetail = detail.copy(
                                    fld01 = p?.fld01,
                                    fld02 = p?.fld02,
                                    fld03 = p?.fld03
                                )
                                checkedTagsCache[epc] = fullDetail
                                applyModeFilterAndAdd(fullDetail)
                            } else {
                                checkedTagsCache[epc] = detail
                                applyModeFilterAndAdd(detail)
                            }
                        } else {
                            checkedTagsCache[epc] = detail
                            applyModeFilterAndAdd(detail)
                        }
                    }
                } else {
                    // Not registered
                    checkedTagsCache[epc] = null
                    applyModeFilterAndAdd(null, epc)
                }
            } catch (e: Exception) {
                android.util.Log.e("TagInfoViewModel", "Error processing tag $epc", e)
            }
        }
    }

    private fun applyModeFilterAndAdd(item: InventoryItemDetail?, epc: String = "") {
        val mode = settingsManager.getTagReadingMode()

        when (mode) {
            "mode_a" -> { // Solo EPC censiti a sistema
                if (item != null) addToListIfMissing(item)
            }
            "mode_b", "mode_c" -> { // Tutti i tags (mode_b con registrazione, mode_c tutti)
                if (item != null) {
                    addToListIfMissing(item)
                } else if (epc.isNotEmpty()) {
                    addToListIfMissing(InventoryItemDetail(epc, "NON CENSITO", null, null, null, null))
                }
            }
            else -> { // Default: tutti
                if (item != null) {
                    addToListIfMissing(item)
                } else if (epc.isNotEmpty()) {
                    addToListIfMissing(InventoryItemDetail(epc, "NON CENSITO", null, null, null, null))
                }
            }
        }
    }

    private fun addToListIfMissing(item: InventoryItemDetail) {
        val currentList = _foundTags.value?.toMutableList() ?: mutableListOf()
        if (currentList.none { it.epc == item.epc }) {
            // ✅ Beep SOLO su nuovo EPC rilevato
            beepHelper.playBeep()

            currentList.add(0, item) // Inserisci in cima
            _foundTags.postValue(currentList)
        }
    }

    fun connectReader() {
        _connectionProgress.value = true
        _readerStatus.value = "Connecting..."
        viewModelScope.launch {
            kotlinx.coroutines.delay(100) // Assicura che la UI si aggiorni
            rfidManager.connectToReader()
        }
    }

    fun disconnectReader() {
        stopScan()
        rfidManager.disconnect()
    }

    fun toggleScan() {
        if (_isScanning.value == true) {
            stopScan()
        } else {
            startScan()
        }
    }

    private fun startScan() {
        viewModelScope.launch {
            rfidManager.startInventory()
            _isScanning.postValue(true)
        }
    }

    private fun stopScan() {
        viewModelScope.launch {
            rfidManager.stopInventory()
            _isScanning.postValue(false)
        }
    }

    fun clearTags() {
        rfidManager.clearTags()
        _foundTags.value = emptyList()
        checkedTagsCache.clear()
    }

    override fun onCleared() {
        super.onCleared()
        rfidManager.dispose()
    }
}
