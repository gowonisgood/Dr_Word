package com.example.dr_word

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {
    private lateinit var etNickname: EditText
    private lateinit var etPassword: EditText
    private lateinit var ivLogin: ImageView
    private lateinit var ivSettings: ImageView /* setting */

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

        /* GO : game */
        ivLogin.setOnClickListener {
            val nickname = etNickname.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (nickname.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "닉네임과 비밀번호를 입력해주세요", Toast.LENGTH_SHORT).show()
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
                            navigateToMain(nickname, password)
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
                                navigateToMain(nickname, password)
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
    }

    private fun navigateToMain(nickname: String, password: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("EXTRA_NICKNAME", nickname)
            putExtra("EXTRA_PASSWORD", password)
        }
        mainLauncher.launch(intent)
    }
}
