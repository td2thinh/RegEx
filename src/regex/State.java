package regex;

import lombok.*;

import java.util.ArrayList;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class State {
    private int stateId;
    private boolean isFinalState;
    private boolean isStartState;
    private ArrayList<Transition> transitions;
    private ArrayList<Transition> epsilonTransitions;

    public State(int stateId, boolean isFinalState, boolean isStartState) {
        this.stateId = stateId;
        this.isFinalState = isFinalState;
        this.isStartState = isStartState;
        this.transitions = new ArrayList<Transition>();
        this.epsilonTransitions = new ArrayList<Transition>();
    }

    public void addTransition(Transition transition) {
        transitions.add(transition);
        if (transition.getTransitionType() == Automaton.EPSILON) {
            epsilonTransitions.add(transition);
        }
    }

    public int getTransition(int symbol) {
        for (Transition transition : transitions) {
            if (transition.getTransitionType() == symbol) {
                return transition.getToStateId();
            }
        }
        return -1;
    }


}
