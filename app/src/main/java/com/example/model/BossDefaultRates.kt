package com.example.model

import kotlin.math.roundToLong

object BossDefaultRates {
    fun normalizeGaugeVariants(gauge: String): List<String> {
        val clean = gauge.trim().removeSuffix("mm").removeSuffix("MM").trim()
        val variants = mutableListOf(clean)
        if (clean.endsWith(".0")) {
            variants.add(clean.removeSuffix(".0"))
        } else if (clean.matches(Regex("^\\d+$"))) {
            variants.add("$clean.0")
        }
        return variants.distinct()
    }

    fun normalizeColorVariants(color: String): List<String> {
        val clean = color.trim().uppercase()
        val variants = mutableListOf(clean)
        if (clean.contains("/")) {
            val noSpaces = clean.split("/").joinToString("/") { it.trim() }
            val withSpaces = clean.split("/").joinToString(" / ") { it.trim() }
            variants.add(noSpaces)
            variants.add(withSpaces)
        }
        if (clean == "SAHARA/BROWN" || clean == "SAHARA / BROWN") {
            variants.add("SAHARA/BROWN")
            variants.add("SAHARA / BROWN")
            variants.add("SAHARA")
            variants.add("BROWN")
        }
        if (clean == "WOOD COAT" || clean == "WOOD COATED") {
            variants.add("WOOD COAT")
            variants.add("WOOD COATED")
        }
        if (clean == "SILVER/DULL" || clean == "SILVER / DULL" || clean == "DULL") {
            variants.add("SILVER / DULL")
            variants.add("SILVER/DULL")
            variants.add("DULL")
        }
        if (clean == "H23/PC-RAL" || clean == "H23 / RAL" || clean == "H23 / PC-RAL" || clean == "H23") {
            variants.add("H23/PC-RAL")
            variants.add("H23 / RAL")
            variants.add("H23 / PC-RAL")
            variants.add("H23")
        }
        if (clean == "BLACK/MULTI" || clean == "BLACK / MULTI") {
            variants.add("BLACK/MULTI")
            variants.add("BLACK / MULTI")
        }
        return variants.distinct()
    }

    val GAUGES = listOf("1.1", "1.2", "1.4", "1.6", "2.0", "2.5", "3.0")

    val DEFAULT_COLORS = listOf(
        "SAHARA / BROWN",
        "BLACK",
        "OFF WHITE",
        "SILVER / DULL",
        "CHAMPAGNE",
        "WOOD COATED",
        "WHITE",
        "H23 / RAL",
        "DULL"
    )

    val DEFAULT_GLASS_COLORS = listOf("CLEAR", "BRONZE / BLACK", "GREEN", "BLUE", "REFLECTIVE", "GREY")
    val DEFAULT_GLASS_COLORS_5MM = listOf("CLEAR", "BRONZE / BLACK", "GREEN", "BLUE", "REFLECTIVE", "GREY")
    val DEFAULT_GLASS_COLORS_8MM = listOf("CLEAR", "BRONZE / BLACK", "GREEN", "BLUE", "REFLECTIVE", "GREY")

    val DEFAULT_SECTIONS = listOf(
        "DC30C", "DC26C", "DC30F", "DC26F",
        "M23", "M28", "M24", "M24A", "D29",
        "D54A", "D50", "D46", "D52", "D41",
        "D51", "D51A", "D51F", "D54F", "D45", "D45A", "D45F", "D42", "D42F",
        "D50A", "D41A", "D59", "D59A",
        "M26", "M30", "M26F", "M30F",
        "DC24", "DC26", "DC30", "M25", "M27", "D28", "D31",
        "D40", "D43", "D44", "D47", "D48", "D48A", "D49", "D53", "D54", "D55", "D56", "D57", "D58", "D60",
        "PIPE1", "PIPE12", "PIPE22", "PIPE13", "PIPE112", "PIPE1123",
        "ANGLE1", "ANGLE112", "PATTI412", "PATTI6", "L39", "Z1", "Z2"
    )

    val SECTION_LABELS = mapOf(
        "DC30C" to "Sliding Outer C",
        "DC26C" to "Sliding Bottom C",
        "DC30F" to "Sliding Outer F",
        "DC26F" to "Sliding Bottom F",
        "DC24" to "Sliding Outer",
        "DC26" to "Sliding Bottom",
        "DC30" to "Sliding Outer",
        "M23" to "Sliding Shutter",
        "M28" to "Interlock",
        "M24" to "Latch Section",
        "M24A" to "Latch Section",
        "M25" to "Sliding Section",
        "M26" to "Sliding Door Outer",
        "M26F" to "Sliding Door Outer F",
        "M27" to "Sliding Section",
        "M30" to "Sliding Door Bottom",
        "M30F" to "Sliding Door Bottom F",
        "D28" to "Jali Section",
        "D29" to "S.S / Jali Frame",
        "D31" to "Jali Section",
        "D40" to "Glazing Bead",
        "D41" to "Door Patti",
        "D41A" to "Door Patti",
        "D42" to "Fix Frame",
        "D42F" to "Fix Frame F",
        "D43" to "Partition Section",
        "D44" to "Partition Section",
        "D45" to "Door Outer",
        "D45A" to "Door Outer",
        "D45F" to "Door Outer F",
        "D46" to "Door Bottom",
        "D47" to "Architectural Profile",
        "D48" to "Architectural Profile",
        "D48A" to "Architectural Profile",
        "D49" to "Architectural Profile",
        "D50" to "Door Top / Vertical",
        "D50A" to "Door Vertical",
        "D51" to "Door Outer",
        "D51A" to "Door Outer",
        "D51F" to "Door Outer F",
        "D52" to "Door Glazing / Bead",
        "D53" to "Architectural Profile",
        "D54" to "Door Outer",
        "D54A" to "Door Outer Frame",
        "D54F" to "Door Outer F-Sec",
        "D55" to "Architectural Profile",
        "D56" to "Architectural Profile",
        "D57" to "Architectural Profile",
        "D58" to "Architectural Profile",
        "D59" to "T-Bar / Mullion",
        "D59A" to "T-Bar / Mullion",
        "D60" to "Architectural Profile",
        "PIPE1" to "Pipe 1*1",
        "PIPE12" to "Pipe 1*2",
        "PIPE22" to "Pipe 2*2",
        "PIPE13" to "Pipe 1*3",
        "PIPE112" to "Pipe 1*1/2",
        "PIPE1123" to "Pipe 1*1/2*3",
        "ANGLE1" to "Angle 1*1",
        "ANGLE112" to "Angle 1-1/2",
        "PATTI412" to "K.Plate 4.5\"",
        "PATTI6" to "K.Plate 6\"",
        "L39" to "L-Angle Profile",
        "Z1" to "Netting Section 1",
        "Z2" to "Netting Section 2"
    )

    fun getInitialRates(): MutableMap<String, MutableMap<String, MutableMap<String, Double>>> {
        val root = mutableMapOf<String, MutableMap<String, MutableMap<String, Double>>>()

        fun setRate(gauge: String, color: String, section: String, rate: Double) {
            val gMap = root.getOrPut(gauge) { mutableMapOf() }
            val cMap = gMap.getOrPut(color) { mutableMapOf() }
            cMap[section] = rate
        }

        val base16SaharaRates = mapOf(
            "DC30C" to 300.0,
            "DC26C" to 250.0,
            "DC30F" to 320.0,
            "DC26F" to 270.0,
            "M23" to 310.0,
            "M28" to 180.0,
            "M24" to 220.0,
            "M24A" to 210.0,
            "D29" to 190.0,
            "D54A" to 290.0,
            "D50" to 310.0,
            "D46" to 340.0,
            "D52" to 145.0,
            "D41" to 125.0,
            "D51" to 285.0,
            "D51A" to 275.0,
            "D51F" to 300.0,
            "D54F" to 305.0,
            "D45" to 265.0,
            "D45A" to 255.0,
            "D45F" to 280.0,
            "D42" to 240.0,
            "D42F" to 260.0,
            "D50A" to 290.0,
            "D41A" to 120.0,
            "D59" to 295.0,
            "D59A" to 310.0,
            "M26" to 350.0,
            "M30" to 280.0,
            "M26F" to 370.0,
            "M30F" to 300.0,
            "DC24" to 260.0,
            "DC26" to 250.0,
            "DC30" to 300.0,
            "M25" to 230.0,
            "M27" to 240.0,
            "D28" to 185.0,
            "D31" to 195.0,
            "D40" to 135.0,
            "D43" to 220.0,
            "D44" to 230.0,
            "D47" to 260.0,
            "D48" to 275.0,
            "D48A" to 260.0,
            "D49" to 270.0,
            "D53" to 250.0,
            "D54" to 285.0,
            "D55" to 290.0,
            "D56" to 295.0,
            "D57" to 300.0,
            "D58" to 305.0,
            "D60" to 320.0,
            "PIPE1" to 180.0,
            "PIPE12" to 240.0,
            "PIPE22" to 310.0,
            "PIPE13" to 295.0,
            "PIPE112" to 255.0,
            "PIPE1123" to 340.0,
            "ANGLE1" to 160.0,
            "ANGLE112" to 220.0,
            "PATTI412" to 210.0,
            "PATTI6" to 280.0,
            "L39" to 155.0,
            "Z1" to 190.0,
            "Z2" to 215.0
        )

        val colorMultipliers = mapOf(
            "SAHARA / BROWN" to 1.0,
            "BLACK" to 1.03,
            "OFF WHITE" to 1.0,
            "SILVER / DULL" to 0.95,
            "DULL" to 0.95,
            "CHAMPAGNE" to 1.02,
            "WOOD COATED" to 1.18,
            "WHITE" to 1.0,
            "H23 / RAL" to 1.05
        )

        GAUGES.forEach { g ->
            val gaugeMultiplier = when (g) {
                "1.1" -> 0.72
                "1.2" -> 0.78
                "1.4" -> 0.89
                "1.6" -> 1.0
                "2.0", "2" -> 1.25
                "2.5" -> 1.50
                "3.0", "3" -> 1.75
                else -> 1.0
            }
            DEFAULT_COLORS.forEach { c ->
                val cm = colorMultipliers[c] ?: 1.0
                base16SaharaRates.forEach { (sec, baseRate) ->
                    val r = (baseRate * cm * gaugeMultiplier).roundToLong().toDouble()
                    setRate(g, c, sec, r)
                }
            }
        }

        return root
    }

    fun getInitialGlassRates(): MutableMap<String, MutableMap<String, Double>> {
        val map = mutableMapOf<String, MutableMap<String, Double>>()
        map["5"] = mutableMapOf(
            "CLEAR" to 230.0,
            "BRONZE / BLACK" to 250.0,
            "GREEN" to 260.0,
            "BLUE" to 260.0,
            "REFLECTIVE" to 300.0,
            "GREY" to 260.0
        )
        map["8"] = mutableMapOf(
            "CLEAR" to 390.0,
            "BRONZE / BLACK" to 410.0,
            "GREEN" to 425.0,
            "BLUE" to 425.0,
            "REFLECTIVE" to 480.0,
            "GREY" to 425.0
        )
        return map
    }
}
