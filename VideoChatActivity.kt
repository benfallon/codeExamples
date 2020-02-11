package com.in2l.olympus.activity

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.*
import android.media.MediaPlayer
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.in2l.olympus.R
import com.in2l.olympus.fragment.ContactFragment
import com.in2l.olympus.model.Contact
import com.in2l.olympus.model.Resident
import com.in2l.olympus.util.initTouchRippleForContext
import com.in2l.olympus.videoservice.ChatSession
import com.in2l.olympus.view.TouchIndicatorLayout
import java.util.*

class VideoChatActivity : BaseActivity() {

    var chatSession : ChatSession? = null

    var ringer : MediaPlayer? = null
    var videoCallRingerTask : TimerTask?  = null
    var videoCallNoAnswerTask : TimerTask?  = null

    private lateinit var contact : Contact
    private lateinit var resident : Resident

    private lateinit var touchIndicatorLayout: TouchIndicatorLayout

    companion object {
        const val PERMISSION_ALL = 0x1
    }

    val permissions = arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA, Manifest.permission.BLUETOOTH)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_chat)

        touchIndicatorLayout = findViewById(R.id.video_chat_activity_touch_indicator_layout) as TouchIndicatorLayout

        contact = intent.extras[ContactFragment.KEY_CONTACT_SELECTED] as Contact

        resident = intent.extras[ContactFragment.KEY_RESIDENT] as Resident

        val titleNameTextView = findViewById(R.id.video_chat_title_name_textview) as TextView
        titleNameTextView.text = contact.name

        findViewById(R.id.video_chat_end_chat_button).setOnClickListener { finish() }

        var miniPhotoImageView = findViewById(R.id.video_chat_mini_photo_imageview) as ImageView
        var contactPhoto = contact.photo
        if(contactPhoto == null) {
            contactPhoto = BitmapFactory.decodeResource(resources, R.drawable.profile_placeholder) !!
        }
        val croppedPhoto = createRoundedRectangleBitmap(contactPhoto)
        miniPhotoImageView.setImageBitmap(croppedPhoto)

        ringer = MediaPlayer.create(this, R.raw.ringtone)

        videoCallRingerTask = VideoCallRingerTask()
        Timer().scheduleAtFixedRate(videoCallRingerTask, 0, 5 * 1000)

        videoCallNoAnswerTask = VideoCallNoAnswerTask()
        Timer().schedule(videoCallNoAnswerTask, 29 * 1000)


        if (checkForPermissions()) {
            launchVideoChat()
        } else {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_ALL)
        }
    }

    override fun onResume() {
        super.onResume()
        initTouchRippleForContext(this, touchIndicatorLayout)
    }

    private fun createRoundedRectangleBitmap(originalPhoto: Bitmap) : Bitmap {
        val radius = 20f
        val margin = 0f

        val paint = Paint()
        paint.isAntiAlias = true
        paint.shader = BitmapShader(originalPhoto, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)

        val croppedPhoto = Bitmap.createBitmap(originalPhoto.width, originalPhoto.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(croppedPhoto)
        canvas.drawRoundRect(RectF(margin, margin, originalPhoto.width - margin, originalPhoto.height - margin), radius, radius, paint)

        if (originalPhoto !== croppedPhoto) {
            originalPhoto.recycle()
        }

        return croppedPhoto
    }

    private fun launchVideoChat() {
        chatSession = ChatSession(baseContext)
        chatSession?.startSession(findViewById(R.id.video_chat_container) as ViewGroup, resident)
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            launchVideoChat()
        } else {
            finish()
        }
    }

    private fun checkForPermissions(): Boolean {
        val permissionGranted = permissions.fold(true) { acc, p ->
            acc && (ActivityCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED)
        }

        if (permissionGranted) {
            return true
        }

        return false
    }

    override fun onPause() {
        super.onPause()

        chatSession?.stopSession()
        chatSession = null
    }

    override fun onDestroy() {
        super.onDestroy()
        ringer?.stop()
        videoCallRingerTask?.cancel()
        videoCallNoAnswerTask?.cancel()
    }

    internal inner class VideoCallRingerTask : TimerTask() {

        override fun run() {
            ringer?.start()
        }
    }

    internal inner class VideoCallNoAnswerTask : TimerTask() {

        override fun run() {
            ringer?.stop()
            videoCallRingerTask?.cancel()
            runOnUiThread {
                val noAnswerDialog = AlertDialog.Builder(this@VideoChatActivity)
                        .setMessage(getString(R.string.video_chat_missed_call_dialog_message))
                        .setPositiveButton(R.string.ok){dialog, which ->
                            finish()
                        }
                        .create()
                noAnswerDialog.show()
                val dialogMessage = noAnswerDialog.findViewById(android.R.id.message) as TextView
                val dialogOkButton = noAnswerDialog.findViewById(android.R.id.button1) as TextView
                dialogMessage.textSize = 32f
                dialogOkButton.textSize = 32f
            }
        }
    }
}
