import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class Transition {
    private int fromStateId;
    private int toStateId;
    private int transitionSymbol;

    public int getTransitionType() {
        return transitionSymbol;
    }
}
