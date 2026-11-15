package com.moviflix.ui.search

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import androidx.recyclerview.widget.*
import com.bumptech.glide.Glide
import com.moviflix.MoviFlixApp
import com.moviflix.R
import com.moviflix.models.ContentItem
import kotlinx.coroutines.*

class SearchViewModel(private val app: MoviFlixApp) : ViewModel() {
    var query = ""
    val results = mutableListOf<ContentItem>()
    val trending = mutableListOf<ContentItem>()
    var currentPage = 1
    var totalPages = 1
    var isSearching = false

    fun loadTrending(onDone: () -> Unit) {
        viewModelScope.launch {
            val r = withContext(Dispatchers.IO) {
                try { app.repository.getSearchDefault() } catch (e: Exception) { null }
            }
            r?.body()?.data?.leaderboard?.let { trending.addAll(it) }
            onDone()
        }
    }

    fun search(keyword: String, page: Int = 1, onDone: (Boolean) -> Unit) {
        if (isSearching) return
        isSearching = true
        query = keyword
        viewModelScope.launch {
            val r = withContext(Dispatchers.IO) {
                try { app.repository.searchContent(keyword, page) } catch (e: Exception) { null }
            }
            isSearching = false
            val data = r?.body()?.data
            if (data != null) {
                if (page == 1) results.clear()
                results.addAll(data.list)
                totalPages = data.totalPage
                onDone(true)
            } else {
                onDone(false)
            }
        }
    }
}

class SearchViewModelFactory(private val app: MoviFlixApp) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return SearchViewModel(app) as T
    }
}

class SearchFragment : Fragment() {

    private val vm: SearchViewModel by viewModels {
        SearchViewModelFactory(requireActivity().application as MoviFlixApp)
    }

    private lateinit var etSearch: EditText
    private lateinit var rvResults: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var pbSearch: ProgressBar
    private lateinit var rvTrending: RecyclerView

    private lateinit var resultsAdapter: SearchResultAdapter
    private val debounceJob = Job()
    private var searchScope = CoroutineScope(Dispatchers.Main + debounceJob)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_search, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etSearch = view.findViewById(R.id.etSearch)
        rvResults = view.findViewById(R.id.rvSearchResults)
        tvEmpty = view.findViewById(R.id.tvSearchEmpty)
        pbSearch = view.findViewById(R.id.pbSearch)
        rvTrending = view.findViewById(R.id.rvTrending)

        resultsAdapter = SearchResultAdapter { item ->
            navigateToDetail(item)
        }

        rvResults.layoutManager = GridLayoutManager(requireContext(), 3)
        rvResults.adapter = resultsAdapter

        // Trending / default page
        if (vm.trending.isEmpty()) {
            vm.loadTrending {
                activity?.runOnUiThread {
                    rvTrending.layoutManager = GridLayoutManager(requireContext(), 2)
                    rvTrending.adapter = SearchResultAdapter(vm.trending) { navigateToDetail(it) }
                }
            }
        } else {
            rvTrending.layoutManager = GridLayoutManager(requireContext(), 2)
            rvTrending.adapter = SearchResultAdapter(vm.trending) { navigateToDetail(it) }
        }

        // If restoring query
        if (vm.query.isNotEmpty()) {
            etSearch.setText(vm.query)
            showResults()
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val kw = s?.toString()?.trim() ?: ""
                if (kw.isEmpty()) {
                    rvTrending.visibility = View.VISIBLE
                    rvResults.visibility = View.GONE
                    tvEmpty.visibility = View.GONE
                    return
                }
                // Debounce 400ms
                searchScope.coroutineContext[Job]?.cancelChildren()
                searchScope.launch {
                    delay(400)
                    doSearch(kw)
                }
            }
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
        })

        // Infinite scroll
        rvResults.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                val lm = rv.layoutManager as GridLayoutManager
                if (!vm.isSearching &&
                    vm.currentPage < vm.totalPages &&
                    lm.findLastVisibleItemPosition() >= resultsAdapter.itemCount - 6
                ) {
                    doSearch(vm.query, vm.currentPage + 1)
                }
            }
        })
    }

    private fun doSearch(kw: String, page: Int = 1) {
        pbSearch.visibility = View.VISIBLE
        vm.search(kw, page) { success ->
            activity?.runOnUiThread {
                pbSearch.visibility = View.GONE
                if (success) showResults()
                else tvEmpty.visibility = if (vm.results.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun showResults() {
        rvTrending.visibility = View.GONE
        rvResults.visibility = View.VISIBLE
        resultsAdapter.submitList(vm.results.toList())
        tvEmpty.visibility = if (vm.results.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun navigateToDetail(item: ContentItem) {
        val bundle = Bundle().apply {
            putString("contentId", item.id)
            putInt("category", item.domainType)
            putString("title", item.title)
            putString("cover", item.cover)
        }
        findNavController().navigate(R.id.action_search_to_detail, bundle)
    }

    private fun findNavController() =
        androidx.navigation.fragment.NavHostFragment.findNavController(this)

    override fun onDestroyView() {
        super.onDestroyView()
        debounceJob.cancel()
    }
}

class SearchResultAdapter(
    private var items: List<ContentItem> = emptyList(),
    private val onClick: (ContentItem) -> Unit
) : RecyclerView.Adapter<SearchResultAdapter.VH>() {

    fun submitList(newItems: List<ContentItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val cover: ImageView = view.findViewById(R.id.ivCardCover)
        val title: TextView = view.findViewById(R.id.tvCardTitle)
        val badge: TextView = view.findViewById(R.id.tvCardBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_content_card, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.title.text = item.title
        holder.badge.text = item.score ?: ""
        holder.badge.visibility = if (holder.badge.text.isBlank()) View.GONE else View.VISIBLE
        Glide.with(holder.cover)
            .load(item.cover)
            .placeholder(R.drawable.placeholder_cover)
            .centerCrop()
            .into(holder.cover)
        holder.itemView.setOnClickListener { onClick(item) }
    }
}
