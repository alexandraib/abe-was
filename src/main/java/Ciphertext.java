import it.unisa.dia.gas.jpbc.Element;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@Getter
@Setter
@ToString
public class Ciphertext {
	private Element Ep, gs;
	private Element[] E;
	private Attribute[] attributes;
}
