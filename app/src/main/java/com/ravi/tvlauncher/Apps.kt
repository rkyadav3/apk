package com.ravi.tvlauncher

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.core.graphics.drawable.toBitmap

data class AppItem(
    val pkg: String,
    val label: String,
    val intent: Intent,
    val image: ImageBitmap,
    val isBanner: Boolean,
    val tint: Color,
)

object AppRepo {

    /** Loads every launchable app (TV apps first, then regular ones), one entry per package. */
    fun load(ctx: Context): List<AppItem> {
        val pm = ctx.packageManager
        val seen = HashSet<String>()
        val out = ArrayList<AppItem>()

        for (category in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
            val query = Intent(Intent.ACTION_MAIN).addCategory(category)
            for (ri in pm.queryIntentActivities(query, 0)) {
                val ai = ri.activityInfo ?: continue
                val pkg = ai.packageName
                if (pkg == ctx.packageName || !seen.add(pkg)) continue

                val launch = Intent(Intent.ACTION_MAIN)
                    .addCategory(category)
                    .setClassName(pkg, ai.name)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)

                val banner = runCatching { ai.loadBanner(pm) }.getOrNull()
                val drawable = banner ?: runCatching { ri.loadIcon(pm) }.getOrNull() ?: continue
                val isBanner = banner != null

                val bmp = if (isBanner) {
                    drawable.toBitmap(320, 180, Bitmap.Config.ARGB_8888)
                } else {
                    drawable.toBitmap(192, 192, Bitmap.Config.ARGB_8888)
                }

                out += AppItem(
                    pkg = pkg,
                    label = ri.loadLabel(pm).toString(),
                    intent = launch,
                    image = bmp.asImageBitmap(),
                    isBanner = isBanner,
                    tint = tintOf(bmp),
                )
            }
        }
        return out
    }

    /** Average colour of the artwork, darkened, used to tint the background. */
    private fun tintOf(bmp: Bitmap): Color {
        val px = Bitmap.createScaledBitmap(bmp, 1, 1, true).getPixel(0, 0)
        return lerp(Color(px).copy(alpha = 1f), Color.Black, 0.55f)
    }
}
