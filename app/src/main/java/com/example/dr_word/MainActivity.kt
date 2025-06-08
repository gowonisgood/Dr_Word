package com.example.dr_word

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import android.net.Uri
import android.os.CountDownTimer
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import android.os.Handler
import org.tensorflow.lite.Interpreter
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.io.FileInputStream

class MainActivity : AppCompatActivity() {
    private val apiKey = "AIzaSyBV0IeygL1W1B2RkP4WWbolzdCowQO3x6Q"  // Gemini API Key
    private var wordList: List<String> = emptyList()    // Gemini로 생성된 출제 단어 목록
    private lateinit var imageView: ImageView
    private lateinit var editText: EditText
    private lateinit var scoreText: TextView
    private lateinit var submitButton: Button
    private lateinit var naverApi: NaverImageSearchApi
    private lateinit var nickname: String


    private var score: Int = 0
    private var currentWordIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Login Acivity에서 intent로 넘겨준 회원 정보를 가져옴
        nickname = intent.getStringExtra("EXTRA_NICKNAME") ?: "defaultNickName"

        imageView = findViewById<ImageView>(R.id.iv_question_image)
        editText = findViewById<EditText>(R.id.et_answer_input)
        scoreText = findViewById<TextView>(R.id.tv_current_score)
        submitButton = findViewById<Button>(R.id.tv_title)


        val retrofit = Retrofit.Builder()
            .baseUrl("https://openapi.naver.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        naverApi = retrofit.create(NaverImageSearchApi::class.java)

        startGameTimer()

        // Gemini AI API에서 단어 목록을 가져옴
        // 코루틴 사용으로 비동기적으로 API 호출을 수행해 UI 스레드를 블로킹하지 않고 작업을 수행
        CoroutineScope(Dispatchers.Main).launch {
            wordList = fetchGeminiWords(apiKey)  // suspend 함수, Gemini 단어 받아옴
            println(wordList)

            if (wordList.isNotEmpty()) {
                loadImageForCurrentWord()
            } else {
                Toast.makeText(this@MainActivity, "단어 로딩 실패", Toast.LENGTH_SHORT).show()
            }
        }

        submitButton.setOnClickListener {
            val input = editText.text.toString().trim()
            val answer = wordList.getOrNull(currentWordIndex) ?: return@setOnClickListener

            if (input == answer) {
                score += 10
                scoreText.text = score.toString()
                currentWordIndex++
                if (currentWordIndex < wordList.size) {
                    editText.text.clear()
                    loadImageForCurrentWord()
                } else {
                    Toast.makeText(this, "퀴즈 종료! 점수: $score", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "오답입니다.", Toast.LENGTH_SHORT).show()
            }
        }

        val btnHint = findViewById<Button>(R.id.btn_hint)

        // 모델 로딩
        val fileDescriptor = assets.openFd("mobilenet_v2_1.0_224.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val model = fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.startOffset, fileDescriptor.declaredLength)
        val interpreter = Interpreter(model)

        // 라벨 로딩
        val labels = assets.open("labels.txt").bufferedReader().useLines { it.toList() }

        btnHint.setOnClickListener {
            // 1. 이미지 추출 및 224x224 크기로 리사이즈
            imageView.isDrawingCacheEnabled = true
            imageView.buildDrawingCache()
            val bitmap = Bitmap.createScaledBitmap(imageView.drawingCache, 224, 224, true)
            imageView.isDrawingCacheEnabled = false

            // 2. Bitmap -> Float 배열로 변환 (모델이 float32 0~1 범위 입력 예상)
            val inputSize = 224
            val imgData = FloatArray(inputSize * inputSize * 3)  // RGB 3채널

            val intValues = IntArray(inputSize * inputSize)
            bitmap.getPixels(intValues, 0, inputSize, 0, 0, inputSize, inputSize)

            for (i in intValues.indices) {
                val pixel = intValues[i]
                // ARGB -> R, G, B 분리 + 0~1 정규화
                imgData[i * 3 + 0] = ((pixel shr 16) and 0xFF) / 255.0f
                imgData[i * 3 + 1] = ((pixel shr 8) and 0xFF) / 255.0f
                imgData[i * 3 + 2] = (pixel and 0xFF) / 255.0f
            }

            // 3. FloatArray -> ByteBuffer 생성 (native order)
            val byteBuffer = java.nio.ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3)
            byteBuffer.order(java.nio.ByteOrder.nativeOrder())
            for (value in imgData) {
                byteBuffer.putFloat(value)
            }
            byteBuffer.rewind()

            // 4. 출력 버퍼 준비
            val outputSize = 1001  // mobilenet_v2 라벨 개수
            val outputBuffer = Array(1) { FloatArray(outputSize) }  // 2D 배열 [1][1001]

            // 5. 추론 수행
            interpreter.run(byteBuffer, outputBuffer)

            // 6. 결과 해석
            val probabilities = outputBuffer[0]
            val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: -1
            val resultLabel = if (maxIndex in labels.indices) labels[maxIndex] else "알 수 없음"

            // 7. 토스트에 힌트표시
            Toast.makeText(this@MainActivity, "추론 결과: $resultLabel", Toast.LENGTH_SHORT).show()
        }

    }

    private fun startGameTimer() {
        val gameDuration = 60_000L // 90초 (밀리초 단위)

        object : CountDownTimer(gameDuration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val timerTextView: TextView = findViewById<TextView>(R.id.tv_remaining_time)
                val secondsLeft = millisUntilFinished / 1000
                timerTextView.text = " ${secondsLeft}초"
            }

            override fun onFinish() {
                Toast.makeText(this@MainActivity, "게임 종료! 최종 점수: $score", Toast.LENGTH_LONG).show()
                updateUserScore(nickname, score)

                submitButton.isEnabled = false
                editText.isEnabled = false
                imageView.visibility = View.GONE

                // 1초 후 로그인 화면으로 이동
                Handler(Looper.getMainLooper()).postDelayed({
                    val intent = Intent(this@MainActivity, LoginActivity::class.java)
                    startActivity(intent)
                    finish()  // 현재 액티비티 종료
                }, 1000)
            }
        }.start()
    }

    fun updateUserScore(nickname: String, newScore: Int) {
        val db = Firebase.firestore
        val docRef = db.collection("login").document(nickname)

        docRef.get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    // 기존 점수 읽어서 업데이트
                    val currentScore = doc.getLong("score") ?: 0L

                    var updatedScore: Int = 0
                    if (currentScore < newScore) {
                        updatedScore = newScore
                    }

                    docRef.update("score", updatedScore)
                        .addOnSuccessListener {
                            Toast.makeText(this, "점수 업데이트 완료! 총 점수: $updatedScore", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "점수 업데이트 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "DB 오류: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadImageForCurrentWord() {
        val word = wordList[currentWordIndex]
        CoroutineScope(Dispatchers.Main).launch {
            val response = fetchNaverImages(naverApi, word, "YL7939UnDI0dqC4u01fQ", "6f6Odzw1ci")
            if (response?.items?.isNotEmpty() == true) {
                val rawUrl = response.items[0].thumbnail
                val parsedUrl = Uri.parse(rawUrl).getQueryParameter("src") ?: rawUrl

                Glide.with(this@MainActivity)
                    .load(parsedUrl)
                    .format(DecodeFormat.PREFER_ARGB_8888)
                    .fitCenter()
                    .into(imageView)
            } else {
                Toast.makeText(this@MainActivity, "이미지를 찾을 수 없음", Toast.LENGTH_SHORT).show()
            }
        }
    }

    suspend fun fetchGeminiWords(apiKey: String): List<String> {
        val generativeModel = GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = apiKey
        )
        val prompt = "아무런 추가 설명없이 동물 단어 10개 생성해"
        val response = generativeModel.generateContent(prompt)
        val text = response.text ?: ""
        return text.split(",").map { it.trim() }
    }

    suspend fun fetchNaverImages(
        api: NaverImageSearchApi,
        query: String,
        clientId: String,
        clientSecret: String
    ): NaverImageResponse? {
        return try {
            val response = api.searchImages(query, clientId, clientSecret)
            if (response.isSuccessful) {
                response.body()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}



//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//            insets
//        }