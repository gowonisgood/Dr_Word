package com.example.dr_word

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

/** 1) 데이터 모델 */
data class ScoreUser(
    val nickname: String = "",
    val score: Int = 0
)

/** 2) 리사이클러뷰 어댑터 */
class ScoreAdapter(private val items: List<ScoreUser>) :
    RecyclerView.Adapter<ScoreAdapter.ScoreViewHolder>() {

    inner class ScoreViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgAvatar: ImageView = itemView.findViewById(R.id.imgAvatar)
        val tvName:   TextView   = itemView.findViewById(R.id.tvName)
        val tvScore:  TextView   = itemView.findViewById(R.id.tvScore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScoreViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_score, parent, false)   // ★ 행 레이아웃 xml 이름
        return ScoreViewHolder(v)
    }

    override fun onBindViewHolder(holder: ScoreViewHolder, position: Int) {
        val user = items[position]
        holder.tvName.text  = user.nickname
        holder.tvScore.text = user.score.toString()
        holder.imgAvatar.setImageResource(R.drawable.ic_default_profile) // 기본 아바타
    }

    override fun getItemCount(): Int = items.size
}

/** 3) 점수판을 보여주는 ScoreActivity */
class ScoreActivity : AppCompatActivity() {

    private lateinit var rvScores: RecyclerView
    private val userList = mutableListOf<ScoreUser>()
    private val adapter  = ScoreAdapter(userList)

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scoreboard)   // 상단 Toolbar + CardView + RecyclerView

        // RecyclerView 설정
        rvScores = findViewById(R.id.rvScores)
        rvScores.layoutManager = LinearLayoutManager(this)
        rvScores.adapter = adapter

        // Firestore에서 score 내림차순으로 가져오기
        db.collection("login")
            .orderBy("score", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                userList.clear()
                for (doc in snapshot) {
                    val nickname = doc.getString("nickname") ?: ""
                    val score    = doc.getLong("score")?.toInt() ?: 0
                    userList.add(ScoreUser(nickname, score))       // ★ 리스트에 추가
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "점수 불러오기 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
