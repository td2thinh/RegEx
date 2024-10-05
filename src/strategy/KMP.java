package strategy;

import java.util.HashMap;
import java.util.Map;

public class KMP implements LineMatcher {
    private final Map<String, int[]> carryOver;
    private String pattern;


    public KMP(String pattern) {
        this.carryOver = new HashMap<>();
        this.pattern = pattern;
        this.computeCarryOver(pattern);
    }

    // Compute the longest proper suffix which is also a prefix
    // Optimized version of BMBX
    private int[] longestPrefixSuffix(String pattern) {
        int n = pattern.length();
        int[] lps = new int[n + 1];
        lps[0] = -1;
        lps[1] = 0;
        int i = 1;
        while (i < n) {
            int j = lps[i];
            while (j != -1 && pattern.charAt(i) != pattern.charAt(j)) {
                j = lps[j];
            }
            lps[i + 1] = j + 1;
            i++;
        }
        return lps;
    }

    // BMBX algorithm for strategy.KMP
    private void computeCarryOver(String pattern) {
        int[] lps = longestPrefixSuffix(pattern);
        int n = pattern.length();
        int i = 1;
        while (i < n) {
            if (pattern.charAt(i) == pattern.charAt(lps[i])) {
                if (lps[lps[i]] == -1) {
                    lps[i] = -1;
                } else {
                    lps[i] = lps[lps[i]];
                }
            }
            i++;
        }
        this.carryOver.put(pattern, lps);
    }

    public void storePattern(String pattern) {
        this.pattern = pattern;
        if (!this.carryOver.containsKey(pattern)) {
            computeCarryOver(pattern);
        }
    }

    @Override
    public boolean matchLine(String line, boolean debugMode) throws Exception {
        int n = line.length();
        int m = pattern.length();
        int i = 0;
        int j = 0;
        while (i < n) {
            if (line.charAt(i) == pattern.charAt(j)) {
                i++;
                j++;
                if (j == m) {
                    return true;
                }
            } else {
                if (j == 0) {
                    i++;
                } else {
                    j = this.carryOver.get(pattern)[j];
                }
            }
        }
        return false;
    }


    public static void main(String[] args) throws Exception {
        String text = "abxabcabcaby";
        String pattern = "abcaby";
        KMP kmp = new KMP(pattern);
        System.out.println(kmp.matchLine(text, false));
    }


    @Override
    public void debug() throws Exception {
        System.out.println("KMP algorithm");
    }
}
