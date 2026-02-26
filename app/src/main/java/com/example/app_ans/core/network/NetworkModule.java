package com.example.app_ans.core.network;

import com.example.app_ans.BuildConfig;
import com.example.app_ans.auth.network.AuthApi;
import com.example.app_ans.auth.network.TokenInterceptor;
import com.example.app_ans.auth.session.AuthSessionManager;
import com.example.app_ans.vehicles.network.VehicleApi;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import android.content.Context;
import okhttp3.Interceptor;
import okhttp3.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Centralized network builders for the app. */
public final class NetworkModule {
    private static volatile Retrofit retrofit;
    
    // We cannot hold a static reference to CookieJar if it needs Context.
    // Instead, we will initialize it in buildRetrofit or pass it.
    // But since Retrofit is singleton, we must be careful with Context leaks.
    // Using Application Context is safe.
    
    private NetworkModule() {}

    public static Retrofit provideRetrofit(Context context, AuthSessionManager sessionManager) {
        if (retrofit == null) {
            synchronized (NetworkModule.class) {
                if (retrofit == null) {
                    retrofit = buildRetrofit(context.getApplicationContext(), sessionManager);
                }
            }
        }
        return retrofit;
    }

    private static Retrofit buildRetrofit(Context context, AuthSessionManager sessionManager) {
        Gson gson = new GsonBuilder().create();
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.NONE);  // Disabled logging to prevent emulator crash

        PersistentCookieJar cookieJar = new PersistentCookieJar(context);

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .addNetworkInterceptor(new CookieFixerInterceptor()) // Fix cookies before they reach the CookieJar
                .addInterceptor(new AcceptHeaderInterceptor())       // Force JSON responses
                .cookieJar(cookieJar)
                .addInterceptor(new TokenInterceptor(sessionManager::getAccessToken))
                .addInterceptor(logging)
                .build();

        return new Retrofit.Builder()
                .baseUrl(BuildConfig.API_BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
    }

    /** Interceptor to strip 'domain=localhost' from Set-Cookie headers so they work on 10.0.2.2. */
    private static class CookieFixerInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Response originalResponse = chain.proceed(chain.request());
            if (!originalResponse.headers("Set-Cookie").isEmpty()) {
                List<String> cookies = new ArrayList<>();
                for (String header : originalResponse.headers("Set-Cookie")) {
                    // Strip 'domain=localhost' (case-insensitive)
                    String fixed = header.replaceAll("(?i);\\s*domain=localhost", "");
                    cookies.add(fixed);
                }
                
                // Rebuild response with new headers
                Response.Builder builder = originalResponse.newBuilder();
                builder.removeHeader("Set-Cookie");
                for (String cookie : cookies) {
                    builder.addHeader("Set-Cookie", cookie);
                }
                return builder.build();
            }
            return originalResponse;
        }
    }
    
    private static class AcceptHeaderInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
             return chain.proceed(chain.request().newBuilder()
                     .addHeader("Accept", "application/json")
                     .build());
        }
    }

    public static AuthApi provideAuthApi(Context context, AuthSessionManager sessionManager) {
        return provideRetrofit(context, sessionManager).create(AuthApi.class);
    }

    public static VehicleApi provideVehicleApi(Context context, AuthSessionManager sessionManager) {
        return provideRetrofit(context, sessionManager).create(VehicleApi.class);
    }

    public static com.example.app_ans.tasks.network.TaskApi provideTaskApi(Context context, AuthSessionManager sessionManager) {
        return provideRetrofit(context, sessionManager).create(com.example.app_ans.tasks.network.TaskApi.class);
    }
}
