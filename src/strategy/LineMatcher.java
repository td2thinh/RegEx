package strategy;

public interface LineMatcher {
    boolean matchLine(String line, boolean debugMode) throws Exception;

    void debug() throws Exception;
}
