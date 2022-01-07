package com.hafidrf.esqcocktail.commoners

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.hafidrf.esqcocktail.utils.PreferenceHelper
import com.hafidrf.esqcocktail.utils.PreferenceHelper.get
import com.hafidrf.esqcocktail.utils.PreferenceHelper.set
import com.google.gson.Gson
import java.io.ByteArrayOutputStream


object BartenderUtil{

    const val HOME_ALCOHOLIC = "https://www.thecocktaildb.com/api/json/v1/1/filter.php?a=Alcoholic"
    const val HOME_NON_ALCOHOLIC = "https://www.thecocktaildb.com/api/json/v1/1/filter.php?a=Non_Alcoholic"
    const val HOME_ORDINARY_DRINK = "https://www.thecocktaildb.com/api/json/v1/1/filter.php?c=Ordinary_Drink"
    const val HOME_COCKTAIL = "https://www.thecocktaildb.com/api/json/v1/1/filter.php?c=Cocktail"
    const val HOME_COCKTAIL_GLASS = "https://www.thecocktaildb.com/api/json/v1/1/filter.php?g=Cocktail_glass"
    const val HOME_CHAMPAGNE_FLUTE = "https://www.thecocktaildb.com/api/json/v1/1/filter.php?g=Champagne_flute"
    const val COCKTAIL = "https://www.thecocktaildb.com/api/json/v1/1/lookup.php?i="
    const val MORE_COCKTAILS = "https://www.thecocktaildb.com/api/json/v1/1/filter.php?i="
    const val SEARCH_URL = "https://www.thecocktaildb.com/api/json/v1/1/search.php?s="
    private const val KEY = "favedCocktails"

    fun containsCocktail(context: Context, cocktailId: String): Boolean {
        val cocktails = getFaveCocktails(context)

        return cocktails != null && cocktails.contains(cocktailId)
    }

    fun addFaveCocktail(context: Context, cocktailId: String) {
        var cocktails: MutableList<String>? = getFaveCocktails(context)

        if (cocktails != null) {
            cocktails.add(cocktailId)
            saveArrayList(context, cocktails)
        } else {
            cocktails = mutableListOf()
            cocktails.add(cocktailId)
            saveArrayList(context, cocktails)
        }
    }

    fun removeFaveCocktail(context: Context, cocktailId: String) {
        val cocktails: MutableList<String>? = getFaveCocktails(context)

        cocktails!!.remove(cocktailId)
        saveArrayList(context, cocktails)
    }

    private fun saveArrayList(context: Context, favedCocktails: List<String>) {
        val prefs = PreferenceHelper.defaultPrefs(context)

        prefs[KEY] = Gson().toJson(favedCocktails)
    }

    private fun getFaveCocktails(context: Context): ArrayList<String>? {
        val prefs = PreferenceHelper.defaultPrefs(context)

        return if (prefs.contains(KEY)) {
            val faveCocktails: String = prefs[KEY]
            val cocktails = Gson().fromJson(faveCocktails, Array<String>::class.java)

            val drinks = cocktails.toCollection(ArrayList())
            if (!drinks.isEmpty())
                 drinks
            else
                null

        } else
            null

    }

    fun getBytes(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 20, stream)
        return stream.toByteArray()
    }

    fun getImage(image: ByteArray): Bitmap {
        return BitmapFactory.decodeByteArray(image, 0, image.size)
    }

}
