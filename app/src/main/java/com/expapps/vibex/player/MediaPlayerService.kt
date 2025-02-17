package com.expapps.vibex.player

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.widget.RemoteViews
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.expapps.vibex.Constants.ACTION_PLAY_PAUSE
import com.expapps.vibex.Constants.ACTION_PLAY_SONG
import com.expapps.vibex.Constants.EXTRA_SONG_ARTIST
import com.expapps.vibex.Constants.EXTRA_SONG_GENRE
import com.expapps.vibex.Constants.EXTRA_SONG_TITLE
import com.expapps.vibex.Constants.EXTRA_SONG_URL
import com.expapps.vibex.MainActivity
import com.expapps.vibex.R


class MediaPlayerService : Service(), MediaPlayer.OnPreparedListener,
    MediaPlayer.OnCompletionListener, AudioManager.OnAudioFocusChangeListener {

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var notificationManager: NotificationManager
    private lateinit var audioManager: AudioManager
    private val binder = MediaPlayerBinder()
    var notification: NotificationCompat.Builder? = null
    var remoteViews: RemoteViews? = null
    private var isServiceRunning = false
    private var currentlyPlayingSongUrl: String? = null

    private var songUrl = ""
    private var songTitle = ""
    private var songArtist = ""
    private var songGenre = ""
    private var isPrepared = false

    inner class MediaPlayerBinder : Binder() {
        fun getService(): MediaPlayerService {
            return this@MediaPlayerService
        }
    }
    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        initMediaPlayer()
        initMediaSession()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // Create notification channel if device is running Android Oreo or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Media Playback",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Handle intent actions here if any
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> {
                playPauseSong()
                changePlayPauseIcon()
            }
            ACTION_PLAY_SONG -> {
                songUrl = intent.getStringExtra(EXTRA_SONG_URL) ?: ""
                songTitle = intent.getStringExtra(EXTRA_SONG_TITLE) ?: ""
                songArtist = intent.getStringExtra(EXTRA_SONG_ARTIST) ?: ""
                songGenre = intent.getStringExtra(EXTRA_SONG_GENRE) ?: ""
                if (!isServiceRunning || songUrl != currentlyPlayingSongUrl) {
                    isPrepared = false
                    mediaPlayer
                    playSongFromUrl(songUrl)
                    currentlyPlayingSongUrl = songUrl
                    buildNotification()
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun initMediaPlayer() {
        mediaPlayer = MediaPlayer()
        mediaPlayer.setOnPreparedListener(this)
        mediaPlayer.setOnCompletionListener(this)
        // Other setup for MediaPlayer
    }

    private fun initMediaSession() {
        mediaSession = MediaSessionCompat(this, "MediaPlayerService")
        mediaSession.setFlags(
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                    or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        )
    }

    fun updateNotificationPlayPauseIcon() {
        changePlayPauseIcon()
    }

    private fun buildNotification() {
        remoteViews = RemoteViews(packageName, R.layout.layout_songs_notification)

        // Customize the RemoteViews with data or actions
        remoteViews?.setTextViewText(R.id.songTitleTv, songTitle)
        remoteViews?.setTextViewText(R.id.artistTv, songArtist)
        remoteViews?.setImageViewResource(R.id.playPauseBtn, R.drawable.ic_pause)

        val notificationIntent = Intent()
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE,
        )

        val playPauseIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, MediaPlayerService::class.java).apply {
                action = ACTION_PLAY_PAUSE
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        remoteViews?.setOnClickPendingIntent(R.id.playPauseBtn, playPauseIntent)


        notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContent(remoteViews)
            .setCustomBigContentView(remoteViews)
            .setContentIntent(pendingIntent)
            .setSilent(true)
            .setOngoing(true)
            .setPriority(NotificationManager.IMPORTANCE_MAX)


        notification?.build()?.let { startForeground(NOTIFICATION_ID, it) }
    }

    private fun notifyNotification() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED) {
            val notificationManager = NotificationManagerCompat.from(this)
            notification?.build()?.let { notificationManager.notify(NOTIFICATION_ID, it) }
        }
    }

    private fun changePlayPauseIcon() {
        if (isPlaying()) {
            remoteViews?.setImageViewResource(R.id.playPauseBtn, R.drawable.ic_pause)
        } else {
            remoteViews?.setImageViewResource(R.id.playPauseBtn, R.drawable.ic_play)
        }
        //buildNotification()
        notifyNotification()
    }

    override fun onPrepared(mp: MediaPlayer?) {
        isPrepared = true
        mediaPlayer.start()
    }

    override fun onCompletion(mp: MediaPlayer?) {
        // Handle completion if needed
    }

    override fun onAudioFocusChange(focusChange: Int) {
        // Handle audio focus changes if needed
    }

    fun playSongFromUrl(url: String) {
        try {
            val headers: MutableMap<String, String> = HashMap()
            headers["Accept-Ranges"] = "bytes"
            headers["Status"] = "206"
            headers["Cache-control"] = "no-cache"
            val uri = Uri.parse(url)
            mediaPlayer.reset()
            mediaPlayer.setDataSource(applicationContext, uri, headers)
            mediaPlayer.prepareAsync()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isSongQueued(): Boolean {
        return songTitle != ""
    }

    fun getCurrentSongInfo(): SongInfo {
        // Implement logic to get current song info
        return SongInfo(isPlaying = mediaPlayer.isPlaying,
            isPrepared = isPrepared,
            songUrl = songUrl,
            songTitle = songTitle,
            artist = songArtist,
            genreTags = songGenre,
            totalDuration = mediaPlayer.duration.toString(),
            currentDuration = mediaPlayer.currentPosition.toString()
        )
    }

    fun playPauseSong() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
        } else {
            mediaPlayer.start()
        }
    }

    fun isPlaying(): Boolean {
        return mediaPlayer.isPlaying
    }

    fun seekTo(position: Int) {
        mediaPlayer.seekTo(position)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
        mediaSession.release()
        stopForeground(true)
    }

    companion object {
        private const val CHANNEL_ID = "media_player_channel"
        private const val NOTIFICATION_ID = 1
    }
}
