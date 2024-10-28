package app.simple.peri.preferences

import android.util.Log
import app.simple.peri.preferences.SharedPreferences.getSharedPreferences

object MainComposePreferences {

    private const val GRID_SPAN_COUNT_VERT = "grid_span_count"
    private const val GRID_SPAN_COUNT_LAND = "grid_span_count_land"
    private const val WARNING_INDICATOR = "warning_indicator"
    private const val IMAGE_SHADOW = "image_shadow"
    private const val IS_LOCK_SOURCE_SET = "is_lock_source_set"
    private const val IS_HOME_SOURCE_SET = "is_home_source_set"
    private const val LOCK_TAG_ID = "lock_tag_id"
    private const val LOCK_FOLDER_ID = "lock_folder_id__"
    private const val LOCK_FOLDER_NAME = "lock_folder_name__"
    private const val HOME_TAG_ID = "home_tag_id"
    private const val HOME_FOLDER_ID = "home_folder_id__"
    private const val HOME_FOLDER_NAME = "home_folder_name__"
    private const val LAST_LOCK_WALLPAPER_POSITION = "last_lock_wallpaper_position"
    private const val LAST_HOME_WALLPAPER_POSITION = "last_home_wallpaper_position"
    private const val AUTO_WALLPAPER_BLUR = "auto_wallpaper_blur"
    private const val AUTO_WALLPAPER_BRIGHTNESS = "auto_wallpaper_brightness"
    private const val AUTO_WALLPAPER_CONTRAST = "auto_wallpaper_contrast"
    private const val AUTO_WALLPAPER_SATURATION = "auto_wallpaper_saturation"
    private const val AUTO_WALLPAPER_HUE_RED = "auto_wallpaper_hue_red"
    private const val AUTO_WALLPAPER_HUE_GREEN = "auto_wallpaper_hue_green"
    private const val AUTO_WALLPAPER_HUE_BLUE = "auto_wallpaper_hue_blue"
    private const val AUTO_WALLPAPER_HOME_BLUR = "auto_wallpaper_home_blur"
    private const val AUTO_WALLPAPER_HOME_BRIGHTNESS = "auto_wallpaper_home_brightness"
    private const val AUTO_WALLPAPER_HOME_CONTRAST = "auto_wallpaper_home_contrast"
    private const val AUTO_WALLPAPER_HOME_SATURATION = "auto_wallpaper_home_saturation"
    private const val AUTO_WALLPAPER_HOME_HUE_RED = "auto_wallpaper_home_hue_red"
    private const val AUTO_WALLPAPER_HOME_HUE_GREEN = "auto_wallpaper_home_hue_green"
    private const val AUTO_WALLPAPER_HOME_HUE_BLUE = "auto_wallpaper_home_hue_blue"
    private const val AUTO_WALLPAPER_LOCK_BLUR = "auto_wallpaper_lock_blur"
    private const val AUTO_WALLPAPER_LOCK_BRIGHTNESS = "auto_wallpaper_lock_brightness"
    private const val AUTO_WALLPAPER_LOCK_CONTRAST = "auto_wallpaper_lock_contrast"
    private const val AUTO_WALLPAPER_LOCK_SATURATION = "auto_wallpaper_lock_saturation"
    private const val AUTO_WALLPAPER_LOCK_HUE_RED = "auto_wallpaper_lock_hue_red"
    private const val AUTO_WALLPAPER_LOCK_HUE_GREEN = "auto_wallpaper_lock_hue_green"
    private const val AUTO_WALLPAPER_LOCK_HUE_BLUE = "auto_wallpaper_lock_hue_blue"
    private const val DEVICE_WIDTH = "device_width"
    private const val DEVICE_HEIGHT = "device_height"
    private const val BOTTOM_HEADER = "bottom_header"
    private const val ORIGINAL_ASPECT_RATIO = "original_aspect_ratio"
    private const val AUTO_WALLPAPER_NOTIFICATION = "auto_wallpaper_notification"

    // ----------------------------------------------------------------------------------------------------- //

    fun getGridSpanCountPortrait(): Int {
        return getSharedPreferences().getInt(GRID_SPAN_COUNT_VERT, 2)
    }

    fun setGridSpanCountPortrait(value: Int) {
        getSharedPreferences().edit().putInt(GRID_SPAN_COUNT_VERT, value).apply()
    }

    fun getGridSpanCountLandscape(): Int {
        return getSharedPreferences().getInt(GRID_SPAN_COUNT_LAND, 3)
    }

    fun setGridSpanCountLandscape(value: Int) {
        getSharedPreferences().edit().putInt(GRID_SPAN_COUNT_LAND, value).apply()
    }

    fun getGridSpanCount(isLandscape: Boolean): Int {
        return if (isLandscape) {
            getGridSpanCountLandscape()
        } else {
            getGridSpanCountPortrait()
        }
    }

    // ----------------------------------------------------------------------------------------------------- //

    fun getShowWarningIndicator(): Boolean {
        return getSharedPreferences().getBoolean(WARNING_INDICATOR, false)
    }

    fun setShowWarningIndicator(value: Boolean) {
        getSharedPreferences().edit().putBoolean(WARNING_INDICATOR, value).apply()
    }

    // ----------------------------------------------------------------------------------------------------- //

    fun getShowImageShadow(): Boolean {
        return getSharedPreferences().getBoolean(IMAGE_SHADOW, false)
    }

    fun setShowImageShadow(value: Boolean) {
        getSharedPreferences().edit().putBoolean(IMAGE_SHADOW, value).apply()
    }

    // ----------------------------------------------------------------------------------------------------- //

    fun isLockSourceSet(): Boolean {
        return getSharedPreferences().getBoolean(IS_LOCK_SOURCE_SET, false)
    }

    fun setIsLockSourceSet(value: Boolean) {
        getSharedPreferences().edit().putBoolean(IS_LOCK_SOURCE_SET, value).apply()
    }

    // ----------------------------------------------------------------------------------------------------- //

    fun isHomeSourceSet(): Boolean {
        return getSharedPreferences().getBoolean(IS_HOME_SOURCE_SET, false)
    }

    fun setIsHomeSourceSet(value: Boolean) {
        getSharedPreferences().edit().putBoolean(IS_HOME_SOURCE_SET, value).apply()
    }

    // ----------------------------------------------------------------------------------------------------- //

    fun getLockTagId(): String? {
        return getSharedPreferences().getString(LOCK_TAG_ID, null)
    }

    fun setLockTagId(value: String?) {
        getSharedPreferences().edit().putString(LOCK_TAG_ID, value).apply()
    }

    // ----------------------------------------------------------------------------------------------------- //

    fun getLockFolderId(): Int {
        return getSharedPreferences().getInt(LOCK_FOLDER_ID, 0)
    }

    fun setLockFolderId(value: Int) {
        getSharedPreferences().edit().putInt(LOCK_FOLDER_ID, value).apply()
    }

    // ----------------------------------------------------------------------------------------------------- //

    fun getLockFolderName(): String? {
        return getSharedPreferences().getString(LOCK_FOLDER_NAME, null)
    }

    fun setLockFolderName(value: String?) {
        getSharedPreferences().edit().putString(LOCK_FOLDER_NAME, value).apply()
    }

    // ----------------------------------------------------------------------------------------------------- //

    fun getHomeTagId(): String? {
        return getSharedPreferences().getString(HOME_TAG_ID, null)
    }

    fun setHomeTagId(value: String?) {
        getSharedPreferences().edit().putString(HOME_TAG_ID, value).apply()
    }

    // ----------------------------------------------------------------------------------------------------- //

    fun getHomeFolderId(): Int {
        return getSharedPreferences().getInt(HOME_FOLDER_ID, 0)
    }

    fun setHomeFolderId(value: Int) {
        getSharedPreferences().edit().putInt(HOME_FOLDER_ID, value).apply()
    }

    // ----------------------------------------------------------------------------------------------------- //

    fun getHomeFolderName(): String? {
        return getSharedPreferences().getString(HOME_FOLDER_NAME, null)
    }

    fun setHomeFolderName(value: String?) {
        getSharedPreferences().edit().putString(HOME_FOLDER_NAME, value).apply()
    }

    // ----------------------------------------------------------------------------------------------------- //

    fun getLastLockWallpaperPosition(): Int {
        return getSharedPreferences().getInt(LAST_LOCK_WALLPAPER_POSITION, 0)
    }

    fun setLastLockWallpaperPosition(value: Int) {
        getSharedPreferences().edit().putInt(LAST_LOCK_WALLPAPER_POSITION, value).apply()
    }

    fun getLastHomeWallpaperPosition(): Int {
        return getSharedPreferences().getInt(LAST_HOME_WALLPAPER_POSITION, 0)
    }

    fun setLastHomeWallpaperPosition(value: Int) {
        getSharedPreferences().edit().putInt(LAST_HOME_WALLPAPER_POSITION, value).apply()
    }

    fun resetLastWallpaperPositions() {
        getSharedPreferences().edit().remove(LAST_LOCK_WALLPAPER_POSITION).apply()
        getSharedPreferences().edit().remove(LAST_HOME_WALLPAPER_POSITION).apply()
    }

    // ----------------------------------------------------------------------------------------------------- //

    fun getAutoWallpaperBlur(): Float {
        return getSharedPreferences().getFloat(AUTO_WALLPAPER_BLUR, 0f)
    }

    fun setAutoWallpaperBlur(value: Float) {
        getSharedPreferences().edit().putFloat(AUTO_WALLPAPER_BLUR, value).apply()
    }

    // ----------------------------------------------------------------------------------------------------- //

    fun getAutoWallpaperBrightness(): Float {
        return getSharedPreferences().getFloat(AUTO_WALLPAPER_BRIGHTNESS, 0f)
    }

    fun setAutoWallpaperBrightness(value: Float) {
        getSharedPreferences().edit().putFloat(AUTO_WALLPAPER_BRIGHTNESS, value).apply()
    }

    // ----------------------------------------------------------------------------------------------------- //

    fun getAutoWallpaperContrast(): Float {
        return getSharedPreferences().getFloat(AUTO_WALLPAPER_CONTRAST, 1f)
    }

    fun setAutoWallpaperContrast(value: Float) {
        getSharedPreferences().edit().putFloat(AUTO_WALLPAPER_CONTRAST, value).apply()
    }

    // ----------------------------------------------------------------------------------------------------- //

    fun getAutoWallpaperSaturation(): Float {
        return getSharedPreferences().getFloat(AUTO_WALLPAPER_SATURATION, 1f)
    }

    fun setAutoWallpaperSaturation(value: Float) {
        getSharedPreferences().edit().putFloat(AUTO_WALLPAPER_SATURATION, value).apply()
    }

    // ----------------------------------------------------------------------------------------------------- //

    fun getAutoWallpaperHueRed(): Float {
        return getSharedPreferences().getFloat(AUTO_WALLPAPER_HUE_RED, 0f)
    }

    fun setAutoWallpaperHueRed(value: Float) {
        getSharedPreferences().edit().putFloat(AUTO_WALLPAPER_HUE_RED, value).apply()
    }

    fun getAutoWallpaperHueGreen(): Float {
        return getSharedPreferences().getFloat(AUTO_WALLPAPER_HUE_GREEN, 0f)
    }

    fun setAutoWallpaperHueGreen(value: Float) {
        getSharedPreferences().edit().putFloat(AUTO_WALLPAPER_HUE_GREEN, value).apply()
    }

    fun getAutoWallpaperHueBlue(): Float {
        return getSharedPreferences().getFloat(AUTO_WALLPAPER_HUE_BLUE, 0f)
    }

    fun setAutoWallpaperHueBlue(value: Float) {
        getSharedPreferences().edit().putFloat(AUTO_WALLPAPER_HUE_BLUE, value).apply()
    }

    // ----------------------------------------------------------------------------------------------------- //

    fun getAutoWallpaperHomeBlur(): Float {
        return getSharedPreferences().getFloat(AUTO_WALLPAPER_HOME_BLUR, 0f)
    }

    fun setAutoWallpaperHomeBlur(value: Float) {
        getSharedPreferences().edit().putFloat(AUTO_WALLPAPER_HOME_BLUR, value).apply()
    }

    // ----------------------------------------------------------------------------------------------------- //

    fun getAutoWallpaperHomeBrightness(): Float {
        return getSharedPreferences().getFloat(AUTO_WALLPAPER_HOME_BRIGHTNESS, 0f)
    }

    fun setAutoWallpaperHomeBrightness(value: Float) {
        getSharedPreferences().edit().putFloat(AUTO_WALLPAPER_HOME_BRIGHTNESS, value).apply()
    }

    // ----------------------------------------------------------------------------------------------------- //

    fun getAutoWallpaperHomeContrast(): Float {
        return getSharedPreferences().getFloat(AUTO_WALLPAPER_HOME_CONTRAST, 1f)
    }

    fun setAutoWallpaperHomeContrast(value: Float) {
        getSharedPreferences().edit().putFloat(AUTO_WALLPAPER_HOME_CONTRAST, value).apply()
    }

    // ----------------------------------------------------------------------------------------------------- //

    fun getAutoWallpaperHomeSaturation(): Float {
        return getSharedPreferences().getFloat(AUTO_WALLPAPER_HOME_SATURATION, 1f)
    }

    fun setAutoWallpaperHomeSaturation(value: Float) {
        getSharedPreferences().edit().putFloat(AUTO_WALLPAPER_HOME_SATURATION, value).apply()
    }

    // ----------------------------------------------------------------------------------------------------- //

    fun getAutoWallpaperHomeHueRed(): Float {
        return getSharedPreferences().getFloat(AUTO_WALLPAPER_HOME_HUE_RED, 0f)
    }

    fun setAutoWallpaperHomeHueRed(value: Float) {
        getSharedPreferences().edit().putFloat(AUTO_WALLPAPER_HOME_HUE_RED, value).apply()
    }

    fun getAutoWallpaperHomeHueGreen(): Float {
        return getSharedPreferences().getFloat(AUTO_WALLPAPER_HOME_HUE_GREEN, 0f)
    }

    fun setAutoWallpaperHomeHueGreen(value: Float) {
        getSharedPreferences().edit().putFloat(AUTO_WALLPAPER_HOME_HUE_GREEN, value).apply()
    }

    fun getAutoWallpaperHomeHueBlue(): Float {
        return getSharedPreferences().getFloat(AUTO_WALLPAPER_HOME_HUE_BLUE, 0f)
    }

    fun setAutoWallpaperHomeHueBlue(value: Float) {
        getSharedPreferences().edit().putFloat(AUTO_WALLPAPER_HOME_HUE_BLUE, value).apply()
    }

    // ----------------------------------------------------------------------------------------------------- //

    fun getAutoWallpaperLockBlur(): Float {
        return getSharedPreferences().getFloat(AUTO_WALLPAPER_LOCK_BLUR, 0f)
    }

    fun setAutoWallpaperLockBlur(value: Float) {
        getSharedPreferences().edit().putFloat(AUTO_WALLPAPER_LOCK_BLUR, value).apply()
    }

    // ----------------------------------------------------------------------------------------------------- //

    fun getAutoWallpaperLockBrightness(): Float {
        return getSharedPreferences().getFloat(AUTO_WALLPAPER_LOCK_BRIGHTNESS, 0f)
    }

    fun setAutoWallpaperLockBrightness(value: Float) {
        getSharedPreferences().edit().putFloat(AUTO_WALLPAPER_LOCK_BRIGHTNESS, value).apply()
    }

    // ----------------------------------------------------------------------------------------------------- //

    fun getAutoWallpaperLockContrast(): Float {
        return getSharedPreferences().getFloat(AUTO_WALLPAPER_LOCK_CONTRAST, 1f)
    }

    fun setAutoWallpaperLockContrast(value: Float) {
        getSharedPreferences().edit().putFloat(AUTO_WALLPAPER_LOCK_CONTRAST, value).apply()
    }

    // ----------------------------------------------------------------------------------------------------- //

    fun getAutoWallpaperLockSaturation(): Float {
        return getSharedPreferences().getFloat(AUTO_WALLPAPER_LOCK_SATURATION, 1f)
    }

    fun setAutoWallpaperLockSaturation(value: Float) {
        getSharedPreferences().edit().putFloat(AUTO_WALLPAPER_LOCK_SATURATION, value).apply()
    }

    // ----------------------------------------------------------------------------------------------------- //

    fun getAutoWallpaperLockHueRed(): Float {
        return getSharedPreferences().getFloat(AUTO_WALLPAPER_LOCK_HUE_RED, 0f)
    }

    fun setAutoWallpaperLockHueRed(value: Float) {
        getSharedPreferences().edit().putFloat(AUTO_WALLPAPER_LOCK_HUE_RED, value).apply()
    }

    fun getAutoWallpaperLockHueGreen(): Float {
        return getSharedPreferences().getFloat(AUTO_WALLPAPER_LOCK_HUE_GREEN, 0f)
    }

    fun setAutoWallpaperLockHueGreen(value: Float) {
        getSharedPreferences().edit().putFloat(AUTO_WALLPAPER_LOCK_HUE_GREEN, value).apply()
    }

    fun getAutoWallpaperLockHueBlue(): Float {
        return getSharedPreferences().getFloat(AUTO_WALLPAPER_LOCK_HUE_BLUE, 0f)
    }

    fun setAutoWallpaperLockHueBlue(value: Float) {
        getSharedPreferences().edit().putFloat(AUTO_WALLPAPER_LOCK_HUE_BLUE, value).apply()
    }

    // ----------------------------------------------------------------------------------------------------- //

    fun clearAutoWallpaperValues() {
        getSharedPreferences().edit().remove(AUTO_WALLPAPER_BLUR).apply()
        getSharedPreferences().edit().remove(AUTO_WALLPAPER_BRIGHTNESS).apply()
        getSharedPreferences().edit().remove(AUTO_WALLPAPER_CONTRAST).apply()
        getSharedPreferences().edit().remove(AUTO_WALLPAPER_HOME_BLUR).apply()
        getSharedPreferences().edit().remove(AUTO_WALLPAPER_HOME_BRIGHTNESS).apply()
        getSharedPreferences().edit().remove(AUTO_WALLPAPER_HOME_CONTRAST).apply()
        getSharedPreferences().edit().remove(AUTO_WALLPAPER_LOCK_BLUR).apply()
        getSharedPreferences().edit().remove(AUTO_WALLPAPER_LOCK_BRIGHTNESS).apply()
        getSharedPreferences().edit().remove(AUTO_WALLPAPER_LOCK_CONTRAST).apply()
    }

    // ----------------------------------------------------------------------------------------------------- //

    private fun getDeviceWidth(): Int {
        return getSharedPreferences().getInt(DEVICE_WIDTH, 1080)
    }

    private fun setDeviceWidth(value: Int) {
        getSharedPreferences().edit().putInt(DEVICE_WIDTH, value).apply()
    }

    private fun getDeviceHeight(): Int {
        return getSharedPreferences().getInt(DEVICE_HEIGHT, 1920)
    }

    private fun setDeviceHeight(value: Int) {
        getSharedPreferences().edit().putInt(DEVICE_HEIGHT, value).apply()
    }

    fun setDeviceDimensions(width: Int, height: Int) {
        Log.i("MainComposePreferences", "Device dimensions: $width x $height")
        if (width > 0 && height > 0) {
            setDeviceWidth(width)
            setDeviceHeight(height)
        } else {
            Log.e("MainComposePreferences", "Invalid device dimensions: $width x $height")
        }
    }

    fun getAspectRatio(): Float {
        return getDeviceWidth().toFloat() / getDeviceHeight().toFloat()
    }

    // ----------------------------------------------------------------------------------------------------- //

    fun getBottomHeader(): Boolean {
        return getSharedPreferences().getBoolean(BOTTOM_HEADER, false)
    }

    fun setBottomHeader(value: Boolean) {
        getSharedPreferences().edit().putBoolean(BOTTOM_HEADER, value).apply()
    }

    // ----------------------------------------------------------------------------------------------------- //

    fun isOriginalAspectRatio(): Boolean {
        return getSharedPreferences().getBoolean(ORIGINAL_ASPECT_RATIO, false)
    }

    fun setOriginalAspectRatio(value: Boolean) {
        getSharedPreferences().edit().putBoolean(ORIGINAL_ASPECT_RATIO, value).apply()
    }

    // ----------------------------------------------------------------------------------------------------- //

    fun getAutoWallpaperNotification(): Boolean {
        return getSharedPreferences().getBoolean(AUTO_WALLPAPER_NOTIFICATION, true)
    }

    fun setAutoWallpaperNotification(value: Boolean) {
        getSharedPreferences().edit().putBoolean(AUTO_WALLPAPER_NOTIFICATION, value).apply()
    }
}
