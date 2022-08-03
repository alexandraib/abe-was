import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;

@EqualsAndHashCode
@Setter
@Getter
@AllArgsConstructor
public class Plaintext {
	BigInteger value;

	public Plaintext(String string){
		value=new BigInteger(string.getBytes());
	}

	@Override
	public String toString() {
		return new String(value.toByteArray());
	}
}
