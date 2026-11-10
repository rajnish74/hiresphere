package com.hiresphere.ai.resume;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class PiiRedactorTest {

    private static final String HEADER = """
            Asha Verma
            asha.verma@mailtest.com | +91 98765 43210 | linkedin.com/in/asha-verma | https://github.com/asha
            Skills: Java 21, Spring Boot, C++, PostgreSQL
            Acme Technologies, 2019 - 2023""";

    private String redact(String text) {
        return PiiRedactor.redact(text).text();
    }

    // ---------- emails ----------

    @Test
    void emailsShouldBeReplacedAndCounted() {
        PiiRedactor.Result result = PiiRedactor.redact("Mail asha.verma+jobs@mailtest.co.in or b@c.org today");

        assertThat(result.text()).isEqualTo("Mail [email] or [email] today");
        assertThat(result.emails()).isEqualTo(2);
    }

    @Test
    void anEmailAtTheEndOfASentenceShouldKeepTheFullStop() {
        assertThat(redact("Write to a@b.com.")).isEqualTo("Write to [email].");
    }

    @Test
    void handlesAndHostsWithoutADotAreNotEmails() {
        String text = "Follow @asha_dev or user@localhost";

        assertThat(redact(text)).isEqualTo(text);
    }

    // ---------- links ----------

    @Test
    void linksWithASchemeShouldBeReplacedWithoutEatingTheTrailingPunctuation() {
        PiiRedactor.Result result = PiiRedactor.redact("Portfolio: https://asha.dev/projects?id=5, and (see www.asha.dev).");

        assertThat(result.text()).isEqualTo("Portfolio: [link], and (see [link]).");
        assertThat(result.links()).isEqualTo(2);
    }

    @Test
    void knownProfileSitesShouldBeRecognisedEvenWithoutAScheme() {
        assertThat(redact("linkedin.com/in/asha-verma and github.com/asha/hiresphere."))
                .isEqualTo("[link] and [link].");
    }

    @Test
    void anOrdinaryDomainWithoutASchemeIsNotRecognisedAndThatIsAKnownLimit() {
        String text = "Visit asha.dev and acme.io/careers";

        assertThat(redact(text)).isEqualTo(text);
    }

    @Test
    void skillNamesThatLookLikeDomainsMustSurvive() {
        String text = "Node.js, Next.js, Vue.js, ASP.NET, .NET and Spring Boot";

        assertThat(redact(text)).isEqualTo(text);
    }

    // ---------- phones ----------

    @ParameterizedTest
    @ValueSource(strings = {"+91 98765 43210", "+91-9876543210", "+919876543210", "9876543210", "09876543210",
            "98765-43210", "98765 43210", "+1 (555) 123-4567", "(555) 123-4567", "555.123.4567", "555-123-4567",
            "+44 20 7946 0958", "123456789012"})
    void commonPhoneAndIdFormatsShouldBeReplaced(String number) {
        PiiRedactor.Result result = PiiRedactor.redact("Call " + number + " now");

        assertThat(result.text()).isEqualTo("Call [phone] now");
        assertThat(result.phones()).isEqualTo(1);
    }

    @Test
    void numbersInARealResumeThatAreNotPhonesMustNotBeTouched() {
        String text = """
                Acme Technologies, 2019 - 2023
                Jan 2020 - Mar 2022, CGPA 8.7/10, 99.9% uptime
                Java 21, Python 3.11, C++17, S3, ISO 27001, 10+ years, +2 years
                Pincode 800001, salary 1200000, 2018 2019 2020 2021""";

        PiiRedactor.Result result = PiiRedactor.redact(text);

        assertThat(result.text()).isEqualTo(text);
        assertThat(result.total()).isZero();
    }

    @Test
    void fullWidthDigitsAndHiddenCharactersMustNotHideAPhoneNumber() {
        PiiRedactor.Result result = PiiRedactor.redact("A: \uFF19\uFF18\uFF17\uFF16\uFF15\uFF14\uFF13\uFF12\uFF11\uFF10 B: 98765\u200B43210");

        assertThat(result.text()).isEqualTo("A: [phone] B: [phone]");
        assertThat(result.phones()).isEqualTo(2);
    }

    // ---------- poora resume ----------

    @Test
    void aTypicalHeaderShouldLoseItsContactDetailsButKeepSkillsAndDates() {
        PiiRedactor.Result result = PiiRedactor.redact(HEADER);

        assertThat(result.text()).isEqualTo("""
                Asha Verma
                [email] | [phone] | [link] | [link]
                Skills: Java 21, Spring Boot, C++, PostgreSQL
                Acme Technologies, 2019 - 2023""");
        assertThat(result.emails()).isEqualTo(1);
        assertThat(result.phones()).isEqualTo(1);
        assertThat(result.links()).isEqualTo(2);
        assertThat(result.total()).isEqualTo(4);
    }

    @Test
    void namesStayBecauseTheyCannotBeFoundReliablyAndThatIsAKnownLimit() {
        assertThat(redact(HEADER)).contains("Asha Verma");
    }

    @Test
    void redactingTwiceShouldChangeNothingTheSecondTime() {
        String once = redact(HEADER);
        PiiRedactor.Result twice = PiiRedactor.redact(once);

        assertThat(twice.text()).isEqualTo(once);
        assertThat(twice.total()).isZero();
    }

    @Test
    void aLinkContainingAnEmailOrNumberShouldBeOneLinkNotThreeThings() {
        PiiRedactor.Result result = PiiRedactor.redact("See https://example.com/u/asha@mail.com/9876543210 now");

        assertThat(result.text()).isEqualTo("See [link] now");
        assertThat(result.links()).isEqualTo(1);
        assertThat(result.emails()).isZero();
        assertThat(result.phones()).isZero();
    }

    // ---------- kinare ke cases ----------

    @Test
    void nullAndBlankInputShouldGiveAnEmptyResult() {
        assertThat(PiiRedactor.redact(null)).isEqualTo(new PiiRedactor.Result("", 0, 0, 0));
        assertThat(PiiRedactor.redact("  \n ").text()).isEmpty();
    }

    @Test
    void veryLongTokensWithoutSpacesShouldBeHandledQuickly() {
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            PiiRedactor.redact("a".repeat(30_000) + " @ " + "1".repeat(30_000));
            PiiRedactor.redact("a.".repeat(15_000));
            PiiRedactor.redact("a@".repeat(10_000));
            PiiRedactor.redact("http://".repeat(4_000));
            PiiRedactor.redact("1-".repeat(15_000));
        });
    }
}