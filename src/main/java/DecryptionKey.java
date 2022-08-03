import it.unisa.dia.gas.jpbc.Element;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@AllArgsConstructor
@Getter
@Setter
@ToString
public class DecryptionKey {
    private int n;
    private List<Element> a;
}
