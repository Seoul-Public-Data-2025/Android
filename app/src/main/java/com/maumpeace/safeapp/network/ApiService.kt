package com.maumpeace.safeapp.network

import com.maumpeace.safeapp.model.AlarmData
import com.maumpeace.safeapp.model.ChildLocationData
import com.maumpeace.safeapp.model.ChildLocationDisconnectData
import com.maumpeace.safeapp.model.CreateRelationData
import com.maumpeace.safeapp.model.FetchAlarmData
import com.maumpeace.safeapp.model.FetchChildLocation
import com.maumpeace.safeapp.model.FetchCreateRelationData
import com.maumpeace.safeapp.model.FetchLoginData
import com.maumpeace.safeapp.model.FetchLogoutData
import com.maumpeace.safeapp.model.FetchRelationChildApproveData
import com.maumpeace.safeapp.model.FetchRelationGuardianDeleteData
import com.maumpeace.safeapp.model.FetchRelationResendData
import com.maumpeace.safeapp.model.LoginData
import com.maumpeace.safeapp.model.LogoutData
import com.maumpeace.safeapp.model.MapMarkerData
import com.maumpeace.safeapp.model.RelationChildApproveData
import com.maumpeace.safeapp.model.RelationChildListData
import com.maumpeace.safeapp.model.RelationGuardianDeleteData
import com.maumpeace.safeapp.model.RelationGuardianListData
import com.maumpeace.safeapp.model.RelationResendData
import com.maumpeace.safeapp.model.SecessionData
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST

/**
 * SafeApp의 모든 REST API 엔드포인트를 정의하는 인터페이스
 * - ViewModel/Repository에서는 suspend 사용
 * - TokenAuthenticator 등에서는 동기 Call 사용
 */
interface ApiService {

    /**
     * 카카오 소셜 로그인 요청
     * @param fetchLoginData 카카오 액세스 토큰 및 이메일
     * @return 서버 JWT 토큰 포함 응답
     */
    @POST("auth/kakao-login/")
    suspend fun loginWithKakao(
        @Body fetchLoginData: FetchLoginData
    ): LoginData

    /**
     * accessToken 갱신 (비동기)
     * @param body refreshToken을 담은 맵
     * @return 새 accessToken 포함 LoginData
     */
    @POST("auth/refresh/")
    suspend fun refreshAccessTokenAsync(
        @Body body: Map<String, String>
    ): LoginData

    /**
     * accessToken 갱신 (동기)
     * TokenAuthenticator에서 사용
     */
    @POST("auth/refresh/")
    fun refreshAccessTokenSync(
        @Body body: Map<String, String>
    ): Call<LoginData>

    /**
     * 로그아웃 요청
     * @param fetchLogoutData 사용자 식별 정보
     * @return 로그아웃 처리 결과
     */
    @POST("auth/logout/")
    suspend fun logout(
        @Body fetchLogoutData: FetchLogoutData
    ): LogoutData

    /**
     * 지도 마커 요청
     * @return 서버에서 받은 마커 데이터
     */
    @GET("display-icon/")
    suspend fun mapMarker(): MapMarkerData

    /**
     * 회원탈퇴 요청
     */
    @HTTP(
        method = "DELETE", path = "user/", hasBody = true
    )
    suspend fun secession(): SecessionData

    /**
     * 알람 요청
     */
    @HTTP(
        method = "PATCH", path = "user/", hasBody = true
    )
    suspend fun alarm(
        @Body fetchAlarmData: FetchAlarmData
    ): AlarmData

    /*
    * 관계 생성
    */
    @POST("relation-request/")
    suspend fun createRelation(
        @Body fetchCreateRelationData: FetchCreateRelationData
    ): CreateRelationData

    /*
    * 보호자 리스트 조회
    */
    @GET("relation-parent-list/")
    suspend fun relationGuardianList(): RelationGuardianListData

    /*
    * 자녀 리스트 조회
    */
    @GET("relation-child-list/")
    suspend fun relationChildList(): RelationChildListData

    /*
    * 자녀 요청 수락
    */
    @POST("relation-approve/")
    suspend fun relationChildApprove(
        @Body fetchRelationChildApproveData: FetchRelationChildApproveData
    ): RelationChildApproveData

    /*
    * 보호자 등록 노티 재발송
    */
    @POST("relation-resend/")
    suspend fun relationResend(
        @Body fetchRelationResendData: FetchRelationResendData
    ): RelationResendData

    /*
    * 자녀 위치 전송(SSE)
    */
    @POST("child-location/")
    suspend fun childLocation(
        @Body fetchChildLocation: FetchChildLocation
    ): ChildLocationData

    /*
    * 자녀 위치 전송 종료(SSE Disconnect)
    */
    @POST("child-disconnection/")
    suspend fun childLocationDisconnect(): ChildLocationDisconnectData

    /*
    * 보호자 해지 요청
    */
    @HTTP(
        method = "DELETE", path = "relation-delete/", hasBody = true
    )
    suspend fun relationGuardianDelete(
        @Body fetchRelationGuardianDeleteData: FetchRelationGuardianDeleteData
    ): RelationGuardianDeleteData
}