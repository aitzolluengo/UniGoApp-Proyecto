package com.tzolas.unigoapp.api;

import com.tzolas.unigoapp.model.ServerResponse;
import com.tzolas.unigoapp.model.User;

import retrofit2.Call;
import retrofit2.http.*;

public interface ApiInterface {
    @FormUrlEncoded
    @POST("register.php")
    Call<ServerResponse> register(
            @Field("name") String name,
            @Field("email") String email,
            @Field("password") String password,
            @Field("transportMode") String transportMode,
            @Field("darkMode") boolean darkMode
    );

    @FormUrlEncoded
    @POST("login.php")
    Call<User> login(
            @Field("email") String email,
            @Field("password") String password
    );

    @GET("get_user.php")
    Call<User> getUser(@Query("id") int userId);

    @FormUrlEncoded
    @POST("update_user.php")
    Call<ServerResponse> updateUser(
            @Field("id") int userId,
            @Field("transportMode") String transportMode,
            @Field("darkMode") boolean darkMode
    );
}
