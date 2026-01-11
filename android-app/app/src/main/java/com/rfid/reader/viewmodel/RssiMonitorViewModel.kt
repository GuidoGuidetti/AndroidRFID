package com.rfid.reader.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.rfid.reader.network.ProductResponse
import com.rfid.reader.network.RetrofitClient
import com.rfid.reader.rfid.RFIDManager
import kotlinx.coroutines.launch

/**
 * ViewModel per RssiMonitorActivity
 * Monitora RSSI di un singolo EPC in modalità continua
 */
class RssiMonitorViewModel(application: Application) : AndroidViewModel(application) {
    private val rfidManager = RFIDManager.getInstance(application)
    private val apiService = RetrofitClient.apiService

    private var targetEpc: String = ""

    private val _rssiValue = MutableLiveData<Int>(-100)
    val rssiValue: LiveData<Int> = _rssiValue

    private val _productData = MutableLiveData<ProductResponse?>()
    val productData: LiveData<ProductResponse?> = _productData

    private val _isScanning = MutableLiveData<Boolean>(false)
    val isScanning: LiveData<Boolean> = _isScanning

    private val _readerStatus = MutableLiveData<String>("Disconnected")
    val readerStatus: LiveData<String> = _readerStatus

    private val _connectionProgress = MutableLiveData<Boolean>(false)
    val connectionProgress: LiveData<Boolean> = _connectionProgress

    private var lastSeenTimestamp: Long = 0

    init {
        observeRFIDManager()
        startRssiMonitorLoop()
    }

    private fun observeRFIDManager() {
        // Osserva connection state
        viewModelScope.launch {
            rfidManager.connectionState.collect { state ->
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
                android.util.Log.d(TAG, "Trigger state: $pressed")
                if (lastTriggerState && !pressed) {
                    android.util.Log.d(TAG, "Trigger released, toggling scan")
                    toggleScan()
                }
                lastTriggerState = pressed
            }
        }

        // Osserva tag letti - filtra solo il target EPC
        viewModelScope.launch {
            rfidManager.tagReadFlow.collect { tagList ->
                tagList.forEach { tag ->
                    if (tag.tagID == targetEpc) {
                        val rssi = tag.peakRSSI.toInt()
                        _rssiValue.value = rssi
                        lastSeenTimestamp = System.currentTimeMillis()
                        android.util.Log.d(TAG, "RSSI update: $rssi dBm")
                    }
                }
            }
        }
    }

    private fun startRssiMonitorLoop() {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(100) // Check ogni 100ms

                if (_isScanning.value == true) {
                    val now = System.currentTimeMillis()

                    // ✅ Timeout aumentato da 800ms a 1500ms per maggiore stabilità
                    if (now - lastSeenTimestamp > 1500 && _rssiValue.value != -100) {
                        _rssiValue.value = -100
                        android.util.Log.d(TAG, "Tag lost (timeout > 1500ms)")
                    }
                }
            }
        }
    }

    fun setTargetEpc(epc: String) {
        targetEpc = epc
        android.util.Log.d(TAG, "Target EPC set: $epc")
        loadProductData(epc)
    }

    private fun loadProductData(epc: String) {
        viewModelScope.launch {
            try {
                // Carica item
                val itemResp = apiService.getItemByEpc(epc)
                if (itemResp.isSuccessful) {
                    val item = itemResp.body()
                    val productId = item?.item_product_id

                    if (productId != null) {
                        // Carica product details
                        val prodResp = apiService.getProductById(productId)
                        if (prodResp.isSuccessful) {
                            _productData.value = prodResp.body()
                            android.util.Log.d(TAG, "Product data loaded: $productId")
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error loading product data", e)
            }
        }
    }

    fun startScan() {
        viewModelScope.launch {
            rfidManager.clearTags()
            rfidManager.startInventory()
            _isScanning.value = true
            lastSeenTimestamp = System.currentTimeMillis()
            android.util.Log.d(TAG, "Scan started for EPC: $targetEpc")
        }
    }

    fun stopScan() {
        viewModelScope.launch {
            rfidManager.stopInventory()
            _isScanning.value = false
            android.util.Log.d(TAG, "Scan stopped")
        }
    }

    fun toggleScan() {
        if (_isScanning.value == true) {
            stopScan()
        } else {
            startScan()
        }
    }

    fun disconnectReader() {
        stopScan()
        rfidManager.disconnect()
    }

    override fun onCleared() {
        super.onCleared()
        rfidManager.dispose()
    }

    companion object {
        private const val TAG = "RssiMonitorViewModel"
    }
}
