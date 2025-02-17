package com.expapps.vibex

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.expapps.vibex.auth.User
import com.expapps.vibex.models.SongsModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class FirebaseSource {

    private var firebaseAuth = FirebaseAuth.getInstance()
    private var firebaseDatabase = FirebaseDatabase.getInstance()
    private var databaseReference = firebaseDatabase.reference


    fun getName() : String {
        return "Shahid"
    }

    fun getAllSongs() : LiveData<ArrayList<SongsModel>?> {
        val songsMutableLiveData = MutableLiveData<ArrayList<SongsModel>?>()
        databaseReference.child("Songs")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val songsList = ArrayList<SongsModel>()
                    if (snapshot.exists()) {
                        snapshot.children.forEach {
                            if (it != null && it.exists()) {
                                val song = it.getValue(SongsModel::class.java)
                                if (song != null) {
                                    songsList.add(song)
                                }
                            }
                        }

                    }
                    if (songsList.isNotEmpty()) {
                        songsMutableLiveData.value = songsList
                    }

                }

                override fun onCancelled(error: DatabaseError) {
                    songsMutableLiveData.value = null
                }

            })
        return songsMutableLiveData
    }

    fun getUserList(): LiveData<ArrayList<User>?> {
        val users = MutableLiveData<ArrayList<User>?>()
        databaseReference.child("Users")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                val userArray = ArrayList<User>()
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.forEach {
                        val user = it.getValue(User::class.java)
                        user?.let { u ->
                            userArray.add(u)
                        }
                    }
                    users.value = userArray
                }

                override fun onCancelled(error: DatabaseError) {
                    users.value = null
                }
            })
        return users
    }

    fun addUser(user: User): LiveData<Boolean?> {
        val isSuccess = MutableLiveData<Boolean>()
        databaseReference.child("Users")
            .child(user.userId ?: "")
            .setValue(user)
            .addOnSuccessListener {
                addMCode(user.email ?: "",
                    Utils.getMCodeFromUserId(user.userId ?: ""), user.userId ?: "")
                isSuccess.value = true
            }
            .addOnFailureListener {
                isSuccess.value = false
            }
        return isSuccess
    }

    fun addMCode(email: String, code: String, uid: String) {
        databaseReference.child("UsersMCode")
            .updateChildren(
                hashMapOf<String, Any>(
                    Pair(
                        Utils.getEmailFromEmailId(email), uid
                    )
                )
            )
            .addOnSuccessListener {}
            .addOnFailureListener {}
    }

    fun getMCodeByEmail(email: String): LiveData<String?> {
        val isSuccess = MutableLiveData<String>()
        val emailStr = Utils.getEmailFromEmailId(email)
        databaseReference.child("UsersMCode/$emailStr")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val child = snapshot.value.toString()
                        isSuccess.value = Utils.getMCodeFromUserId(child)
                    } else {
                        isSuccess.value = null
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    isSuccess.value = null
                }
            })
        return isSuccess
    }

    fun fetchUserInfo(userId: String): LiveData<Boolean?> {
        val isUserRegistrationCompleted = MutableLiveData<Boolean?>()
        databaseReference.child("Users/$userId")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val user = snapshot.getValue(User::class.java)
                        isUserRegistrationCompleted.value = user?.userRegistrationCompleted
                    } else {
                        isUserRegistrationCompleted.value = null
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    isUserRegistrationCompleted.value = null
                }
            })
        return isUserRegistrationCompleted
    }

    fun isLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    fun uploadSong(songsModel: SongsModel): LiveData<Boolean> {
        val isSuccess = MutableLiveData<Boolean>()
        val songId = firebaseDatabase.reference.push().key
        songsModel.songId = songId
        val map = HashMap<String, Any>()
        map[songId ?: ""] = songsModel
        firebaseDatabase.getReference("Songs")
            .updateChildren(map)
            .addOnSuccessListener { isSuccess.value = true }
            .addOnFailureListener { isSuccess.value = false }
        return isSuccess
    }

    fun logout() {
        firebaseAuth.signOut()
    }

    fun getCurrentUserEmail(): String {
        return firebaseAuth.currentUser?.email.toString()
    }
}