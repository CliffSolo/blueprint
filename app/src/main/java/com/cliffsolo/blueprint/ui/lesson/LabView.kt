package com.cliffsolo.blueprint.ui.lesson

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import com.cliffsolo.blueprint.data.CourseStore
import java.io.File

/**
 * Serves lab files over a fake https origin so they behave like a normal web page:
 * /assets/... from the APK (bundled courses) and /courses/... from downloaded courses.
 */
fun labAssetLoader(context: Context): WebViewAssetLoader = WebViewAssetLoader.Builder()
    .setDomain(CourseStore.LAB_HOST)
    .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
    .addPathHandler("/courses/", WebViewAssetLoader.InternalStoragePathHandler(context, File(context.filesDir, "courses")))
    .build()

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LabView(url: String, heightDp: Int, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val loader = remember(context) { labAssetLoader(context) }
    AndroidView(
        modifier = modifier.fillMaxWidth().height(heightDp.dp),
        factory = { ctx ->
            WebView(ctx).apply {
                // Fill the fixed-height box; with the default WRAP_CONTENT the page sees a zero-height viewport.
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                setBackgroundColor(Color.TRANSPARENT)
                settings.javaScriptEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.setSupportZoom(false)
                isVerticalScrollBarEnabled = false
                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                        loader.shouldInterceptRequest(request.url)

                    // Labs are self-contained; never navigate away from them.
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                        request.url.host != CourseStore.LAB_HOST
                }
                loadUrl(url)
                tag = url
            }
        },
        update = { view -> if (view.tag != url) { view.tag = url; view.loadUrl(url) } },
        onRelease = { it.destroy() },
    )
}
