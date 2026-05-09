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
 *
 * Design:
 * - Session-scoped via ViewModel (survives orientation changes)
 * - Backed by MutableLiveData for observable updates
 * - All monetary values stored in dollars with cent precision
 * - Thread-safe LiveData ensures UI updates on main thread
 *
 * Lifecycle:
 * - Created once per activity lifecycle
 * - Balance persists across configuration changes (rotate, etc.)
 * - Balance resets when activity is destroyed or app closes
 */
class BalanceViewModel : ViewModel() {

    private val _balance = MutableLiveData(0.0)
    val balance: LiveData<Double> = _balance

    /**
     * Adds money to the current balance.
     *
     * @param amount Dollar amount to add (automatically rounded to cents)
     */
    fun addAmount(amount: Double) {
        val currentBalance = _balance.value ?: 0.0
        _balance.value = roundToCents(currentBalance + amount)
    }

    /**
     * Tries to spend money from the current balance.
     *
     * @param amount Dollar amount to spend
     * @return true if the spend succeeds, false if it would result in a negative balance
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
     * Resets the piggy bank balance to zero.
     * Called when the user presses the Reset Piggy Bank button.
     */
    fun resetBalance() {
        _balance.value = 0.0
    }

    /**
     * Keeps monetary values stable at two decimal places (cent precision).
     * Prevents floating-point precision errors from accumulating.
     *
     * @param value The value to round
     * @return The value rounded to the nearest cent
     */
    private fun roundToCents(value: Double): Double {
        return round(value * 100) / 100
    }
}
