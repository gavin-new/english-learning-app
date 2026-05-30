package com.englishlearning.app.audio

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale
import java.util.UUID

/**
 * 音频播放管理器
 * 管理TTS语音合成和音频播放
 * 支持调速播放
 */
class AudioPlayerManager(context: Context) {

    companion object {
        private const val TAG = "AudioPlayerManager"
    }

    // TTS实例
    private var textToSpeech: TextToSpeech? = null
    
    // 播放状态
    private var isInitialized = false
    private var currentUtteranceId: String? = null
    
    // 播放速度（0.5 - 2.0）
    var playbackSpeed by mutableStateOf(1.0f)
        private set
    
    // 是否正在播放
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying
    
    // 当前播放的文本
    private val _currentText = MutableStateFlow<String?>(null)
    val currentText: StateFlow<String?> = _currentText
    
    // 播放队列
    private val playQueue = mutableListOf<String>()
    private var currentQueueIndex = 0
    
    // 播放完成回调
    private var onCompletionListener: (() -> Unit)? = null
    
    // 播放进度回调
    private var onProgressListener: ((String) -> Unit)? = null

    init {
        initializeTTS(context)
    }

    /**
     * 初始化TTS
     */
    private fun initializeTTS(context: Context) {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                
                // 设置语言为美式英语
                val result = textToSpeech?.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w(TAG, "English language not supported, using default")
                    textToSpeech?.setLanguage(Locale.ENGLISH)
                }
                
                // 设置语速
                textToSpeech?.setSpeechRate(playbackSpeed)
                
                // 设置音调
                textToSpeech?.setPitch(1.0f)
                
                // 设置播放监听器
                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isPlaying.value = true
                        Log.d(TAG, "TTS started: $utteranceId")
                    }

                    override fun onDone(utteranceId: String?) {
                        _isPlaying.value = false
                        Log.d(TAG, "TTS completed: $utteranceId")
                        
                        // 播放下一个队列项
                        playNextInQueue()
                    }

                    override fun onError(utteranceId: String?) {
                        _isPlaying.value = false
                        Log.e(TAG, "TTS error: $utteranceId")
                        
                        // 错误时也继续播放下一个
                        playNextInQueue()
                    }
                })
                
                Log.d(TAG, "TTS initialized successfully")
            } else {
                Log.e(TAG, "TTS initialization failed")
            }
        }
    }

    /**
     * 播放文本
     */
    fun speak(text: String, onComplete: (() -> Unit)? = null) {
        if (!isInitialized) {
            Log.w(TAG, "TTS not initialized yet")
            return
        }

        stop() // 停止当前播放
        
        onCompletionListener = onComplete
        _currentText.value = text
        
        currentUtteranceId = UUID.randomUUID().toString()
        
        val params = HashMap<String, String>()
        params[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = currentUtteranceId!!
        
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, params)
        
        Log.d(TAG, "Speaking: $text")
    }

    /**
     * 添加文本到播放队列
     */
    fun enqueue(text: String) {
        playQueue.add(text)
        
        // 如果当前没有在播放，开始播放
        if (!_isPlaying.value && playQueue.isNotEmpty()) {
            playNextInQueue()
        }
    }

    /**
     * 播放队列中的下一个
     */
    private fun playNextInQueue() {
        if (currentQueueIndex < playQueue.size) {
            val text = playQueue[currentQueueIndex]
            currentQueueIndex++
            
            _currentText.value = text
            currentUtteranceId = UUID.randomUUID().toString()
            
            val params = HashMap<String, String>()
            params[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = currentUtteranceId!!
            
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, params)
            
            onProgressListener?.invoke(text)
        } else {
            // 队列播放完成
            onCompletionListener?.invoke()
            clearQueue()
        }
    }

    /**
     * 清空播放队列
     */
    fun clearQueue() {
        playQueue.clear()
        currentQueueIndex = 0
    }

    /**
     * 设置播放队列并播放
     */
    fun playQueue(texts: List<String>, onComplete: (() -> Unit)? = null) {
        clearQueue()
        playQueue.addAll(texts)
        currentQueueIndex = 0
        onCompletionListener = onComplete
        
        if (playQueue.isNotEmpty()) {
            playNextInQueue()
        }
    }

    /**
     * 停止播放
     */
    fun stop() {
        textToSpeech?.stop()
        _isPlaying.value = false
        _currentText.value = null
    }

    /**
     * 暂停播放（TTS不支持真正的暂停，只能停止）
     */
    fun pause() {
        stop()
    }

    /**
     * 设置播放速度
     * @param speed 0.5 - 2.0
     */
    fun setSpeed(speed: Float) {
        playbackSpeed = speed.coerceIn(0.5f, 2.0f)
        textToSpeech?.setSpeechRate(playbackSpeed)
        Log.d(TAG, "Playback speed set to: $playbackSpeed")
    }

    /**
     * 设置播放完成监听器
     */
    fun setOnCompletionListener(listener: () -> Unit) {
        onCompletionListener = listener
    }

    /**
     * 设置播放进度监听器
     */
    fun setOnProgressListener(listener: (String) -> Unit) {
        onProgressListener = listener
    }

    /**
     * 释放资源
     */
    fun release() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        isInitialized = false
    }
}
