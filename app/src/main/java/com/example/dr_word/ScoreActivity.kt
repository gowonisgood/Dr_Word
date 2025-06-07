package com.example.dr_word

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ImageView
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
import android.widget.TextView

/* ───── 1) 데이터 모델 ───── */
data class ScoreUser(val nickname: String = "", val score: Int = 0)

/* ───── 2) RecyclerView 어댑터 ───── */
class ScoreAdapter(
    private val items: MutableList<ScoreUser>,
    private val avatarClick: (Int) -> Unit         // 클릭 콜백
) : RecyclerView.Adapter<ScoreAdapter.ScoreVH>() {

    /** 각 포지션별 사용자 지정 아바타 URI or Bitmap */
    private val avatarUri = mutableMapOf<Int, Uri>()
    private val avatarBmp = mutableMapOf<Int, Bitmap>()

    inner class ScoreVH(v: View) : RecyclerView.ViewHolder(v) {
        val img: ImageView = v.findViewById(R.id.imgAvatar)
        val name: TextView = v.findViewById(R.id.tvName)
        val score: TextView = v.findViewById(R.id.tvScore)
        init {
            img.setOnClickListener {
                val pos = absoluteAdapterPosition
                if (pos != RecyclerView.NO_POSITION) avatarClick(pos)
            }
        }
    }

    override fun onCreateViewHolder(p: ViewGroup, vt: Int): ScoreVH =
        ScoreVH(LayoutInflater.from(p.context)
            .inflate(R.layout.activity_score, p, false))

    override fun onBindViewHolder(h: ScoreVH, pos: Int) {
        val u = items[pos]
        h.name.text = u.nickname
        h.score.text = u.score.toString()

        when {
            avatarBmp[pos] != null -> h.img.setImageBitmap(avatarBmp[pos])
            avatarUri[pos] != null -> h.img.setImageURI(avatarUri[pos])
            else -> h.img.setImageResource(R.drawable.ic_default_doctor)
        }
    }

    override fun getItemCount() = items.size

    fun setAvatar(pos: Int, uri: Uri) {
        avatarUri[pos] = uri
        notifyItemChanged(pos)
    }
    fun setAvatar(pos: Int, bmp: Bitmap) {
        avatarBmp[pos] = bmp
        notifyItemChanged(pos)
    }
}

/* ───── 3) ScoreActivity ───── */
class ScoreActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val users = mutableListOf<ScoreUser>()
    private lateinit var adapter: ScoreAdapter
    private var pendingPos = RecyclerView.NO_POSITION   // 클릭한 셀 위치

    /* 갤러리에서 이미지 선택 */
    private val getImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null && pendingPos != RecyclerView.NO_POSITION) {
                // 필요하다면 리사이즈
                val bmp = resizeBitmap(
                    MediaStore.Images.Media.getBitmap(contentResolver, uri),
                    300, 300
                )
                adapter.setAvatar(pendingPos, bmp)      // Bitmap 저장
            }
            pendingPos = RecyclerView.NO_POSITION
        }

    /* 사진 권한 요청 */
    private val imgPerm =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) getImage.launch("image/*")
            else {
                Toast.makeText(this, "사진 접근 권한이 필요합니다", Toast.LENGTH_SHORT).show()
                pendingPos = RecyclerView.NO_POSITION
            }
        }

    /* 연락처 권한 (툴바 네비게이션) */
    private val contactsPerm =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startActivity(Intent(this, FriendsActivity::class.java))
            else Toast.makeText(this, "연락처 권한이 필요합니다", Toast.LENGTH_SHORT).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scoreboard)

        /* RecyclerView 초기화 */
        adapter = ScoreAdapter(users) { pos -> onAvatarClick(pos) }
        findViewById<RecyclerView>(R.id.rvScores).apply {
            layoutManager = LinearLayoutManager(this@ScoreActivity)
            adapter = this@ScoreActivity.adapter
        }

        loadScores()
        initToolbar()
        findViewById<ImageView>(R.id.iv_back).setOnClickListener { finish() }
    }

    /* 아바타 클릭 → 권한 확인 후 갤러리 실행 */
    private fun onAvatarClick(pos: Int) {
        pendingPos = pos
        val perm = if (Build.VERSION.SDK_INT >= 33)
            Manifest.permission.READ_MEDIA_IMAGES
        else Manifest.permission.READ_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(this, perm) ==
            PackageManager.PERMISSION_GRANTED) {
            getImage.launch("image/*")
        } else imgPerm.launch(perm)
    }

    /* Firestore 점수 읽기 */
    private fun loadScores() {
        db.collection("login")
            .orderBy("score", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snap ->
                users.clear()
                for (doc in snap) {
                    users += ScoreUser(
                        doc.getString("nickname") ?: "",
                        doc.getLong("score")?.toInt() ?: 0
                    )
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "점수 불러오기 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /* 툴바 네비게이션 → 연락처 화면 */
    private fun initToolbar() {
        findViewById<MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener {
                val perm = Manifest.permission.READ_CONTACTS
                if (ContextCompat.checkSelfPermission(this, perm) ==
                    PackageManager.PERMISSION_GRANTED
                ) startActivity(Intent(this, FriendsActivity::class.java))
                else contactsPerm.launch(perm)
            }
    }

    /* Bitmap 리사이즈 (슬라이드 예시 차용) */
    private fun resizeBitmap(src: Bitmap, w: Int, h: Int): Bitmap =
        Bitmap.createScaledBitmap(src, w, h, false)
}
