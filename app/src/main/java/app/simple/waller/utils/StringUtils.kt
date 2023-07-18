package app.simple.waller.utils

object StringUtils {

    fun String?.endsWithAny(vararg strings: String): Boolean {
        strings.forEach {
            if (this?.endsWith(it) == true) {
                return true
            }
        }
        return false
    }
}