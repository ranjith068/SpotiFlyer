/*
 * Copyright (C)  2020  Shabinder Singh
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.shabinder.spotiflyer.utils

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Environment
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.shabinder.spotiflyer.R
import com.shabinder.spotiflyer.models.DownloadObject
import com.shabinder.spotiflyer.models.spotify.Source
import com.shabinder.spotiflyer.utils.Provider.defaultDir
import com.shabinder.spotiflyer.utils.Provider.mainActivity
import com.shabinder.spotiflyer.worker.ForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

fun loadAllImages(context: Context?, images:ArrayList<String>? = null ) {
    val serviceIntent = Intent(context, ForegroundService::class.java)
    images?.let {  serviceIntent.putStringArrayListExtra("imagesList",it) }
    context?.let { ContextCompat.startForegroundService(it, serviceIntent) }
}

fun startService(context:Context?,objects:ArrayList<DownloadObject>? = null ) {
    val serviceIntent = Intent(context, ForegroundService::class.java)
    objects?.let {  serviceIntent.putParcelableArrayListExtra("object",it) }
    context?.let { ContextCompat.startForegroundService(it, serviceIntent) }
}

fun finalOutputDir(itemName:String? = null,type:String, subFolder:String?=null,extension:String? = ".mp3"): String{
    return Environment.getExternalStorageDirectory().toString() + File.separator +
            defaultDir + removeIllegalChars(type) + File.separator +
            (if(subFolder == null){""}else{ removeIllegalChars(subFolder) + File.separator}
                    + itemName?.let { removeIllegalChars(it) + extension})
}

/**
 * Util. Function To Check Connection Status
 **/
@Suppress("DEPRECATION")
fun isOnline(): Boolean {
    var result = false
    val connectivityManager =
        mainActivity.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
    connectivityManager?.let {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            it.getNetworkCapabilities(connectivityManager.activeNetwork)?.apply {
                result = when {
                    hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                    hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                    hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ->  true
                    else -> false
                }
            }
        } else {
            val netInfo =
                (mainActivity.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetworkInfo
            result = netInfo != null && netInfo.isConnected
        }
    }
    return result
}

fun showMessage(message: String, long: Boolean = false,isSuccess:Boolean = false , isError:Boolean = false){
    CoroutineScope(Dispatchers.Main).launch{
        Snackbar.make(
            mainActivity.snackBarAnchor,
            message,
            if (long) Snackbar.LENGTH_LONG else Snackbar.LENGTH_SHORT
        ).apply {
            setAction("Ok") {
                dismiss()
            }
            setActionTextColor(ContextCompat.getColor(mainActivity,R.color.black))
            when{
                isSuccess -> setBackgroundTint(ContextCompat.getColor(mainActivity,R.color.successGreen))
                isError -> setBackgroundTint(ContextCompat.getColor(mainActivity,R.color.errorRed))
            }
        }.show()
    }
}


fun rotateAnim(view: View){
    val rotate = RotateAnimation(
        0F, 360F,
        Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f
    )
    rotate.duration = 1000
    rotate.repeatCount = Animation.INFINITE
    rotate.repeatMode = Animation.INFINITE
    rotate.interpolator = LinearInterpolator()
    view.animation = rotate
}

fun showNoConnectionAlert(){
    CoroutineScope(Dispatchers.Main).launch {
        mainActivity.apply {
            MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme)
                .setTitle(resources.getString(R.string.title))
                .setMessage(resources.getString(R.string.supporting_text))
                .setPositiveButton(resources.getString(R.string.cancel)) { _, _ ->
                    // Respond to neutral button press
                }.show()
        }
    }
}
fun bindImage(imgView: ImageView, imgUrl: String?,source: Source?) {
    imgUrl?.let {
        val imgUri = imgUrl.toUri().buildUpon().scheme("https").build()
        Glide
            .with(imgView)
            .asFile()
            .load(imgUri)
            .placeholder(R.drawable.ic_song_placeholder)
            .error(R.drawable.ic_musicplaceholder)
            .listener(object:RequestListener<File>{
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<File>?,
                    isFirstResource: Boolean
                ): Boolean {
                    Log.i("Glide","LoadFailed")
                    return false
                }

                override fun onResourceReady(
                    resource: File?,
                    model: Any?,
                    target: Target<File>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    CoroutineScope(Dispatchers.Main).launch {
                            try {
                                val file = when(source){
                                    Source.Spotify->{
                                        File(
                                            Environment.getExternalStorageDirectory(),
                                            defaultDir+".Images/" + imgUrl.substringAfterLast('/',imgUrl) + ".jpeg"
                                        )
                                    }
                                    Source.YouTube->{
                                        //Url Format: https://i.ytimg.com/vi/$searchId/maxresdefault.jpg"
                                        // We Are Naming using "$searchId"
                                        File(
                                            Environment.getExternalStorageDirectory(),
                                            defaultDir+".Images/" + imgUrl.substringBeforeLast('/',imgUrl).substringAfterLast('/',imgUrl) + ".jpeg"
                                        )
                                    }
                                    Source.Gaana -> {
                                        File(
                                            Environment.getExternalStorageDirectory(),
                                            Provider.defaultDir +".Images/" + (imgUrl.substringBeforeLast('/').substringAfterLast('/')) + ".jpeg")
                                    }
                                    else ->  File(
                                        Environment.getExternalStorageDirectory(),
                                        defaultDir+".Images/" + imgUrl.substringAfterLast('/',imgUrl) + ".jpeg"
                                    )
                                }
                                 // the File to save , append increasing numeric counter to prevent files from getting overwritten.
                                resource?.copyTo(file)
                                Glide.with(imgView)
                                    .load(file)
                                    .placeholder(R.drawable.ic_song_placeholder)
                                    .into(imgView)
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                    }
                     return false
                }
            }).submit()
        }
    }

/**
 *Extension Function For Copying Files!
 **/
fun File.copyTo(file: File) {
    inputStream().use { input ->
        file.outputStream().use { output ->
            input.copyTo(output)
        }
    }
}
fun createDirectory(dir:String){
    val yourAppDir = File(Environment.getExternalStorageDirectory(),
         dir)

    if(!yourAppDir.exists() && !yourAppDir.isDirectory)
    { // create empty directory
        if (yourAppDir.mkdirs())
        {Log.i("CreateDir","App dir created")}
        else
        {Log.w("CreateDir","Unable to create app dir!")}
    }
    else
    {Log.i("CreateDir","App dir already exists")}
}
/**
 * Removing Illegal Chars from File Name
 * **/
fun removeIllegalChars(fileName: String): String? {
    val illegalCharArray = charArrayOf(
        '/',
        '\n',
        '\r',
        '\t',
        '\u0000',
        '\u000C',
        '`',
        '?',
        '*',
        '\\',
        '<',
        '>',
        '|',
        '\"',
        '.',
        '-',
        '\''
    )

    var name = fileName
    for (c in illegalCharArray) {
        name = fileName.replace(c, '_')
    }
    name = name.replace("\\s".toRegex(), "_")
    name = name.replace("\\)".toRegex(), "")
    name = name.replace("\\(".toRegex(), "")
    name = name.replace("\\[".toRegex(), "")
    name = name.replace("]".toRegex(), "")
    name = name.replace("\\.".toRegex(), "")
    name = name.replace("\"".toRegex(), "")
    name = name.replace("\'".toRegex(), "")
    name = name.replace(":".toRegex(), "")
    name = name.replace("\\|".toRegex(), "")
    return name
}

fun createDirectories() {
    createDirectory(defaultDir)
    createDirectory(defaultDir + ".Images/")
    createDirectory(defaultDir + "Tracks/")
    createDirectory(defaultDir + "Albums/")
    createDirectory(defaultDir + "Playlists/")
    createDirectory(defaultDir + "YT_Downloads/")
}
fun getEmojiByUnicode(unicode: Int): String? {
    return String(Character.toChars(unicode))
}

/*
internal val nullOnEmptyConverterFactory = object : Converter.Factory() {
    fun converterFactory() = this
    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ) = object : Converter<ResponseBody, Any?> {
        val nextResponseBodyConverter =
            retrofit.nextResponseBodyConverter<Any?>(converterFactory(), type, annotations)

        override fun convert(value: ResponseBody) =
            if (value.contentLength() != 0L) nextResponseBodyConverter.convert(value) else null
    }
}*/
