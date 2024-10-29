package app.simple.peri.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import app.simple.peri.R
import app.simple.peri.activities.LegacyActivity
import app.simple.peri.activities.MainComposeActivity
import app.simple.peri.database.instances.TagsDatabase
import app.simple.peri.database.instances.WallpaperDatabase
import app.simple.peri.models.Wallpaper
import app.simple.peri.preferences.MainComposePreferences
import app.simple.peri.preferences.MainPreferences
import app.simple.peri.preferences.SharedPreferences
import app.simple.peri.receivers.WallpaperActionReceiver
import app.simple.peri.utils.BitmapUtils
import app.simple.peri.utils.BitmapUtils.applyEffects
import app.simple.peri.utils.BitmapUtils.cropBitmap
import app.simple.peri.utils.ConditionUtils.invert
import app.simple.peri.utils.ConditionUtils.isNotNull
import app.simple.peri.utils.ConditionUtils.isNull
import app.simple.peri.utils.FileUtils.toFile
import app.simple.peri.utils.ScreenUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File

class AutoWallpaperService : Service() {

    /**
     * Flag to prevent multiple next wallpaper actions from running at the same time
     * This is necessary because the widget can be clicked multiple times in a short period of time
     */
    private var isNextWallpaperActionRunning = false

    private val displayWidth: Int by lazy {
        ScreenUtils.getScreenSize(applicationContext).width
    }

    private val displayHeight: Int by lazy {
        ScreenUtils.getScreenSize(applicationContext).height
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")

        when (intent?.action) {
            ACTION_NEXT_WALLPAPER -> {
                Log.d(TAG, "Next wallpaper action received")
                if (!isNextWallpaperActionRunning) {
                    isNextWallpaperActionRunning = true
                    runCatching {
                        Toast.makeText(applicationContext, R.string.changing_wallpaper, Toast.LENGTH_SHORT)
                            .show()
                    }
                    init()
                    isNextWallpaperActionRunning = false
                } else {
                    Log.d(TAG, "Next wallpaper action already running, ignoring")
                    Toast.makeText(applicationContext, R.string.next_wallpaper_already_running, Toast.LENGTH_SHORT)
                        .show()
                }
            }
            ACTION_DELETE_WALLPAPER -> {
                val file = File(intent.getStringExtra(EXTRA_WALLPAPER_PATH)!!)
                Log.i(TAG, "Deleting wallpaper: ${file.absolutePath}")
                if (file.exists()) {
                    Log.i(TAG, "File exists, deleting")
                    file.delete()
                    Log.i(TAG, "File deleted")
                } else {
                    Log.e(TAG, "File does not exist, skipping")
                }
            }
            else -> {
                init()
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
    }

    private fun init() {
        SharedPreferences.init(this)

        if (isLegacyInterface()) {
            Log.i(TAG, "Legacy interface detected, switching to old approach")
            if (MainPreferences.isWallpaperWhenSleeping()) {
                setWallpaper()
                Log.d(TAG, "Wallpaper set when the user is sleeping")
            } else {
                if (ScreenUtils.isDeviceSleeping(applicationContext)) {
                    Log.d(TAG, "Device is sleeping, waiting for next alarm to set wallpaper")
                } else {
                    setWallpaper()
                }
            }
        } else {
            Log.i(TAG, "Compose interface detected, switching to new approach")
            setWallpaperCompose()
        }
    }

    private fun setWallpaper() {
        CoroutineScope(Dispatchers.Default).launch {
            runCatching {
                val files = getWallpapersFromDatabase()
                if (MainPreferences.isTweakOptionSelected(MainPreferences.LINEAR_AUTO_WALLPAPER)) {
                    if (MainPreferences.getLastWallpaperPosition() >= (files?.size?.minus(1)
                                ?: 0)
                    ) {
                        files?.get(0)?.uri?.toUri()?.let { uri ->
                            setWallpaperFromUri(uri, files)
                            MainPreferences.setLastWallpaperPosition(0)
                        }
                    } else {
                        files?.get(MainPreferences.getLastWallpaperPosition().plus(1))?.uri?.toUri()
                            ?.let { uri ->
                                setWallpaperFromUri(uri, files)
                                MainPreferences.setLastWallpaperPosition(MainPreferences.getLastWallpaperPosition() + 1)
                            }
                    }
                } else {
                    files?.random()?.uri?.toUri()?.let { uri ->
                        setWallpaperFromUri(uri, files)
                        MainPreferences.setLastWallpaperPosition(files.indexOf(files.find { it.uri == uri.toString() }))
                    }
                }
            }.getOrElse {
                Log.e(TAG, "Error setting wallpaper: $it")
            }
        }
    }

    private suspend fun getWallpapersFromDatabase(): List<Wallpaper>? {
        return withContext(Dispatchers.IO) {
            val dao = WallpaperDatabase.getInstance(applicationContext)?.wallpaperDao()
            dao?.sanitizeEntries()
            dao?.getWallpapers()
        }
    }

    private suspend fun setWallpaperFromUri(uri: Uri, files: List<Wallpaper>) {
        withContext(Dispatchers.IO) {
            val wallpaperManager = WallpaperManager.getInstance(applicationContext)
            contentResolver.openInputStream(uri)?.use { stream ->
                val byteArray = stream.readBytes()
                var bitmap = decodeBitmap(byteArray)

                // Correct orientation of the bitmap if faulty due to EXIF data
                bitmap = BitmapUtils.correctOrientation(bitmap, ByteArrayInputStream(byteArray))

                val visibleCropHint = calculateVisibleCropHint(bitmap)

                if (MainPreferences.getCropWallpaper()) {
                    bitmap = bitmap.cropBitmap(visibleCropHint)
                }

                setWallpaperBasedOnPreference(bitmap, wallpaperManager, files)

                bitmap.recycle()

                withContext(Dispatchers.Main) {
                    stopSelf()
                }
            }
        }
    }

    private fun decodeBitmap(byteArray: ByteArray): Bitmap {
        val bitmapOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        BitmapFactory.decodeStream(ByteArrayInputStream(byteArray), null, bitmapOptions)

        return BitmapFactory.decodeStream(
                ByteArrayInputStream(byteArray), null, BitmapFactory.Options().apply {
            inPreferredConfig = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Bitmap.Config.RGBA_1010102
            } else {
                Bitmap.Config.ARGB_8888
            }

            inMutable = true

            Log.d(TAG, "Expected bitmap size: $displayWidth x $displayHeight")
            inSampleSize =
                BitmapUtils.calculateInSampleSize(bitmapOptions, displayWidth, displayHeight)
            inJustDecodeBounds = false
            Log.d(TAG, "Bitmap decoded with sample size: ${this.inSampleSize}")
        })!!
    }

    private fun calculateVisibleCropHint(bitmap: Bitmap): Rect {
        // Calculate the aspect ratio of the display
        val aspectRatio = displayWidth.toFloat() / displayHeight.toFloat()
        // Calculate the aspect ratio of the bitmap
        val bitmapAspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()

        // Determine the crop width and height based on the aspect ratios
        val (cropWidth, cropHeight) = if (bitmapAspectRatio > aspectRatio) {
            // If the bitmap is wider than the desired aspect ratio
            val width = (bitmap.height * aspectRatio).toInt()
            width to bitmap.height
        } else {
            // If the bitmap is taller than the desired aspect ratio
            val height = (bitmap.width / aspectRatio).toInt()
            bitmap.width to height
        }

        // Calculate the left, top, right, and bottom coordinates for the crop rectangle
        val left = (bitmap.width - cropWidth) / 2
        val top = (bitmap.height - cropHeight) / 2
        val right = left + cropWidth
        val bottom = top + cropHeight

        // Return the calculated crop rectangle
        return Rect(left, top, right, bottom)
    }

    private fun setWallpaperBasedOnPreference(bitmap: Bitmap, wallpaperManager: WallpaperManager, files: List<Wallpaper>) {
        when (MainPreferences.getWallpaperSetFor()) {
            MainPreferences.BOTH -> {
                if (MainPreferences.isDifferentWallpaperForLockScreen()) {
                    wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                    setLockScreenWallpaper(files)
                } else {
                    /**
                     * Setting them separately to avoid the wallpaper not setting
                     * in some devices for lock screen.
                     */
                    wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                    wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                }
            }

            MainPreferences.HOME -> {
                wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
            }

            MainPreferences.LOCK -> {
                wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
            }
        }
    }

    private fun setLockScreenWallpaper(files: List<Wallpaper>?) {
        runCatching {
            files?.random()?.uri?.toUri()?.let { uri ->
                setLockScreenWallpaperFromUri(uri)
            }
        }.getOrElse {
            Log.e(TAG, "Error setting wallpaper: $it")
            Log.d(TAG, "Service stopped, wait for next alarm to start again")
        }
    }

    private fun setLockScreenWallpaperFromUri(uri: Uri) {
        val wallpaperManager = WallpaperManager.getInstance(applicationContext)
        contentResolver.openInputStream(uri)?.use { stream ->
            val byteArray = stream.readBytes()
            var bitmap = decodeBitmap(byteArray)

            // Correct orientation of the bitmap if faulty due to EXIF data
            bitmap = BitmapUtils.correctOrientation(bitmap, ByteArrayInputStream(byteArray))

            val visibleCropHint = calculateVisibleCropHint(bitmap)

            if (MainPreferences.getCropWallpaper()) {
                bitmap = bitmap.cropBitmap(visibleCropHint)
            }

            wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)

            bitmap.recycle()
        }
    }

    private fun isLegacyInterface(): Boolean {
        return applicationContext.packageManager.getComponentEnabledSetting(
                ComponentName(applicationContext, LegacyActivity::class.java)
        ) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED && applicationContext.packageManager.getComponentEnabledSetting(
                ComponentName(applicationContext, MainComposeActivity::class.java)
        ) == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
    }

    // ----------------------------------------- Compose Interface Settings ----------------------------------------- //

    private fun setWallpaperCompose() {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val wallpaperManager = WallpaperManager.getInstance(applicationContext)
                var homeWallpaper: Wallpaper? = getHomeScreenWallpaper()
                var lockWallpaper: Wallpaper? = getLockScreenWallpaper()

                if (homeWallpaper.isNotNull()) {
                    Log.d(TAG, "Home wallpaper found: ${homeWallpaper?.filePath}")
                    getBitmapFromFile(homeWallpaper!!) {
                        var bitmap = it.copy(it.config ?: Bitmap.Config.ARGB_8888, true)
                        bitmap = bitmap.applyEffects(
                                brightness = MainComposePreferences.getAutoWallpaperHomeBrightness(),
                                contrast = MainComposePreferences.getAutoWallpaperHomeContrast(),
                                blur = MainComposePreferences.getAutoWallpaperHomeBlur(),
                                saturation = MainComposePreferences.getAutoWallpaperHomeSaturation(),
                                hueRed = MainComposePreferences.getAutoWallpaperHomeHueRed(),
                                hueGreen = MainComposePreferences.getAutoWallpaperHomeHueGreen(),
                                hueBlue = MainComposePreferences.getAutoWallpaperHomeHueBlue(),
                                scaleRed = MainComposePreferences.getAutoWallpaperHomeScaleRed(),
                                scaleGreen = MainComposePreferences.getAutoWallpaperHomeScaleGreen(),
                                scaleBlue = MainComposePreferences.getAutoWallpaperHomeScaleBlue()
                        )

                        wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                        showWallpaperChangedNotification(true, homeWallpaper!!.filePath.toFile())
                    }
                }

                if (lockWallpaper.isNotNull()) {
                    Log.d(TAG, "Lock wallpaper found: ${lockWallpaper?.filePath}")
                    getBitmapFromFile(lockWallpaper!!) {
                        var bitmap = it.copy(it.config ?: Bitmap.Config.ARGB_8888, true)
                        bitmap = bitmap.applyEffects(
                                brightness = MainComposePreferences.getAutoWallpaperLockBrightness(),
                                contrast = MainComposePreferences.getAutoWallpaperLockContrast(),
                                blur = MainComposePreferences.getAutoWallpaperLockBlur(),
                                saturation = MainComposePreferences.getAutoWallpaperLockSaturation(),
                                hueRed = MainComposePreferences.getAutoWallpaperLockHueRed(),
                                hueGreen = MainComposePreferences.getAutoWallpaperLockHueGreen(),
                                hueBlue = MainComposePreferences.getAutoWallpaperLockHueBlue(),
                                scaleRed = MainComposePreferences.getAutoWallpaperLockScaleRed(),
                                scaleGreen = MainComposePreferences.getAutoWallpaperLockScaleGreen(),
                                scaleBlue = MainComposePreferences.getAutoWallpaperLockScaleBlue()
                        )

                        wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                        showWallpaperChangedNotification(false, lockWallpaper!!.filePath.toFile())
                    }
                }

                when {
                    lockWallpaper.isNull() && homeWallpaper.isNull() -> {
                        Log.d(TAG, "No wallpapers found, setting random wallpaper")
                        homeWallpaper = getWallpapersFromDatabase()?.random()
                        lockWallpaper = if (MainPreferences.isLinearAutoWallpaper()) {
                            homeWallpaper
                        } else {
                            getWallpapersFromDatabase()?.random()
                        }

                        if (MainPreferences.isSettingForHomeScreen()) {
                            getBitmapFromFile(homeWallpaper!!) {
                                var bitmap = it.copy(it.config ?: Bitmap.Config.ARGB_8888, true)
                                bitmap = bitmap.applyEffects(
                                        brightness = MainComposePreferences.getAutoWallpaperBrightness(),
                                        contrast = MainComposePreferences.getAutoWallpaperContrast(),
                                        blur = MainComposePreferences.getAutoWallpaperBlur(),
                                        saturation = MainComposePreferences.getAutoWallpaperSaturation(),
                                        hueRed = MainComposePreferences.getAutoWallpaperHueRed(),
                                        hueGreen = MainComposePreferences.getAutoWallpaperHueGreen(),
                                        hueBlue = MainComposePreferences.getAutoWallpaperHueBlue(),
                                        scaleRed = MainComposePreferences.getAutoWallpaperScaleRed(),
                                        scaleGreen = MainComposePreferences.getAutoWallpaperScaleGreen(),
                                        scaleBlue = MainComposePreferences.getAutoWallpaperScaleBlue()
                                )

                                wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                                showWallpaperChangedNotification(true, homeWallpaper.filePath.toFile())

                                if (MainPreferences.isLinearAutoWallpaper()) {
                                    wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                                    showWallpaperChangedNotification(false, homeWallpaper.filePath.toFile())
                                }
                            }
                        }

                        if (MainPreferences.isLinearAutoWallpaper().invert()) {
                            if (MainPreferences.isSettingForLockScreen()) {
                                getBitmapFromFile(lockWallpaper!!) {
                                    var bitmap = it.copy(it.config ?: Bitmap.Config.ARGB_8888, true)
                                    bitmap = bitmap.applyEffects(
                                            brightness = MainComposePreferences.getAutoWallpaperBrightness(),
                                            contrast = MainComposePreferences.getAutoWallpaperContrast(),
                                            blur = MainComposePreferences.getAutoWallpaperBlur(),
                                            saturation = MainComposePreferences.getAutoWallpaperSaturation(),
                                            hueRed = MainComposePreferences.getAutoWallpaperHueRed(),
                                            hueGreen = MainComposePreferences.getAutoWallpaperHueGreen(),
                                            hueBlue = MainComposePreferences.getAutoWallpaperHueBlue(),
                                            scaleRed = MainComposePreferences.getAutoWallpaperScaleRed(),
                                            scaleGreen = MainComposePreferences.getAutoWallpaperScaleGreen(),
                                            scaleBlue = MainComposePreferences.getAutoWallpaperScaleBlue()
                                    )

                                    wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                                    showWallpaperChangedNotification(false, lockWallpaper.filePath.toFile())
                                }
                            }
                        }
                    }

                    lockWallpaper.isNull() -> {
                        Log.d(TAG, "No lock wallpaper found, setting random wallpaper")
                        if (MainPreferences.isSettingForLockScreen()) {
                            val randomWallpaper = getWallpapersFromDatabase()?.random()!!
                            getBitmapFromFile(randomWallpaper) {
                                var bitmap = it.copy(it.config ?: Bitmap.Config.ARGB_8888, true)
                                bitmap = bitmap.applyEffects(
                                        brightness = MainComposePreferences.getAutoWallpaperBrightness(),
                                        contrast = MainComposePreferences.getAutoWallpaperContrast(),
                                        blur = MainComposePreferences.getAutoWallpaperBlur(),
                                        saturation = MainComposePreferences.getAutoWallpaperSaturation(),
                                        hueRed = MainComposePreferences.getAutoWallpaperHueRed(),
                                        hueGreen = MainComposePreferences.getAutoWallpaperHueGreen(),
                                        hueBlue = MainComposePreferences.getAutoWallpaperHueBlue(),
                                        scaleRed = MainComposePreferences.getAutoWallpaperScaleRed(),
                                        scaleGreen = MainComposePreferences.getAutoWallpaperScaleGreen(),
                                        scaleBlue = MainComposePreferences.getAutoWallpaperScaleBlue()
                                )

                                wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                                showWallpaperChangedNotification(false, randomWallpaper.filePath.toFile())
                            }
                        }
                    }

                    homeWallpaper.isNull() -> {
                        Log.d(TAG, "No home wallpaper found, setting random wallpaper")
                        if (MainPreferences.isSettingForHomeScreen()) {
                            val randomWallpaper = getWallpapersFromDatabase()?.random()!!
                            getBitmapFromFile(randomWallpaper) {
                                var bitmap = it.copy(it.config ?: Bitmap.Config.ARGB_8888, true)
                                bitmap = bitmap.applyEffects(
                                        brightness = MainComposePreferences.getAutoWallpaperBrightness(),
                                        contrast = MainComposePreferences.getAutoWallpaperContrast(),
                                        blur = MainComposePreferences.getAutoWallpaperBlur(),
                                        saturation = MainComposePreferences.getAutoWallpaperSaturation(),
                                        hueRed = MainComposePreferences.getAutoWallpaperHueRed(),
                                        hueGreen = MainComposePreferences.getAutoWallpaperHueGreen(),
                                        hueBlue = MainComposePreferences.getAutoWallpaperHueBlue(),
                                        scaleRed = MainComposePreferences.getAutoWallpaperScaleRed(),
                                        scaleGreen = MainComposePreferences.getAutoWallpaperScaleGreen(),
                                        scaleBlue = MainComposePreferences.getAutoWallpaperScaleBlue()
                                )

                                wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                                showWallpaperChangedNotification(true, randomWallpaper.filePath.toFile())
                            }
                        }
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
        if (MainPreferences.isSettingForHomeScreen().invert()) {
            return null
        }

        val wallpaperDatabase = WallpaperDatabase.getInstance(applicationContext)
        val wallpaperDao = wallpaperDatabase?.wallpaperDao()
        var wallpaper: Wallpaper? = null

        if (MainComposePreferences.isHomeSourceSet().invert()) {
            return null
        }

        when {
            MainComposePreferences.getHomeTagId().isNotNull() -> {
                val tagsDatabase = TagsDatabase.getInstance(applicationContext)
                val tagsDao = tagsDatabase?.tagsDao()
                val tag = tagsDao?.getTagByID(MainComposePreferences.getHomeTagId()!!)
                if (MainPreferences.isLinearAutoWallpaper()) {
                    val wallpapers = wallpaperDao?.getWallpapersByMD5s(tag?.sum!!)
                    try {
                        wallpaper = wallpapers?.get(
                                MainComposePreferences.getLastHomeWallpaperPosition().plus(1)
                        )
                        MainComposePreferences.setLastHomeWallpaperPosition(
                                MainComposePreferences.getLastHomeWallpaperPosition().plus(1)
                        )
                    } catch (e: IndexOutOfBoundsException) {
                        MainComposePreferences.setLastHomeWallpaperPosition(0)
                        wallpapers?.get(0)
                    }
                } else {
                    val wallpapers = wallpaperDao?.getWallpapersByMD5s(tag?.sum!!)
                    wallpaper = wallpapers?.random()
                }

                return wallpaper
            }

            MainComposePreferences.getHomeFolderName().isNotNull() -> {
                val wallpapers =
                    wallpaperDao?.getWallpapersByPathHashcode(MainComposePreferences.getHomeFolderId())

                if (MainPreferences.isLinearAutoWallpaper()) {
                    try {
                        wallpaper = wallpapers?.get(
                                MainComposePreferences.getLastHomeWallpaperPosition().plus(1)
                        )
                        MainComposePreferences.setLastHomeWallpaperPosition(
                                MainComposePreferences.getLastHomeWallpaperPosition().plus(1)
                        )
                    } catch (e: IndexOutOfBoundsException) {
                        MainComposePreferences.setLastHomeWallpaperPosition(0)
                        wallpapers?.get(0)
                    }
                } else {
                    wallpaper = wallpapers?.random()
                }

                return wallpaper
            }

            else -> {
                return null
            }
        }
    }

    private fun getLockScreenWallpaper(): Wallpaper? {
        if (MainPreferences.isSettingForLockScreen().invert()) {
            return null
        }

        val wallpaperDatabase = WallpaperDatabase.getInstance(applicationContext)
        val wallpaperDao = wallpaperDatabase?.wallpaperDao()
        val wallpaper: Wallpaper?

        if (MainComposePreferences.isLockSourceSet().invert()) {
            return null
        }

        when {
            MainComposePreferences.getLockTagId().isNotNull() -> {
                val tagsDatabase = TagsDatabase.getInstance(applicationContext)
                val tagsDao = tagsDatabase?.tagsDao()
                val tag = tagsDao?.getTagByID(MainComposePreferences.getLockTagId()!!)
                val wallpapers = wallpaperDao?.getWallpapersByMD5s(tag?.sum!!)
                wallpaper = if (MainPreferences.isLinearAutoWallpaper()) {
                    try {
                        wallpapers?.get(
                                MainComposePreferences.getLastLockWallpaperPosition().plus(1)
                        ).also {
                            MainComposePreferences.setLastLockWallpaperPosition(
                                    MainComposePreferences.getLastLockWallpaperPosition().plus(1)
                            )
                        }
                    } catch (e: IndexOutOfBoundsException) {
                        MainComposePreferences.setLastLockWallpaperPosition(0)
                        wallpapers?.get(0)
                    }
                } else {
                    wallpapers?.random()
                }

                return wallpaper
            }

            MainComposePreferences.getLockFolderName().isNotNull() -> {
                val wallpapers =
                    wallpaperDao?.getWallpapersByPathHashcode(MainComposePreferences.getLockFolderId())
                wallpaper = if (MainPreferences.isLinearAutoWallpaper()) {
                    try {
                        wallpapers?.get(
                                MainComposePreferences.getLastLockWallpaperPosition().plus(1)
                        ).also {
                            MainComposePreferences.setLastLockWallpaperPosition(
                                    MainComposePreferences.getLastLockWallpaperPosition().plus(1)
                            )
                        }
                    } catch (e: IndexOutOfBoundsException) {
                        MainComposePreferences.setLastLockWallpaperPosition(0)
                        wallpapers?.get(0)
                    }
                } else {
                    wallpapers?.random()
                }

                return wallpaper
            }

            else -> {
                return null
            }
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
            .setContentTitle(if (isHomeScreen) applicationContext.getString(R.string.home_screen) else applicationContext.getString(R.string.lock_screen))
            .setContentText(applicationContext.getString(R.string.wallpaper_changed))
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

        private const val TAG = "AutoWallpaperService"
        private const val CHANNEL_ID_HOME = "wallpaper_home_channel"
        private const val CHANNEL_ID_LOCK = "wallpaper_lock_channel"

        const val HOME_NOTIFICATION_ID = 1234
        const val LOCK_NOTIFICATION_ID = 5367
    }
}
