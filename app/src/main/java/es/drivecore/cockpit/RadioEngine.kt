package es.drivecore.cockpit

import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.decoder.ffmpeg.FfmpegLibrary
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** Audio only, no video surface, no bypass of vehicle video restrictions. All calls on main thread. */
@androidx.annotation.OptIn(UnstableApi::class)
class RadioEngine(private val ctx: Context, private val status: (String) -> Unit) {
    private var player: ExoPlayer? = null
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followSslRedirects(false)
        .build()

    fun play(url: String) {
        stop()
        val uri = Uri.parse(url)
        if (uri.scheme != "https" || uri.host.isNullOrBlank() || uri.userInfo != null) {
            status("Se requiere una URL HTTPS válida de audio")
            return
        }
        try {
            val selector = DefaultTrackSelector(ctx).apply {
                parameters = buildUponParameters().setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true).build()
            }
            val factory = OkHttpDataSource.Factory(http).setUserAgent("DriveCore Native/0.7")
            val renderers = DefaultRenderersFactory(ctx).setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            val p = ExoPlayer.Builder(ctx, renderers)
                .setTrackSelector(selector)
                .setMediaSourceFactory(DefaultMediaSourceFactory(ctx).setDataSourceFactory(factory))
                .build()
            player = p
            p.setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true)
            p.setHandleAudioBecomingNoisy(true)
            p.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (player !== p) return
                    when (state) {
                        Player.STATE_BUFFERING -> status("Cargando emisión…")
                        Player.STATE_READY -> status(if (p.isPlaying) "Radio en reproducción" else "Radio preparada")
                        Player.STATE_ENDED -> status("Emisión finalizada")
                    }
                }
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (player === p) status(if (isPlaying) "Radio en reproducción" else "Radio pausada o esperando audio")
                }
                override fun onTracksChanged(tracks: Tracks) {
                    if (player === p && !tracks.groups.isEmpty() && !tracks.isTypeSupported(C.TRACK_TYPE_AUDIO)) {
                        status("La fuente no proporciona audio compatible")
                    }
                }
                override fun onPlayerError(error: PlaybackException) {
                    if (player === p) { stop(); status("Error multimedia: ${error.errorCodeName}") }
                }
            })
            p.setMediaItem(MediaItem.fromUri(uri))
            status("Conectando…")
            p.prepare()
            p.play()
        } catch (e: Exception) {
            stop(); status("No se pudo iniciar radio: ${e.javaClass.simpleName}")
        }
    }
    fun decoderReport(): String = runCatching { if (FfmpegLibrary.isAvailable()) "FFmpeg audio: " + FfmpegLibrary.getVersion() else "FFmpeg audio: no disponible para esta arquitectura" }.getOrDefault("FFmpeg audio: error de carga")
    fun stop() {
        val old = player
        player = null
        runCatching { old?.release() }
    }
}
