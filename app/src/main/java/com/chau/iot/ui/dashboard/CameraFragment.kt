package com.chau.iot.ui.dashboard

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.chau.iot.CustomCategory
import com.chau.iot.GestureRecognizerHelper
import com.chau.iot.GestureRecognizerResultsAdapter
import com.chau.iot.MainViewModel
import com.chau.iot.databinding.FragmentCameraBinding
import com.google.mediapipe.tasks.vision.core.RunningMode
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import com.chau.iot.MqttHelper

class CameraFragment : Fragment(), GestureRecognizerHelper.GestureRecognizerListener {

    companion object {
        private const val TAG = "HandGestureRecognizer"
    }

    private var _fragmentCameraBinding: FragmentCameraBinding? = null
    private val fragmentCameraBinding get() = _fragmentCameraBinding!!

    private lateinit var gestureRecognizerHelper: GestureRecognizerHelper
    private val viewModel: MainViewModel by activityViewModels()
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraFacing = CameraSelector.LENS_FACING_FRONT

    private lateinit var backgroundExecutor: ExecutorService
    private lateinit var mqttHelper: MqttHelper  // Thêm MqttHelper để gửi tín hiệu

    // Biến lưu hành động trước đó
    private var previousGesture: String? = null

    // Initialize gestureRecognizerResultAdapter
    private val gestureRecognizerResultAdapter: GestureRecognizerResultsAdapter by lazy {
        GestureRecognizerResultsAdapter().apply {
            updateAdapterSize(1) // Adjust the size as needed
        }
    }

    override fun onResume() {
        super.onResume()
        if (!PermissionsFragment.hasPermissions(requireContext())) {
            Toast.makeText(requireContext(), "Camera permission not granted", Toast.LENGTH_SHORT).show()
            return
        }

        backgroundExecutor.execute {
            if (gestureRecognizerHelper.isClosed()) {
                gestureRecognizerHelper.setupGestureRecognizer()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (this::gestureRecognizerHelper.isInitialized) {
            viewModel.setMinHandDetectionConfidence(gestureRecognizerHelper.minHandDetectionConfidence)
            viewModel.setMinHandTrackingConfidence(gestureRecognizerHelper.minHandTrackingConfidence)
            viewModel.setMinHandPresenceConfidence(gestureRecognizerHelper.minHandPresenceConfidence)
            viewModel.setDelegate(gestureRecognizerHelper.currentDelegate)

            backgroundExecutor.execute { gestureRecognizerHelper.clearGestureRecognizer() }
        }
    }

    override fun onDestroyView() {
        _fragmentCameraBinding = null
        super.onDestroyView()
        backgroundExecutor.shutdown()
        backgroundExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentCameraBinding = FragmentCameraBinding.inflate(inflater, container, false)
        return fragmentCameraBinding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(fragmentCameraBinding.recyclerviewResults) {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = gestureRecognizerResultAdapter
        }

        backgroundExecutor = Executors.newSingleThreadExecutor()

        // Khởi tạo MQTT Helper
        mqttHelper = MqttHelper(requireContext())

        fragmentCameraBinding.viewFinder.post {
            setUpCamera()
        }

        backgroundExecutor.execute {
            gestureRecognizerHelper = GestureRecognizerHelper(
                context = requireContext(),
                runningMode = RunningMode.LIVE_STREAM,
                minHandDetectionConfidence = viewModel.currentMinHandDetectionConfidence,
                minHandTrackingConfidence = viewModel.currentMinHandTrackingConfidence,
                minHandPresenceConfidence = viewModel.currentMinHandPresenceConfidence,
                currentDelegate = viewModel.currentDelegate,
                gestureRecognizerListener = this
            )
        }
    }

    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: throw IllegalStateException("Camera initialization failed.")
        val cameraSelector = CameraSelector.Builder().requireLensFacing(cameraFacing).build()

        preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
            .build()

        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
            .also {
                it.setAnalyzer(backgroundExecutor) { image ->
                    recognizeHand(image)
                }
            }

        cameraProvider.unbindAll()

        try {
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            preview?.setSurfaceProvider(fragmentCameraBinding.viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun recognizeHand(imageProxy: ImageProxy) {
        gestureRecognizerHelper.recognizeLiveStream(imageProxy = imageProxy)
    }

//    override fun onResults(resultBundle: GestureRecognizerHelper.ResultBundle) {
//        activity?.runOnUiThread {
//            if (_fragmentCameraBinding != null) {
//                val gestureCategories = resultBundle.results.first().gestures()
//                if (gestureCategories.isNotEmpty()) {
//                    gestureRecognizerResultAdapter.updateResults(gestureCategories.first())
//                } else {
//                    gestureRecognizerResultAdapter.updateResults(emptyList())
//                }
//                fragmentCameraBinding.overlay.setResults(
//                    resultBundle.results.first(),
//                    resultBundle.inputImageHeight,
//                    resultBundle.inputImageWidth,
//                    RunningMode.LIVE_STREAM
//                )
//                fragmentCameraBinding.overlay.invalidate()
//            }
//        }
//    }



    override fun onResults(resultBundle: GestureRecognizerHelper.ResultBundle) {
        activity?.runOnUiThread {
            if (_fragmentCameraBinding != null) {
                val gestureCategories = resultBundle.results.first().gestures()
                if (gestureCategories.isNotEmpty()) {
                    val firstGesture = gestureCategories.first().first()
                    val gestureName = firstGesture.categoryName()
                    // Chuyển đổi tên cử chỉ thành hành động mong muốn
                    val customGestureName = when (gestureName) {
                        "Open_Palm" -> "Tiến"
                        "Closed_Fist" -> "Lùi"
                        "Pointing_Up" -> "Trái"
                        "Victory" -> "Phải"
                        "Thumb_Up" -> "Bật đèn"
                        "Thumb_Down" -> "Tắt đèn"
                        "None" -> "None"
                        else -> gestureName
                    }

                    // Chỉ gửi tín hiệu khi hành động hiện tại khác hành động trước đó
                    if (customGestureName != previousGesture) {
                        when (customGestureName) {
                            "Tiến" -> mqttHelper.publishMessage("tien")
                            "Lùi" -> mqttHelper.publishMessage("lui")
                            "Trái" -> mqttHelper.publishMessage("trai")
                            "Phải" -> mqttHelper.publishMessage("phai")
                            "Bật đèn" -> mqttHelper.publishMessage("den_on")
                            "Tắt đèn" -> mqttHelper.publishMessage("den_off")
                        }
                        // Cập nhật hành động trước đó
                        previousGesture = customGestureName
                    }

                    val customCategory = CustomCategory(customGestureName, firstGesture.score())
                    gestureRecognizerResultAdapter.updateResults(listOf(customCategory))
                } else {
                    gestureRecognizerResultAdapter.updateResults(emptyList())
                }
                fragmentCameraBinding.overlay.setResults(
                    resultBundle.results.first(),
                    resultBundle.inputImageHeight,
                    resultBundle.inputImageWidth,
                    RunningMode.LIVE_STREAM
                )
                fragmentCameraBinding.overlay.invalidate()
            }
        }
    }



    override fun onError(error: String, errorCode: Int) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
        }
    }
}
