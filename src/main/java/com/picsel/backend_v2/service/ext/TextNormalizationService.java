package com.picsel.backend_v2.service.ext;

import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * 한국어 상품명 정규화 서비스.
 * Python의 Kiwi 기반 정규화를 Java regex로 재현.
 */
@Service
public class TextNormalizationService {

    private static final Pattern BRACKET_CONTENT   = Pattern.compile("[\\(\\[\\{][^)\\]\\}]{0,30}[)\\]\\}]");
    private static final Pattern SPECIAL_CHARS     = Pattern.compile("[^a-zA-Z0-9가-힣\\s\\-_./]");
    private static final Pattern MULTI_SPACE        = Pattern.compile("\\s+");
    private static final Pattern KR_EN_BOUNDARY    = Pattern.compile("([가-힣])([a-zA-Z0-9])");
    private static final Pattern EN_KR_BOUNDARY    = Pattern.compile("([a-zA-Z0-9])([가-힣])");
    private static final Pattern NOISE_WORDS_IT    = Pattern.compile(
            "\\b(시리즈|모델|타입|버전|정품|공식|국내|호환|최신|신형|구형|단품|세트)\\b");
    private static final Pattern NOISE_WORDS_PRICE = Pattern.compile(
            "\\b(무료배송|당일배송|로켓배송|배송료포함|배송비포함)\\b");

    /**
     * 검색용 정규화: 괄호 제거 → 특수문자 제거 → 한/영 경계 공백 삽입 → 노이즈 제거
     */
    public String normalize(String productName) {
        if (productName == null || productName.isBlank()) return "";

        String s = productName.trim();

        // 괄호 안 짧은 내용 제거 (단, M-chip 등 유의미한 표현 보존)
        s = BRACKET_CONTENT.matcher(s).replaceAll(" ");

        // 특수문자 제거
        s = SPECIAL_CHARS.matcher(s).replaceAll(" ");

        // 한글↔영문 경계에 공백 삽입
        s = KR_EN_BOUNDARY.matcher(s).replaceAll("$1 $2");
        s = EN_KR_BOUNDARY.matcher(s).replaceAll("$1 $2");

        // IT/가격 노이즈 제거
        s = NOISE_WORDS_IT.matcher(s).replaceAll(" ");
        s = NOISE_WORDS_PRICE.matcher(s).replaceAll(" ");

        // 공백 정리
        s = MULTI_SPACE.matcher(s).replaceAll(" ").trim();

        return s;
    }

    /**
     * 검색 후보 쿼리 생성 (브랜드+모델 조합 변형)
     */
    public String[] generateCandidates(String normalized) {
        // 기본 쿼리 그대로 + 앞 2단어만으로 축약 쿼리
        String[] tokens = normalized.split("\\s+");
        if (tokens.length <= 2) return new String[]{normalized};

        String shortened = tokens[0] + " " + tokens[1];
        return new String[]{normalized, shortened};
    }
}
