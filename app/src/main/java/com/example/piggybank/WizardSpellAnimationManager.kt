package com.example.piggybank

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.BitmapDrawable
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.doOnLayout

/**
 * WizardSpellAnimationManager - Manages wizard sprite-sheet animation on Calculate button
 *
 * Purpose: Handles all aspects of the sprite animation lifecycle:
 * - Captures button dimensions via layout callbacks
 * - Slices sprite sheet bitmap into individual frames (cached for reuse)
 * - Resizes overlay ImageView to match button width
 * - Plays frame-based animation with proper timing
 * - Cleans up resources and resets state after playback
 *
 * Design:
 * - Lazy bitmap decoding and caching to avoid repeated resource loading
 * - Frame caching prevents redundant bitmap slicing on repeated Calculate presses
 * - Centralized lifecycle management (setup, play, reset, cleanup)
 * - Safe exception handling and null checks
 *
 * Usage:
 * ```
 * val wizardAnimMgr = WizardSpellAnimationManager(context, animationView, calculateButton)
 * wizardAnimMgr.setupAnimation()
 * wizardAnimMgr.playAnimation()
 * // In onDestroy():
 * wizardAnimMgr.cleanup()
 * ```
 */
class WizardSpellAnimationManager(
    private val context: Context,
    private val animationView: ImageView,
    private val calculateButton: Button
) {

    private val frameCount = 8
    private val frameDurationMs = 120
    private var measuredButtonWidth: Int = 0
    private var currentAnimation: AnimationDrawable? = null
    private var resetRunnable: Runnable? = null

    // Frame caching: store sliced frames after first playback to avoid repeated slicing
    private var cachedSpriteSheet: Bitmap? = null
    private var cachedFrames: List<Bitmap>? = null

    /**
     * Called during activity creation to set up dimension callbacks and initial state.
     * Captures the Calculate button width after layout inflation so animation sizing
     * can match the button proportionally.
     */
    fun setupAnimation() {
        calculateButton.doOnLayout {
            measuredButtonWidth = it.width
        }
        animationView.visibility = View.GONE
        animationView.setImageDrawable(null)
    }

    /**
     * Main entry point for playing the animation. Handles resizing, frame generation,
     * and animation lifecycle in correct order.
     */
    fun playAnimation() {
        // Stop and clean up any prior animation
        stopCurrentAnimation()

        // Resize the view to match button width (must happen before drawable assignment)
        if (!resizeAnimationViewToButtonWidth()) {
            return
        }

        // Generate and play animation frames
        playAnimationFrames()
    }

    /**
     * Resizes the animation overlay ImageView to match the Calculate button width,
     * keeping the aspect ratio square and centered via ConstraintLayout bias.
     *
     * @return true if resize succeeded, false if button dimensions unavailable
     */
    private fun resizeAnimationViewToButtonWidth(): Boolean {
        val buttonWidth = calculateButton.width.takeIf { it > 0 } ?: measuredButtonWidth
        if (buttonWidth <= 0) {
            return false
        }

        val layoutParams = animationView.layoutParams as ConstraintLayout.LayoutParams
        layoutParams.width = buttonWidth
        layoutParams.height = buttonWidth
        layoutParams.horizontalBias = 0.5f
        layoutParams.verticalBias = 0.5f
        animationView.layoutParams = layoutParams
        animationView.requestLayout()

        return true
    }

    /**
     * Loads sprite sheet bitmap, slices into frames (with caching), builds AnimationDrawable,
     * and starts playback.
     */
    private fun playAnimationFrames() {
        val frames = getCachedFrames()
        if (frames.isEmpty()) {
            return
        }

        val animation = AnimationDrawable().apply {
            isOneShot = true
            frames.forEach { frame ->
                addFrame(BitmapDrawable(context.resources, frame), frameDurationMs)
            }
        }

        currentAnimation = animation
        animationView.visibility = View.VISIBLE
        animationView.setImageDrawable(animation)
        animation.stop()
        animation.selectDrawable(0)
        animation.start()

        // Schedule animation reset after one-shot playback completes
        scheduleAnimationReset(frames.first())
    }

    /**
     * Retrieves or generates sprite sheet frames. Uses cached frames on subsequent calls
     * to avoid redundant bitmap decoding and slicing.
     *
     * @return List of frame bitmaps (empty list if loading failed)
     */
    private fun getCachedFrames(): List<Bitmap> {
        // Return cached frames if available
        if (cachedFrames != null) {
            return cachedFrames!!
        }

        // Decode sprite sheet
        val spriteSheet = cachedSpriteSheet ?: run {
            val decoded = BitmapFactory.decodeResource(context.resources, R.drawable.wizardcat_money_spell)
            cachedSpriteSheet = decoded
            decoded
        } ?: return emptyList()

        // Slice sprite sheet into individual frames
        val frameWidth = spriteSheet.width / frameCount
        if (frameWidth <= 0 || spriteSheet.width % frameCount != 0) {
            return emptyList()
        }

        val frameHeight = spriteSheet.height
        val frames = mutableListOf<Bitmap>()
        repeat(frameCount) { index ->
            val x = index * frameWidth
            val frame = Bitmap.createBitmap(spriteSheet, x, 0, frameWidth, frameHeight)
            frames.add(frame)
        }

        cachedFrames = frames
        return frames
    }

    /**
     * Schedules the animation reset callback to fire after one-shot playback completes.
     * The total duration is frame count × frame duration (in milliseconds).
     */
    private fun scheduleAnimationReset(firstFrame: Bitmap) {
        resetRunnable?.let { animationView.removeCallbacks(it) }

        val totalDuration = frameCount * frameDurationMs
        val resetRunnable = Runnable {
            resetAnimation(firstFrame)
        }
        this.resetRunnable = resetRunnable
        animationView.postDelayed(resetRunnable, totalDuration.toLong())
    }

    /**
     * Resets animation state after one-shot playback: stops the drawable,
     * shows the first frame momentarily, then hides the view.
     */
    private fun resetAnimation(firstFrame: Bitmap) {
        currentAnimation?.stop()
        animationView.setImageDrawable(BitmapDrawable(context.resources, firstFrame))
        animationView.visibility = View.GONE
        resetRunnable = null
    }

    /**
     * Stops the current animation immediately (used when animation is interrupted).
     */
    private fun stopCurrentAnimation() {
        resetRunnable?.let { animationView.removeCallbacks(it) }
        resetRunnable = null
        currentAnimation?.stop()
        currentAnimation = null
    }

    /**
     * Releases all cached resources and cleans up callbacks.
     * Call this in Activity.onDestroy() to prevent memory leaks.
     */
    fun cleanup() {
        stopCurrentAnimation()
        cachedSpriteSheet?.recycle()
        cachedSpriteSheet = null
        cachedFrames?.forEach { it.recycle() }
        cachedFrames = null
    }
}
