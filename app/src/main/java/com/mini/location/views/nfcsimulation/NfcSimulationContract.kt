package com.mini.location.views.nfcsimulation

import com.mini.location.R

sealed class NfcSimulationContract {
    data class NavigateDestination(val navId: Int)
    
    data class NfcHistoryItem(
        val id: Long,
        val content: String,
        val type: String,
        val timestamp: Long,
        val name: String = ""
    )
}