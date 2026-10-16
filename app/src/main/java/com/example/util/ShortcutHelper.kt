package com.example.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.MainActivity
import com.example.data.model.ProjectManifest
import java.io.File

object ShortcutHelper {

    const val EXTRA_PROJECT_ID = "com.example.htmlpack.EXTRA_PROJECT_ID"

    fun canPinShortcut(context: Context): Boolean {
        return ShortcutManagerCompat.isRequestPinShortcutSupported(context)
    }

    fun requestPinShortcut(
        context: Context,
        manifest: ProjectManifest,
        iconDir: File?
    ): Boolean {
        if (!canPinShortcut(context)) return false

        val projectId = manifest.meta.id
        val appName = manifest.meta.name

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(EXTRA_PROJECT_ID, projectId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val iconCompat = createProjectIcon(manifest, iconDir)

        val shortcutInfo = ShortcutInfoCompat.Builder(context, "shortcut_$projectId")
            .setShortLabel(appName)
            .setLongLabel(appName)
            .setIcon(iconCompat)
            .setIntent(launchIntent)
            .build()

        val callbackIntent = ShortcutManagerCompat.createShortcutResultIntent(context, shortcutInfo)
        val successCallback = PendingIntent.getBroadcast(
            context,
            0,
            callbackIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return ShortcutManagerCompat.requestPinShortcut(context, shortcutInfo, successCallback.intentSender)
    }

    private fun createProjectIcon(manifest: ProjectManifest, iconDir: File?): IconCompat {
        // Generate a crisp circular icon with project color and initial / emoji
        val size = 192
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgColor = try {
            Color.parseColor(manifest.icon.background)
        } catch (e: Exception) {
            Color.parseColor("#1E3A8A")
        }

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bgColor
            style = Paint.Style.FILL
        }

        val rect = RectF(0f, 0f, size.toFloat(), size.toFloat())
        when (manifest.icon.adaptiveShape) {
            "circle" -> canvas.drawOval(rect, bgPaint)
            "rounded" -> canvas.drawRoundRect(rect, 40f, 40f, bgPaint)
            "squircle" -> canvas.drawRoundRect(rect, 56f, 56f, bgPaint)
            else -> canvas.drawRoundRect(rect, 40f, 40f, bgPaint)
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 90f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }

        val label = if (manifest.icon.emoji.isNotBlank()) {
            manifest.icon.emoji
        } else if (manifest.icon.initialLetter.isNotBlank()) {
            manifest.icon.initialLetter.take(1)
        } else {
            manifest.meta.name.take(1).uppercase()
        }

        val yPos = (size / 2 - (textPaint.descent() + textPaint.ascent()) / 2)
        canvas.drawText(label, size / 2f, yPos, textPaint)

        return IconCompat.createWithBitmap(bitmap)
    }
}
