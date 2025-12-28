package com.occaecat.ztoeschedule.data.model

enum class DisplayMode {
    Compact,      // Щільний - більше інформації
    Comfortable,  // Звичайний (стандарт)
    Spacious      // Просторий - великі елементи
}

enum class ColorTheme {
    System,       // Системна
    Light,        // Світла
    Dark,         // Темна
    Amoled,       // Чорна (економія)
    Contrast      // Високий контраст
}

enum class FontScale(val multiplier: Float) {
    Small(0.85f),
    Normal(1.0f),
    Large(1.15f),
    Xlarge(1.3f),
    Accessibility(1.5f)
}