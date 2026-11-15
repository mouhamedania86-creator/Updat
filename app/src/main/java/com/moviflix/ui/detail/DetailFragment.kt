package com.moviflix.ui.detail

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import androidx.recyclerview.widget.*
import com.bumptech.glide.Glide
import com.moviflix.MoviFlixApp
import com.moviflix.R
import com.moviflix.models.*
import com.moviflix.ui.player.PlayerActivity
import kotlinx.coroutines.*

// ── ViewModel ─────────────────────────────────────────────────────────────────
class DetailViewModel(private val app: MoviFlixApp) : ViewModel() {
    var detail: ContentDetail? = null
    var isLoading = false

    fun load(contentId: String, category: Int, onDone: (Boolean) -> Unit) {
        if (isLoading) return
        isLoading = true
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try { app.repository.getContentDetail(contentId, category) }
                catch (e: Exception) { null }
            }
            isLoading = false
            detail = result
            onDone(result != null)
        }
    }
}

class DetailViewModelFactory(private val app: MoviFlixApp) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return DetailViewModel(app) as T
    }
}

// ── Fragment ──────────────────────────────────────────────────────────────────
class DetailFragment : Fragment() {

    private val vm: DetailViewModel by viewModels {
        DetailViewModelFactory(requireActivity().application as MoviFlixApp)
    }

    private lateinit var ivHero: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var tvMeta: TextView
    private lateinit var tvScore: TextView
    private lateinit var tvDesc: TextView
    private lateinit var tvDirector: TextView
    private lateinit var tvActors: TextView
    private lateinit var pbLoading: ProgressBar
    private lateinit var btnPlay: Button
    private lateinit var rvEpisodes: RecyclerView
    private lateinit var rvCast: RecyclerView
    private lateinit var tvEpisodeHeader: TextView
    private lateinit var spinnerSeason: Spinner

    private var contentId = ""
    private var category = 1
    private var selectedSeason = 0
    private var allEpisodes: List<EpisodeItem> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_detail, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        contentId = arguments?.getString("contentId") ?: return
        category = arguments?.getInt("category") ?: 1
        val quickTitle = arguments?.getString("title") ?: ""
        val quickCover = arguments?.getString("cover") ?: ""

        bindViews(view)

        // Show quick title/cover while loading
        tvTitle.text = quickTitle
        Glide.with(ivHero).load(quickCover).centerCrop().into(ivHero)

        view.findViewById<ImageButton>(R.id.ibDetailBack).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        if (vm.detail != null) {
            renderDetail(vm.detail!!)
        } else {
            pbLoading.visibility = View.VISIBLE
            vm.load(contentId, category) { success ->
                activity?.runOnUiThread {
                    pbLoading.visibility = View.GONE
                    if (success) renderDetail(vm.detail!!)
                    else Toast.makeText(requireContext(), "فشل التحميل", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun bindViews(view: View) {
        ivHero = view.findViewById(R.id.ivDetailHero)
        tvTitle = view.findViewById(R.id.tvDetailTitle)
        tvMeta = view.findViewById(R.id.tvDetailMeta)
        tvScore = view.findViewById(R.id.tvDetailScore)
        tvDesc = view.findViewById(R.id.tvDetailDesc)
        tvDirector = view.findViewById(R.id.tvDetailDirector)
        tvActors = view.findViewById(R.id.tvDetailActors)
        pbLoading = view.findViewById(R.id.pbDetailLoading)
        btnPlay = view.findViewById(R.id.btnDetailPlay)
        rvEpisodes = view.findViewById(R.id.rvDetailEpisodes)
        rvCast = view.findViewById(R.id.rvDetailCast)
        tvEpisodeHeader = view.findViewById(R.id.tvEpisodeHeader)
        spinnerSeason = view.findViewById(R.id.spinnerSeason)
    }

    private fun renderDetail(detail: ContentDetail) {
        tvTitle.text = detail.title
        tvScore.text = "⭐ ${detail.score ?: "N/A"}"

        val meta = listOfNotNull(
            detail.country,
            detail.year?.toString(),
            detail.tags?.take(3)?.joinToString(" · ") { it.name }
        ).joinToString(" · ")
        tvMeta.text = meta

        tvDesc.text = detail.desc ?: ""
        tvDirector.text = "المخرج: ${detail.director ?: "-"}"
        tvActors.text = "الممثلون: ${detail.actors ?: "-"}"

        Glide.with(ivHero)
            .load(detail.horizontalCover ?: detail.cover)
            .centerCrop()
            .into(ivHero)

        // ── Episode list ──
        val seriesList = detail.seriesList
        val flatEpisodes = when {
            seriesList?.isNotEmpty() == true -> {
                // Multi-season
                val seasonTitles = seriesList.map { "الموسم ${it.season}" }
                val spinnerAdapter = ArrayAdapter(
                    requireContext(), android.R.layout.simple_spinner_item, seasonTitles
                )
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerSeason.adapter = spinnerAdapter
                spinnerSeason.visibility = View.VISIBLE
                spinnerSeason.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>, v: View?, pos: Int, id: Long) {
                        selectedSeason = pos
                        allEpisodes = seriesList[pos].episodeList ?: emptyList()
                        renderEpisodes(allEpisodes, detail)
                    }
                    override fun onNothingSelected(parent: AdapterView<*>) {}
                }
                seriesList[0].episodeList ?: emptyList()
            }
            detail.episodeList?.isNotEmpty() == true -> detail.episodeList
            else -> emptyList()
        }

        allEpisodes = flatEpisodes

        if (flatEpisodes.isNotEmpty()) {
            tvEpisodeHeader.visibility = View.VISIBLE
            renderEpisodes(flatEpisodes, detail)
        }

        // ── Cast ──
        detail.starList?.let { stars ->
            if (stars.isNotEmpty()) {
                rvCast.visibility = View.VISIBLE
                rvCast.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                rvCast.adapter = CastAdapter(stars)
            }
        }

        // ── Play button ──
        // For movies: single play. For series: play first episode
        btnPlay.visibility = View.VISIBLE
        btnPlay.setOnClickListener {
            val firstEp = allEpisodes.firstOrNull()
            val epId = firstEp?.id ?: ""
            PlayerActivity.launch(requireContext(), contentId, epId, category, detail.title)
        }
    }

    private fun renderEpisodes(episodes: List<EpisodeItem>, detail: ContentDetail) {
        rvEpisodes.layoutManager = LinearLayoutManager(requireContext())
        rvEpisodes.adapter = EpisodeAdapter(episodes) { ep ->
            PlayerActivity.launch(requireContext(), contentId, ep.id, category, "${detail.title} - ${ep.title}")
        }
    }
}

// ── Episode adapter ───────────────────────────────────────────────────────────
class EpisodeAdapter(
    private val episodes: List<EpisodeItem>,
    private val onClick: (EpisodeItem) -> Unit
) : RecyclerView.Adapter<EpisodeAdapter.EpVH>() {

    inner class EpVH(view: View) : RecyclerView.ViewHolder(view) {
        val number: TextView = view.findViewById(R.id.tvEpNumber)
        val title: TextView = view.findViewById(R.id.tvEpTitle)
        val duration: TextView = view.findViewById(R.id.tvEpDuration)
        val lock: ImageView = view.findViewById(R.id.ivEpLock)
        val cover: ImageView = view.findViewById(R.id.ivEpCover)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        EpVH(LayoutInflater.from(parent.context).inflate(R.layout.item_episode, parent, false))

    override fun getItemCount() = episodes.size

    override fun onBindViewHolder(holder: EpVH, position: Int) {
        val ep = episodes[position]
        holder.number.text = ep.episodeNo.toString()
        holder.title.text = ep.title
        holder.duration.text = ep.duration?.let { formatMs(it) } ?: ""
        holder.lock.visibility = if (ep.vipEpisode) View.VISIBLE else View.GONE
        ep.cover?.let {
            Glide.with(holder.cover).load(it).centerCrop().into(holder.cover)
        }
        holder.itemView.setOnClickListener { onClick(ep) }
    }

    private fun formatMs(ms: Long): String {
        val sec = ms / 1000
        val m = sec / 60; val s = sec % 60
        return "%d:%02d".format(m, s)
    }
}

// ── Cast adapter ──────────────────────────────────────────────────────────────
class CastAdapter(
    private val stars: List<StarItem>
) : RecyclerView.Adapter<CastAdapter.CastVH>() {

    inner class CastVH(view: View) : RecyclerView.ViewHolder(view) {
        val img: ImageView = view.findViewById(R.id.ivCastImg)
        val name: TextView = view.findViewById(R.id.tvCastName)
        val role: TextView = view.findViewById(R.id.tvCastRole)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        CastVH(LayoutInflater.from(parent.context).inflate(R.layout.item_cast, parent, false))

    override fun getItemCount() = stars.size

    override fun onBindViewHolder(holder: CastVH, position: Int) {
        val star = stars[position]
        holder.name.text = star.name
        holder.role.text = star.role ?: ""
        Glide.with(holder.img)
            .load(star.headImg)
            .circleCrop()
            .placeholder(R.drawable.placeholder_avatar)
            .into(holder.img)
    }
}
