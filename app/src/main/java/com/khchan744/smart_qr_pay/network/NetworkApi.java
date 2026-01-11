package com.khchan744.smart_qr_pay.network;

import com.khchan744.smart_qr_pay.network.dto.CredentialsReqDto;
import com.khchan744.smart_qr_pay.network.dto.HistoryRespDto;
import com.khchan744.smart_qr_pay.network.dto.LoginRespDto;
import com.khchan744.smart_qr_pay.network.dto.PaymentReqDto;
import com.khchan744.smart_qr_pay.network.dto.StandardRespDto;
import com.khchan744.smart_qr_pay.network.dto.TopUpReqDto;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface NetworkApi {

    @POST("/api/register")
    Call<StandardRespDto> register(@Body CredentialsReqDto request);

    @POST("/api/log-in")
    Call<LoginRespDto> logIn(@Body CredentialsReqDto request);

    @POST("/api/verify-payment")
    Call<StandardRespDto> verifyPayment(@Header("Authorization") String bearerToken, @Body PaymentReqDto request);

    @POST("/api/top-up")
    Call<StandardRespDto> topUp(@Header("Authorization") String bearerToken, @Body TopUpReqDto request);

    @GET("/api/get-balance")
    Call<StandardRespDto> getBalance(@Header("Authorization") String bearerToken);

    @GET("/api/get-history")
    Call<HistoryRespDto> getHistory(@Header("Authorization") String bearerToken);
}

