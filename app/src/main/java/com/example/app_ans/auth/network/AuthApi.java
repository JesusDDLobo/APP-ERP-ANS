package com.example.app_ans.auth.network;

import com.example.app_ans.auth.network.dto.AckRequest;
import com.example.app_ans.auth.network.dto.GoogleCodeRequest;
import com.example.app_ans.auth.network.dto.LoginRequest;
import com.example.app_ans.auth.network.dto.LogoutRequest;
import com.example.app_ans.auth.network.dto.PasswordChangeRequest;
import com.example.app_ans.auth.network.dto.PasswordResetRequest;
import com.example.app_ans.auth.network.dto.PasswordSetupRequest;
import com.example.app_ans.auth.network.dto.SessionResponse;
import com.example.app_ans.auth.network.dto.StatusResponse;
import com.example.app_ans.auth.network.dto.TokenResponse;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

/** Retrofit client for the mobile auth endpoints. */
public interface AuthApi {
    @POST("api/mobile/auth/google")
    Call<SessionResponse> exchangeGoogleCode(@Body GoogleCodeRequest request);

    @POST("api/mobile/auth/login")
    Call<SessionResponse> login(@Body LoginRequest request);

    @POST("api/mobile/force-password-change")
    Call<TokenResponse> forcePasswordChange(@Body PasswordChangeRequest request);

    @POST("api/mobile/auth/password-setup")
    Call<TokenResponse> completePasswordSetup(@Body PasswordSetupRequest request);

    @POST("api/mobile/logout")
    Call<Void> logout(@Body LogoutRequest request);

    @POST("api/mobile/auth/forgot-password")
    Call<Void> forgotPassword(@Body PasswordResetRequest request);

    @POST("api/mobile/auth/status")
    Call<StatusResponse> status(@Body AckRequest request);

    @POST("api/mobile/fcm-token")
    Call<Map<String, Object>> registerFCMToken(@Body Map<String, Object> payload);

    @GET("api/mobile/auth/me")
    Call<SessionResponse> me();
}