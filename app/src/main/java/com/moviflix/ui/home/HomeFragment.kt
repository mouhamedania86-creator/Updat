package com.moviflix.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.moviflix.MoviFlixApp
import com.moviflix.R
import com.moviflix.models.BannerItem
import com.moviflix.models.ContentItem
import com.moviflix.models.HomeData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ── ViewModel ─────────────────────────────────────────────────────────────────
class HomeViewModel(private val app: MoviFlixApp) : ViewModel() {
    val pages = mutableListOf<HomeData>()
    var isLoading = false
    var currentPage = 0

    val banners = mutableListOf<BannerItem>()
    val rows = mutableListOf<Pair<String, List<ContentItem>>>()   // title → items

    fun loadPage(onDone: (Boolean) -> Unit) {
        if (isLoading) return
        isLoading = true
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try { app.repository.getHome(++currentPage) } catch (e: Exception) { null }
            }
            isLoading = false
            val data = result?.body()?.data
            if (data != null) {
                // Extract banners and rows from recommendItems
                data.recommendItems.forEach { item ->
                    when {
                        item.bannerList?.isNotEmpty() == true -> {
                            banners.addAll(item.bannerList)
                        }
                        item.contentList?.isNotEmpty() == true -> {
                            rows.add(Pair(item.moduleTitle ?: item.navigationName ?: "", item.contentList))
                        }
                    }
                }
                onDone(true)
            } else {
                onDone(false)
            }
        }
    }
}

class HomeViewModelFactory(private val app: MoviFlixApp) :
    androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return HomeViewModel(app) as T
    }
}

// ── Fragment ──────────────────────────────────────────────────────────────────
class HomeFragment : Fragment() {

    private val vm: HomeViewModel by viewModels {
        HomeViewModelFactory(requireActivity().application as MoviFlixApp)
    }

    private lateinit var shimmer: ShimmerFrameLayout
    private lateinit var heroPager: ViewPager2
    private lateinit var heroTabs: TabLayout
    private lateinit var rvRows: RecyclerView
    private lateinit var rowsAdapter: HomeRowsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        shimmer = view.findViewById(R.id.shimmerHome)
        heroPager = view.findViewById(R.id.heroPager)
        heroTabs = view.findViewById(R.id.heroTabs)
        rvRows = view.findViewById(R.id.rvHomeRows)

        rowsAdapter = HomeRowsAdapter { item ->
            navigateToDetail(item)
        }

        rvRows.layoutManager = LinearLayoutManager(requireContext())
        rvRows.adapter = rowsAdapter
        rvRows.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                val lm = rv.layoutManager as LinearLayoutManager
                val last = lm.findLastVisibleItemPosition()
                if (!vm.isLoading && last >= rowsAdapter.itemCount - 3) {
                    loadMore()
                }
            }
        })

        if (vm.rows.isEmpty()) {
            shimmer.visibility = View.VISIBLE
            shimmer.startShimmer()
            vm.loadPage { success ->
                activity?.runOnUiThread {
                    shimmer.stopShimmer()
                    shimmer.visibility = View.GONE
                    if (success) {
                        setupHero()
                        rowsAdapter.submitRows(vm.rows)
                    }
                }
            }
        } else {
            shimmer.visibility = View.GONE
            setupHero()
            rowsAdapter.submitRows(vm.rows)
        }
    }

    private fun setupHero() {
        if (vm.banners.isEmpty()) return
        val heroBannerAdapter = HeroBannerAdapter(vm.banners) { item ->
            navigateToDetail(
                ContentItem(
                    id = item.id, title = item.title, cover = item.cover,
                    domainType = item.domainType, subType = "TV",
                    score = item.score, cornerInfoList = item.cornerInfoList
                )
            )
        }
        heroPager.adapter = heroBannerAdapter
        TabLayoutMediator(heroTabs, heroPager) { _, _ -> }.attach()

        // Auto-scroll hero banner
        val autoScroll = object : Runnable {
            override fun run() {
                val next = (heroPager.currentItem + 1) % vm.banners.size
                heroPager.setCurrentItem(next, true)
                heroPager.postDelayed(this, 4000)
            }
        }
        heroPager.postDelayed(autoScroll, 4000)
    }

    private fun loadMore() {
        vm.loadPage { success ->
            activity?.runOnUiThread {
                if (success) rowsAdapter.submitRows(vm.rows)
            }
        }
    }

    private fun navigateToDetail(item: ContentItem) {
        val bundle = Bundle().apply {
            putString("contentId", item.id)
            putInt("category", item.domainType)
            putString("title", item.title)
            putString("cover", item.cover)
        }
        findNavController().navigate(R.id.action_home_to_detail, bundle)
    }

    private fun findNavController() =
        androidx.navigation.fragment.NavHostFragment.findNavController(this)
}

// ── Hero banner adapter ───────────────────────────────────────────────────────
class HeroBannerAdapter(
    private val items: List<BannerItem>,
    private val onClick: (BannerItem) -> Unit
) : RecyclerView.Adapter<HeroBannerAdapter.BannerVH>() {

    inner class BannerVH(view: View) : RecyclerView.ViewHolder(view) {
        val img: ImageView = view.findViewById(R.id.ivHeroBanner)
        val title: TextView = view.findViewById(R.id.tvHeroTitle)
        val score: TextView = view.findViewById(R.id.tvHeroScore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        BannerVH(LayoutInflater.from(parent.context).inflate(R.layout.item_hero_banner, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: BannerVH, position: Int) {
        val item = items[position]
        holder.title.text = item.title
        holder.score.text = item.score ?: ""
        Glide.with(holder.img)
            .load(item.bannerImg ?: item.cover)
            .centerCrop()
            .into(holder.img)
        holder.itemView.setOnClickListener { onClick(item) }
    }
}

// ── Home rows adapter (each row = title + horizontal RecyclerView) ─────────────
class HomeRowsAdapter(
    private val onClick: (ContentItem) -> Unit
) : RecyclerView.Adapter<HomeRowsAdapter.RowVH>() {

    private val rows = mutableListOf<Pair<String, List<ContentItem>>>()

    fun submitRows(newRows: List<Pair<String, List<ContentItem>>>) {
        rows.clear()
        rows.addAll(newRows)
        notifyDataSetChanged()
    }

    inner class RowVH(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvRowTitle)
        val rv: RecyclerView = view.findViewById(R.id.rvRowItems)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        RowVH(LayoutInflater.from(parent.context).inflate(R.layout.item_home_row, parent, false))

    override fun getItemCount() = rows.size

    override fun onBindViewHolder(holder: RowVH, position: Int) {
        val (title, items) = rows[position]
        holder.tvTitle.text = title
        val cardAdapter = ContentCardAdapter(items, onClick)
        holder.rv.layoutManager = LinearLayoutManager(
            holder.rv.context, LinearLayoutManager.HORIZONTAL, false
        )
        holder.rv.adapter = cardAdapter
    }
}

// ── Content card adapter ──────────────────────────────────────────────────────
class ContentCardAdapter(
    private val items: List<ContentItem>,
    private val onClick: (ContentItem) -> Unit
) : RecyclerView.Adapter<ContentCardAdapter.CardVH>() {

    inner class CardVH(view: View) : RecyclerView.ViewHolder(view) {
        val cover: ImageView = view.findViewById(R.id.ivCardCover)
        val title: TextView = view.findViewById(R.id.tvCardTitle)
        val badge: TextView = view.findViewById(R.id.tvCardBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        CardVH(LayoutInflater.from(parent.context).inflate(R.layout.item_content_card, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: CardVH, position: Int) {
        val item = items[position]
        holder.title.text = item.title
        val eps = item.updateToEpisodes
        val total = item.totalEpisodes
        holder.badge.text = when {
            eps != null && total != null && eps < total -> "EP $eps"
            else -> item.score ?: ""
        }
        holder.badge.visibility = if (holder.badge.text.isBlank()) View.GONE else View.VISIBLE

        Glide.with(holder.cover)
            .load(item.cover)
            .placeholder(R.drawable.placeholder_cover)
            .centerCrop()
            .into(holder.cover)

        holder.itemView.setOnClickListener { onClick(item) }
    }
}
