package com.in2l.olympus.util

import com.google.gson.GsonBuilder
import com.in2l.olympus.listener.CaptivePortalDetectionListener
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

/**
 *  Checks if the user requires secondary authentication from a captive portal. While isConnectedTo(Wifi wifi) checks
 *  for an internet connection through the Android API, it returns true even if the user has not yet
 *  gained full access (i.e. "Connected, No Internet Access" aka the user is behind a captive portal).
 *
 *  This method checks for the presence of a captive portal by sending a basic request & evaluating the
 *  response -- 204 is a successful request, anything else (including unexpected 200-level responses) indicates
 *  the presence of a captive portal.
 */
fun checkForCaptivePortal(retrofitCall: Call<Any>, listener : CaptivePortalDetectionListener) {

    retrofitCall.enqueue(object : Callback<Any> {
        override fun onResponse(call: Call<Any>, response: Response<Any>) {
            if (response.code() != 204) {
                listener.captivePortalDetected(response.raw().request().url().toString())
            } else {
                listener.noCaptivePortalDetected()
            }
        }

        override fun onFailure(call: Call<Any>, t: Throwable) {
            listener.captivePortalDetectionFailure()
        }
    })
}

fun getDefaultRetrofitClient() : Call<Any> {
    val service = RetrofitClient.getRetrofitInstance("http://clients3.google.com/")!!.create(RetrofitClient.GetDataService::class.java)
    return service.responseCode
}

object RetrofitClient {

    private var retrofit: Retrofit? = null

    fun getRetrofitInstance(baseUrl: String): Retrofit? {
        if (retrofit == null) {
            retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create(GsonBuilder().setLenient().create()))
                    .build()
        }
        return retrofit
    }

    interface GetDataService {
        @get:GET("generate_204")
        val responseCode: Call<Any>
    }
}