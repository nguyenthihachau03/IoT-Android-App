package com.chau.iot.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import com.chau.iot.MqttHelper
import com.chau.iot.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var mqttHelper: MqttHelper

    private val defaultTemp = "Đang chờ..."
    private val defaultHumidity = "Đang chờ..."
    companion object {
        private var webView: WebView? = null // Giữ WebView ở mức toàn cục để tái sử dụng
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        // Khởi tạo MQTT Helper
        mqttHelper = MqttHelper(requireContext())
        mqttHelper.connect()

        // Khởi tạo giá trị mặc định cho TextView và ẩn các icon
        binding.nNhietdo.text = defaultTemp
        binding.nDoam.text = defaultHumidity
        binding.iconTemperature.visibility = View.GONE
        binding.iconHumidity.visibility = View.GONE

        if (webView == null) {
            setupWebView()
        } else {
            // Di chuyển WebView đã được giữ trạng thái vào lại layout
            (webView?.parent as? ViewGroup)?.removeView(webView)
            binding.webViewContainer.addView(webView)
        }

        // Đặt giá trị mặc định
        binding.nNhietdo.text = defaultTemp
        binding.nDoam.text = defaultHumidity

        // Lắng nghe dữ liệu từ MQTT
        mqttHelper.onTemperatureReceived = { temp ->
            requireActivity().runOnUiThread {
                binding.nNhietdo.text = "$temp °C"
                binding.iconTemperature.visibility = View.VISIBLE
            }
        }

        mqttHelper.onHumidityReceived = { humidity ->
            requireActivity().runOnUiThread {
                binding.nDoam.text = "$humidity %RH"
                binding.iconHumidity.visibility = View.VISIBLE
            }
        }

        // Đăng ký sự kiện nút điều khiển
        setupButtonListeners()

        return binding.root
    }

    private fun setupWebView() {
        webView = WebView(requireContext())
        val webSettings: WebSettings = webView!!.settings
        webSettings.javaScriptEnabled = true
        webSettings.loadWithOverviewMode = true
        webSettings.useWideViewPort = true
        webSettings.builtInZoomControls = true

        webView!!.webViewClient = WebViewClient()
        webView!!.loadUrl("http://192.168.181.139:81/stream") // Địa chỉ ESP32-CAM

        binding.webViewContainer.addView(webView) // Thêm WebView vào container
        binding.webViewContainer.layoutParams.height = (resources.displayMetrics.heightPixels * 0.3).toInt()
    }

    private fun setupButtonListeners() {
        binding.btnTien.setOnClickListener {
            mqttHelper.publishMessage("tien")
        }
        binding.btnLui.setOnClickListener {
            mqttHelper.publishMessage("lui")
        }
        binding.btnTrai.setOnClickListener {
            mqttHelper.publishMessage("trai")
        }
        binding.btnPhai.setOnClickListener {
            mqttHelper.publishMessage("phai")
        }
        binding.btnCoi.setOnClickListener {
            mqttHelper.publishMessage("coi")
        }
        binding.sDen.setOnCheckedChangeListener { _, isChecked ->
            val message = if (isChecked) "den_on" else "den_off"
            mqttHelper.publishMessage(message)
        }
        binding.btnGear0.setOnClickListener {
            mqttHelper.publishMessage("gear0")
        }
        binding.btnGear1.setOnClickListener {
            mqttHelper.publishMessage("gear1")
        }
        binding.btnGear2.setOnClickListener {
            mqttHelper.publishMessage("gear2")
        }
        binding.btnGear3.setOnClickListener {
            mqttHelper.publishMessage("gear3")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onPause() {
        super.onPause()
        webView?.onPause() // Tạm dừng WebView để tiết kiệm tài nguyên
    }

    override fun onResume() {
        super.onResume()
        webView?.onResume() // Tiếp tục WebView khi quay lại Fragment
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isRemoving) {
            webView?.destroy() // Giải phóng WebView nếu Fragment bị xóa hoàn toàn
            webView = null
        }
    }
}
