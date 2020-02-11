package com.in2l.olympus.activity

import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.in2l.olympus.R
import com.in2l.olympus.listener.CaptivePortalDetectionListener
import com.in2l.olympus.util.checkForCaptivePortal
import com.in2l.olympus.util.getDefaultRetrofitClient
import kotlinx.android.synthetic.main.wifi_wizard_dialog_captive_portal_auth.*

class CaptivePortalActivity : BaseActivity() {

    private val captivePortalUrlKey = "captive_portal_url_extra_key"
    private var isFirstLoad : Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.wifi_wizard_dialog_captive_portal_auth)

        initWebViewChecker(intent.getStringExtra(captivePortalUrlKey))
        initUI()
    }

    private fun initUI() {
        wifi_wizard_captive_portal_spinner_view.visibility = View.VISIBLE
        wifi_wizard_captive_portal_back.setOnClickListener {
            wifi_wizard_captive_portal_spinner_view.visibility = View.GONE
            finish()
        }

        wifi_wizard_captive_retry_button.setOnClickListener {
            checkInternetFullyConnected()
        }
    }

    private fun initWebViewChecker(captivePortalUrl: String?) {
        val webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                wifi_wizard_captive_portal_spinner_view.visibility = View.GONE
                if (!isFirstLoad) {
                    checkInternetFullyConnected()
                } else {
                    isFirstLoad = false
                }
            }
        }

        wifi_wizard_captive_portal_webview.setWebViewClient(webViewClient)
        wifi_wizard_captive_portal_webview.loadUrl(captivePortalUrl)
    }

    private fun checkInternetFullyConnected() {
        checkForCaptivePortal(getDefaultRetrofitClient(), object : CaptivePortalDetectionListener {
            override fun noCaptivePortalDetected() {
                Toast.makeText(applicationContext, R.string.connected_text, Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            }

            override fun captivePortalDetected(captivePortalUrl: String) {
                Toast.makeText(applicationContext, R.string.not_connected_try_again, Toast.LENGTH_LONG).show()
            }

            override fun captivePortalDetectionFailure() {}
        })
    }
}