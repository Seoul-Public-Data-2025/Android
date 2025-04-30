package com.maumpeace.safeapp.ui.settings

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.maumpeace.safeapp.databinding.ActivityWebBinding
import java.net.URISyntaxException


class WebActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWebBinding
    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.onBackPressedDispatcher.addCallback(this, callback)
        binding = ActivityWebBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initWebView()

        this.onBackPressedDispatcher.addCallback(this, callback)
        setSupportActionBar()
    }

    private fun setSupportActionBar() {
        setSupportActionBar(binding.headTb)
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setDisplayHomeAsUpEnabled(true)
        }
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE
        ) // 캡처 방지
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    @SuppressLint(
        "ObsoleteSdkInt", "SetJavaScriptEnabled", "WebViewApiAvailability", "JavascriptInterface"
    )
    private fun initWebView() {
        val webView = binding.webView
        val webTitle = binding.nameTv
        val webImg = binding.webImg

        val settings = webView.settings
        settings.loadWithOverviewMode = true
        settings.javaScriptEnabled = true
        settings.builtInZoomControls = true
        settings.defaultTextEncodingName = "UTF-8"

        webView.webChromeClient = WebChromeClient()

        webView.settings.apply {
            domStorageEnabled = true // 로컬 스토리지 사용 허용
            databaseEnabled = true // 데이터베이스 저장 허용
            javaScriptCanOpenWindowsAutomatically = true // JS로 새 창 열기 허용
        }

        webView.settings.userAgentString =
            webView.settings.userAgentString.replace("; wv", "") // WebView 표시 제거 (필요시)

        webView.webViewClient = WebViewClient() //클릭 시 새창 뜨지 않게
        webView.webViewClient = WebViewClientClass()

        webImg.webViewClient = WebViewClient() //클릭 시 새창 뜨지 않게
        webImg.webViewClient = WebViewClientClass()

        //setup cache
        /*
         * WebView에서 캐시사용 관련 Default 설정은 WebSettings.LOAD_DEFAULT 입니다.
         * ex) settings.cacheMode = WebSettings.LOAD_DEFAULT
         * 가급적 캐시 사용 설정을 변경하지 않을것을 권고 드립니다.
         * @중요 : 'WebSettings.LOAD_CACHE_ELSE_NETWORK' 로 변경금지.
         * @중요 : Do not change the setting to 'WebSettings.LOAD_CACHE_ELSE_NETWORK'
        */
        settings.cacheMode = WebSettings.LOAD_DEFAULT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {    //Android 5.0 이상
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(webImg, true)
        } else {
            CookieManager.getInstance().setAcceptCookie(true)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            webView.webChromeClient
        }

        //줌 설정
        webView.settings.builtInZoomControls = true
        webView.settings.setSupportZoom(true)
        webView.settings.displayZoomControls = false

        webImg.settings.builtInZoomControls = true
        webImg.settings.setSupportZoom(true)
        webImg.settings.displayZoomControls = false

        when (intent.getIntExtra("type", -1)) {
            1 -> {
                webTitle.text = "공지사항"
                webView.loadUrl("https://www.notion.so/1e02af92761081319f16c4cc3194ff5d")
            }

            2 -> {
                webTitle.text = "도움말"
                webView.loadUrl("https://www.notion.so/1e02af9276108161bd49c364c4191a9f?pvs=4")
            }

            3 -> {
                webTitle.text = "개인정보처리방침"
                webView.loadUrl("https://www.notion.so/1e02af92761081c88a58caf99620d569?pvs=4")
            }
        }
    }

    private class WebViewClientClass : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            println("url : " + url)

            try {
                if (url != null && (url.startsWith("intent:") || url.contains("market://") || url.contains(
                        "vguard"
                    ) || url.contains("droidxantivirus") || url.contains("v3mobile") || url.contains(
                        ".apk"
                    ) || url.contains("mvaccine") || url.contains("smartwall://") || url.contains("nidlogin://") || url.contains(
                        "http://m.ahnlab.com/kr/site/download"
                    ))
                ) {

                    var intent: Intent? = null

                    try {
                        intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                    } catch (e: URISyntaxException) {
                        println("error : " + e.printStackTrace())
                    }

                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                        if (view?.context?.packageManager?.resolveActivity(intent!!, 0) == null) {
                            val pkgName = intent?.`package`
                            if (pkgName != null) {
                                val uri = Uri.parse("market://search?q=pname:" + pkgName)
                                intent = Intent(Intent.ACTION_VIEW, uri)
                                view?.context?.startActivity(intent)
                            }
                        } else {
                            val uri = Uri.parse(intent?.dataString)
                            intent = Intent(Intent.ACTION_VIEW, uri)
                            view.context?.startActivity(intent)
                        }
                    } else {
                        try {
                            view?.context?.startActivity(intent)
                        } catch (e: ActivityNotFoundException) {
                            val pkgName = intent?.`package`
                            if (pkgName != null) {
                                val uri = Uri.parse("market://search?q=pname:" + pkgName)
                                intent = Intent(Intent.ACTION_VIEW, uri)
                                view?.context?.startActivity(intent)
                            }
                        }
                    }
                } else {
                    if (url != null) {
                        view?.loadUrl(url)
                    }
                }
            } catch (e: Exception) {
                println("error : " + e.printStackTrace())
                return false
            }

            return true
        }
    }
}