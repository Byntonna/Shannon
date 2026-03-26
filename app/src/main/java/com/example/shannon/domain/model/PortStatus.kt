package com.example.shannon.domain.model

enum class PortStatus(val title: String) {
    OPEN("Open"),
    CLOSED("Closed"),
    FILTERED("Filtered"),
    TIMEOUT("Timeout"),
    ERROR("Error"),
}
