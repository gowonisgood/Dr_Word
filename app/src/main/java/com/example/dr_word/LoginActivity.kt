package com.example.dr_word

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {
    private lateinit var etNickname: EditText
    private lateinit var etPassword: EditText
    private lateinit var ivLogin: ImageView

    // Firestore 인스턴스
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etNickname = findViewById(R.id.et_nickname)
        etPassword = findViewById(R.id.et_password)
        ivLogin   = findViewById(R.id.iv_login)

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
                            navigateToMain()
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
                                navigateToMain()
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
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
