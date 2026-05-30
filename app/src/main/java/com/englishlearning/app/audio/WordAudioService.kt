package com.englishlearning.app.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import com.englishlearning.app.MainActivity
import com.englishlearning.app.R
import com.englishlearning.app.data.model.Word
import com.englishlearning.app.utils.PhonicsUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*

/**
 * 后台单词播放服务
 * 支持顺序播放单词、音节拆分、中文解释
 */
class WordAudioService : Service(), TextToSpeech.OnInitListener {

    companion object {
        const val CHANNEL_ID = "word_audio_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_PLAY = "action_play"
        const val ACTION_PAUSE = "action_pause"
        const val ACTION_NEXT = "action_next"
        const val ACTION_PREVIOUS = "action_previous"
        const val ACTION_STOP = "action_stop"
        const val EXTRA_WORD_LIST = "extra_word_list"
        const val EXTRA_CURRENT_INDEX = "extra_current_index"
        const val EXTRA_PLAYBACK_SPEED = "extra_playback_speed"
        const val EXTRA_PLAY_MODE = "extra_play_mode"
    }

    // 播放模式
    enum class PlayMode {
        WORD_ONLY,           // 仅播放单词
        WORD_SYLLABLE,       // 单词 + 音节拆分
        WORD_SYLLABLE_MEANING, // 单词 + 音节 + 中文意思
        FULL_DETAIL          // 完整：单词 + 音节 + 发音技巧 + 中文 + 例句
    }

    private var textToSpeech: TextToSpeech? = null
    private var chineseTts: TextToSpeech? = null
    private var isTtsReady = false
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // 播放状态
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentWord = MutableStateFlow<Word?>(null)
    val currentWord: StateFlow<Word?> = _currentWord

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed

    private val _playMode = MutableStateFlow(PlayMode.WORD_SYLLABLE_MEANING)
    val playMode: StateFlow<PlayMode> = _playMode

    private var wordList: List<Word> = emptyList()
    private var isPaused = false
    private var currentUtteranceId: String? = null

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): WordAudioService = this@WordAudioService
    }

    override fun onCreate() {
        super.onCreate()
        textToSpeech = TextToSpeech(this, this)
        createNotificationChannel()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale.US)
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                isTtsReady = true
                setupTtsListener()
            }
        }
    }

    private fun setupTtsListener() {
        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                currentUtteranceId = utteranceId
            }

            override fun onDone(utteranceId: String?) {
                if (utteranceId == currentUtteranceId && !isPaused) {
                    serviceScope.launch {
                        delay(500) // 短暂停顿
                        playNextItem()
                    }
                }
            }

            override fun onError(utteranceId: String?) {
                serviceScope.launch {
                    delay(500)
                    playNextItem()
                }
            }
        })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                val words = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra(EXTRA_WORD_LIST, Word::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayListExtra(EXTRA_WORD_LIST)
                }
                val index = intent.getIntExtra(EXTRA_CURRENT_INDEX, 0)
                val speed = intent.getFloatExtra(EXTRA_PLAYBACK_SPEED, 1.0f)
                val modeOrdinal = intent.getIntExtra(EXTRA_PLAY_MODE, PlayMode.WORD_SYLLABLE_MEANING.ordinal)

                words?.let {
                    wordList = it
                    _currentIndex.value = index.coerceIn(0, wordList.size - 1)
                    _playbackSpeed.value = speed
                    _playMode.value = PlayMode.values()[modeOrdinal]
                    startPlayback()
                }
            }
            ACTION_PAUSE -> pausePlayback()
            ACTION_NEXT -> playNextWord()
            ACTION_PREVIOUS -> playPreviousWord()
            ACTION_STOP -> stopPlayback()
        }
        return START_STICKY
    }

    private fun startPlayback() {
        if (wordList.isEmpty()) return

        isPaused = false
        _isPlaying.value = true
        updateNotification()
        playCurrentWord()
    }

    private fun playCurrentWord() {
        if (wordList.isEmpty() || _currentIndex.value >= wordList.size) return

        val word = wordList[_currentIndex.value]
        _currentWord.value = word

        when (_playMode.value) {
            PlayMode.WORD_ONLY -> speakWord(word)
            PlayMode.WORD_SYLLABLE -> speakWordWithSyllables(word)
            PlayMode.WORD_SYLLABLE_MEANING -> speakWordWithSyllablesAndMeaning(word)
            PlayMode.FULL_DETAIL -> speakFullDetail(word)
        }

        updateNotification()
    }

    private fun speakWord(word: Word) {
        if (!isTtsReady) return

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            putFloat(TextToSpeech.Engine.KEY_PARAM_SPEED, _playbackSpeed.value)
        }

        textToSpeech?.speak(
            word.word,
            TextToSpeech.QUEUE_FLUSH,
            params,
            "word_${word.id}"
        )
    }

    private fun speakWordWithSyllables(word: Word) {
        if (!isTtsReady) return

        textToSpeech?.stop()

        val syllables = PhonicsUtils.splitSyllables(word.word)
        val utteranceId = "word_syllable_${word.id}_${System.currentTimeMillis()}"

        // 播放单词
        textToSpeech?.speak(
            word.word,
            TextToSpeech.QUEUE_FLUSH,
            createSpeechParams(),
            "${utteranceId}_word"
        )

        // 播放音节拆分
        if (syllables.size > 1) {
            textToSpeech?.speak(
                ", ",
                TextToSpeech.QUEUE_ADD,
                createSpeechParams(),
                "${utteranceId}_pause1"
            )

            syllables.forEachIndexed { index, syllable ->
                textToSpeech?.speak(
                    syllable.text,
                    TextToSpeech.QUEUE_ADD,
                    createSpeechParams(),
                    "${utteranceId}_syllable_$index"
                )

                if (index < syllables.size - 1) {
                    textToSpeech?.speak(
                        ", ",
                        TextToSpeech.QUEUE_ADD,
                        createSpeechParams(),
                        "${utteranceId}_pause_$index"
                    )
                }
            }
        }
    }

    private fun speakWordWithSyllablesAndMeaning(word: Word) {
        if (!isTtsReady) return

        textToSpeech?.stop()

        val syllables = PhonicsUtils.splitSyllables(word.word)
        val utteranceId = "word_full_${word.id}_${System.currentTimeMillis()}"

        // 1. 播放单词
        textToSpeech?.speak(
            word.word,
            TextToSpeech.QUEUE_FLUSH,
            createSpeechParams(),
            "${utteranceId}_word"
        )

        // 2. 播放音节拆分
        if (syllables.size > 1) {
            textToSpeech?.playSilentUtterance(300, TextToSpeech.QUEUE_ADD, "${utteranceId}_pause1")

            syllables.forEachIndexed { index, syllable ->
                textToSpeech?.speak(
                    syllable.text,
                    TextToSpeech.QUEUE_ADD,
                    createSpeechParams(),
                    "${utteranceId}_syllable_$index"
                )

                if (index < syllables.size - 1) {
                    textToSpeech?.playSilentUtterance(200, TextToSpeech.QUEUE_ADD, "${utteranceId}_pause_$index")
                }
            }
        }

        // 3. 播放中文意思
        textToSpeech?.playSilentUtterance(500, TextToSpeech.QUEUE_ADD, "${utteranceId}_pause2")

        // 使用中文TTS播放中文意思（复用实例）
        if (chineseTts == null) {
            chineseTts = TextToSpeech(this) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    chineseTts?.setLanguage(Locale.CHINESE)
                }
            }
        }
        chineseTts?.speak(
            word.meaning,
            TextToSpeech.QUEUE_ADD,
            createSpeechParams(),
            "${utteranceId}_meaning"
        )
    }

    private fun speakFullDetail(word: Word) {
        // 完整详情模式：单词 + 音节 + 中文 + 例句
        speakWordWithSyllablesAndMeaning(word)

        // 如果有例句，播放例句
        if (!word.example.isNullOrBlank()) {
            val utteranceId = "full_${word.id}_${System.currentTimeMillis()}"
            textToSpeech?.playSilentUtterance(500, TextToSpeech.QUEUE_ADD, "${utteranceId}_pause3")
            textToSpeech?.speak(
                "Example: ${word.example}",
                TextToSpeech.QUEUE_ADD,
                createSpeechParams(),
                "${utteranceId}_example"
            )
        }
    }

    private fun createSpeechParams(): Bundle {
        return Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            putFloat(TextToSpeech.Engine.KEY_PARAM_SPEED, _playbackSpeed.value)
        }
    }

    private fun playNextItem() {
        if (isPaused) return

        _currentIndex.value++
        if (_currentIndex.value >= wordList.size) {
            _currentIndex.value = 0 // 循环播放
        }

        playCurrentWord()
    }

    fun playNextWord() {
        textToSpeech?.stop()
        playNextItem()
    }

    fun playPreviousWord() {
        textToSpeech?.stop()
        _currentIndex.value--
        if (_currentIndex.value < 0) {
            _currentIndex.value = wordList.size - 1
        }
        playCurrentWord()
    }

    fun pausePlayback() {
        isPaused = true
        _isPlaying.value = false
        textToSpeech?.stop()
        updateNotification()
    }

    fun resumePlayback() {
        if (isPaused) {
            isPaused = false
            _isPlaying.value = true
            playCurrentWord()
            updateNotification()
        }
    }

    fun stopPlayback() {
        isPaused = false
        _isPlaying.value = false
        textToSpeech?.stop()
        stopForeground(true)
        stopSelf()
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed.coerceIn(0.5f, 2.0f)
        textToSpeech?.setSpeechRate(_playbackSpeed.value)
    }

    fun setPlayMode(mode: PlayMode) {
        _playMode.value = mode
    }

    fun seekToWord(index: Int) {
        if (index in wordList.indices) {
            textToSpeech?.stop()
            _currentIndex.value = index
            playCurrentWord()
        }
    }

    fun setWordList(words: List<Word>, startIndex: Int = 0) {
        wordList = words
        _currentIndex.value = startIndex.coerceIn(0, wordList.size - 1)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "单词播放",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "后台播放单词学习音频"
                setSound(null, null)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification() {
        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(): Notification {
        val word = _currentWord.value
        val title = word?.word ?: "单词学习"
        val content = buildString {
            append("${word?.meaning ?: ""}")
            if (wordList.isNotEmpty()) {
                append(" (${_currentIndex.value + 1}/${wordList.size})")
            }
        }

        // 创建点击通知打开应用的 PendingIntent
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 播放/暂停按钮
        val playPauseIntent = Intent(this, WordAudioService::class.java).apply {
            action = if (_isPlaying.value) ACTION_PAUSE else ACTION_PLAY
        }
        val playPausePendingIntent = PendingIntent.getService(
            this, 1, playPauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val playPauseIcon = if (_isPlaying.value) R.drawable.ic_pause else R.drawable.ic_play
        val playPauseTitle = if (_isPlaying.value) "暂停" else "播放"

        // 下一首按钮
        val nextIntent = Intent(this, WordAudioService::class.java).apply {
            action = ACTION_NEXT
        }
        val nextPendingIntent = PendingIntent.getService(
            this, 2, nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 上一首按钮
        val prevIntent = Intent(this, WordAudioService::class.java).apply {
            action = ACTION_PREVIOUS
        }
        val prevPendingIntent = PendingIntent.getService(
            this, 3, prevIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 停止按钮
        val stopIntent = Intent(this, WordAudioService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 4, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(openPendingIntent)
            .setOngoing(_isPlaying.value)
            .addAction(R.drawable.ic_previous, "上一首", prevPendingIntent)
            .addAction(playPauseIcon, playPauseTitle, playPausePendingIntent)
            .addAction(R.drawable.ic_next, "下一首", nextPendingIntent)
            .addAction(R.drawable.ic_stop, "停止", stopPendingIntent)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(1, 2)
            )
            .build()
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        chineseTts?.stop()
        chineseTts?.shutdown()
        serviceScope.cancel()
    }
}