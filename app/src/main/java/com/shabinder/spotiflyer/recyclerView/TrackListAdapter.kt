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

package com.shabinder.spotiflyer.recyclerView

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.shabinder.spotiflyer.R
import com.shabinder.spotiflyer.databinding.TrackListItemBinding
import com.shabinder.spotiflyer.downloadHelper.DownloadHelper
import com.shabinder.spotiflyer.downloadHelper.YTDownloadHelper
import com.shabinder.spotiflyer.models.DownloadStatus
import com.shabinder.spotiflyer.models.TrackDetails
import com.shabinder.spotiflyer.models.spotify.Source
import com.shabinder.spotiflyer.utils.*
import kotlinx.coroutines.launch

class TrackListAdapter(private val viewModel :TrackListViewModel): ListAdapter<TrackDetails, TrackListAdapter.ViewHolder>(TrackDiffCallback()) {

    var source:Source =Source.Spotify

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = TrackListItemBinding.inflate(layoutInflater,parent,false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        if(itemCount == 1){ holder.binding.imageUrl.visibility = View.GONE}else{
            viewModel.uiScope.launch {
                bindImage(holder.binding.imageUrl,item.albumArtURL, source)
            }
        }

        when (item.downloaded) {
            DownloadStatus.Downloaded -> {
                holder.binding.btnDownload.setImageResource(R.drawable.ic_tick)
                holder.binding.btnDownload.clearAnimation()
            }
            DownloadStatus.Downloading -> {
                holder.binding.btnDownload.setImageResource(R.drawable.ic_refresh)
                rotateAnim(holder.binding.btnDownload)
            }
            DownloadStatus.NotDownloaded -> {
                holder.binding.btnDownload.setImageResource(R.drawable.ic_arrow)
                holder.binding.btnDownload.clearAnimation()
                holder.binding.btnDownload.setOnClickListener{
                    if(!isOnline()){
                        showNoConnectionAlert()
                        return@setOnClickListener
                    }
                    showMessage("Processing!")
                    holder.binding.btnDownload.setImageResource(R.drawable.ic_refresh)
                    rotateAnim(it)
                    item.downloaded = DownloadStatus.Downloading
                    when(source){
                        Source.YouTube -> {
                            viewModel.uiScope.launch {
                                YTDownloadHelper.downloadYTTracks(
                                    viewModel.folderType,
                                    viewModel.subFolder,
                                    listOf(item)
                                )
                            }
                        }
                        else -> {
                            viewModel.uiScope.launch {
                                DownloadHelper.downloadAllTracks(
                                    viewModel.folderType,
                                    viewModel.subFolder,
                                    listOf(item)
                                )
                            }
                        }
                    }
                    notifyItemChanged(position)//start showing anim!
                }
            }
        }

        holder.binding.trackName.text = "${if(item.title.length > 17){"${item.title.subSequence(0,16)}..."}else{item.title}}"
        holder.binding.artist.text = "${item.artists.get(0)}..."
        holder.binding.duration.text =  "${item.durationSec/60} minutes, ${item.durationSec%60} sec"
    }

    class ViewHolder(val binding: TrackListItemBinding) : RecyclerView.ViewHolder(binding.root)

    fun submitList(list: MutableList<TrackDetails>?, source: Source) {
        super.submitList(list)
        this.source = source
    }
}
class TrackDiffCallback: DiffUtil.ItemCallback<TrackDetails>(){
    override fun areItemsTheSame(oldItem: TrackDetails, newItem: TrackDetails): Boolean {
        return oldItem.title == newItem.title
    }

    override fun areContentsTheSame(oldItem: TrackDetails, newItem: TrackDetails): Boolean {
        return oldItem == newItem
    }
}