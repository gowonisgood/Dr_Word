package com.example.dr_word

import android.os.Bundle
import android.provider.ContactsContract
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

/* 연락처 모델 */
data class Friend(val name: String, val phone: String)

/* 어댑터 */
class FriendAdapter(private val friends: List<Friend>) :
    RecyclerView.Adapter<FriendAdapter.FriendVH>() {

    inner class FriendVH(v: View) : RecyclerView.ViewHolder(v) {
        val tvName:  TextView = v.findViewById(R.id.tvName)
        val tvPhone: TextView = v.findViewById(R.id.tvPhone)
    }

    override fun onCreateViewHolder(p: ViewGroup, vt: Int): FriendVH {
        val v = LayoutInflater.from(p.context)
            .inflate(R.layout.item_friend, p, false)
        return FriendVH(v)
    }

    override fun onBindViewHolder(h: FriendVH, pos: Int) {
        val f = friends[pos]
        h.tvName.text  = f.name
        h.tvPhone.text = f.phone
    }

    override fun getItemCount() = friends.size
}

/* 친구 목록 Activity */
class FriendsActivity : AppCompatActivity() {

    private val friendList = mutableListOf<Friend>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friends)

        val rv = findViewById<RecyclerView>(R.id.rvFriends).apply {
            layoutManager = LinearLayoutManager(this@FriendsActivity)
            adapter = FriendAdapter(friendList)
        }

        loadContacts()
        rv.adapter?.notifyDataSetChanged()
    }

    private fun loadContacts() {
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null, null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )

        cursor?.use {
            val nameIdx  = it.getColumnIndex(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
            )
            val phoneIdx = it.getColumnIndex(
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            while (it.moveToNext()) {
                val name  = it.getString(nameIdx)
                val phone = it.getString(phoneIdx)
                friendList.add(Friend(name, phone))
            }
        }
    }
}
