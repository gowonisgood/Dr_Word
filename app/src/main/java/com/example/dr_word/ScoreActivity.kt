package com.example.dr_word

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

/** 데이터 모델 */
data class ScoreUser(
    val nickname: String = "",
    val score: Int = 0
)

/** 점수판 행 어댑터 */
class ScoreAdapter(private val items: List<ScoreUser>) :
    RecyclerView.Adapter<ScoreAdapter.ScoreViewHolder>() {

    inner class ScoreViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val imgAvatar: ImageView = v.findViewById(R.id.imgAvatar)
        val tvName:    TextView  = v.findViewById(R.id.tvName)
        val tvScore:   TextView  = v.findViewById(R.id.tvScore)
    }

    override fun onCreateViewHolder(p: ViewGroup, vt: Int): ScoreViewHolder {
        val v = LayoutInflater.from(p.context)
            .inflate(R.layout.activity_score, p, false)
        return ScoreViewHolder(v)
    }

    override fun onBindViewHolder(h: ScoreViewHolder, pos: Int) {
        val user = items[pos]
        h.tvName.text  = user.nickname
        h.tvScore.text = user.score.toString()
        h.imgAvatar.setImageResource(R.drawable.ic_default_doctor)
    }

    override fun getItemCount() = items.size
}

/** 점수판 Activity */
class ScoreActivity : AppCompatActivity() {

    private lateinit var rvScores: RecyclerView
    private val userList = mutableListOf<ScoreUser>()
    private val adapter  = ScoreAdapter(userList)
    private val db       = FirebaseFirestore.getInstance()

    /* 연락처 권한 런처 */
    private val contactsPermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startActivity(Intent(this, FriendsActivity::class.java))
            else Toast.makeText(this, "연락처 권한이 필요합니다", Toast.LENGTH_SHORT).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scoreboard)

        /* RecyclerView */
        rvScores = findViewById(R.id.rvScores)
        rvScores.layoutManager = LinearLayoutManager(this)
        rvScores.adapter = adapter

        /* Firestore 점수 내림차순 로드 */
        db.collection("login")
            .orderBy("score", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snap ->
                userList.clear()
                for (doc in snap) {
                    val nick = doc.getString("nickname") ?: ""
                    val score = doc.getLong("score")?.toInt() ?: 0
                    userList.add(ScoreUser(nick, score))
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "점수 불러오기 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }

        /* 툴바 네비게이션 아이콘 → 친구 목록 */
        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.READ_CONTACTS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                startActivity(Intent(this, FriendsActivity::class.java))
            } else {
                contactsPermLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
        }

        findViewById<ImageView>(R.id.iv_back).setOnClickListener {
            setResult(Activity.RESULT_OK)   // scoreLauncher로 열었다면 결과 전달
            finish()                        // 단순 startActivity였다면 이 한 줄이면 충분
        }
    }

}
