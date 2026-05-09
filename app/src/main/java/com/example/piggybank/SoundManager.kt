package com.example.piggybank

import android.content.Context
import android.media.MediaPlayer

/**
 * SoundManager - Centralized audio playback handler
 *
 * Purpose: Manages MediaPlayer instances for UI sound effects. Provides clean playback,
 * reset, and resource cleanup patterns to prevent audio overlap and memory leaks.
 *
 * Design:
 * - Lazy initialization of MediaPlayer instances
 * - Automatic pause + seek(0) before each play to prevent overlaps
 * - Safe exception handling for audio focus and state issues
 * - Proper resource cleanup in onDestroy()
 *
 * Usage:
 * ```
 * val soundManager = SoundManager(context)
 * soundManager.playCoinClink()
 * soundManager.playCash()
 * // In onDestroy():
 * soundManager.release()
 * ```
 */
class SoundManager(private val context: Context) {

    private var coinClinkPlayer: MediaPlayer? = null
    private var cashPlayer: MediaPlayer? = null

    /**
     * Initializes the coin clink sound player. Called once, on first play.
     */
    private fun initializeCoinClinkPlayer() {
        if (coinClinkPlayer == null) {
            coinClinkPlayer = MediaPlayer.create(context, R.raw.coin_clink)
        }
    }

    /**
     * Initializes the cash sound player. Called once, on first play.
     */
    private fun initializeCashPlayer() {
        if (cashPlayer == null) {
            cashPlayer = MediaPlayer.create(context, R.raw.cash)
        }
    }

    /**
     * Plays the coin clink sound effect (used for coin add/remove feedback).
     * Resets playback position to 0 so repeated rapid calls don't stack overlapping audio.
     */
    fun playCoinClink() {
        initializeCoinClinkPlayer()
        playSound(coinClinkPlayer)
    }

    /**
     * Plays the cash sound effect (used for successful save/spend actions).
     * Resets playback position to 0 so repeated Calculate presses don't stack overlapping audio.
     */
    fun playCash() {
        initializeCashPlayer()
        playSound(cashPlayer)
    }

    /**
     * Internal helper: resets and plays a MediaPlayer instance.
     * Safely handles any audio focus or state exceptions.
     */
    private fun playSound(player: MediaPlayer?) {
        if (player == null) return
        try {
            if (player.isPlaying) {
                player.pause()
            }
            player.seekTo(0)
            player.start()
        } catch (_: IllegalStateException) {
            // Ignore audio focus or state transitions that occur unexpectedly.
            // The UI will continue normally without the sound effect.
        }
    }

    /**
     * Releases all MediaPlayer resources. Call this in Activity.onDestroy()
     * to prevent native audio resource leaks.
     */
    fun release() {
        coinClinkPlayer?.release()
        coinClinkPlayer = null
        cashPlayer?.release()
        cashPlayer = null
    }
}
