package com.example.piggybank

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
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
 * - ImageButtons for each coin type (click-to-increment functionality)
 * - Calculate button to compute total value with wizard animation
 * - Results display with formatted currency
 * - Reset button to clear balance and inputs
 * - Visual coin icons with drop animations on coin add/remove
 *
 * Architecture:
 * - Delegates sound playback to [SoundManager]
 * - Delegates coin icon/animation to [CoinAnimationManager]
 * - Delegates wizard sprite animation to [WizardSpellAnimationManager]
 * - Delegates balance persistence to [BalanceViewModel]
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
    private lateinit var resetButton: Button
    private lateinit var resultTextView: TextView
    private lateinit var coinDropAnimationView: ImageView
    private lateinit var wizardSpellAnimationView: ImageView

    // Coin image buttons for interactive input
    private lateinit var quarterButton: ImageButton
    private lateinit var dimeButton: ImageButton
    private lateinit var nickelButton: ImageButton
    private lateinit var pennyButton: ImageButton

    // Visual coin containers shown under each matching input field
    private lateinit var quartersIconsContainer: LinearLayout
    private lateinit var dimesIconsContainer: LinearLayout
    private lateinit var nickelsIconsContainer: LinearLayout
    private lateinit var penniesIconsContainer: LinearLayout

    // Manager delegates for complex systems
    private lateinit var soundManager: SoundManager
    private lateinit var coinAnimationManager: CoinAnimationManager
    private lateinit var wizardAnimationManager: WizardSpellAnimationManager
    private lateinit var balanceViewModel: BalanceViewModel

    private val currencyFormatter: NumberFormat = NumberFormat.getCurrencyInstance()
    private var suppressCoinFeedback = false

    // Counters for button clicks
    private var quarterCount = 0
    private var dimeCount = 0
    private var nickelCount = 0
    private var pennyCount = 0

    // Coin type with properties for drawable, description, and values
    private enum class CoinType(
        val drawableResId: Int,
        val descriptionResId: Int,
        val valueInDollars: Double
    ) {
        QUARTER(R.drawable.quarter, R.string.quarter_description, 0.25),
        DIME(R.drawable.dime, R.string.dime_description, 0.10),
        NICKEL(R.drawable.nickel, R.string.nickel_description, 0.05),
        PENNY(R.drawable.penny, R.string.penny_description, 0.01)
    }

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
        initializeManagers()
        setupBalanceViewModel()
        setupSpinner()
        setupCoinInputWatchers()
        setupCoinButtons()
        setupCalculateButton()
        setupResetButton()
    }

    /**
     * Initialize all manager delegates for sound, animation, and balance management.
     */
    private fun initializeManagers() {
        soundManager = SoundManager(this)
        coinAnimationManager = CoinAnimationManager(this, coinDropAnimationView)
        wizardAnimationManager = WizardSpellAnimationManager(this, wizardSpellAnimationView, calculateButton)
        wizardAnimationManager.setupAnimation()
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
        resetButton = findViewById(R.id.reset_button)
        resultTextView = findViewById(R.id.result_text)
        coinDropAnimationView = findViewById(R.id.coin_drop_animation_view)
        wizardSpellAnimationView = findViewById(R.id.wizard_spell_animation_view)

        quarterButton = findViewById(R.id.quarter_button)
        dimeButton = findViewById(R.id.dime_button)
        nickelButton = findViewById(R.id.nickel_button)
        pennyButton = findViewById(R.id.penny_button)

        quartersIconsContainer = findViewById(R.id.quarters_icons_container)
        dimesIconsContainer = findViewById(R.id.dimes_icons_container)
        nickelsIconsContainer = findViewById(R.id.nickels_icons_container)
        penniesIconsContainer = findViewById(R.id.pennies_icons_container)
    }
    /**
     * Watches all coin input fields so visual icons and feedback always match
     * typed values and button-driven updates.
     */
    private fun setupCoinInputWatchers() {
        attachCoinWatcher(quartersEditText, CoinType.QUARTER)
        attachCoinWatcher(dimesEditText, CoinType.DIME)
        attachCoinWatcher(nickelsEditText, CoinType.NICKEL)
        attachCoinWatcher(penniesEditText, CoinType.PENNY)
    }

    /**
     * Attaches a TextWatcher to an EditText for a specific coin type.
     * Triggers feedback (sound + animation) when the coin count changes.
     */
    private fun attachCoinWatcher(editText: EditText, coinType: CoinType) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                val newCount = (s?.toString()?.toIntOrNull() ?: 0).coerceAtLeast(0)
                onCoinCountChanged(coinType, newCount)
            }
        })
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
            incrementCoinInput(quartersEditText)
        }

        dimeButton.setOnClickListener {
            incrementCoinInput(dimesEditText)
        }

        nickelButton.setOnClickListener {
            incrementCoinInput(nickelsEditText)
        }

        pennyButton.setOnClickListener {
            incrementCoinInput(penniesEditText)
        }
    }

    private fun incrementCoinInput(editText: EditText) {
        val updatedCount = (editText.text.toString().toIntOrNull() ?: 0) + 1
        editText.setText(updatedCount.toString())
        editText.setSelection(editText.text.length)
    }

    /**
     * Compares old/new coin count values, updates icon display, and plays feedback
     * (sound + animation) when coins are added or removed.
     */
    private fun onCoinCountChanged(coinType: CoinType, newCount: Int) {
        val oldCount = getCountForCoinType(coinType)
        if (newCount == oldCount) {
            return
        }

        setCountForCoinType(coinType, newCount)
        updateCoinDisplay(coinType, newCount)

        if (!suppressCoinFeedback) {
            soundManager.playCoinClink()
            coinAnimationManager.playDropAnimation(coinType.drawableResId)
        }
    }

    /**
     * Returns the current coin count for the given coin type.
     */
    private fun getCountForCoinType(coinType: CoinType): Int = when (coinType) {
        CoinType.QUARTER -> quarterCount
        CoinType.DIME -> dimeCount
        CoinType.NICKEL -> nickelCount
        CoinType.PENNY -> pennyCount
    }

    /**
     * Sets the coin count for the given coin type.
     */
    private fun setCountForCoinType(coinType: CoinType, count: Int) {
        when (coinType) {
            CoinType.QUARTER -> quarterCount = count
            CoinType.DIME -> dimeCount = count
            CoinType.NICKEL -> nickelCount = count
            CoinType.PENNY -> pennyCount = count
        }
    }

    /**
     * Returns the LinearLayout container for displaying coin icons of the given type.
     */
    private fun getContainerForCoinType(coinType: CoinType): LinearLayout = when (coinType) {
        CoinType.QUARTER -> quartersIconsContainer
        CoinType.DIME -> dimesIconsContainer
        CoinType.NICKEL -> nickelsIconsContainer
        CoinType.PENNY -> penniesIconsContainer
    }

    /**
     * Updates the coin icon display for a specific coin type.
     * Delegates to CoinAnimationManager for container management.
     */
    private fun updateCoinDisplay(coinType: CoinType, count: Int) {
        val container = getContainerForCoinType(coinType)
        val description = getString(coinType.descriptionResId)
        coinAnimationManager.updateIconContainer(container, count, coinType.drawableResId, description)
    }

    /**
     * Set up the Calculate button to compute total monetary value and trigger animation.
     */
    private fun setupCalculateButton() {
        calculateButton.setOnClickListener {
            wizardAnimationManager.playAnimation()
            calculateTotal()
        }
    }

    private fun setupResetButton() {
        resetButton.setOnClickListener {
            resetPiggyBank()
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

        // Calculate total value in dollars using coin type values
        val total = (quarters * CoinType.QUARTER.valueInDollars) +
                    (dimes * CoinType.DIME.valueInDollars) +
                    (nickels * CoinType.NICKEL.valueInDollars) +
                    (pennies * CoinType.PENNY.valueInDollars)

        // Round to 2 decimal places for currency
        val roundedTotal = round(total * 100) / 100

        // Get selected mode from spinner
        val selectedMode = modeSpinner.selectedItem.toString()

        if (selectedMode == getString(R.string.mode_prompt)) {
            resultTextView.text = getString(R.string.select_mode_message)
            return
        }

        // Apply action to persistent balance
        if (selectedMode == getString(R.string.mode_saving)) {
            balanceViewModel.addAmount(roundedTotal)
            soundManager.playCash()
            resultTextView.text = getString(
                R.string.saving_result_message,
                currencyFormatter.format(roundedTotal)
            )
            clearInputsAndResetMode()
            return
        }

        // Attempt spending action
        val wasSpendSuccessful = balanceViewModel.spendAmount(roundedTotal)
        if (!wasSpendSuccessful) {
            resultTextView.text = getString(R.string.not_enough_money_message)
            clearInputsAndResetMode()
            return
        }

        soundManager.playCash()
        resultTextView.text = getString(
            R.string.spending_result_message,
            currencyFormatter.format(roundedTotal)
        )
        clearInputsAndResetMode()
    }

    /**
     * Clears coin counts and returns spinner to prompt state after an action.
     */
    private fun clearInputsAndResetMode() {
        suppressCoinFeedback = true

        quarterCount = 0
        dimeCount = 0
        nickelCount = 0
        pennyCount = 0

        quartersEditText.text.clear()
        dimesEditText.text.clear()
        nickelsEditText.text.clear()
        penniesEditText.text.clear()

        clearCoinIconContainers()
        modeSpinner.setSelection(0)
        coinAnimationManager.stopDropAnimation()

        suppressCoinFeedback = false
    }

    /**
     * Clears all coin icon containers after a successful calculate action.
     */
    private fun clearCoinIconContainers() {
        CoinType.values().forEach { coinType ->
            val container = getContainerForCoinType(coinType)
            coinAnimationManager.clearIconContainer(container)
        }
    }

    /**
     * Full reset requested by Part II: clear inputs/icons/mode, reset the session
     * balance to $0.00, and stop any active animations.
     */
    private fun resetPiggyBank() {
        clearInputsAndResetMode()
        balanceViewModel.resetBalance()
        resultTextView.text = getString(R.string.result_placeholder)

        Toast.makeText(this, getString(R.string.reset_confirmation_message), Toast.LENGTH_SHORT)
            .show()
    }

    override fun onDestroy() {
        soundManager.release()
        wizardAnimationManager.cleanup()
        super.onDestroy()
    }
}
