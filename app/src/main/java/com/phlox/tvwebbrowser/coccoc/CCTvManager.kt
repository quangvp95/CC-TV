package com.phlox.tvwebbrowser.coccoc

import android.text.TextUtils
import android.util.Log
import com.google.firebase.database.*
import com.google.firebase.messaging.FirebaseMessaging
import com.phlox.tvwebbrowser.TVBro
import com.phlox.tvwebbrowser.model.CcPushedLink

class CCTvManager {
    val config = TVBro.config

    init {
        observerFirebaseMessaging()
        observerFirebaseDB()
    }

    companion object {
        private const val PATH: String = "cctv"
        val instance: CCTvManager by lazy { CCTvManager() }
        private val firebaseDB: FirebaseDatabase by lazy { FirebaseDatabase.getInstance() }
        val ref: DatabaseReference by lazy { firebaseDB.getReference(PATH) }
    }

    private fun observerFirebaseMessaging() {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            updateFirebaseToken(token)
        }
    }

    private fun observerFirebaseDB() {
        Log.i("Nguyenleeee", "observerFirebaseDB")
        ref.child(getFirebaseToken()).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                handleDataChange(dataSnapshot)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("Nguyenleee", "Failed to read value: ${error.toException()}")
            }
        })
    }

    fun handleDataChange(data: DataSnapshot) {
        try {
            for (item in data.children) {
                val link = item.getValue(CcPushedLink::class.java)
                Log.e("Nguyenleee", "item: $link")
            }
        } catch (e: Exception) {
            Log.e("Nguyenleee", "Failed to cast value: $e")
        }
    }

    private fun getFirebaseToken(): String {
//        return config.getFirebaseToken()
        return "fake_id"
    }

    private fun updateFirebaseToken(token: String) {
        if (TextUtils.isEmpty(token) || token == getFirebaseToken()) return
        Log.i("Nguyenleee", "updateFirebaseToken: $token")
        config.setFirebaseToken(token)
        observerFirebaseDB()
    }
}