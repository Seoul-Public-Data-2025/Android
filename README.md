# 마음편

자녀의 안전한 귀가를 돕는 위치 기반 Android 애플리케이션입니다.  
긴급 상황 알림, 실시간 위치 공유, 안전 경로 안내 등의 기능을 제공합니다.  
MVVM 아키텍처 기반으로 안정적이고 유지보수 가능한 구조로 설계되었습니다.

![Web_App_Reference_Architecture_re3_nogrid](https://github.com/user-attachments/assets/612f4407-ae5f-4fbd-b4dc-329b9afc7b83)

---

## 🧩 주요 기능

- **소셜 로그인**: 카카오 로그인으로 간편한 시작
- **실시간 지도 기반 위치 정보**: CCTV, 경찰서, 지킴이집 등 안전 시설 표시
- **긴급 버튼**: 위험 상황 발생 시 가장 가까운 안전지점으로 자동 안내
- **안전 경로 탐색**: 도착지 및 최대 3개의 경유지를 포함한 최적 경로 제공
- **자녀 위치 실시간 SSE 수신**: 보호자는 앱 내에서 자녀의 움직임을 실시간으로 확인 가능
- **보호자/자녀 관계 연동**: 관계 등록, 승인, 삭제 기능 제공
- **푸시 알림**: Firebase Messaging을 활용한 알림 수신
- **설정**: 공지사항, 도움말, 개인정보처리방침 확인 및 로그아웃 가능

---

## 🛠️ 기술 스택

- Kotlin
- MVVM 아키텍처
- Hilt (DI)
- Retrofit (네트워크 통신)
- Jetpack Components (ViewModel, LiveData, Navigation, Room 등)
- OkHttp (SSE 실시간 통신)
- Glide (이미지 로딩)
- Coroutine (비동기 처리)
- Naver Maps SDK

---

## 📁 프로젝트 구조

마음편은 Android MVVM 아키텍처 패턴을 기반으로 모듈화되어 있으며, 각 계층은 다음과 같은 역할을 수행합니다:
- 📁 model/ - 데이터 클래스 (API 응답 모델 등)
- 📁 network - Retrofit API 정의 및 Hilt 모듈
- 📁 repository - 데이터 처리 및 추상화 계층
- 📁 service - FCM 서비스
- 📁 ui - 화면 구성 (Activity, Fragment, Adapter 등)
- 📁 util - 유틸 클래스 및 공통 도구
- 📁 viewModel - ViewModel (Hilt + State 관리)

---

🙌 기여
기여는 언제나 환영입니다!
1. 이슈를 생성해 주세요.
2. Fork 후 새로운 브랜치에서 작업합니다.
3. Pull Request를 생성합니다.
