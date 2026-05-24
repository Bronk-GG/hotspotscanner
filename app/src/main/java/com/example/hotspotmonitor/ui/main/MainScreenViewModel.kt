package com.example.hotspotmonitor.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hotspotmonitor.data.AppRepository
import com.example.hotspotmonitor.data.ConnectedDevice
import com.example.hotspotmonitor.data.HotspotConfig
import com.example.hotspotmonitor.data.HotspotState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(private val repository: AppRepository) : ViewModel() {

    val hotspotState: StateFlow<HotspotState> = repository.hotspotState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HotspotState.OFF)

    val hotspotConfig: StateFlow<HotspotConfig> = repository.hotspotConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HotspotConfig())

    val gatewayIp: StateFlow<String> = repository.gatewayIp
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "192.168.43.1")

    val devices: StateFlow<List<ConnectedDevice>> = repository.devices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun toggleHotspot() {
        viewModelScope.launch {
            if (repository.hotspotState.value == HotspotState.ON) {
                repository.stopHotspot()
            } else {
                repository.startHotspot(
                    onStarted = { _errorMessage.value = null },
                    onError = { msg -> _errorMessage.value = msg },
                )
            }
        }
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    fun updateConfig(config: HotspotConfig) {
        repository.updateConfig(config)
    }
}
