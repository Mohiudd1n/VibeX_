package com.expapps.vibex.player;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;

import androidx.annotation.Nullable;

import com.expapps.vibex.VibexApplication;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.ui.PlayerNotificationManager;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;

public class AudioPlayerService extends Service {

    public static final String CHANNEL_ID = "RADIO_MEDIA_CHANNEL";
    private static final int NOTIFICATION_ID = 1;
    public static  boolean IS_AUDIO_RUNNING = false;

    private SimpleExoPlayer simpleExoPlayer;
    private PlayerNotificationManager playerNotificationManager;
    private String title, streamUrl;
    private boolean isPlay = true;

    @Override
    public void onCreate() {
        super.onCreate();
        IS_AUDIO_RUNNING = true;
        //        startPlayer();
    }

    @Override
    public void onDestroy() {
        IS_AUDIO_RUNNING = false;
        releasePlayer();
        super.onDestroy();
    }

    private void releasePlayer() {
        if (simpleExoPlayer != null) {
            //            playerNotificationManager.setPlayer(null);
            simpleExoPlayer.release();
            simpleExoPlayer = null;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //        releasePlayer();
        Bundle bundle = intent.getExtras();
        title = bundle.getString("title");
        streamUrl = bundle.getString("stream_url");
        isPlay = bundle.getBoolean("is_play", true);
        simpleExoPlayer = null;
        if (simpleExoPlayer == null) {
            startPlayer();
        } else {
            simpleExoPlayer.setPlayWhenReady(isPlay);
        }
        return START_STICKY;
    }

    private void startPlayer() {
        final Context context = this;
        HttpDataSource.Factory httpDataSourceFactory =
                new DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true);
        DataSource.Factory cacheDataSourceFactory =
                new CacheDataSource.Factory().setCache(VibexApplication.Companion.getSimpleCache())
                        .setUpstreamDataSourceFactory(httpDataSourceFactory)
                        .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);
        DefaultRenderersFactory defaultRenderersFactory = new DefaultRenderersFactory(context);
        defaultRenderersFactory.experimentalSetSynchronizeCodecInteractionsWithQueueingEnabled(true);

        DefaultLoadControl.Builder builder = new DefaultLoadControl.Builder();
        final long loadControlStartBufferMs = 1500;
        builder.setBufferDurationsMs(
                DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
                DefaultLoadControl.DEFAULT_MAX_BUFFER_MS,
                (int) loadControlStartBufferMs,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
        );
        DefaultLoadControl loadControl = builder.build();

        simpleExoPlayer =
                new SimpleExoPlayer.Builder(context, defaultRenderersFactory).setLoadControl(loadControl)
                        .setMediaSourceFactory(new DefaultMediaSourceFactory(cacheDataSourceFactory))
                        .build();

        simpleExoPlayer.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT);

        MediaSource mediaSource = buildMediaSource();

        simpleExoPlayer.seekTo(0, 0);
        simpleExoPlayer.setMediaSource(mediaSource, true);
        simpleExoPlayer.setRepeatMode(Player.REPEAT_MODE_OFF);
        simpleExoPlayer.prepare();
        simpleExoPlayer.setPlayWhenReady(isPlay);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Intent restartServiceIntent = new Intent(getApplicationContext(), this.getClass());
        restartServiceIntent.setPackage(getPackageName());

        PendingIntent restartServicePendingIntent =
                PendingIntent.getService(getApplicationContext(),
                        1,
                        restartServiceIntent,
                        PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
                );
        AlarmManager alarmService =
                (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        alarmService.set(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + 1000,
                restartServicePendingIntent
        );

        super.onTaskRemoved(rootIntent);
    }

    private MediaSource buildMediaSource() {
        DataSource.Factory dataSourceFactory = new DefaultHttpDataSource.Factory();
        MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(streamUrl));
        Uri uri = Uri.parse(streamUrl);
        if (uri.getLastPathSegment() != null && uri.getLastPathSegment().contains("m3u8")) {
            mediaSource = new HlsMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(streamUrl));
        }
        return mediaSource;
    }
}