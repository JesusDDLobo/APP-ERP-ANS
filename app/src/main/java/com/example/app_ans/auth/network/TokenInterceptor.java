package com.example.app_ans.auth.network;

import androidx.annotation.Nullable;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/** Adds the bearer token to every protected request. */
public class TokenInterceptor implements Interceptor {
    public interface TokenProvider {
        @Nullable String getAccessToken();
    }

    private final TokenProvider tokenProvider;

    public TokenInterceptor(TokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        String token = tokenProvider.getAccessToken();
        if (token == null || request.header("Authorization") != null) {
            return chain.proceed(request);
        }
        Request authorized = request.newBuilder()
                .header("Authorization", "Bearer " + token)
                .build();
        return chain.proceed(authorized);
    }
}

