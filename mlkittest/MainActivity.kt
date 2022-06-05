package com.example.mlkittest

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.mlkittest.databinding.ActivityMainBinding
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import org.jsoup.select.Elements
import java.lang.Exception
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private val scope = CoroutineScope(Dispatchers.IO)
    lateinit var cameraProvider: ProcessCameraProvider
    lateinit var barcodeDetection: ImageAnalysis
    var permissions =
        arrayOf(
            Manifest.permission.CAMERA,
        )

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { it ->
            if (it) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
                getPermission()
            } else {
                Toast.makeText(this, "어플리케이션 정보 > 권한 에서\n 권한을 허용해 주십시오.", Toast.LENGTH_LONG).show()

            }
        }

    private lateinit var intentResult: Intent

    // 사진 저장 format + permissions
    companion object {
        private const val TAG = "CameraXApp"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA,
            )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        getPermission()
        init()
        cameraExecutor = Executors.newSingleThreadExecutor()

    }

    private fun init() {
        // intentResult 초기화
        intentResult = Intent()

        binding.apply {
            textButton.setOnClickListener {
                // text 입력으로 전환
                intentResult.putExtra("productName", "none")
                setResult(RESULT_CANCELED, intentResult)
                finish()
            }
        }


    }

    // Camera 초기화
    private fun startCamera() {
        // camera 의 lifecycle 을 bind 함
        val cameraProvideFuture = ProcessCameraProvider.getInstance(this)

        cameraProvideFuture.addListener(
            {
                cameraProvider = cameraProvideFuture.get()

                // 초기화 + build + surfaceProvider 를 얻어옴 (surface: 맨 앞에서 실행되는 view?)
                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                    }

                // barcode 감지하는 부분
                barcodeDetection = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)   //?
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, ImageAnalyzer())
                    }

                // imageCapture 실행
                imageCapture = ImageCapture.Builder().build()

                // default camera: back camera
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()

                    // 사진을 찍도록 앱을 구성함..? 몰?루
                    // camera 객체를 반환함.
                    cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageCapture, barcodeDetection
                    )

                } catch (exc: Exception) {
                    // app 이 더이상 focus 를 가지지 않을 때..
                    Log.i(TAG, "Binding failed", exc)

                }
            }, ContextCompat.getMainExecutor(this)  // main thread

        )
    }


    // 모든 permission 이 승인되었는지 확인
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    // camera 종료를 위해
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }


    // permission request
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                // 직접 구현할 때는 전에 했던 것처럼 알림 메세지를 띄워주는 것이 좋다고 생각함.
                Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show()
                callPermissionDlg()

            }
        }
    }

    private fun getPermission() {
        when {
            (ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED)
            -> {
                startCamera()
            }
            (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.CAMERA
            ))
            -> {
                callPermissionDlg()

            }
            else
            -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    // API 접근해서 가져옴
    private fun getXMLData(productID: String): Boolean {
        var pName = ""
        scope.launch {
            lateinit var doc: Document

            // API 가 1000개 단위로 호출
            searchloop@ for (i: Int in 0..250000 step (1000)) {
                doc =
                    Jsoup.connect("https://openapi.foodsafetykorea.go.kr/api/cb3605d6af0442069388/C005/xml/${i + 1}/${i + 1000}/BAR_CD=${productID}")
                        .parser(Parser.xmlParser()).get()

                val resultCode: Elements = doc.select("RESULT")
                // 해당 page 에 바코드가 없을 때
                if (resultCode.select("CODE").toString() == "INFO-200") {
                    continue
                } else {
                    val productNames = doc.select("PRDLST_NM")
                    for (tmp in productNames) {
                        withContext(Dispatchers.Main) {
                            pName = tmp.text()
//                            binding.textView2.text = pName
                            Log.i("XML Read", pName)

                        }

                    }
                    intentResult.putExtra("productName", pName)
                    setResult(RESULT_OK, intentResult)
                    finish()
                    break@searchloop

                }

            }


        }

        return true

    }

    @SuppressLint("UnsafeOptInUsageError")
    inner class ImageAnalyzer() : ImageAnalysis.Analyzer {
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                // mediaImage 로부터 capture 한 image 를 가져오는 과정?
                val image =
                    InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                Log.i("ImageAnalyzer", "Processing..!")

                val options = BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(
                        Barcode.FORMAT_EAN_13,
                    )
                    .build()

                // option 을 줄 수도 있음
                val scanner = BarcodeScanning.getClient(options)

                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        Log.i("Barcode Scanner", "Detecting..!")

                        for (barcode in barcodes) {
                            val bounds = barcode.boundingBox
                            val corners = barcode.cornerPoints
                            val format = barcode.format //EAN_13

                            val id = barcode.rawValue!!.toString()
                            val value = barcode.valueType   // TYPE_PRODUCT
                            Log.i(
                                "Barcode Scanner",
                                "Success!: id: $id, valueType: $value format: $format"
                            )

//                            binding.textView.text = id
                            if (getXMLData(id)) {
                                cameraProvider.unbind(barcodeDetection)
                            }

                        }
                    }
                    .addOnFailureListener {
                        Log.i("Barcode Scanner", "failed")

                    }
                    .addOnCompleteListener {
                        imageProxy.image?.close()
                        imageProxy.close()

                    }
            }
        }
    }

    private fun callPermissionDlg() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("앱 실행을 위해 권한이 필요합니다")
            .setTitle("권한 요청")
            .setPositiveButton("허용") { _, _ ->
                // 권한 다시 요청
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            .setNegativeButton("거부") { dlg, _ ->
                dlg.dismiss()
                // 바코드 인식 화면으로 보냄냄

            }
        val dlg = builder.create()
        dlg.show()
    }

}



