package com.example.dr_word

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class SettingsActivity : AppCompatActivity() {
    private lateinit var etNickname: EditText
    private lateinit var etPassword: EditText

    // Firestore 인스턴스
    private val db = FirebaseFirestore.getInstance()

    // 원래 LoginActivity에서 전달된 닉네임/비밀번호
    private var originalNickname: String? = null
    private var originalPassword: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change) // XML 파일 이름

        etNickname = findViewById(R.id.nickname)
        etPassword = findViewById(R.id.password)

        // Intent로부터 원래 값 받아오기
        originalNickname = intent.getStringExtra("EXTRA_NICKNAME")
        originalPassword = intent.getStringExtra("EXTRA_PASSWORD")

        // 받아온 값이 null이 아니라면 EditText에 초기 세팅
        originalNickname?.let { etNickname.setText(it) }
        originalPassword?.let { etPassword.setText(it) }
    }

    // XML의 onClick="onSaveProfile"과 연결되는 메서드
    fun onSaveProfile(view: View) {
        val newNickname = etNickname.text.toString().trim()
        val newPassword = etPassword.text.toString().trim()

        // 입력값 검사
        if (newNickname.isEmpty() || newPassword.isEmpty()) {
            Toast.makeText(this, "닉네임과 비밀번호를 모두 입력해주세요", Toast.LENGTH_SHORT).show()
            return
        }

        // originalNickname이 null일 리는 없지만 안전하게 null 체크
        val oldNickname = originalNickname ?: return

        // 1) 닉네임이 그대로인 경우 → 기존 문서 업데이트
        if (newNickname == oldNickname) {
            // Firestore에서 "login" 컬렉션 내 문서 ID = oldNickname
            db.collection("login")
                .document(oldNickname)
                .update("password", newPassword)
                .addOnSuccessListener {
                    // 변경 성공 시 LoginActivity로 결과 반환
                    val resultIntent = Intent().apply {
                        putExtra("RESULT_NICKNAME", newNickname)
                        putExtra("RESULT_PASSWORD", newPassword)
                    }
                    setResult(Activity.RESULT_OK, resultIntent)
                    Toast.makeText(this, "비밀번호가 업데이트되었습니다", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "비밀번호 업데이트 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                }

        } else {
            // 2) 닉네임이 바뀐 경우 → 새 문서 생성 후 기존 문서 삭제
            // 새로 저장할 데이터 맵
            val updatedUser = hashMapOf(
                "nickname" to newNickname,
                "password" to newPassword
            )

            // 2-1) 새 문서 생성 (Document ID = newNickname)
            db.collection("login")
                .document(newNickname)
                .get()
                .addOnSuccessListener { existingDoc ->
                    if (existingDoc.exists()) {
                        // 이미 같은 닉네임이 있는 경우
                        Toast.makeText(this, "이미 사용 중인 닉네임입니다", Toast.LENGTH_SHORT).show()
                    } else {
                        // 2-2) 새 문서에 데이터 저장
                        db.collection("login")
                            .document(newNickname)
                            .set(updatedUser)
                            .addOnSuccessListener {
                                // 2-3) 기존 문서 삭제
                                db.collection("login")
                                    .document(oldNickname)
                                    .delete()
                                    .addOnSuccessListener {
                                        // 변경 완료 후 LoginActivity로 결과 반환
                                        val resultIntent = Intent().apply {
                                            putExtra("RESULT_NICKNAME", newNickname)
                                            putExtra("RESULT_PASSWORD", newPassword)
                                        }
                                        setResult(Activity.RESULT_OK, resultIntent)
                                        Toast.makeText(this, "닉네임 및 비밀번호가 업데이트되었습니다", Toast.LENGTH_SHORT).show()
                                        finish()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(this, "기존 계정 삭제 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "새 계정 생성 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "닉네임 중복 검사 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
