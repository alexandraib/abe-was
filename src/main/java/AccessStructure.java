import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@AllArgsConstructor
@Getter
@Setter
@ToString
public class AccessStructure {
    private int n, t;
    private List<Integer> compartmentThreshold;
    private List<List<Attribute>> compartments;
}
