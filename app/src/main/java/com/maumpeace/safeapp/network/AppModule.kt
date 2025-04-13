package com.maumpeace.safeapp.network

import android.content.Context
import com.maumpeace.safeapp.repository.LoginRepository
import com.maumpeace.safeapp.util.GlobalApplication
import com.maumpeace.safeapp.util.TokenManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val BASE_URL = "http://54.85.78.12:8000/api/"

    /**
     * ✅ AccessToken을 헤더에 자동 추가하는 Interceptor
     */
    @Provides
    @Singleton
    fun provideAuthInterceptor(): Interceptor {
        val context = GlobalApplication.INSTANCE.applicationContext
        return Interceptor { chain ->
            val token = TokenManager.getAccessToken(context)
            val request = chain.request().newBuilder().apply {
                if (!token.isNullOrBlank()) {
                    header("Authorization", "Bearer $token")
                }
            }.build()
            chain.proceed(request)
        }
    }

    /**
     * ✅ OkHttpClient: Interceptor + Authenticator 연결
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: Interceptor): OkHttpClient {
        val context = GlobalApplication.INSTANCE.applicationContext
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)                         // accessToken 자동 주입
            .authenticator(TokenAuthenticator(context))              // accessToken 만료 시 자동 갱신
            .build()
    }

    /**
     * ✅ Retrofit 인스턴스 구성
     */
    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * ✅ API 호출 정의 클래스 주입
     */
    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }

    /**
     * ✅ LoginRepository 주입
     */
    @Provides
    @Singleton
    fun provideLoginRepository(apiService: ApiService): LoginRepository {
        return LoginRepository(apiService)
    }
}