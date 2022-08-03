import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;


@EqualsAndHashCode
@Setter
@Getter
@AllArgsConstructor
public class Attribute {
	BigInteger value;

	public Attribute(String string) {
		value = new BigInteger(string.getBytes());
	}

	public Attribute(int i) {
		value = BigInteger.valueOf(i);
	}

	@Override
	public String toString() {
		return new String(value.toByteArray());
	}
}
