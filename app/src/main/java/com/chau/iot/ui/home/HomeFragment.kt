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

        // Gọi hàm connect() để bắt đầu kết nối
        mqttHelper.connect()

        // Đăng ký sự kiện khi bấm các button điều khiển xe
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

        // Đăng ký sự kiện cho nút điều khiển gear
        binding.btnGear0.setOnClickListener {
            mqttHelper.publishMessage("stop") // Gửi tín hiệu 'stop' khi gear 0
        }

        binding.btnGear1.setOnClickListener {
            mqttHelper.publishMessage("gear1") // Gửi tín hiệu 'gear1'
        }

        binding.btnGear2.setOnClickListener {
            mqttHelper.publishMessage("gear2") // Gửi tín hiệu 'gear2'
        }

        binding.btnGear3.setOnClickListener {
            mqttHelper.publishMessage("gear3") // Gửi tín hiệu 'gear3'
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
