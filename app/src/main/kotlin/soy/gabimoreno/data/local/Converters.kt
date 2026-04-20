package soy.gabimoreno.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import soy.gabimoreno.coredomain.model.audio.Saga

class Converters {
    @TypeConverter
    fun stringToSaga(s: String): Saga = s.toObject()

    @TypeConverter
    fun sagaToString(saga: Saga): String = saga.fromObject()

    private inline fun <reified T> String.toObject(): T =
        Gson().fromJson(
            this,
            object : com.google.gson.reflect.TypeToken<T>() {}.type,
        )

    private inline fun <reified T> T.fromObject(): String = Gson().toJson(this)
}
