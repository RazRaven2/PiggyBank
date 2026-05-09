package com.example.piggybank

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlin.math.round

/**
 * BalanceViewModel - Session Balance Store
 *
 * Purpose: Holds and exposes the child's running piggy bank balance for the
 * lifetime of the current app session. The activity observes this value to keep
 * the balance TextView in sync across UI updates and configuration changes.
 */
class BalanceViewModel : ViewModel() {

    private val _balance = MutableLiveData(0.0)
    val balance: LiveData<Double> = _balance

    /**
     * Adds money to the current balance.
     */
    fun addAmount(amount: Double) {
        val currentBalance = _balance.value ?: 0.0
        _balance.value = roundToCents(currentBalance + amount)
    }

    /**
     * Tries to spend money from the current balance.
     * Returns true when the spend succeeds, false when it would go negative.
     */
    fun spendAmount(amount: Double): Boolean {
        val currentBalance = _balance.value ?: 0.0
        if (amount > currentBalance) {
            return false
        }

        _balance.value = roundToCents(currentBalance - amount)
        return true
    }

    /**
     * Keeps monetary values stable at two decimal places.
     */
    private fun roundToCents(value: Double): Double {
        return round(value * 100) / 100
    }
}
