package com.expapps.vibex.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.expapps.vibex.databinding.LayoutSongItemBinding
import com.expapps.vibex.listeners.DownloadClickListener
import com.expapps.vibex.listeners.MusicClickListener
import com.expapps.vibex.models.SongsModel
import com.google.android.gms.ads.AdRequest


class SongListAdapter(var isFromRecommended: Boolean = false, var isFromLocal: Boolean = false) : RecyclerView.Adapter<SongListAdapter.SongViewHolder>() {
    private val items = ArrayList<SongsModel>()
    var musicClickListener: MusicClickListener? = null
    var downloadClickListener: DownloadClickListener? = null
    private var currPos = -1
    val adRequest = AdRequest.Builder().build()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        return SongViewHolder(LayoutSongItemBinding.inflate(LayoutInflater.from(parent.context), parent, false), parent.context)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        holder.bindItems(items[position])
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun getCurrentPlayingPosition(): Int {
        return currPos
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(items: ArrayList<SongsModel>) {
        try {
            this.items.clear()
            this.items.addAll(items)
            notifyDataSetChanged()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    inner class SongViewHolder(private val binding: LayoutSongItemBinding, private val context: Context) : ViewHolder(binding.root) {
        fun bindItems(item: SongsModel) {
            if (!isFromRecommended) {
                if (position % 10 == 0) {
                    binding.adView.visibility = View.VISIBLE
                    binding.adView.loadAd(adRequest)
                } else {
                    binding.adView.visibility = View.GONE
                }
            } else {
                binding.adView.visibility = View.GONE
            }
            if (!isFromRecommended && !isFromLocal) {
                binding.downloadLayout.visibility = View.VISIBLE
            } else {
                binding.downloadLayout.visibility = View.GONE
            }
            binding.downloadIv.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.songUrl))
                context.startActivity(intent)
                downloadClickListener?.onDownloadClick(item.songUrl ?: "")
            }
            binding.songTitleTv.text = item.songTitle
            binding.artistTv.text = item.songArtists
            itemView.setOnClickListener {
                currPos = adapterPosition
                musicClickListener?.onClick(
                    filePath = item.localSongPath ?: "",
                    songUrl = item.songUrl ?: "",
                    songTitle = item.songTitle ?: "",
                    artistName = item.songArtists ?: "",
                    songsModel = item
                )
            }
        }
    }
}