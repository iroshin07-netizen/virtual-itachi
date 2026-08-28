package com.example.virtualfriend.overlay

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.*
import android.view.inputmethod.InputMethodManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.virtualfriend.MainActivity
import com.example.virtualfriend.R
import com.example.virtualfriend.chat.ChatManager
import com.example.virtualfriend.data.SettingsRepository
import com.example.virtualfriend.model.FriendAnimationState
import com.example.virtualfriend.model.FriendSettings
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

class FriendOverlayService : LifecycleService(), SavedStateRegistryOwner, ViewModelStoreOwner {
    companion object {
        const val ACTION_SUMMON = "com.example.virtualfriend.SUMMON"
        const val ACTION_TOGGLE = "com.example.virtualfriend.TOGGLE"
        const val ACTION_BRING_BACK = "com.example.virtualfriend.BRING_BACK"
        const val ACTION_PAUSE_30 = "com.example.virtualfriend.PAUSE_30"
        const val ACTION_PAUSE_60 = "com.example.virtualfriend.PAUSE_60"
        const val ACTION_PAUSE_TOMORROW = "com.example.virtualfriend.PAUSE_TOMORROW"
        const val ACTION_RESUME = "com.example.virtualfriend.RESUME"
        private const val CHANNEL_ID = "virtual_friend"
        private const val NOTIFICATION_ID = 77
    }

    private val savedStateController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateController.savedStateRegistry

    override val viewModelStore: ViewModelStore
        get() = store

    private lateinit var wm: WindowManager
    private lateinit var repo: SettingsRepository
    
    // UI LIVE Refresh fix (MutableStateFlow added)
    private val state = MutableStateFlow(FriendAnimationState.IDLE)
    private val message = MutableStateFlow<String?>(null)
    private val settings = MutableStateFlow(FriendSettings())
    private val chatMessages = MutableStateFlow(listOf<Pair<Boolean, String>>())
    
    private var petView: ComposeView? = null
    private var summonView: ComposeView? = null
    private var chatView: ComposeView? = null
    private var petParams: WindowManager.LayoutParams? = null
    private var summonParams: WindowManager.LayoutParams? = null
    private var reminderJob: Job? = null
    private var settingsJob: Job? = null
    private val chatManager by lazy { ChatManager(this) }
    private var controller: FriendAnimationController? = null

    override fun onCreate() {
        super.onCreate()
        savedStateController.performRestore(null)
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        repo = MainActivity.getRepo(this)
        
        createNotificationChannel()

        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTIFICATION_ID, notification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(NOTIFICATION_ID, notification())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        observeSettings()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SUMMON -> summon()
            ACTION_TOGGLE -> lifecycleScope.launch { repo.update { it.copy(enabled = !it.enabled) } }
            ACTION_BRING_BACK -> lifecycleScope.launch { repo.update { it.copy(petX = 40, petY = 300, summonX = 0, summonY = 600) }; bringBack() }
            ACTION_PAUSE_30 -> pauseFor(30)
            ACTION_PAUSE_60 -> pauseFor(60)
            ACTION_PAUSE_TOMORROW -> pauseUntilTomorrow()
            ACTION_RESUME -> lifecycleScope.launch { repo.update { it.copy(pauseUntil = 0L) } }
        }
        return START_STICKY
    }

    private fun observeSettings() {
        settingsJob?.cancel()
        settingsJob = lifecycleScope.launch {
            repo.settings.collect { s ->
                settings.value = s
                if (s.enabled && Settings.canDrawOverlays(this@FriendOverlayService)) {
                    ensureWindows()
                    startAnimationIfNeeded()
                } else {
                    removeWindows()
                    controller?.stop()
                }
            }
        }
    }

    private fun startAnimationIfNeeded() {
        if (controller != null) return
        controller = FriendAnimationController(
            scope = lifecycleScope,
            speedProvider = { settings.value.animationSpeed },
            onState = { state.value = it },
            onMessage = { message.value = it },
            onMove = { dx -> movePetBy(dx.toFloat(), 0f) },
            canMove = { !isPaused() }
        ).also { it.start() }
        startReminderLoop()
    }

    private fun ensureWindows() {
        if (!Settings.canDrawOverlays(this)) return
        if (petView == null) {
            // Box size increased from 400 to 600 for scaling fix
            petParams = overlayParams(600, 600, settings.value.petX, settings.value.petY, focusable = false)
            petView = ComposeView(this).apply {
                setViewTreeLifecycleOwner(this@FriendOverlayService)
                setViewTreeSavedStateRegistryOwner(this@FriendOverlayService)
                setViewTreeViewModelStoreOwner(this@FriendOverlayService)
                setContent {
                    // Observing values LIVE
                    val currentSettings by settings.collectAsState()
                    val currentState by state.collectAsState()
                    val currentMessage by message.collectAsState()

                    PetOverlayContent(
                        state = currentState,
                        sizeScale = currentSettings.friendSize,
                        message = currentMessage,
                        bubbleOnLeft = currentSettings.petX > screenWidth() / 2,
                        onDrag = ::movePetBy,
                        onClick = { showChatPanel() }
                    )
                }
            }
            runCatching { wm.addView(petView, petParams) }
        }
        if (settings.value.summonButtonEnabled && summonView == null) {
            summonParams = overlayParams(100, 100, settings.value.summonX, settings.value.summonY, focusable = false)
            summonView = ComposeView(this).apply {
                setViewTreeLifecycleOwner(this@FriendOverlayService)
                setViewTreeSavedStateRegistryOwner(this@FriendOverlayService)
                setViewTreeViewModelStoreOwner(this@FriendOverlayService)
                setContent { SummonButtonContent(::moveSummonBy, ::summon) }
            }
            runCatching { wm.addView(summonView, summonParams) }
        }
        if (!settings.value.summonButtonEnabled) removeSummon()
    }

    private fun overlayParams(w: Int, h: Int, x: Int, y: Int, focusable: Boolean): WindowManager.LayoutParams =
        WindowManager.LayoutParams(
            w, h,
            if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            (if (!focusable) WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE else 0) or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = x
            this.y = y
            if (focusable) softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }

    private fun movePetBy(dx: Float, dy: Float) {
        val p = petParams ?: return
        p.x = clamp(p.x + dx.toInt(), -120, max(0, screenWidth() - p.width + 30))
        p.y = clamp(p.y + dy.toInt(), 0, max(0, screenHeight() - p.height))
        runCatching { wm.updateViewLayout(petView, p) }
        lifecycleScope.launch { repo.update { it.copy(petX = p.x, petY = p.y) } }
    }

    private fun moveSummonBy(dx: Float, dy: Float) {
        val p = summonParams ?: return
        p.x = clamp(p.x + dx.toInt(), 0, max(0, screenWidth() - p.width))
        p.y = clamp(p.y + dy.toInt(), 0, max(0, screenHeight() - p.height))
        runCatching { wm.updateViewLayout(summonView, p) }
        lifecycleScope.launch { repo.update { it.copy(summonX = p.x, summonY = p.y) } }
    }

    private fun startReminderLoop() {
        reminderJob?.cancel()
        reminderJob = lifecycleScope.launch {
            var visitElapsed = 0L
            var waterElapsed = 0L
            while (true) {
                delay(60_000L)
                if (isPaused()) continue
                visitElapsed += 1
                waterElapsed += 1
                val s = settings.value
                if (s.checkInsEnabled && visitElapsed >= s.visitMinutes) {
                    visitElapsed = 0
                    visit()
                }
                if (s.waterEnabled && waterElapsed >= s.waterMinutes) {
                    waterElapsed = 0
                    waterVisit()
                }
            }
        }
    }

    private fun visit() {
        if (!settings.value.enabled) return
        controller?.summon()
        lifecycleScope.launch {
            delay(850)
            controller?.speak(listOf("Hi! How are you doing?", "Just checking in.", "Quick stretch?", "Still doing okay?").random())
        }
    }

    private fun waterVisit() {
        if (!settings.value.enabled) return
        controller?.summon()
        lifecycleScope.launch {
            delay(850)
            controller?.speak(listOf("Water check? \uD83D\uDCA7", "Have you had some water?", "Hydration break?").random())
        }
    }

    private fun summon() {
        if (!settings.value.enabled) return
        ensureWindows()
        controller?.summon()
    }

    private fun bringBack() {
        ensureWindows()
        val p = petParams ?: return
        p.x = 40; p.y = 300
        runCatching { wm.updateViewLayout(petView, p) }
        val s = summonParams ?: return
        s.x = 0; s.y = 600
        runCatching { wm.updateViewLayout(summonView, s) }
    }

    private fun showChatPanel() {
        if (chatView != null) return
        val p = petParams ?: return
        val x = clamp(p.x - 20, 8, max(8, screenWidth() - 328))
        val y = clamp(p.y - 430, 20, max(20, screenHeight() - 450))
        val params = overlayParams(320, 430, x, y, focusable = true)
        
        chatView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FriendOverlayService)
            setViewTreeSavedStateRegistryOwner(this@FriendOverlayService)
            setViewTreeViewModelStoreOwner(this@FriendOverlayService)
            setContent {
                // Observing messages LIVE
                val currentMessages by chatMessages.collectAsState()
                val currentSettings by settings.collectAsState()
                
                ChatPanelContent(
                    messages = currentMessages,
                    onSend = { text ->
                        chatMessages.value = chatMessages.value + (true to text)
                        val reply = chatManager.reply(text, currentSettings.aiChatEnabled, chatMessages.value.map { it.second to "" })
                        chatMessages.value = chatMessages.value + (false to reply)
                    },
                    onClose = ::hideChatPanel
                )
            }
            isFocusableInTouchMode = true
            requestFocus()
        }
        runCatching { wm.addView(chatView, params) }
        chatView?.post {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(chatView, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun hideChatPanel() {
        val view = chatView ?: return
        runCatching { wm.removeView(view) }
        chatView = null
    }

    private fun removeSummon() {
        summonView?.let { runCatching { wm.removeView(it) } }
        summonView = null
        summonParams = null
    }

    private fun removeWindows() {
        hideChatPanel()
        petView?.let { runCatching { wm.removeView(it) } }
        petView = null; petParams = null
        removeSummon()
    }

    private fun isPaused(): Boolean {
        val now = System.currentTimeMillis()
        if (settings.value.pauseUntil > now) return true
        val c = java.util.Calendar.getInstance()
        val minutes = c.get(java.util.Calendar.HOUR_OF_DAY) * 60 + c.get(java.util.Calendar.MINUTE)
        val start = settings.value.quietStart
        val end = settings.value.quietEnd
        val quiet = if (start <= end) (start until end).contains(minutes) else minutes >= start || minutes < end
        return quiet
    }

    private fun pauseFor(minutes: Int) = lifecycleScope.launch { repo.update { it.copy(pauseUntil = System.currentTimeMillis() + minutes * 60_000L) } }

    private fun pauseUntilTomorrow() = lifecycleScope.launch {
        val c = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, 1); set(java.util.Calendar.HOUR_OF_DAY, 7); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0) }
        repo.update { it.copy(pauseUntil = c.timeInMillis) }
    }

    private fun clamp(v: Int, minV: Int, maxV: Int) = min(max(v, minV), maxV)
    private fun screenWidth() = resources.displayMetrics.widthPixels
    private fun screenHeight() = resources.displayMetrics.heightPixels

    private fun notification(): Notification {
        val intent = Intent(this, com.example.virtualfriend.MainActivity::class.java)
        val pi = PendingIntent.getActivity(this, 1, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.itachi_front)
            .setContentTitle("Virtual Friend is active")
            .setContentText("Your floating companion is nearby.")
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(CHANNEL_ID, "Virtual Friend", NotificationManager.IMPORTANCE_LOW)
            channel.description = "Keeps the floating companion service running."
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        reminderJob?.cancel(); settingsJob?.cancel(); controller?.stop(); removeWindows(); super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? = super.onBind(intent)
}
