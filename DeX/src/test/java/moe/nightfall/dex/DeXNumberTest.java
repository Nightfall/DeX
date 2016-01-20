package moe.nightfall.dex;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class DeXNumberTest {

	@Test
	public void testNumberParser() {
		
		// That's a 0
		assertThat(DeX.parseDeXNumber("0")).isEqualTo(0);
		assertThat(DeX.parseDeXNumber("-0")).isEqualTo(0);
		assertThat(DeX.parseDeXNumber("+0")).isEqualTo(0);
		
		// Special cases, Infinity and NaN
		assertThat(DeX.parseDeXNumber("NaN")).isNaN();
		assertThat(DeX.parseDeXNumber("+Infinity")).isEqualTo(Double.POSITIVE_INFINITY);
		assertThat(DeX.parseDeXNumber("-Infinity")).isEqualTo(Double.NEGATIVE_INFINITY);

		// Generic number without any flavor
		assertThat(DeX.parseDeXNumber("12345.6789E-10")).isEqualTo(12345.6789E-10);
		
		// Let's try some different radix
		assertThat(DeX.parseDeXNumber("0xABCDEF")).isEqualTo(0xABCDEF);
		assertThat(DeX.parseDeXNumber("0b110110001110001")).isEqualTo(0b110110001110001);
		assertThat(DeX.parseDeXNumber("0o777")).isEqualTo(0777);
	}
}
