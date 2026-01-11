package com.khchan744.smart_qr_pay.network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Simple Retrofit singleton for app-wide API calls.
 */
public final class NetworkClient {

    // Using 10.0.2.2 is common for Android emulator to access host machine,
    // but your server is in LAN so we use its IP directly.
    private static final String BASE_URL = "http://192.168.0.104:8080";

    public static final boolean IS_AVAILABLE = true;

    private static volatile Retrofit retrofit;

    private NetworkClient() {
    }

    public static Retrofit getRetrofit() {
        if (retrofit == null) {
            synchronized (NetworkClient.class) {
                if (retrofit == null) {
                    retrofit = new Retrofit.Builder()
                            .baseUrl(BASE_URL)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();
                }
            }
        }
        return retrofit;
    }

    public static NetworkApi getAuthApi() {
        return getRetrofit().create(NetworkApi.class);
    }
}

