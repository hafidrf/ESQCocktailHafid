package com.hafidrf.esqcocktail.fragments

import android.app.AlertDialog
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.appcompat.widget.Toolbar
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.hafidrf.esqcocktail.R
import com.hafidrf.esqcocktail.activities.CocktailActivity
import com.hafidrf.esqcocktail.activities.MainActivity
import com.hafidrf.esqcocktail.adapters.HomeAdapter
import com.hafidrf.esqcocktail.commoners.BartenderSingleton
import com.hafidrf.esqcocktail.commoners.BartenderUtil
import com.hafidrf.esqcocktail.models.HomeModel
import com.hafidrf.esqcocktail.utils.*
import com.hafidrf.esqcocktail.utils.PreferenceHelper.get
import com.hafidrf.esqcocktail.utils.PreferenceHelper.set
import com.github.amlcurran.showcaseview.OnShowcaseEventListener
import com.github.amlcurran.showcaseview.ShowcaseView
import com.github.amlcurran.showcaseview.targets.ViewTarget
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.ionicons_typeface_library.Ionicons
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.fragment_home.view.*
import org.json.JSONException
import org.json.JSONObject

/**
 * A simple [Fragment] subclass.
 */
class HomeFragment : Fragment(), MainActivity.ScrollToTopListener, OnShowcaseEventListener {
    private var cocktails: MutableList<HomeModel>? = null
    private var homeAdapter: HomeAdapter? = null
    private var cacheManager: CacheManager? = null

    companion object {
        private const val jsonName = "cocktails.json"
        private var KEY = "first_launch"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val v = inflater.inflate(R.layout.fragment_home, container, false)
        cacheManager = CacheManager(activity!!)
        return v

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        loadCocktails(1)
    }

    private fun initViews(v: View) {
        (activity as MainActivity).setSupportActionBar(v.homeToolbar)
        (activity as MainActivity).supportActionBar!!.setDisplayShowTitleEnabled(false)
        v.homeToolbarTitle.text = getString(R.string.cocktails)
        setupToolbar(v.homeToolbar)

        v.homeCocktailsRv?.setHasFixedSize(true)
        v.homeCocktailsRv?.layoutManager = GridLayoutManager(activity, 2)
        v.homeCocktailsRv?.addItemDecoration(RecyclerFormatter.ItemSpacing(activity!!, R.dimen.spacing))

        cocktails = mutableListOf()
        homeAdapter = HomeAdapter(cocktails as ArrayList<HomeModel>) {loadDetails(it)}
        v.homeCocktailsRv?.adapter = homeAdapter!!
        (activity as MainActivity).scrollRv(this)
    }

    private fun setupToolbar(toolbar: Toolbar) {
        toolbar.inflateMenu(R.menu.home_toolbar_menu)
        val item = toolbar.menu.findItem(R.id.filter_home)
        item?.icon = IconicsDrawable(activity!!).icon(Ionicons.Icon.ion_ios_settings).sizeDp(24).color(ContextCompat.getColor(activity!!, R.color.colorAccent))

        val prefs = PreferenceHelper.defaultPrefs(activity!!)
        if (!prefs[KEY, false]) {
            val target = ViewTarget(R.id.filter_home, activity)
            ShowcaseView.Builder(activity)
                    .withMaterialShowcase()
                    .setStyle(R.style.CustomShowcaseTheme)
                    .setTarget(target)
                    .setShowcaseEventListener(this)
                    .setContentTitle("Filter")
                    .setContentText("Tap here to filter cocktails by type i.e. alcoholic or non-alcoholic")
                    .build()
        }

        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.filter_home -> filter()
            }
            true
        }
    }

    private fun initFromCache() {
       val response = cacheManager!!.readJsonFile(jsonName)

        if (response != null) {
            setCocktails(JSONObject(response))
        } else {
            view?.homeNoInternet?.visibility = View.VISIBLE
            view?.homeLoading?.visibility = View.GONE
        }
    }

    private fun loadCocktails(type: Int) {
        initFromCache()

        if(Connectivity.isConnected(activity!!)){
            if (view?.homeNoInternet?.visibility == View.VISIBLE) view?.homeNoInternet?.visibility = View.GONE
            if (view?.homeLoading?.visibility == View.GONE) view?.homeLoading?.visibility = View.VISIBLE

            when (type) {
                0 -> loadAlcoholic()
                1 -> loadNonAlcoholic()
                2 -> loadOrdinaryDrink()
                3 -> loadCocktail()
                4 -> loadCocktailGlass()
                5 -> loadChampagneFlute()
            }
        } else {
            view?.snack("No internet connection", 8000) {
                action("Turn on", R.color.orange) {startActivity(Intent(WifiManager.ACTION_PICK_WIFI_NETWORK))}
            }
        }

    }

    private fun loadAlcoholic() {
        cocktails?.clear()
        val fetchCocktails = JsonObjectRequest(Request.Method.GET, BartenderUtil.HOME_ALCOHOLIC, null, Response.Listener { response ->
            setCocktails(response)
            cacheManager?.createJsonFile(jsonName, response.toString())
        }, Response.ErrorListener { })
        BartenderSingleton.getInstance(activity).addToRequestQueue(fetchCocktails)

    }

    private fun loadNonAlcoholic() {
        cocktails?.clear()
        val fetchCocktails = JsonObjectRequest(Request.Method.GET, BartenderUtil.HOME_NON_ALCOHOLIC, null, Response.Listener { response ->
            setCocktails(response)
            cacheManager?.createJsonFile(jsonName, response.toString())
        }, Response.ErrorListener { })

        BartenderSingleton.getInstance(activity).addToRequestQueue(fetchCocktails)
    }

    private fun loadOrdinaryDrink() {
        cocktails?.clear()
        val fetchCocktails = JsonObjectRequest(Request.Method.GET, BartenderUtil.HOME_ORDINARY_DRINK, null, Response.Listener { response ->
            setCocktails(response)
            cacheManager?.createJsonFile(jsonName, response.toString())
        }, Response.ErrorListener { })

        BartenderSingleton.getInstance(activity).addToRequestQueue(fetchCocktails)
    }

    private fun loadCocktail() {
        cocktails?.clear()
        val fetchCocktails = JsonObjectRequest(Request.Method.GET, BartenderUtil.HOME_COCKTAIL, null, Response.Listener { response ->
            setCocktails(response)
            cacheManager?.createJsonFile(jsonName, response.toString())
        }, Response.ErrorListener { })

        BartenderSingleton.getInstance(activity).addToRequestQueue(fetchCocktails)
    }

    private fun loadCocktailGlass() {
        cocktails?.clear()
        val fetchCocktails = JsonObjectRequest(Request.Method.GET, BartenderUtil.HOME_COCKTAIL_GLASS, null, Response.Listener { response ->
            setCocktails(response)
            cacheManager?.createJsonFile(jsonName, response.toString())
        }, Response.ErrorListener { })

        BartenderSingleton.getInstance(activity).addToRequestQueue(fetchCocktails)
    }

    private fun loadChampagneFlute() {
        cocktails?.clear()
        val fetchCocktails = JsonObjectRequest(Request.Method.GET, BartenderUtil.HOME_CHAMPAGNE_FLUTE, null, Response.Listener { response ->
            setCocktails(response)
            cacheManager?.createJsonFile(jsonName, response.toString())
        }, Response.ErrorListener { })

        BartenderSingleton.getInstance(activity).addToRequestQueue(fetchCocktails)
    }

    private fun setCocktails(jsonObject: JSONObject) {
        cocktails?.clear()

        try {
            val cocktailArray = jsonObject.getJSONArray("drinks")

            for (i in 0 until cocktailArray.length()) {
                val cocktailObject = cocktailArray.getJSONObject(i)

                val cocktail = HomeModel()
                cocktail.name = cocktailObject.getString("strDrink")
                cocktail.imageUrl = cocktailObject.getString("strDrinkThumb")
                cocktail.id = cocktailObject.getInt("idDrink")

                cocktails?.add(getRandomIndex(cocktails!!.size), cocktail)

            }

            homeAdapter!!.notifyDataSetChanged()
            view?.homeCocktailsRv?.visibility = View.VISIBLE
            view?.homeLoading?.visibility = View.GONE

        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private fun loadDetails(cocktail: HomeModel){
        val intent = Intent(activity, CocktailActivity::class.java)
        intent.putExtra("cocktail", cocktail)
        startActivity(intent)
        activity?.overridePendingTransition(R.anim.enter_b, R.anim.exit_a)
    }

    private fun filter() {
        val items = arrayOf<CharSequence>("Alcoholic", "Non-alcoholic","Ordinary drink","Cocktail",
            "Cocktail glass", "Champagne flute")

        val builder = AlertDialog.Builder(activity)
        builder.setTitle("Filter cocktails")
        builder.setItems(items) { _, item ->
            homeCocktailsRv.visibility = View.GONE
            homeLoading.visibility = View.VISIBLE
            loadCocktails(item)
        }
        val alert = builder.create()
        alert.show()
    }

    private fun getRandomIndex(size: Int): Int {
        return (Math.random() * size).toInt()
    }

    override fun scrollToTop() {
        view?.homeCocktailsRv?.smoothScrollToPosition(0)
    }

    fun onClick(model: HomeModel) {
        val intent = Intent(activity, CocktailActivity::class.java)
        intent.putExtra("cocktail", model)
        startActivity(intent)
        activity!!.overridePendingTransition(R.anim.enter_b, R.anim.exit_a)
    }

    override fun onShowcaseViewShow(showcaseView: ShowcaseView?) {

    }

    override fun onShowcaseViewHide(showcaseView: ShowcaseView?) {
        val prefs = PreferenceHelper.defaultPrefs(activity!!)
        prefs[KEY] = true
    }

    override fun onShowcaseViewDidHide(showcaseView: ShowcaseView?) {

    }

    override fun onShowcaseViewTouchBlocked(motionEvent: MotionEvent?) {

    }
}
