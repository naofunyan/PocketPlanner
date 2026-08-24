package com.example.pocketplanner.utils

import com.mapbox.geojson.Point
import com.mapbox.geojson.LineString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

object RouteFetcher {
    private val client = OkHttpClient()

    suspend fun getRoute(points: List<Point>, accessToken: String): List<Point>? = withContext(Dispatchers.IO) {
        if (points.size < 2) return@withContext null
        if (points.size > 25) return@withContext null

        val coordinates = points.joinToString(";") { "${it.longitude()},${it.latitude()}" }
        val url = "https://api.mapbox.com/directions/v5/mapbox/driving/$coordinates?access_token=$accessToken&geometries=geojson"

        val request = Request.Builder().url(url).build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val json = response.body?.string() ?: return@withContext null
            val root = JSONObject(json)
            val routes = root.optJSONArray("routes")
            if (routes != null && routes.length() > 0) {
                val route = routes.getJSONObject(0)
                val geometry = route.getJSONObject("geometry")
                
                val lineString = LineString.fromJson(geometry.toString())
                return@withContext lineString.coordinates()
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
