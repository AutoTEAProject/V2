package com.autotea.backend.domain;

/**
 * 장치비를 계산할 때 어떤 값을 우선 쓸지: 직접 등록한 수식으로 계산한 값(FORMULA, 기본값)인지,
 * Aspen Plus가 자체적으로 계산해 낸 값(ASPEN)인지. ASPEN을 골랐는데 Aspen이 해당 장치의 값을
 * 제공하지 않으면(REPORT에 없으면) FORMULA로 자동 대체된다.
 */
public enum CostSource {
    FORMULA,
    ASPEN
}
