package com.chau.iot.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.chau.iot.MqttHelper
import com.chau.iot.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var mqttHelper: MqttHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        // Khởi tạo MQTT Helper
        mqttHelper = MqttHelper(requireContext())

        // Đăng ký sự kiện khi bấm các button
        binding.btnTien.setOnClickListener {
            mqttHelper.publishMessage("tien") // Gửi tín hiệu 'tien'
        }

        binding.btnLui.setOnClickListener {
            mqttHelper.publishMessage("lui") // Gửi tín hiệu 'lui'
        }

        binding.btnTrai.setOnClickListener {
            mqttHelper.publishMessage("trai") // Gửi tín hiệu 'trai'
        }

        binding.btnPhai.setOnClickListener {
            mqttHelper.publishMessage("phai") // Gửi tín hiệu 'phai'
        }

        binding.btnCoi.setOnClickListener {
            mqttHelper.publishMessage("coi") // Gửi tín hiệu 'coi'
        }

        binding.sDen.setOnCheckedChangeListener { _, isChecked ->
            val message = if (isChecked) "den_on" else "den_off"
            mqttHelper.publishMessage(message) // Gửi tín hiệu bật/tắt đèn
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
