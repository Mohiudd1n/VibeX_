package com.expapps.vibex

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.expapps.vibex.Utils.openActivity
import com.expapps.vibex.databinding.ActivityMainBinding
import com.expapps.vibex.fragments.LocalSongsFragment
import com.expapps.vibex.fragments.ProfileFragment
import com.expapps.vibex.fragments.SongsFragment
import com.expapps.vibex.listeners.PermissionListener
import com.expapps.vibex.player.MediaPlayerService
import com.expapps.vibex.player.PlayerActivity

class MainActivity : AppCompatActivity() {

    var song = "https://cdn.jattpendu.com/download/128k-avzyo/Let&039;s-Nacho.mp3"
    private lateinit var binding: ActivityMainBinding

    private var mediaPlayerService: MediaPlayerService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MediaPlayerService.MediaPlayerBinder
            mediaPlayerService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mediaPlayerService = null
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        val serviceIntent = Intent(this, MediaPlayerService::class.java)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)


        if (AndroidPermissionService().checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Permissions.withContext(this).apply {
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE))
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (AndroidPermissionService().checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Permissions.withContext(this).apply {
                    permissionListener = object : PermissionListener {
                        override fun onNext(isGranted: Boolean) {
                            if (isGranted) {
                            }
                        }

                        override fun onMultipleNext(permissionName: String, isGranted: Boolean) {}
                    }
                    requestPermission(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Permissions.withContext(this).apply {
                requestPermissions(arrayOf(Manifest.permission.FOREGROUND_SERVICE))
            }
        }

        updateUI()

        binding.minifiedLayout.playPauseIv.setOnClickListener {
            mediaPlayerService?.playPauseSong()
            updatePlayPauseButton()

        }

        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed(object : Runnable {
            override fun run() {
                // Update progress again
                updateUI()
                // Repeat after a certain delay (e.g., 1 second)
                handler.postDelayed(this, 1000)
            }
        }, 1000)

        initDefaultFragment()
        setBottomMenuListener()

        binding.minifiedLayout.root.setOnClickListener {
            openActivity(PlayerActivity::class.java)
        }
    }



    private fun updateUI() {
        if (isBound) {
            val songInfo = mediaPlayerService?.getCurrentSongInfo()
            if (mediaPlayerService?.isSongQueued() == true) {
                if (binding.minifiedLayout.root.visibility == View.GONE) {
                    binding.minifiedLayout.root.visibility = View.VISIBLE
                }
            }
            if (songInfo?.isPrepared == true) {
                binding.minifiedLayout.progressCircle.visibility = View.GONE
                binding.minifiedLayout.playPauseIv.visibility = View.VISIBLE
            } else {
                binding.minifiedLayout.progressCircle.visibility = View.VISIBLE
                binding.minifiedLayout.playPauseIv.visibility = View.GONE
            }
            binding.minifiedLayout.progressBar.max = songInfo?.totalDuration?.toIntOrNull() ?: 0
            binding.minifiedLayout.progressBar.progress = songInfo?.currentDuration?.toIntOrNull() ?: 0
            binding.minifiedLayout.songTitleTv.text = songInfo?.songTitle
            binding.minifiedLayout.artistTv.text = songInfo?.artist
            updatePlayPauseButton()
        }
    }

    private fun updatePlayPauseButton() {
        if (mediaPlayerService?.isPlaying() == true) {
            binding.minifiedLayout.playPauseIv.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_pause))
        } else {
            binding.minifiedLayout.playPauseIv.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_play))
        }
        mediaPlayerService?.updateNotificationPlayPauseIcon()
    }

    private fun initDefaultFragment() {
        val songsFragment = SongsFragment()
        openFragment(R.id.content, songsFragment, "Songs")
    }

    private fun setBottomMenuListener() {
        binding.bottomNavigation.setOnItemSelectedListener {
            when(it.itemId) {
                R.id.home -> {
                    val songsFragment = SongsFragment()
                    openFragment(R.id.content, songsFragment, "Songs")
                }
                R.id.favorites -> {
                    val localSongsFragment = LocalSongsFragment()
                    openFragment(R.id.content, localSongsFragment, "Favorites")
                }
                R.id.profile -> {
                    val profileFragment = ProfileFragment()
                    openFragment(R.id.content, profileFragment, "Profile")
                }
            }
            true
        }
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun openFragment(containerId: Int, fragment: Fragment, tag: String) {
        supportFragmentManager
            .beginTransaction()
            .replace(containerId, fragment, tag)
            .commitAllowingStateLoss()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }
}