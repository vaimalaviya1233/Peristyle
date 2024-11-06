package app.simple.peri.abstraction

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import app.simple.peri.R
import app.simple.peri.database.instances.TagsDatabase
import app.simple.peri.database.instances.WallpaperDatabase
import app.simple.peri.models.Wallpaper
import app.simple.peri.preferences.MainComposePreferences
import app.simple.peri.preferences.MainPreferences
import app.simple.peri.receivers.WallpaperActionReceiver
import app.simple.peri.utils.BitmapUtils
import app.simple.peri.utils.BitmapUtils.applyEffects
import app.simple.peri.utils.BitmapUtils.cropBitmap
import app.simple.peri.utils.ConditionUtils.invert
import app.simple.peri.utils.ConditionUtils.isNotNull
import app.simple.peri.utils.FileUtils.toFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File

abstract class AbstractComposeAutoWallpaperService : AbstractLegacyAutoWallpaperService() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    protected fun setComposeWallpaper() {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                when {
                    MainPreferences.isSettingForBoth() -> {
                        if (shouldSetSameWallpaper()) {
                            setSameWallpaper()
                        } else {
                            setHomeScreenWallpaper()
                            setLockScreenWallpaper()
                        }
                    }
                    MainPreferences.isSettingForHomeScreen() -> {
                        setHomeScreenWallpaper()
                    }
                    MainPreferences.isSettingForLockScreen() -> {
                        setLockScreenWallpaper()
                    }
                }

                withContext(Dispatchers.Main) {
                    stopSelf()
                }
            }.getOrElse {
                it.printStackTrace()
                Log.e(TAG, "Error setting wallpaper: $it")
            }
        }
    }

    private fun setHomeScreenWallpaper() {
        getHomeScreenWallpaper()?.let {
            Log.d(TAG, "Home wallpaper found: ${it.filePath}")
            getBitmapFromFile(it) { bitmap ->
                val modifiedBitmap = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
                    .applyEffects(MainComposePreferences.getHomeScreenEffects())
                wallpaperManager.setBitmap(modifiedBitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                showWallpaperChangedNotification(true, it.filePath.toFile())
            }
        }
    }

    private fun setLockScreenWallpaper() {
        getLockScreenWallpaper()?.let {
            Log.d(TAG, "Lock wallpaper found: ${it.filePath}")
            getBitmapFromFile(it) { bitmap ->
                val modifiedBitmap = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
                    .applyEffects(MainComposePreferences.getLockScreenEffects())
                wallpaperManager.setBitmap(modifiedBitmap, null, true, WallpaperManager.FLAG_LOCK)
                showWallpaperChangedNotification(false, it.filePath.toFile())
            }
        }
    }

    private fun setSameWallpaper() {
        getHomeScreenWallpaper()?.let { wallpaper ->
            MainComposePreferences.setLastLockWallpaperPosition(MainComposePreferences.getLastHomeWallpaperPosition())
            Log.d(TAG, "Wallpaper found: ${wallpaper.filePath}")
            getBitmapFromFile(wallpaper) { bitmap ->
                var homeBitmap = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
                var lockBitmap = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)

                homeBitmap = homeBitmap.applyEffects(MainComposePreferences.getWallpaperEffects())
                lockBitmap = lockBitmap.applyEffects(MainComposePreferences.getWallpaperEffects())

                wallpaperManager.setBitmap(homeBitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                wallpaperManager.setBitmap(lockBitmap, null, true, WallpaperManager.FLAG_LOCK)
                showWallpaperChangedNotification(true, wallpaper.filePath.toFile())
            }
        }
    }

    private fun shouldSetSameWallpaper(): Boolean {
        if (!MainPreferences.isSettingForHomeScreen() || !MainPreferences.isSettingForLockScreen()) {
            return false
        }

        return isSameFolderOrTagUsed() && MainPreferences.isLinearAutoWallpaper()
    }

    private fun isSameFolderOrTagUsed(): Boolean {
        if (MainComposePreferences.isHomeSourceSet() && MainComposePreferences.isLockSourceSet()) {
            if (MainComposePreferences.getHomeTagId() == MainComposePreferences.getLockTagId()) {
                return true
            }
            if (MainComposePreferences.getHomeFolderId() == MainComposePreferences.getLockFolderId()) {
                return true
            }
        }

        return false
    }

    private fun getBitmapFromFile(wallpaper: Wallpaper, onBitmap: (Bitmap) -> Unit) {
        wallpaper.filePath.toFile().inputStream().use { stream ->
            val byteArray = stream.readBytes()
            Log.i(TAG, "Compose Wallpaper URI Decoding: ${wallpaper.uri}")
            var bitmap = decodeBitmap(byteArray)

            // Correct orientation of the bitmap if faulty due to EXIF data
            bitmap = BitmapUtils.correctOrientation(bitmap, ByteArrayInputStream(byteArray))

            val visibleCropHint = calculateVisibleCropHint(bitmap)

            if (MainPreferences.getCropWallpaper()) {
                bitmap = bitmap.cropBitmap(visibleCropHint)
            }

            onBitmap(bitmap)

            bitmap.recycle()
        }
    }

    private fun getHomeScreenWallpaper(): Wallpaper? {
        val wallpaperDatabase = WallpaperDatabase.getInstance(applicationContext)
        val wallpaperDao = wallpaperDatabase?.wallpaperDao()
        val position = MainComposePreferences.getLastHomeWallpaperPosition().plus(1)
        val wallpaper: Wallpaper?

        val tagId = MainComposePreferences.getHomeTagId()
        val folderId = MainComposePreferences.getHomeFolderId()

        when {
            tagId.isNotNull() -> {
                val tagsDatabase = TagsDatabase.getInstance(applicationContext)
                val tagsDao = tagsDatabase?.tagsDao()
                val tag = tagsDao?.getTagByID(tagId!!)
                val wallpapers = wallpaperDao?.getWallpapersByMD5s(tag?.sum!!)

                wallpaper = getWallpaperFromList(wallpapers, position, isHomeScreen = true)
            }

            folderId.isNotNull() -> {
                val wallpapers = wallpaperDao?.getWallpapersByPathHashcode(folderId)
                wallpaper = getWallpaperFromList(wallpapers, position, isHomeScreen = true)
            }

            else -> {
                wallpaper = wallpaperDao?.getRandomWallpaper()
            }
        }

        return wallpaper
    }

    private fun getLockScreenWallpaper(): Wallpaper? {
        val wallpaperDatabase = WallpaperDatabase.getInstance(applicationContext)
        val wallpaperDao = wallpaperDatabase?.wallpaperDao()
        val position = MainComposePreferences.getLastLockWallpaperPosition().plus(1)
        val wallpaper: Wallpaper?

        val tagId = MainComposePreferences.getLockTagId()
        val folderId = MainComposePreferences.getLockFolderId()

        when {
            tagId.isNotNull() -> {
                val tagsDatabase = TagsDatabase.getInstance(applicationContext)
                val tagsDao = tagsDatabase?.tagsDao()
                val tag = tagsDao?.getTagByID(tagId!!)
                val wallpapers = wallpaperDao?.getWallpapersByMD5s(tag?.sum!!)

                wallpaper = getWallpaperFromList(wallpapers, position, false)
            }

            folderId.isNotNull() -> {
                val wallpapers = wallpaperDao?.getWallpapersByPathHashcode(folderId)
                wallpaper = getWallpaperFromList(wallpapers, position, false)
            }

            else -> {
                wallpaper = wallpaperDao?.getRandomWallpaper()
            }
        }

        return wallpaper
    }

    private fun getWallpaperFromList(wallpapers: List<Wallpaper>?, position: Int, isHomeScreen: Boolean): Wallpaper? {
        return if (MainPreferences.isLinearAutoWallpaper()) {
            try {
                wallpapers?.get(position).also {
                    MainComposePreferences.setLastWallpaperPosition(isHomeScreen, position)
                }
            } catch (e: IndexOutOfBoundsException) {
                MainComposePreferences.resetLastWallpaperPosition(isHomeScreen)
                wallpapers?.get(0)
            }
        } else {
            wallpapers?.random()
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val homeChannel = NotificationChannel(
                    CHANNEL_ID_HOME,
                    "Home Screen Wallpaper",
                    NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for home screen wallpaper changes"
            }

            val lockChannel = NotificationChannel(
                    CHANNEL_ID_LOCK,
                    "Lock Screen Wallpaper",
                    NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for lock screen wallpaper changes"
            }

            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(homeChannel)
            notificationManager.createNotificationChannel(lockChannel)
        }
    }

    private fun showWallpaperChangedNotification(isHomeScreen: Boolean, file: File) {
        Log.i(TAG, "Showing notification for wallpaper change for file: ${file.absolutePath}")
        if (MainComposePreferences.getAutoWallpaperNotification().invert()) {
            return
        }

        val channelId = if (isHomeScreen) CHANNEL_ID_HOME else CHANNEL_ID_LOCK
        val notificationId = if (isHomeScreen) HOME_NOTIFICATION_ID else LOCK_NOTIFICATION_ID
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.cancel(notificationId) // Clear existing notification

        val deleteIntent = Intent(this, WallpaperActionReceiver::class.java).apply {
            action = if (isHomeScreen) ACTION_DELETE_WALLPAPER_HOME else ACTION_DELETE_WALLPAPER_LOCK
            putExtra(EXTRA_IS_HOME_SCREEN, isHomeScreen)
            putExtra(EXTRA_WALLPAPER_PATH, file.absolutePath)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }

        val sendIntent = createSendIntent(file, this)

        val deletePendingIntent: PendingIntent = PendingIntent.getBroadcast(
                this, notificationId, deleteIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val sendPendingIntent: PendingIntent = PendingIntent.getActivity(
                this, notificationId, Intent.createChooser(sendIntent, null), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_peristyle)
            .setContentTitle(applicationContext.getString(R.string.auto_wallpaper))
            .setContentText(applicationContext.getString(
                    R.string.wallpaper_changed,
                    if (isHomeScreen) {
                        applicationContext.getString(R.string.home_screen)
                    } else {
                        applicationContext.getString(R.string.lock_screen)
                    }))
            .addAction(R.drawable.ic_delete, applicationContext.getString(R.string.delete_current_wallpaper), deletePendingIntent)
            .addAction(R.drawable.ic_share, applicationContext.getString(R.string.send), sendPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSilent(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    private fun createSendIntent(file: File, context: Context): Intent {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    companion object {
        const val ACTION_NEXT_WALLPAPER: String = "app.simple.peri.services.action.NEXT_WALLPAPER"
        const val ACTION_DELETE_WALLPAPER: String = "app.simple.peri.services.action.DELETE_WALLPAPER"
        const val ACTION_DELETE_WALLPAPER_HOME = "app.simple.peri.services.action.DELETE_WALLPAPER_HOME"
        const val ACTION_DELETE_WALLPAPER_LOCK = "app.simple.peri.services.action.DELETE_WALLPAPER_LOCK"

        const val EXTRA_IS_HOME_SCREEN = "app.simple.peri.services.extra.IS_HOME_SCREEN"
        const val EXTRA_WALLPAPER_PATH = "app.simple.peri.services.extra.PATH"
        const val EXTRA_NOTIFICATION_ID = "app.simple.peri.services.extra.NOTIFICATION_ID"

        const val TAG = "AutoWallpaperService"
        private const val CHANNEL_ID_HOME = "wallpaper_home_channel"
        private const val CHANNEL_ID_LOCK = "wallpaper_lock_channel"

        const val HOME_NOTIFICATION_ID = 1234
        const val LOCK_NOTIFICATION_ID = 5367
    }
}