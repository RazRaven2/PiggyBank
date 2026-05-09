package com.example.piggybank

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import java.text.NumberFormat
import kotlin.math.round

/**
 * MainActivity - Coin Calculator Screen
 *
 * Purpose: Allows children to enter quantities of quarters, dimes, nickels, and pennies,
 * then calculates the total monetary value based on the selected mode (Saving or Spending).
 *
 * Features:
 * - Spinner to select between "Saving" and "Spending" modes
 * - EditText fields for entering coin quantities
 * - ImageButtons for each coin type (optional click-to-increment functionality)
 * - Calculate button to compute total value
 * - Results display with formatted currency
 *
 * Usage: Navigated to from WelcomeActivity via the Start button.
 */
class MainActivity : AppCompatActivity() {

    // UI Components
    private lateinit var balanceTextView: TextView
    private lateinit var modeSpinner: Spinner
    private lateinit var quartersEditText: EditText
    private lateinit var dimesEditText: EditText
    private lateinit var nickelsEditText: EditText
    private lateinit var penniesEditText: EditText
    private lateinit var calculateButton: Button
    private lateinit var resultTextView: TextView

    // Coin image buttons for interactive input
    private lateinit var quarterButton: ImageButton
    private lateinit var dimeButton: ImageButton
    private lateinit var nickelButton: ImageButton
    private lateinit var pennyButton: ImageButton

    private lateinit var balanceViewModel: BalanceViewModel

    private val currencyFormatter: NumberFormat = NumberFormat.getCurrencyInstance()

    // Counters for button clicks
    private var quarterCount = 0
    private var dimeCount = 0
    private var nickelCount = 0
    private var pennyCount = 0

    // Coin values in dollars
    private val quarterValue = 0.25
    private val dimeValue = 0.10
    private val nickelValue = 0.05
    private val pennyValue = 0.01

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Apply window insets for edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_container)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initializeUI()
        setupBalanceViewModel()
        setupSpinner()
        setupCoinButtons()
        setupCalculateButton()
    }

    /**
     * Initialize all UI components from the layout file.
     */
    private fun initializeUI() {
        balanceTextView = findViewById(R.id.balance_text)
        modeSpinner = findViewById(R.id.mode_spinner)
        quartersEditText = findViewById(R.id.quarters_input)
        dimesEditText = findViewById(R.id.dimes_input)
        nickelsEditText = findViewById(R.id.nickels_input)
        penniesEditText = findViewById(R.id.pennies_input)
        calculateButton = findViewById(R.id.calculate_button)
        resultTextView = findViewById(R.id.result_text)

        quarterButton = findViewById(R.id.quarter_button)
        dimeButton = findViewById(R.id.dime_button)
        nickelButton = findViewById(R.id.nickel_button)
        pennyButton = findViewById(R.id.penny_button)
    }

    /**
     * Creates and observes the balance ViewModel so the balance indicator stays
     * updated for the entire app session.
     */
    private fun setupBalanceViewModel() {
        balanceViewModel = ViewModelProvider(this)[BalanceViewModel::class.java]
        balanceViewModel.balance.observe(this) { balance ->
            balanceTextView.text = getString(
                R.string.balance_label,
                currencyFormatter.format(balance)
            )
        }
    }

    /**
     * Set up the Spinner with "Saving" and "Spending" options.
     */
    private fun setupSpinner() {
        val spinnerOptions = arrayOf(
            getString(R.string.mode_prompt),
            getString(R.string.mode_saving),
            getString(R.string.mode_spending)
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, spinnerOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        modeSpinner.adapter = adapter
        modeSpinner.setSelection(0)
    }

    /**
     * Set up click listeners for coin image buttons to increment counters
     * and update the respective EditText fields.
     */
    private fun setupCoinButtons() {
        quarterButton.setOnClickListener {
            quarterCount++
            quartersEditText.setText(quarterCount.toString())
        }

        dimeButton.setOnClickListener {
            dimeCount++
            dimesEditText.setText(dimeCount.toString())
        }

        nickelButton.setOnClickListener {
            nickelCount++
            nickelsEditText.setText(nickelCount.toString())
        }

        pennyButton.setOnClickListener {
            pennyCount++
            penniesEditText.setText(pennyCount.toString())
        }
    }

    /**
     * Set up the Calculate button to compute total monetary value.
     */
    private fun setupCalculateButton() {
        calculateButton.setOnClickListener {
            calculateTotal()
        }
    }

    /**
     * Calculate the total monetary value based on entered quantities and display the result.
        * Applies save/spend rules, prevents negative balances, and resets inputs after each action.
     */
    private fun calculateTotal() {
        // Get values from EditText fields
        val quarters = quartersEditText.text.toString().toIntOrNull() ?: 0
        val dimes = dimesEditText.text.toString().toIntOrNull() ?: 0
        val nickels = nickelsEditText.text.toString().toIntOrNull() ?: 0
        val pennies = penniesEditText.text.toString().toIntOrNull() ?: 0

        // Update button counters to keep them in sync
        quarterCount = quarters
        dimeCount = dimes
        nickelCount = nickels
        pennyCount = pennies

        // Calculate total value in dollars
        val total = (quarters * quarterValue) +
                    (dimes * dimeValue) +
                    (nickels * nickelValue) +
                    (pennies * pennyValue)

        // Round to 2 decimal places for currency
        val roundedTotal = round(total * 100) / 100

        // Get selected mode from spinner
        val selectedMode = modeSpinner.selectedItem.toString()

        if (selectedMode == getString(R.string.mode_prompt)) {
            resultTextView.text = getString(R.string.select_mode_message)
            return
        }

        // Apply action to persistent balance.
        if (selectedMode == getString(R.string.mode_saving)) {
            balanceViewModel.addAmount(roundedTotal)
            resultTextView.text = getString(
                R.string.saving_result_message,
                currencyFormatter.format(roundedTotal)
            )
            clearInputsAndResetMode()
            return
        }

        val wasSpendSuccessful = balanceViewModel.spendAmount(roundedTotal)
        if (!wasSpendSuccessful) {
            resultTextView.text = getString(R.string.not_enough_money_message)
            clearInputsAndResetMode()
            return
        }

        resultTextView.text = getString(
            R.string.spending_result_message,
            currencyFormatter.format(roundedTotal)
        )
        clearInputsAndResetMode()
    }

    /**
     * Clears coin counts and returns spinner to prompt after an action.
     */
    private fun clearInputsAndResetMode() {
        quarterCount = 0
        dimeCount = 0
        nickelCount = 0
        pennyCount = 0

        quartersEditText.text.clear()
        dimesEditText.text.clear()
        nickelsEditText.text.clear()
        penniesEditText.text.clear()

        modeSpinner.setSelection(0)
    }
}
