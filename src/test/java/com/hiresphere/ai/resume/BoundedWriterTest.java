package com.hiresphere.ai.resume;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BoundedWriterTest {

    @Test
    void shouldKeepEverythingThatFitsAndNotBeCapped() throws Exception {
        BoundedWriter writer = new BoundedWriter(10);

        writer.write("hello");
        writer.write(" world".substring(0, 5));   // 5 + 5 = 10: bilkul limit par

        assertThat(writer.text()).isEqualTo("hello worl");
        assertThat(writer.capped()).isFalse();
    }

    @Test
    void shouldKeepTheTextThatFitsThenStopTheProducerWithAnException() throws Exception {
        BoundedWriter writer = new BoundedWriter(8);
        writer.write("abcde");

        assertThatThrownBy(() -> writer.write("fghijk")).isInstanceOf(BoundedWriter.LimitReached.class);

        assertThat(writer.text()).isEqualTo("abcdefgh");
        assertThat(writer.capped()).isTrue();
    }

    @Test
    void aWriteThatStartsAtTheLimitShouldAddNothingAndStop() throws Exception {
        BoundedWriter writer = new BoundedWriter(3);
        writer.write("abc");

        assertThatThrownBy(() -> writer.write("d")).isInstanceOf(BoundedWriter.LimitReached.class);

        assertThat(writer.text()).isEqualTo("abc");
        assertThat(writer.capped()).isTrue();
    }

    @Test
    void aCutInTheMiddleOfAnEmojiShouldNotLeaveABrokenHalf() throws Exception {
        BoundedWriter writer = new BoundedWriter(3);

        assertThatThrownBy(() -> writer.write("ab\uD83D\uDE00")).isInstanceOf(BoundedWriter.LimitReached.class);

        assertThat(writer.text()).isEqualTo("ab");   // emoji ka pehla aadha hissa hata diya
    }

    @Test
    void aLimitBelowOneIsAProgrammingError() {
        assertThatThrownBy(() -> new BoundedWriter(0)).isInstanceOf(IllegalArgumentException.class);
    }
}