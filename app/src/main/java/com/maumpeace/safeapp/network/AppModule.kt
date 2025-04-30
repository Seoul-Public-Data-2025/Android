package com.maumpeace.safeapp.network

import com.maumpeace.safeapp.BuildConfig
import com.maumpeace.safeapp.repository.AlarmRepository
import com.maumpeace.safeapp.repository.CreateRelationRepository
import com.maumpeace.safeapp.repository.LoginRepository
import com.maumpeace.safeapp.repository.LogoutRepository
import com.maumpeace.safeapp.repository.RelationChildListRepository
import com.maumpeace.safeapp.repository.RelationGuardianListRepository
import com.maumpeace.safeapp.repository.SecessionRepository
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

/**
 * 앱 전역에서 사용될 의존성을 정의하는 Hilt 모듈
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val BASE_URL = BuildConfig.BASE_URL

    /**
     * Authorization 헤더 자동 주입 Interceptor
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
     * OkHttpClient 구성
     * - 인증 Interceptor 포함
     * - 토큰 자동 재발급을 위한 Authenticator 포함
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: Interceptor): OkHttpClient {
        val context = GlobalApplication.INSTANCE.applicationContext
        return OkHttpClient.Builder().addInterceptor(authInterceptor)
            .authenticator(TokenAuthenticator(context)).build()
    }

    /**
     * Retrofit 클라이언트 구성
     */
    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder().baseUrl(BASE_URL).client(client)
            .addConverterFactory(GsonConverterFactory.create()).build()
    }

    /**
     * SafeApp API 서비스 주입
     */
    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }

    /**
     * LoginRepository 주입
     */
    @Provides
    @Singleton
    fun provideLoginRepository(apiService: ApiService): LoginRepository {
        return LoginRepository(apiService)
    }

    /**
     * LogoutRepository 주입
     */
    @Provides
    @Singleton
    fun provideLogoutRepository(apiService: ApiService): LogoutRepository {
        return LogoutRepository(apiService)
    }

    /**
     * SecessionRepository 주입
     */
    @Provides
    @Singleton
    fun provideSecessionRepository(apiService: ApiService): SecessionRepository {
        return SecessionRepository(apiService)
    }

    /**
     * AlarmRepository 주입
     */
    @Provides
    @Singleton
    fun provideAlarmRepository(apiService: ApiService): AlarmRepository {
        return AlarmRepository(apiService)
    }

    /**
     * 관계 생성 주입
     */
    @Provides
    @Singleton
    fun provideCreateRelationRepository(apiService: ApiService): CreateRelationRepository {
        return CreateRelationRepository(apiService)
    }

    /**
     * 보호자 리스트 조회
     */
    @Provides
    @Singleton
    fun provideRelationGuardianListRepository(apiService: ApiService): RelationGuardianListRepository {
        return RelationGuardianListRepository(apiService)
    }

    /**
     * 자녀 리스트 조회
     */
    @Provides
    @Singleton
    fun provideRelationChildListRepository(apiService: ApiService): RelationChildListRepository {
        return RelationChildListRepository(apiService)
    }

    /**
     * 네이버 경로 API용 Retrofit 서비스 주입
     */
    @Provides
    @Singleton
    fun provideNaverDirectionsService(): NaverDirectionsService {
        return Retrofit.Builder().baseUrl("https://maps.apigw.ntruss.com/map-direction/")
            .addConverterFactory(GsonConverterFactory.create()).build()
            .create(NaverDirectionsService::class.java)
    }
}