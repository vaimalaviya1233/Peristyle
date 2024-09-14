package app.simple.peri.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import app.simple.peri.database.instances.TagsDatabase
import app.simple.peri.database.instances.WallpaperDatabase
import app.simple.peri.models.Tag
import app.simple.peri.models.Wallpaper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TagsViewModel(application: Application, private val md5: String? = null, private val tag: String? = null) : AndroidViewModel(application) {

    private val tags: MutableLiveData<List<Tag>> by lazy {
        MutableLiveData<List<Tag>>().also {
            loadTags()
        }
    }

    private val wallpaperTags: MutableLiveData<List<String>> by lazy {
        MutableLiveData<List<String>>().also {
            if (md5 != null) {
                loadWallpaperTags(md5)
            }
        }
    }

    private val wallpapers: MutableLiveData<List<Wallpaper>> by lazy {
        MutableLiveData<List<Wallpaper>>().also {
            if (tag != null) {
                loadWallpapers(tag)
            }
        }
    }

    fun getTags(): LiveData<List<Tag>> {
        return tags
    }

    fun getWallpaperTags(): LiveData<List<String>> {
        return wallpaperTags
    }

    fun getWallpapers(): LiveData<List<Wallpaper>> {
        return wallpapers
    }

    private fun loadTags() {
        viewModelScope.launch(Dispatchers.IO) {
            val database = TagsDatabase.getInstance(getApplication())
            val tagsDao = database?.tagsDao()
            val tags = tagsDao?.getAllTags()
            this@TagsViewModel.tags.postValue(tags)
        }
    }

    private fun loadWallpaperTags(md5: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val database = TagsDatabase.getInstance(getApplication())
            val tagsDao = database?.tagsDao()
            val tags = tagsDao?.getTagNamesByMD5(md5)
            wallpaperTags.postValue(tags)
        }
    }

    private fun loadWallpapers(factoryTag: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val tagsDatabase = TagsDatabase.getInstance(getApplication())
            val wallpaperDatabase = WallpaperDatabase.getInstance(getApplication())
            val tagsDao = tagsDatabase?.tagsDao()
            val tag = tagsDao?.getTagById(factoryTag)
            val wallpapers = wallpaperDatabase?.wallpaperDao()?.getWallpapersByMD5s(tag?.sum!!)
            this@TagsViewModel.wallpapers.postValue(wallpapers)
        }
    }

    fun addTag(tagName: String, wallpaper: Wallpaper) {
        viewModelScope.launch(Dispatchers.IO) {
            val database = TagsDatabase.getInstance(getApplication())
            val tagsDao = database?.tagsDao()

            if (tagsDao?.isTagExists(tagName.trim())!!) {
                val tag = tagsDao.getTagById(tagName.trim())
                tag.addSum(wallpaper.md5)
                tagsDao.insertTag(tag)
            } else {
                val tag = Tag(tagName.trim(), hashSetOf(wallpaper.md5))
                tagsDao.insertTag(tag)
            }

            loadTags()
        }
    }

    fun deleteTag(tag: Tag) {
        viewModelScope.launch(Dispatchers.IO) {
            val database = TagsDatabase.getInstance(getApplication())
            val tagsDao = database?.tagsDao()
            tagsDao?.deleteTag(tag)
            loadTags()
        }
    }

    private fun createRandomTagsForTesting() {
        viewModelScope.launch(Dispatchers.IO) {
            val database = TagsDatabase.getInstance(getApplication())
            val tagsDao = database?.tagsDao()
            val wallpapers = WallpaperDatabase.getInstance(getApplication())?.wallpaperDao()?.getWallpapers()

            val randomTags = listOf("Red", "Grey", "Blue", "Black", "White")
            val random = java.util.Random()

            randomTags.forEach { tagName ->
                val randomWallpapers = wallpapers?.shuffled()?.take(random.nextInt(10) + 6) ?: emptyList()
                val tag = Tag(tagName, randomWallpapers.map { it.md5 }.toHashSet())
                tagsDao?.insertTag(tag)
            }

            loadTags()
        }
    }
}