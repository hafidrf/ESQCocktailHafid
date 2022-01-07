package com.hafidrf.esqcocktail.fragments


import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.appcompat.widget.Toolbar
import android.view.*
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.hafidrf.esqcocktail.R
import com.hafidrf.esqcocktail.activities.CocktailActivity
import com.hafidrf.esqcocktail.activities.MainActivity
import com.hafidrf.esqcocktail.adapters.SearchAdapter
import com.hafidrf.esqcocktail.commoners.BartenderSingleton
import com.hafidrf.esqcocktail.commoners.BartenderUtil
import com.hafidrf.esqcocktail.models.HomeModel
import com.hafidrf.esqcocktail.utils.RecyclerFormatter
import com.miguelcatalan.materialsearchview.MaterialSearchView
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.ionicons_typeface_library.Ionicons
import kotlinx.android.synthetic.main.fragment_search.*
import kotlinx.android.synthetic.main.fragment_search.view.*
import org.json.JSONException
import android.os.Handler
import android.util.Log
import android.view.inputmethod.InputMethodManager
import com.hafidrf.esqcocktail.commoners.DatabaseHelper


/**
 * A simple [Fragment] subclass.
 */
class SearchFragment : Fragment(), SearchAdapter.OnSearchResultClick, SearchAdapter.OnSearchHistoryClick, MaterialSearchView.OnQueryTextListener {

    private var searchAdapter: SearchAdapter? = null
    private var query: String? = null
    private var myHandler = Handler()
    private val WAIT = 1300L
    private lateinit var db: DatabaseHelper

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_search, container, false)
        db = DatabaseHelper(context)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        searchHistory()
    }

    private fun initViews(v: View) {
        (activity as MainActivity).setSupportActionBar(v.searchToolbar)
        (activity as MainActivity).supportActionBar?.title = null
        v.searchToolbarTitle.text = getString(R.string.search)
        setupToolbar(v.searchToolbar, v)

        v.searchRv.setHasFixedSize(true)
        v.searchRv.layoutManager = LinearLayoutManager(context)
        v.searchRv.addItemDecoration(RecyclerFormatter.SimpleDividerItemDecoration(activity as MainActivity))

        searchAdapter = SearchAdapter(activity, this, this)
        v.searchRv.adapter = searchAdapter

        v.searchingIcon.setImageDrawable(IconicsDrawable(activity as MainActivity).icon(Ionicons.Icon.ion_ios_search).sizeDp(22).color(ContextCompat.getColor(activity as MainActivity, R.color.dark_gray)))
    }

    private fun setupToolbar(toolbar: Toolbar, v: View) {
        toolbar.inflateMenu(R.menu.search_toolbar_menu)
        val item = toolbar.menu.findItem(R.id.action_search)
        item?.icon = IconicsDrawable(activity!!).icon(Ionicons.Icon.ion_ios_search_strong).sizeDp(22).color(ContextCompat.getColor(activity!!, R.color.colorAccent))
        v.searchView.setMenuItem(item)
        v.searchView.setOnQueryTextListener(this)

    }

    private fun searchHistory(){
        val history = db.fetchHistory()

        if(history.size > 0){
            with(history) {
                forEach { searchAdapter?.addCocktail(it) }
            }
        } else {
            view?.noSearchHistory?.visibility = View.VISIBLE
        }
    }

    private fun searchCocktails(name: String?) {
        searchAdapter?.clear()

        val fetchCocktails = JsonObjectRequest(Request.Method.GET, BartenderUtil.SEARCH_URL + name, null, Response.Listener { response ->
            try {
                val cocktailArray = response.getJSONArray("drinks")

                view?.noSearchHistory?.visibility = View.GONE

                for (i in 0 until cocktailArray.length()) {
                    val cocktailObject = cocktailArray.getJSONObject(i)

                    val cocktail = HomeModel()
                    cocktail.name = cocktailObject.getString("strDrink")
                    cocktail.imageUrl = cocktailObject.getString("strDrinkThumb")
                    cocktail.id = cocktailObject.getInt("idDrink")
                    cocktail.type = 1

                    searchAdapter?.addCocktail(cocktail)
                    Handler().postDelayed({view?.searchingLayout?.visibility = View.GONE}, 1500)
                }

            } catch (e: JSONException) {
                Log.d(javaClass.simpleName, "Json Exception: $e.localizedMessage")
                Handler().postDelayed({view?.searchingLayout?.visibility = View.GONE}, 1500)
            }
        }, Response.ErrorListener {
            Log.d(javaClass.simpleName, "Response error: $it.localizedMessage")
            Handler().postDelayed({view?.searchingLayout?.visibility = View.GONE}, 1500)
        })

        BartenderSingleton.getInstance(activity).addToRequestQueue(fetchCocktails)
    }

    override fun onSearchResultClick(model: HomeModel) {
        db.addHistory(model.id.toString(), model.name, model.imageUrl)

        val intent = Intent(activity, CocktailActivity::class.java)
        intent.putExtra("cocktail", model)
        startActivity(intent)
        activity?.overridePendingTransition(R.anim.enter_b, R.anim.exit_a)
    }

    override fun onSearchHistoryClick(model: HomeModel?) {
        val intent = Intent(activity, CocktailActivity::class.java)
        intent.putExtra("cocktail", model)
        startActivity(intent)
        activity?.overridePendingTransition(R.anim.enter_b, R.anim.exit_a)
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        view?.searchingLayout?.visibility = View.VISIBLE

        Handler().postDelayed({searchCocktails(query?.trim())}, 2000)
        val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.applicationWindowToken, 0)
        return true
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        newText?.let {
            query = it.trim()
            restart() }
        return true
    }

    fun onBackPressed():Boolean = when(view?.searchView?.isSearchOpen){
        true->{
            searchView.closeSearch()
            stop()
            false
        }
        else->true
    }

    private var myRunnable: Runnable = Runnable {
        if(query?.isNotEmpty()!! && query?.isNotBlank()!!){
            view?.searchingLayout?.visibility = View.VISIBLE
            searchCocktails(query)
        }
    }


    fun start() {
        myHandler.postDelayed(myRunnable, WAIT)
    }

    private fun stop() {
        myHandler.removeCallbacks(myRunnable)
    }

    private fun restart() {
        myHandler.removeCallbacks(myRunnable)
        myHandler.postDelayed(myRunnable, WAIT)
    }

    override fun onPause() {
        super.onPause()
        stop()
    }


}
