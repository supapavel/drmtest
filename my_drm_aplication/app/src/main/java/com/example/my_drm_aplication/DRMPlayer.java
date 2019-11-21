package com.example.my_drm_aplication;

import  androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaDrm;
import com.google.android.exoplayer2.drm.HttpMediaDrmCallback;
import com.google.android.exoplayer2.drm.UnsupportedDrmException;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.TransferListener;

import java.util.UUID;

public class DRMPlayer extends AppCompatActivity {

    private final String USER_AGENT = "user-agent";

    private final String HLS_URL = "https://bitdash-a.akamaihd.net/content/MI201109210084_1/m3u8s/f08e80da-bf1d-4e3d-8899-f0f6155f6efa.m3u8";
    private final String DASH_URL = "https://media.axprod.net/TestVectors/v7-Clear/Manifest.mpd";
    //private final String DASH_URL = "https://media.axprod.net/TestVectors/v7-Clear/Manifest_1080p.mpd";

    private final String DRM_HLS_URL = "${BuildConfig.END_POINT}/drm/hls2/master.m3u8";

    //content servers
    private final String CLEARKEY_DASH_URL = "https://media.axprod.net/TestVectors/v7-MultiDRM-SingleKey/Manifest_1080p_ClearKey.mpd";
    private final String WIDEVINE_DASH_URL = "";
    private final String PLAYREADY_DASH_URL = "https://media.axprod.net/TestVectors/v7-MultiDRM-MultiKey-MultiPeriod/Manifest_1080p.mpd";

    private String DRM_DASH_URL;

    //license server's
    private final String CLEARKEY_LICENSE_URL = "https://drm-clearkey-testvectors.axtest.net/AcquireLicense";
    private final String WIDEVINE_LICENSE_URL = "";
    private final String PLAYREADY_LICENSE_URL = "https://drm-playready-licensing.axtest.net/AcquireLicense";

    private String DRM_LICENSE_URL;
    private UUID drmTypeUuid = null;

    private Handler handler;
    private BandwidthMeter bandwidthMeter;

    private HttpMediaDrmCallback drmCallback;
    private DrmSessionManager drmSessionManager;

    private TrackSelector selector;
    private LoadControl loadControl;
    SimpleExoPlayer player = null;


    public enum StreamingType {
        DASH, HLS
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.drmplayer_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.back_btn) {
            if(player != null) {
                player.release();
            }
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drmplayer);

        Intent intent = getIntent();

        StreamingType streamingType = null;

        boolean isDrm = intent.getBooleanExtra("isDrm", false);

        handler = new Handler();
        bandwidthMeter = new DefaultBandwidthMeter();
        selector = new DefaultTrackSelector();
        loadControl = new DefaultLoadControl();

        if(isDrm) {

            String drmType = intent.getStringExtra("drmType");

            switch (drmType) {
                case "ClearKey":
                    DRM_LICENSE_URL = CLEARKEY_LICENSE_URL;
                    DRM_DASH_URL = CLEARKEY_DASH_URL;
                    drmTypeUuid = C.CLEARKEY_UUID;
                    break;
                case "PlayReady":
                    DRM_LICENSE_URL = PLAYREADY_LICENSE_URL;
                    DRM_DASH_URL = PLAYREADY_DASH_URL;
                    drmTypeUuid = C.PLAYREADY_UUID;
                    break;
                case "WideVine":
                    DRM_LICENSE_URL = WIDEVINE_LICENSE_URL;
                    DRM_DASH_URL = WIDEVINE_DASH_URL;
                    drmTypeUuid = C.WIDEVINE_UUID;
                    break;
                default:
                    break;
            }

            drmCallback = new HttpMediaDrmCallback(DRM_LICENSE_URL, new DefaultHttpDataSourceFactory(USER_AGENT));

            try {
//                  drmSessionManager = new DefaultDrmSessionManager(drmTypeUuid,
//                  FrameworkMediaDrm.newInstance(drmTypeUuid), drmCallback, null,
//                  true,10);
                //ExoPlayer 2.7.3 or bellow
                drmSessionManager = new DefaultDrmSessionManager(drmTypeUuid,
                        FrameworkMediaDrm.newInstance(drmTypeUuid), drmCallback, null, handler, null);
            } catch (UnsupportedDrmException e) {
                e.printStackTrace();
            }
        }

        switch (intent.getStringExtra("streamType")) {
            case "DASH":
                streamingType = StreamingType.DASH;
                break;
            case "HLS":
                streamingType = StreamingType.HLS;
                break;
            default:
                break;
        }

        player = initPlayer(isDrm, streamingType);
        player.setPlayWhenReady(true);
    }

    private SimpleExoPlayer initPlayer(boolean isDrm, DRMPlayer.StreamingType type) {

        RenderersFactory renderersFactory = isDrm ?
                new DefaultRenderersFactory(this, drmSessionManager)
                : new DefaultRenderersFactory(this);

//        SimpleExoPlayer player = ExoPlayerFactory.newSimpleInstance(/* context= */ this, renderersFactory, selector, loadControl);
//        ExoPlayer 2.7.3 or bellow
        SimpleExoPlayer player = ExoPlayerFactory.newSimpleInstance(renderersFactory, selector, loadControl);

        PlayerView playerView = findViewById(R.id.player_view);
        playerView.setPlayer(player);

//        DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(this,
//                ((TransferListener) bandwidthMeter),
//                new DefaultHttpDataSourceFactory(USER_AGENT, (TransferListener) bandwidthMeter));
        //ExoPlayer 2.7.3 or bellow
        DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(this, (TransferListener<? super DataSource>) bandwidthMeter,
                new DefaultHttpDataSourceFactory(USER_AGENT, (TransferListener<? super DataSource>) bandwidthMeter));


        player.prepare (
                type == DRMPlayer.StreamingType.DASH
                        ?   createDashSource(isDrm ? DRM_DASH_URL : DASH_URL, dataSourceFactory)
                        :   createHlsSource(isDrm ? DRM_HLS_URL : HLS_URL, dataSourceFactory)
        );

        return player;
    }

    private DashMediaSource createDashSource(String url, DefaultDataSourceFactory dataSourceFactory) {

        DefaultDashChunkSource.Factory dashChunkSource = new DefaultDashChunkSource.Factory(dataSourceFactory);
        DashMediaSource.Factory dashMediaSource = new DashMediaSource.Factory(dashChunkSource, dataSourceFactory);
        return dashMediaSource.createMediaSource(Uri.parse(url));
    }

    private HlsMediaSource createHlsSource(String url, DefaultDataSourceFactory dataSourceFactory) {

        HlsMediaSource.Factory hlsMediaSource = new HlsMediaSource.Factory(dataSourceFactory);
        return hlsMediaSource.createMediaSource(Uri.parse(url));
    }
}
