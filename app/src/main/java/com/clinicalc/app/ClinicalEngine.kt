package com.clinicalc.app

import kotlin.math.ln
import kotlin.math.sqrt

data class CalculationResult(val value: String, val interpretation: String? = null)

object ClinicalEngine {
    fun anionGap(na: Double, cl: Double, hco3: Double, albumin: Double? = null): CalculationResult {
        val ag = na - (cl + hco3)
        val corrected = albumin?.let { ag + 2.5 * (4.0 - it) }
        return CalculationResult(corrected?.let { "AG %.2f · corrected %.2f".format(ag, it) } ?: "AG %.2f".format(ag))
    }

    fun correctedCalcium(calcium: Double, albumin: Double) = CalculationResult("%.2f mg/dL".format(calcium + 0.8 * (4.0 - albumin)))
    fun osmolality(na: Double, glucose: Double, bun: Double) = CalculationResult("%.2f mOsm/kg".format(2 * na + glucose / 18 + bun / 2.8))
    fun map(sbp: Double, dbp: Double) = CalculationResult("%.2f mmHg".format(dbp + (sbp - dbp) / 3))
    fun meld(bilirubin: Double, inr: Double, creatinine: Double, dialysis: Boolean = false): CalculationResult {
        val bili = bilirubin.coerceAtLeast(1.0)
        val clot = inr.coerceAtLeast(1.0)
        val cr = if (dialysis) 4.0 else creatinine.coerceIn(1.0, 4.0)
        val score = (3.78 * ln(bili) + 11.2 * ln(clot) + 9.57 * ln(cr) + 6.43).coerceIn(6.0, 40.0)
        return CalculationResult("%.0f (%.2f before rounding)".format(score, score))
    }
    fun qtcBazett(qtMs: Double, heartRate: Double): CalculationResult {
        val rr = 60.0 / heartRate
        return CalculationResult("%.1f ms".format(qtMs / sqrt(rr)))
    }
    fun cha2ds2Vasc(chf: Boolean, hypertension: Boolean, age: Int, diabetes: Boolean, stroke: Boolean, vascular: Boolean, female: Boolean): CalculationResult {
        val score = (if (chf) 1 else 0) + (if (hypertension) 1 else 0) + when { age >= 75 -> 2; age >= 65 -> 1; else -> 0 } +
            (if (diabetes) 1 else 0) + (if (stroke) 2 else 0) + (if (vascular) 1 else 0) + (if (female) 1 else 0)
        return CalculationResult(score.toString(), if (score >= 2) "Higher stroke risk; interpret with current guideline and clinical context." else "Low score; do not use alone to make treatment decisions.")
    }

    fun convert(value: Double, from: String, to: String): Double = when ("$from->$to") {
        "mg/dL->µmol/L" -> value * 88.4
        "µmol/L->mg/dL" -> value / 88.4
        "mg/dL->mmol/L glucose" -> value * 0.0555
        "mmol/L glucose->mg/dL" -> value / 0.0555
        "kg->lb" -> value * 2.20462
        "lb->kg" -> value / 2.20462
        "mmHg->kPa" -> value * 0.133322
        "kPa->mmHg" -> value / 0.133322
        "C->F" -> value * 9 / 5 + 32
        "F->C" -> (value - 32) * 5 / 9
        "cm->in" -> value / 2.54
        "in->cm" -> value * 2.54
        "g/dL->g/L" -> value * 10
        "g/L->g/dL" -> value / 10
        "mL->L" -> value / 1000
        "L->mL" -> value * 1000
        else -> value
    }
}
