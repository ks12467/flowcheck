package com.bootcamp.flowcheck.global.sheets;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.Permission;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class GoogleSheetsService {

    private static final String APPLICATION_NAME = "FlowCheck";
    private static final List<String> SCOPES = List.of(
            SheetsScopes.SPREADSHEETS_READONLY,
            DriveScopes.DRIVE
    );
    private static final long CACHE_TTL_MS = 5 * 60 * 1000L; // 5분 캐시

    @Value("${google.sheets.credentials-path:classpath:credentials.json}")
    private Resource credentialsResource;

    private Sheets sheetsClient;
    private Drive driveClient;
    private String serviceAccountEmail;

    // 캐시: spreadsheetId → [responseCount, expiresAtMillis]
    private final Map<String, long[]> cache = new ConcurrentHashMap<>();

    // 캐시: "spreadsheetId\0scoreColumnHeader" → [averageScore*1000, expiresAtMillis]
    private final Map<String, long[]> scoreCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        try {
            if (!credentialsResource.exists()) {
                log.warn("[Sheets] credentials 파일을 찾을 수 없습니다: {} — 구글 시트 연동이 비활성화됩니다.", credentialsResource);
                return;
            }
            try (var stream = credentialsResource.getInputStream()) {
                GoogleCredentials credentials = GoogleCredentials
                        .fromStream(stream)
                        .createScoped(SCOPES);

                if (credentials instanceof ServiceAccountCredentials saCredentials) {
                    serviceAccountEmail = saCredentials.getClientEmail();
                    log.info("[Sheets] 서비스 계정: {}", serviceAccountEmail);
                }

                var transport = GoogleNetHttpTransport.newTrustedTransport();
                var jsonFactory = GsonFactory.getDefaultInstance();
                var httpCredentials = new HttpCredentialsAdapter(credentials);

                this.sheetsClient = new Sheets.Builder(transport, jsonFactory, httpCredentials)
                        .setApplicationName(APPLICATION_NAME).build();
                this.driveClient = new Drive.Builder(transport, jsonFactory, httpCredentials)
                        .setApplicationName(APPLICATION_NAME).build();

                log.info("[Sheets] Google Sheets/Drive 서비스 계정 인증 완료");
            }
        } catch (Exception e) {
            log.warn("[Sheets] 초기화 실패: {} — 구글 시트/드라이브 연동이 비활성화됩니다.", e.getMessage());
        }
    }

    /**
     * 서비스 계정 이메일을 반환합니다.
     *
     * @return 서비스 계정 이메일, 초기화 실패 시 null
     */
    public String getServiceAccountEmail() {
        return serviceAccountEmail;
    }

    /**
     * 스프레드시트에 서비스 계정을 뷰어로 추가합니다.
     *
     * @return true: 성공, false: 실패 (이미 공유된 경우 포함)
     */
    public boolean shareSpreadsheetWithServiceAccount(String spreadsheetId) {
        if (driveClient == null || spreadsheetId == null || spreadsheetId.isBlank()) {
            log.warn("[Drive] Drive 클라이언트가 초기화되지 않았거나 spreadsheetId가 비어있습니다.");
            return false;
        }
        if (serviceAccountEmail == null) {
            log.warn("[Drive] 서비스 계정 이메일을 확인할 수 없습니다.");
            return false;
        }

        try {
            Permission permission = new Permission()
                    .setType("user")
                    .setRole("reader")
                    .setEmailAddress(serviceAccountEmail);

            driveClient.permissions().create(spreadsheetId, permission)
                    .setSendNotificationEmail(false)
                    .execute();

            log.info("[Drive] 스프레드시트 공유 완료: spreadsheetId={}, email={}", spreadsheetId, serviceAccountEmail);
            return true;
        } catch (Exception e) {
            log.warn("[Drive] 스프레드시트 공유 실패: spreadsheetId={}, error={}", spreadsheetId, e.getMessage());
            log.warn("[Drive] 수동으로 {} 을(를) 스프레드시트에 뷰어로 추가해주세요.", serviceAccountEmail);
            return false;
        }
    }

    /**
     * 스프레드시트의 응답 수를 반환합니다 (A열 기준, 헤더 1행 제외).
     *
     * @return 응답 수 (≥ 0), 초기화 실패 또는 오류 시 -1
     */
    public int getResponseCount(String spreadsheetId) {
        if (sheetsClient == null || spreadsheetId == null || spreadsheetId.isBlank()) {
            return -1;
        }

        // 캐시 확인
        long[] cached = cache.get(spreadsheetId);
        if (cached != null && System.currentTimeMillis() < cached[1]) {
            log.debug("[Sheets] 캐시 히트: spreadsheetId={}, count={}", spreadsheetId, (int) cached[0]);
            return (int) cached[0];
        }

        try {
            ValueRange response = sheetsClient.spreadsheets().values()
                    .get(spreadsheetId, "A:A")
                    .execute();
            List<List<Object>> values = response.getValues();
            int count = (values == null || values.size() <= 1) ? 0 : values.size() - 1;
            cache.put(spreadsheetId, new long[]{count, System.currentTimeMillis() + CACHE_TTL_MS});
            log.info("[Sheets] 응답 수 조회 완료: spreadsheetId={}, count={}", spreadsheetId, count);
            return count;
        } catch (Exception e) {
            log.error("[Sheets] 응답 수 조회 실패: spreadsheetId={}, error={}", spreadsheetId, e.getMessage());
            return -1;
        }
    }

    /**
     * 스프레드시트의 특정 컬럼 평균 점수를 반환합니다 (소수점 1자리, 헤더명으로 컬럼 탐색).
     *
     * @return 평균 점수 (≥ 0), 컬럼 미발견 또는 오류 시 -1
     */
    public double getAverageScore(String spreadsheetId, String scoreColumnHeader) {
        if (sheetsClient == null
                || spreadsheetId == null || spreadsheetId.isBlank()
                || scoreColumnHeader == null || scoreColumnHeader.isBlank()) {
            return -1;
        }

        String cacheKey = spreadsheetId + "\0" + scoreColumnHeader;
        long[] cached = scoreCache.get(cacheKey);
        if (cached != null && System.currentTimeMillis() < cached[1]) {
            log.debug("[Sheets] 점수 캐시 히트: key={}, avg={}", cacheKey, cached[0] / 1000.0);
            return cached[0] / 1000.0;
        }

        try {
            // 1행에서 헤더 컬럼 인덱스 탐색
            ValueRange headerRow = sheetsClient.spreadsheets().values()
                    .get(spreadsheetId, "1:1").execute();
            List<List<Object>> headerValues = headerRow.getValues();
            if (headerValues == null || headerValues.isEmpty()) return -1;

            List<Object> headers = headerValues.get(0);
            int colIndex = -1;
            for (int i = 0; i < headers.size(); i++) {
                if (scoreColumnHeader.trim().equalsIgnoreCase(String.valueOf(headers.get(i)).trim())) {
                    colIndex = i;
                    break;
                }
            }
            if (colIndex < 0) {
                log.warn("[Sheets] 헤더 '{}' 를 찾을 수 없습니다: spreadsheetId={}", scoreColumnHeader, spreadsheetId);
                return -1;
            }

            // 해당 컬럼 전체 데이터 읽기 (2행부터)
            String colLetter = columnIndexToLetter(colIndex);
            ValueRange colData = sheetsClient.spreadsheets().values()
                    .get(spreadsheetId, colLetter + "2:" + colLetter).execute();
            List<List<Object>> rows = colData.getValues();
            if (rows == null || rows.isEmpty()) return 0;

            double sum = 0;
            int cnt = 0;
            for (List<Object> row : rows) {
                if (row.isEmpty()) continue;
                try {
                    sum += Double.parseDouble(String.valueOf(row.get(0)).trim());
                    cnt++;
                } catch (NumberFormatException ignored) {}
            }

            double avg = cnt > 0 ? Math.round(sum / cnt * 10.0) / 10.0 : 0;
            scoreCache.put(cacheKey, new long[]{Math.round(avg * 1000), System.currentTimeMillis() + CACHE_TTL_MS});
            log.info("[Sheets] 평균 점수 조회: spreadsheetId={}, col={}, avg={}", spreadsheetId, colLetter, avg);
            return avg;

        } catch (Exception e) {
            log.error("[Sheets] 평균 점수 조회 실패: spreadsheetId={}, error={}", spreadsheetId, e.getMessage());
            return -1;
        }
    }

    /** 특정 스프레드시트의 캐시를 강제 무효화합니다. */
    public void invalidateCache(String spreadsheetId) {
        cache.remove(spreadsheetId);
        scoreCache.entrySet().removeIf(e -> e.getKey().startsWith(spreadsheetId + "\0"));
    }

    /** 0-based 컬럼 인덱스를 Sheets 컬럼 문자로 변환합니다 (0→A, 26→AA 등). */
    private static String columnIndexToLetter(int colIndex) {
        StringBuilder sb = new StringBuilder();
        int n = colIndex + 1;
        while (n > 0) {
            n--;
            sb.insert(0, (char) ('A' + n % 26));
            n /= 26;
        }
        return sb.toString();
    }
}
