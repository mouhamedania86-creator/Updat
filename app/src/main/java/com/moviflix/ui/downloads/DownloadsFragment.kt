package com.moviflix.ui.downloads

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.*
import com.bumptech.glide.Glide
import com.moviflix.MoviFlixApp
import com.moviflix.R
import com.moviflix.models.DownloadEntity
import com.moviflix.service.DownloadDatabase
import com.moviflix.ui.player.PlayerActivity
import kotlinx.coroutines.launch

class DownloadsFragment : Fragment() {

    private lateinit var rvDownloads: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: DownloadsAdapter
    private lateinit var db: DownloadDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_downloads, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvDownloads = view.findViewById(R.id.rvDownloads)
        tvEmpty = view.findViewById(R.id.tvDownloadsEmpty)
        db = DownloadDatabase.getInstance(requireContext())

        adapter = DownloadsAdapter(
            onPlay = { entity ->
                PlayerActivity.launchOffline(
                    requireContext(),
                    entity.localPath,
                    "${entity.contentTitle} - ${entity.episodeTitle}",
                    entity.subtitlePath
                )
            },
            onDelete = { entity ->
                lifecycleScope.launch {
                    db.downloadDao().deleteDownload(entity.episodeId)
                    java.io.File(entity.localPath).parentFile?.deleteRecursively()
                }
            }
        )

        rvDownloads.layoutManager = LinearLayoutManager(requireContext())
        rvDownloads.adapter = adapter

        lifecycleScope.launch {
            db.downloadDao().getAllDownloads().collect { downloads ->
                activity?.runOnUiThread {
                    adapter.submitList(downloads)
                    tvEmpty.visibility = if (downloads.isEmpty()) View.VISIBLE else View.GONE
                    rvDownloads.visibility = if (downloads.isEmpty()) View.GONE else View.VISIBLE
                }
            }
        }
    }
}

class DownloadsAdapter(
    private val onPlay: (DownloadEntity) -> Unit,
    private val onDelete: (DownloadEntity) -> Unit
) : RecyclerView.Adapter<DownloadsAdapter.DLVh>() {

    private var items = listOf<DownloadEntity>()

    fun submitList(newItems: List<DownloadEntity>) {
        items = newItems
        notifyDataSetChanged()
    }

    inner class DLVh(view: View) : RecyclerView.ViewHolder(view) {
        val cover: ImageView = view.findViewById(R.id.ivDownloadCover)
        val contentTitle: TextView = view.findViewById(R.id.tvDownloadContentTitle)
        val episodeTitle: TextView = view.findViewById(R.id.tvDownloadEpisodeTitle)
        val quality: TextView = view.findViewById(R.id.tvDownloadQuality)
        val status: TextView = view.findViewById(R.id.tvDownloadStatus)
        val btnPlay: ImageButton = view.findViewById(R.id.btnDownloadPlay)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDownloadDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        DLVh(LayoutInflater.from(parent.context).inflate(R.layout.item_download, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: DLVh, position: Int) {
        val item = items[position]
        holder.contentTitle.text = item.contentTitle
        holder.episodeTitle.text = item.episodeTitle
        holder.quality.text = when (item.quality) {
            "GROOT_LD" -> "360p"
            "GROOT_SD" -> "480p"
            "GROOT_FHD" -> "1080p"
            else -> "720p"
        }
        holder.status.text = when (item.status) {
            0 -> "في الانتظار"
            1 -> "جاري التنزيل..."
            2 -> "مكتمل"
            3 -> "فشل"
            else -> ""
        }
        holder.btnPlay.isEnabled = item.status == 2
        holder.btnPlay.alpha = if (item.status == 2) 1f else 0.4f
        holder.btnPlay.setOnClickListener { if (item.status == 2) onPlay(item) }
        holder.btnDelete.setOnClickListener { onDelete(item) }
        Glide.with(holder.cover)
            .load(item.coverUrl)
            .placeholder(R.drawable.placeholder_cover)
            .centerCrop()
            .into(holder.cover)
    }
}
