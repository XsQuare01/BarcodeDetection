package com.example.mlkittest

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.example.mlkittest.databinding.ActivityMain3Binding

class MainActivity3 : AppCompatActivity() {
    private lateinit var binding: ActivityMain3Binding
    private val activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()){
        if(it.resultCode == Activity.RESULT_OK){    // Barcode 성공적 인식
            val tmpData = it.data?.getStringExtra("productName")
            Toast.makeText(this, "Product Name: $tmpData", Toast.LENGTH_SHORT).show()
            binding.productNameEditText.setText(tmpData)

            // 바코드로 상품 이름을 받으면 수정 불가능!
            binding.productNameEditText.isFocusable = false
        }
        else if(it.resultCode == Activity.RESULT_CANCELED){ // Barcode 인식 X
            Toast.makeText(this, "텍스트 입력으로 전환", Toast.LENGTH_SHORT).show()

        }
    }
    private var myDBHelper:MyDBHelper = MyDBHelper(this)
    private var multipleTextWatcherFlag = arrayOf(false, false, false)

    // 냉장고 db 가져와서 넣어줄 예정
    var items = ArrayList<Fridge>()
    lateinit var myAdapter:ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMain3Binding.inflate(layoutInflater)
        setContentView(binding.root)
        initDB()
        initLayout()

    }

    private fun initLayout() {
        var pName: String = ""    // 상품 이름
        var fId: Int = 0         // 냉장고 번호
        var pQuantity: Int = 0  // 상품 수량
        var expDate: Int = 0    // 유통 기한

        binding.apply {

            // 일단 비활성화
            okButton.isEnabled = false

            // 바코드 or txt
            insertButton.setOnClickListener {
                callInputTypeAlertDlg()
            }


            // 상품 이름 입력
            productNameEditText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                }

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                }

                override fun afterTextChanged(p0: Editable?) {
                    if (p0.toString().isNotEmpty()) {
                        multipleTextWatcherFlag[0] = true
                    }

                    // 전부 다 true -> is_enable
                    if (multipleTextWatcherFlag[0] && multipleTextWatcherFlag[1] && multipleTextWatcherFlag[2]) {
                        okButton.isEnabled = true
                    }
                }

            })

            // 상품 수량 입력
            quantityEditText.addTextChangedListener(object : TextWatcher {
//                var result: String = ""
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                }

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
//                    result = getString(R.string.input_lesson_quantity, p0.toString())
//                    expEditText.setText(result)
                }

                override fun afterTextChanged(p0: Editable?) {


                    if (p0.toString().isNotEmpty()) {
                        multipleTextWatcherFlag[1] = true
                    }

                    if (multipleTextWatcherFlag[0] && multipleTextWatcherFlag[1] && multipleTextWatcherFlag[2]) {
                        okButton.isEnabled = true
                    }

                }

            })

            // 상품 유통기한 입력력
            expEditText.addTextChangedListener(object : TextWatcher {
//                var result: String = ""
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                }

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                }

                override fun afterTextChanged(p0: Editable?) {
                    // 단위 설정
//                    result = getString(R.string.input_lesson_date, p0.toString())
//                    expEditText.setText(result)

                    if (p0.toString().isNotEmpty()) {
                        multipleTextWatcherFlag[2] = true
                    }

                    if (multipleTextWatcherFlag[0] && multipleTextWatcherFlag[1] && multipleTextWatcherFlag[2]) {
                        okButton.isEnabled = true
                    }

                }

            })

            myAdapter = ArrayAdapter<String>(this@MainActivity3, android.R.layout.simple_spinner_dropdown_item)

            // 냉장고 db에 대한 정보를 가져오기.
            myDBHelper.getTNameRecords()
            for(i:Int in 0 until items.size) {
                myAdapter.add(items[i].fname)
            }
            spinner.adapter = myAdapter

            // 냉장고 spinner
            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    p0: AdapterView<*>?,
                    p1: View?,
                    position: Int,
                    id: Long
                ) {
                    // spinner 의 item 이 선택되었을 때, 해당 position 에 해당하는 name, pid 가져와야 함.
                    fId = items[position].fid

                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    // 0번째 가져오면 안되나?
                }

            }

            okButton.setOnClickListener {
                val product = Product(0, fId, pName, pQuantity, expDate)
                if(myDBHelper.insertProduct(product)){
                    Log.i("DB", "insert success")
                }
                else{
                    Log.i("DB", "insert failed")
                }

            }

        }
    }

    // temp db fname 을 읽어들임.
    private fun initTempDB(){
        for(i in 0 until 4){
            val fridge = Fridge(i, "$i 번 냉장고")
            myDBHelper.insertTempProduct(fridge)

        }

    }

    private fun initDB(){
        val dbFile = getDatabasePath("fridgedb.db")
        if(!dbFile.parentFile.exists()){
            dbFile.mkdir()
        }

        if(!dbFile.exists()){
            dbFile.createNewFile()
            initTempDB()
        }

    }

    private fun callInputTypeAlertDlg(){
        val builder = AlertDialog.Builder(this)
        builder.setMessage("어떤 방식을 선택하실 것인가요?")
            .setTitle("상품 추가")
            .setPositiveButton("텍스트 입력"){
                _, _ ->
            }
            .setNegativeButton("바코드 인식"){
                _, _ ->
                // 바코드 인식 화면으로 보냄냄
               var intent = Intent(this, MainActivity::class.java)
                activityResultLauncher.launch(intent)
            }
        val dlg = builder.create()
        dlg.show()
    }




}