import it.unisa.dia.gas.jpbc.Element;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@Getter
@Setter
@ToString
public class SecretParameters {
    Element y;
    Element[] t;
    int n;
}
