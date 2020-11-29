package com.example.chatApp

import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import com.google.firebase.database.ktx.getValue
import java.util.*


class MainActivity : AppCompatActivity() {

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
    private var menu: MutableList<FriendlyMessage> = mutableListOf()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContentView(R.layout.activity_main)
        mUsername = ANONYMOUS
//firebase begin
        mFirebaseDatabase = FirebaseDatabase.getInstance()
        mMessageDatabaseReference = mFirebaseDatabase.reference.child("messages")


        // Initialize references to views
        mProgressBar = findViewById<ProgressBar>(R.id.progressBar)
        mMessageListView = findViewById<ListView>(R.id.messageListView)
        mPhotoPickerButton = findViewById<ImageButton>(R.id.photoPickerButton)
        mMessageEditText = findViewById<EditText>(R.id.messageEditText)
        mSendButton = findViewById<Button>(R.id.sendButton)

        // Initialize message ListView and its adapter
        val friendlyMessages: List<FriendlyMessage> = ArrayList<FriendlyMessage>()
        menu= friendlyMessages as MutableList<FriendlyMessage>
        mMessageAdapter = MessageAdapter(this, R.layout.item_message, friendlyMessages)
        mMessageListView!!.adapter = mMessageAdapter

        // Initialize progress bar
        mProgressBar!!.visibility = ProgressBar.INVISIBLE

        // ImagePickerButton shows an image picker to upload a image for a message
        mPhotoPickerButton!!.setOnClickListener {
            // TODO: Fire an intent to show an image picker
        }

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

                Log.e(this.toString(),"%%%%%%%%%%%$friendlyMessage")
                    // ...
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException())
                // ...
            }
        }

        mMessageDatabaseReference.addValueEventListener(postListener)


        mChildEventListener = object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                val friendlyMessage = dataSnapshot.getValue(
                    FriendlyMessage::class.java
                )
                Log.e(this.toString(),"%%%%%%%%%%%$friendlyMessage")
                mMessageAdapter!!.add(friendlyMessage)
            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {}
            override fun onChildRemoved(dataSnapshot: DataSnapshot) {}
            override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {}
            override fun onCancelled(databaseError: DatabaseError) {}
        }
        mMessageDatabaseReference.addChildEventListener(mChildEventListener)


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