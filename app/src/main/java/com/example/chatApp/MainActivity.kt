package com.example.chatApp

import android.R.attr
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.AuthUI.IdpConfig.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.getValue
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.*


class MainActivity : AppCompatActivity() {
    @Suppress("PropertyName")
    val RC_SIGN_IN = 1

    private var mMessageListView: ListView? = null
    private var mMessageAdapter: MessageAdapter? = null
    private var mProgressBar: ProgressBar? = null
    private var mPhotoPickerButton: ImageButton? = null
    private var mMessageEditText: EditText? = null
    private var mSendButton: Button? = null
    private var mUsername: String? = null
    private lateinit var mFirebaseDatabase: FirebaseDatabase
    private lateinit var mMessageDatabaseReference: DatabaseReference
    private lateinit var mChildEventListener: ChildEventListener
    private lateinit var mFirebaseAuth: FirebaseAuth
    private lateinit var mAuthStateListener: FirebaseAuth.AuthStateListener
    private var RC_PHOTO_PICKER = 2
    private lateinit var mFirebaseStorage: FirebaseStorage
    private lateinit var mChatPhotoStorageReference: StorageReference
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContentView(R.layout.activity_main)
//        mUsername = ANONYMOUS

        // Initialize references to views
        mProgressBar = findViewById(R.id.progressBar)
        mMessageListView = findViewById(R.id.messageListView)
        mPhotoPickerButton = findViewById(R.id.photoPickerButton)
        mMessageEditText = findViewById(R.id.messageEditText)
        mSendButton = findViewById(R.id.sendButton)


//firebase begin
        mFirebaseDatabase = FirebaseDatabase.getInstance()
        mMessageDatabaseReference = mFirebaseDatabase.reference.child("messages")

//authenticating
        mFirebaseAuth = FirebaseAuth.getInstance()


//adding photos to storage
//pick image image from device
        mFirebaseStorage = FirebaseStorage.getInstance()
        mChatPhotoStorageReference = mFirebaseStorage.reference.child("chat_photos")


//check wether sign in or not
        mAuthStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                //user signed in
                Log.e(this.toString(), "IIIIIIIIIIIIIIIIIIIIIIImmm")

                onSignedInitialise(user.displayName)
                Toast.makeText(this, "you're signed in", Toast.LENGTH_SHORT).show()
            } else {
                //if user is not sign in
                //user sign out
                Log.e(this.toString(), "IIIIIIIIIIIIIIIIIIIIIIInhhbhb ")

                onSignedOutCleanUp()
                startActivityForResult(
                    AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setIsSmartLockEnabled(false)//saves the data so set to false
                        .setAvailableProviders(
                            listOf(
                                GoogleBuilder().build(),
                                EmailBuilder().build(),
                            )
                        )
                        .build(),
                    RC_SIGN_IN
                )
            }
        }


        // Initialize message ListView and its adapter
        val friendlyMessages: List<FriendlyMessage> = ArrayList<FriendlyMessage>()
        mMessageAdapter = MessageAdapter(this, R.layout.item_message, friendlyMessages)
        mMessageListView!!.adapter = mMessageAdapter

        // Initialize progress bar
        mProgressBar!!.visibility = ProgressBar.INVISIBLE


        // Enable Send button when there's text to send
        mMessageEditText!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                mSendButton!!.isEnabled = charSequence.toString().trim { it <= ' ' }.isNotEmpty()
            }

            override fun afterTextChanged(editable: Editable) {}
        })
        mMessageEditText!!.filters = arrayOf<InputFilter>(LengthFilter(DEFAULT_MSG_LENGTH_LIMIT))
//reading from database
        val postListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Get Post object and use the values to update the UI

                val friendlyMessage = dataSnapshot.getValue<FriendlyMessage>()

                Log.e(this.toString(), "%%%%%%%%%%%$friendlyMessage")
                // ...
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException())
                // ...
            }
        }

        mMessageDatabaseReference.addValueEventListener(postListener)


        // Send button sends a message and clears the EditText
        mSendButton!!.setOnClickListener { // TODO: Send messages on click
            val friendlyMessage =
                FriendlyMessage(mMessageEditText!!.text.toString(), mUsername, null)
            //setting message to data base
            //push is required for assigning unique id to the message in the databse
            mMessageDatabaseReference.push().setValue(friendlyMessage)
            // Clear input box
            mMessageEditText!!.setText("")
        }
        mPhotoPickerButton!!.setOnClickListener {
            Log.e(this.toString(), "fxngccccccccccnd")
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/jpeg"
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
            startActivityForResult(
                Intent.createChooser(intent, "Complete action using"),
                RC_PHOTO_PICKER
            )

        }

    }

    private fun onSignedOutCleanUp() {
        mUsername = ANONYMOUS
        mMessageAdapter?.clear()
        dettachDatabaseReadListener()
    }

    /**
     * Dispatch incoming result to the correct fragment.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (RC_SIGN_IN == requestCode) {
            if (RESULT_OK == resultCode) {
                Toast.makeText(this, "Signed In", Toast.LENGTH_SHORT).show()
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Sign is cancelled", Toast.LENGTH_SHORT).show()
                finish()
            }

        } else if (requestCode == RC_PHOTO_PICKER && resultCode== RESULT_OK) {
            val selectedImage: Uri = data?.data!!
            val ref = mChatPhotoStorageReference.child(selectedImage.lastPathSegment.toString())

            var uploadTask = ref.putFile(selectedImage)
            Log.e(this.toString(), "gffffffffffffffffffjbbbbbbbbb")
            val urlTask = uploadTask.continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let {
                        throw it
                    }
                }
                ref.downloadUrl
            }.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val downloadUri = task.result.toString()
                    val friendlyMessage = FriendlyMessage(null, mUsername, downloadUri)
                    mMessageDatabaseReference.push().setValue(friendlyMessage)
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.sign_out_menu -> {
//                onSignedOutCleanUp()
                AuthUI.getInstance().signOut(this)
            }
            R.id.delete_all -> {
                mMessageDatabaseReference.ref.removeValue()
                mMessageAdapter?.clear()
            }
        }
        return super.onOptionsItemSelected(item)

    }

    private fun onSignedInitialise(displayName: String?) {
        mUsername = displayName
        attachDatabaseReadListener()

    }


    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    fun attachDatabaseReadListener() {
        Log.e(this.toString(), "IIIIIIIIIIIIIIIIIIIIIII")
        mChildEventListener = object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                val friendlyMessage = dataSnapshot.getValue(
                    FriendlyMessage::class.java
                )
                Log.e(this.toString(), "%%%%%%%%%%%$friendlyMessage")
                mMessageAdapter!!.add(friendlyMessage)
            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {}
            override fun onChildRemoved(dataSnapshot: DataSnapshot) {}
            override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {}
            override fun onCancelled(databaseError: DatabaseError) {}
        }
        mMessageDatabaseReference.addChildEventListener(mChildEventListener)

    }

    private fun dettachDatabaseReadListener() {


        Log.e(this.toString(), "mmmmmmmmmmmmmmmmmm")
        if (mChildEventListener != null) mMessageDatabaseReference.removeEventListener(
            mChildEventListener
        )


    }

    override fun onStart() {
        super.onStart()
        mChildEventListener = object : ChildEventListener {

            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                TODO("Not yet implemented")
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                TODO("Not yet implemented")
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                TODO("Not yet implemented")
            }


            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                TODO("Not yet implemented")
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        }

    }

    override fun onResume() {
        super.onResume()
        mFirebaseAuth.addAuthStateListener(mAuthStateListener)
    }

    override fun onPause() {
        super.onPause()
        if (mAuthStateListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener)
        }
        mMessageAdapter?.clear()
        dettachDatabaseReadListener()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }


    companion object {
        private const val TAG = "MainActivity"
        const val ANONYMOUS = "anonymous"
        const val DEFAULT_MSG_LENGTH_LIMIT = 1000

    }
}