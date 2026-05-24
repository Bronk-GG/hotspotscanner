package com.example.hotspotmonitor.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hotspotmonitor.data.AppRepository
import com.example.hotspotmonitor.data.ConnectedDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(private val repository: AppRepository) : ViewModel() {

    val localIp: StateFlow<String> = repository.localIp
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "Detecting...")

    val networkName: StateFlow<String> = repository.networkName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "Detecting...")

    val devices: StateFlow<List<ConnectedDevice>> = repository.devices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    fun toggleScanning() {
        viewModelScope.launch {
            if (_isScanning.value) {
                repository.stopScanning()
                _isScanning.value = false
            } else {
                repository.startScanning()
                _isScanning.value = true
            }
        }
    }
}
