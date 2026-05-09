package com.example.piggybank

import android.content.Context
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.DrawableRes

/**
 * CoinAnimationManager - Manages coin icon display and drop animations
 *
 * Purpose: Handles the visual feedback for coin additions/removals, including:
 * - Updating icon strips (add/remove icon ImageViews)
 * - Playing coin drop animations on a floating overlay
 * - Cleaning up animation state
 *
 * Design:
 * - Stateless helper with no retained animation references
 * - Accepts container LinearLayouts for icon management
 * - Reusable across multiple coin types
 * - Clean animation lifecycle management
 *
 * Usage:
 * ```
 * val coinAnimManager = CoinAnimationManager(context, coinDropView)
 * coinAnimManager.updateIconContainer(container, count, iconResId)
 * coinAnimManager.playDropAnimation(iconResId)
 * ```
 */
class CoinAnimationManager(
    private val context: Context,
    private val dropAnimationView: ImageView
) {

    private val iconSize = 22.dpToPx()
    private val iconSpacing = 6.dpToPx()

    /**
     * Updates a coin icon container to display [count] icons for the given coin type.
     * Removes all existing icons and recreates the set based on the new count.
     *
     * @param container The LinearLayout to populate with coin icons
     * @param count Number of icons to display (0 clears the container)
     * @param coinDrawableResId The drawable resource ID for the coin type
     * @param coinDescription String description for accessibility
     */
    fun updateIconContainer(
        container: LinearLayout,
        count: Int,
        @DrawableRes coinDrawableResId: Int,
        coinDescription: String
    ) {
        container.removeAllViews()
        repeat(count) { index ->
            val iconView = ImageView(context).apply {
                setImageResource(coinDrawableResId)
                contentDescription = coinDescription
            }

            val params = LinearLayout.LayoutParams(iconSize, iconSize)
            if (index > 0) {
                params.marginStart = iconSpacing
            }
            iconView.layoutParams = params
            container.addView(iconView)
        }
    }

    /**
     * Plays a quick drop animation on the overlay image view, then hides it.
     * Called whenever a coin is added or removed by the user.
     *
     * @param coinDrawableResId The drawable to show during the drop animation
     */
    fun playDropAnimation(@DrawableRes coinDrawableResId: Int) {
        dropAnimationView.setImageResource(coinDrawableResId)
        dropAnimationView.visibility = View.VISIBLE
        dropAnimationView.clearAnimation()

        val dropAnimation = AnimationUtils.loadAnimation(context, R.anim.coin_drop)
        dropAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) = Unit
            override fun onAnimationEnd(animation: Animation?) {
                dropAnimationView.visibility = View.GONE
            }
            override fun onAnimationRepeat(animation: Animation?) = Unit
        })

        dropAnimationView.startAnimation(dropAnimation)
    }

    /**
     * Stops and clears any active drop animation, hiding the overlay view.
     * Called during input clearing or app destruction.
     */
    fun stopDropAnimation() {
        dropAnimationView.animate().cancel()
        dropAnimationView.clearAnimation()
        dropAnimationView.visibility = View.GONE
    }

    /**
     * Clears all icons from the given container.
     */
    fun clearIconContainer(container: LinearLayout) {
        container.removeAllViews()
    }

    /**
     * Converts dp (density-independent pixels) to px (pixels).
     * Used for consistent sizing across different device densities.
     */
    private fun Int.dpToPx(): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }
}
