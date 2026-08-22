package nwd.fokuslauncher.data.search

import java.text.DecimalFormat
import kotlin.math.*

sealed interface QuickActionResult {
    val input: String
    val displayResult: String

    data class MathResult(
        override val input: String,
        override val displayResult: String,
        val numericValue: Double
    ) : QuickActionResult

    data class ConversionResult(
        override val input: String,
        override val displayResult: String,
        val fromAmount: Double,
        val fromUnit: String,
        val toAmount: Double,
        val toUnit: String
    ) : QuickActionResult
}

object QuickActionCalculator {

    private val df = DecimalFormat("#,##0.######")

    fun evaluate(query: String): QuickActionResult? {
        val trimmed = query.trim()
        if (trimmed.length < 2) return null

        // 1. Try Unit/Currency Conversion first if keyword 'to' or 'in' is present
        val conversion = evaluateConversion(trimmed)
        if (conversion != null) return conversion

        // 2. Try Math Expression
        val math = evaluateMath(trimmed)
        if (math != null) return math

        return null
    }

    private fun evaluateConversion(query: String): QuickActionResult.ConversionResult? {
        // Match patterns like "100 usd to eur", "5 km in miles", "32 f to c"
        val regex = Regex("""^([\d.,]+)\s*([a-zA-Z°%]+)\s+(?:to|in)\s+([a-zA-Z°%]+)$""", RegexOption.IGNORE_CASE)
        val match = regex.find(query) ?: return null

        val amountStr = match.groupValues[1].replace(",", "")
        val amount = amountStr.toDoubleOrNull() ?: return null
        val fromUnitRaw = match.groupValues[2].lowercase()
        val toUnitRaw = match.groupValues[3].lowercase()

        // Currency Conversions (approximate market rates for offline responsiveness)
        val currencyRates = mapOf(
            "usd" to 1.0,
            "eur" to 0.92,
            "gbp" to 0.79,
            "inr" to 83.50,
            "jpy" to 155.0,
            "cad" to 1.37,
            "aud" to 1.51,
            "chf" to 0.90,
            "cny" to 7.23,
            "bdt" to 117.0,
            "rub" to 90.0,
            "brl" to 5.40,
            "sgd" to 1.35,
            "nzd" to 1.63,
            "krw" to 1380.0
        )

        if (currencyRates.containsKey(fromUnitRaw) && currencyRates.containsKey(toUnitRaw)) {
            val fromRate = currencyRates[fromUnitRaw]!!
            val toRate = currencyRates[toUnitRaw]!!
            val result = (amount / fromRate) * toRate
            val fromUnitUpper = fromUnitRaw.uppercase()
            val toUnitUpper = toUnitRaw.uppercase()
            val formattedAmount = df.format(amount)
            val formattedResult = df.format(result)
            return QuickActionResult.ConversionResult(
                input = query,
                displayResult = "$formattedAmount $fromUnitUpper = $formattedResult $toUnitUpper",
                fromAmount = amount,
                fromUnit = fromUnitUpper,
                toAmount = result,
                toUnit = toUnitUpper
            )
        }

        // Temperature Conversions
        if (isTemp(fromUnitRaw) && isTemp(toUnitRaw)) {
            val celsius = when (fromUnitRaw) {
                "c", "celsius", "°c" -> amount
                "f", "fahrenheit", "°f" -> (amount - 32) * 5 / 9
                "k", "kelvin" -> amount - 273.15
                else -> return null
            }
            val result = when (toUnitRaw) {
                "c", "celsius", "°c" -> celsius
                "f", "fahrenheit", "°f" -> (celsius * 9 / 5) + 32
                "k", "kelvin" -> celsius + 273.15
                else -> return null
            }
            val fromLabel = tempLabel(fromUnitRaw)
            val toLabel = tempLabel(toUnitRaw)
            return QuickActionResult.ConversionResult(
                input = query,
                displayResult = "${df.format(amount)} $fromLabel = ${df.format(result)} $toLabel",
                fromAmount = amount,
                fromUnit = fromLabel,
                toAmount = result,
                toUnit = toLabel
            )
        }

        // Standard Unit Conversions (Length, Weight, Volume, Digital, Time)
        val unitCategoryMap = mapOf(
            // Length in meters
            "km" to 1000.0, "m" to 1.0, "cm" to 0.01, "mm" to 0.001,
            "mi" to 1609.344, "miles" to 1609.344, "mile" to 1609.344,
            "ft" to 0.3048, "feet" to 0.3048, "foot" to 0.3048,
            "in" to 0.0254, "inch" to 0.0254, "inches" to 0.0254,
            "yd" to 0.9144, "yard" to 0.9144, "yards" to 0.9144,

            // Weight in grams
            "kg" to 1000.0, "g" to 1.0, "mg" to 0.001,
            "lb" to 453.59237, "lbs" to 453.59237, "pound" to 453.59237, "pounds" to 453.59237,
            "oz" to 28.3495, "ounce" to 28.3495, "ounces" to 28.3495,
            "ton" to 1000000.0, "tons" to 1000000.0,

            // Digital Storage in MB
            "bytes" to 0.000001, "b" to 0.000001, "kb" to 0.001, "mb" to 1.0, "gb" to 1000.0, "tb" to 1000000.0,

            // Time in Seconds
            "sec" to 1.0, "secs" to 1.0, "s" to 1.0, "second" to 1.0, "seconds" to 1.0,
            "min" to 60.0, "mins" to 60.0, "m" to 60.0, "minute" to 60.0, "minutes" to 60.0,
            "hr" to 3600.0, "hrs" to 3600.0, "h" to 3600.0, "hour" to 3600.0, "hours" to 3600.0,
            "day" to 86400.0, "days" to 86400.0, "d" to 86400.0,
            "week" to 604800.0, "weeks" to 604800.0
        )

        val fromFactor = unitCategoryMap[fromUnitRaw]
        val toFactor = unitCategoryMap[toUnitRaw]

        if (fromFactor != null && toFactor != null) {
            val result = (amount * fromFactor) / toFactor
            val fromDisp = fromUnitRaw.lowercase()
            val toDisp = toUnitRaw.lowercase()
            return QuickActionResult.ConversionResult(
                input = query,
                displayResult = "${df.format(amount)} $fromDisp = ${df.format(result)} $toDisp",
                fromAmount = amount,
                fromUnit = fromDisp,
                toAmount = result,
                toUnit = toDisp
            )
        }

        return null
    }

    private fun isTemp(u: String) = u in listOf("c", "celsius", "°c", "f", "fahrenheit", "°f", "k", "kelvin")
    private fun tempLabel(u: String) = when(u) {
        "c", "celsius", "°c" -> "°C"
        "f", "fahrenheit", "°f" -> "°F"
        "k", "kelvin" -> "K"
        else -> u.uppercase()
    }

    private fun evaluateMath(query: String): QuickActionResult.MathResult? {
        // Must contain numbers and at least one math symbol or math function
        val clean = query.replace(" ", "")
        if (!clean.any { it in "0123456789" }) return null
        if (!clean.any { it in "+-*/%^()" } && !clean.contains("sqrt", ignoreCase = true) && !clean.contains("sin", ignoreCase = true) && !clean.contains("cos", ignoreCase = true) && !clean.contains("tan", ignoreCase = true)) {
            return null
        }

        return try {
            val valResult = MathParser(clean).parse()
            if (valResult.isNaN() || valResult.isInfinite()) return null
            val formatted = df.format(valResult)
            QuickActionResult.MathResult(
                input = query,
                displayResult = "= $formatted",
                numericValue = valResult
            )
        } catch (e: Exception) {
            null
        }
    }

    private class MathParser(val str: String) {
        var pos = -1
        var ch = 0

        fun nextChar() {
            ch = if (++pos < str.length) str[pos].code else -1
        }

        fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val x = parseExpression()
            if (pos < str.length) throw RuntimeException("Unexpected char: " + ch.toChar())
            return x
        }

        fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                if (eat('+'.code)) x += parseTerm()
                else if (eat('-'.code)) x -= parseTerm()
                else return x
            }
        }

        fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                if (eat('*'.code)) x *= parseFactor()
                else if (eat('/'.code)) x /= parseFactor()
                else if (eat('%'.code)) x %= parseFactor()
                else return x
            }
        }

        fun parseFactor(): Double {
            if (eat('+'.code)) return parseFactor()
            if (eat('-'.code)) return -parseFactor()

            var x: Double
            val startPos = this.pos
            if (eat('('.code)) {
                x = parseExpression()
                eat(')'.code)
            } else if (ch in '0'.code..'9'.code || ch == '.'.code) {
                while (ch in '0'.code..'9'.code || ch == '.'.code) nextChar()
                x = str.substring(startPos, this.pos).toDouble()
            } else if (ch in 'a'.code..'z'.code || ch in 'A'.code..'Z'.code) {
                while (ch in 'a'.code..'z'.code || ch in 'A'.code..'Z'.code) nextChar()
                val func = str.substring(startPos, this.pos).lowercase()
                x = parseFactor()
                x = when (func) {
                    "sqrt" -> sqrt(x)
                    "sin" -> sin(Math.toRadians(x))
                    "cos" -> cos(Math.toRadians(x))
                    "tan" -> tan(Math.toRadians(x))
                    "abs" -> abs(x)
                    else -> throw RuntimeException("Unknown function: $func")
                }
            } else {
                throw RuntimeException("Unexpected char: " + ch.toChar())
            }

            if (eat('^'.code)) x = x.pow(parseFactor())

            return x
        }
    }
}
