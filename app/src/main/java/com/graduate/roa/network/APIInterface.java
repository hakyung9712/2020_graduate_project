package com.graduate.roa.network;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface APIInterface {
    @Headers("Accept:application/json")
    @Multipart
    @POST("/upload")
    Call<WavFile> registerFile(/*@Query("name") String name,*/ @Part MultipartBody.Part file);

    @Headers("Accept:application/json")
    @GET("/result")
    Call<Object> getResult();

}
