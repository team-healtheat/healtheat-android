package com.healtheat.app.ui.components

import android.graphics.Rect
import android.view.ViewTreeObserver
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Compose IME insets can be consumed by a legacy decorFitsSystemWindows window.
 * Check native root insets too, so the bottom navigation yields space to the keyboard.
 */
@Composable
fun rememberImeVisibility(): Boolean {
    val view = LocalView.current
    val density = LocalDensity.current
    var nativeVisible by remember(view) { mutableStateOf(false) }

    DisposableEffect(view) {
        val observer = ViewTreeObserver.OnGlobalLayoutListener {
            val nativeInsets = ViewCompat.getRootWindowInsets(view)
            val fromInsets = nativeInsets?.isVisible(WindowInsetsCompat.Type.ime()) == true
            val rect = Rect()
            view.getWindowVisibleDisplayFrame(rect)
            val screenHeight = view.rootView.height
            val fromResize = screenHeight > 0 &&
                (screenHeight - rect.bottom) > screenHeight * 0.22f
            nativeVisible = fromInsets || fromResize
        }
        view.viewTreeObserver.addOnGlobalLayoutListener(observer)
        observer.onGlobalLayout()
        onDispose { view.viewTreeObserver.removeOnGlobalLayoutListener(observer) }
    }
    return nativeVisible || WindowInsets.ime.getBottom(density) > 0
}
