package com.example.dr_word

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {
    private lateinit var etNickname: EditText
    private lateinit var etPassword: EditText
    private lateinit var ivLogin: ImageView
    private lateinit var ivSettings: ImageView /* setting */
    private lateinit var tvWelcome: TextView /* GO : 환영 메시지용 */
    private lateinit var ivFavorite: ImageView
    // Firestore 인스턴스
    private val db = FirebaseFirestore.getInstance()

    /* GO : 양방향 엑티비티 */
    // 1) SettingsActivity에서 돌아올 때 결과를 받을 Launcher 선언
    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            data?.let {
                val newNickname = it.getStringExtra("RESULT_NICKNAME")
                val newPassword = it.getStringExtra("RESULT_PASSWORD")

                // 받아온 값으로 EditText 업데이트
                if (!newNickname.isNullOrEmpty()) {
                    etNickname.setText(newNickname)
                }
                if (!newPassword.isNullOrEmpty()) {
                    etPassword.setText(newPassword)
                }
                Toast.makeText(this, "설정에서 변경된 값을 반영했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private var hasWelcomed = false /* GO : 첫 로그인 후 환영 메시지 상태 */

    // 2) MainActivity에서 돌아올 때 결과를 받을 Launcher 선언 (별도 런처)
    private val mainLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            data?.let {
                val returnedNick = it.getStringExtra("RETURN_NICKNAME")
                val returnedPwd = it.getStringExtra("RETURN_PASSWORD")
                if (!returnedNick.isNullOrEmpty()) {
                    etNickname.setText(returnedNick)
                }
                if (!returnedPwd.isNullOrEmpty()) {
                    etPassword.setText(returnedPwd)
                }
                Toast.makeText(this, "MainActivity에서 변경된 값을 반영했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }
    /* GO : 양방향 엑티비티 end */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etNickname = findViewById(R.id.et_nickname)
        etPassword = findViewById(R.id.et_password)
        ivLogin   = findViewById(R.id.iv_login)
        ivSettings = findViewById(R.id.iv_settings) /* setting */
        tvWelcome  = findViewById(R.id.tv_welcome) /* welcome */
        ivFavorite = findViewById(R.id.iv_favorite) /* score */

        /* GO : game */
        ivLogin.setOnClickListener {
            val nickname = etNickname.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (nickname.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "닉네임과 비밀번호를 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (hasWelcomed) {  // GO: 두 번째 클릭부터는 MainActivity로 이동
                navigateToMain(nickname, password)
                return@setOnClickListener
            }

            // 컬렉션 "login" -> 문서 ID = nickname
            val docRef = db.collection("login").document(nickname)
            docRef.get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        // 기존 사용자: 필드 "password" 비교
                        val storedPwd = doc.getString("password")
                        if (storedPwd == password) {
                            showWelcome(nickname)  // GO: 첫 로그인 시 환영 메시지
                            hasWelcomed = true
                        } else {
                            Toast.makeText(this, "비밀번호가 틀렸습니다", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // 신규 사용자: 문서 생성 후 로그인
                        val user = hashMapOf(
                            "nickname" to nickname,
                            "password" to password
                        )
                        docRef.set(user)
                            .addOnSuccessListener {
                                Toast.makeText(this, "회원가입 및 로그인 성공", Toast.LENGTH_SHORT).show()
                                showWelcome(nickname)  // GO: 신규 가입 후 환영 메시지
                                hasWelcomed = true

                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "회원가입 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "DB 오류: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
        /* GO : game end */

        /* GO : setting */
        ivSettings.setOnClickListener {
            val nickname = etNickname.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (nickname.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "설정을 보려면 먼저 닉네임과 비밀번호를 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 설정 화면으로 이동하면서 닉네임과 비밀번호 전달
            val intent = Intent(this, SettingsActivity::class.java).apply {
                putExtra("EXTRA_NICKNAME", nickname)
                putExtra("EXTRA_PASSWORD", password)
            }
            settingsLauncher.launch(intent)
        }
        /* GO : settings end */

        /* GO : go to score board */
        ivFavorite.setOnClickListener {
            // 닉네임·비번 입력 여부와 무관하게 점수판 열고 싶다면 바로 이동
            startActivity(Intent(this, ScoreActivity::class.java))

            // 만약 닉네임이 비어 있으면 막고 싶다면:
            /*
            val nickname = etNickname.text.toString().trim()
            if (nickname.isEmpty()) {
                Toast.makeText(this, "닉네임 입력 후 이용해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(this, ScoreActivity::class.java)
            intent.putExtra("EXTRA_NICKNAME", nickname)   // 필요 시 전달
            startActivity(intent)
            */
        }
        /* GO : go to score board end */

    }

    private fun showWelcome(nickname: String) {
        etNickname.visibility = EditText.GONE
        etPassword.visibility = EditText.GONE
        tvWelcome.text = "환영합니다 $nickname 님"
        tvWelcome.visibility = TextView.VISIBLE
    }

    private fun navigateToMain(nickname: String, password: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("EXTRA_NICKNAME", nickname)
            putExtra("EXTRA_PASSWORD", password)
        }
        mainLauncher.launch(intent)
    }
}
