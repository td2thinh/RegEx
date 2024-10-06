package tests;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import strategy.Uhlmann;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class UlhmanPerformanceTests {
    private static final String GUTENBERG_DIR = "testbed"; // Update this path
    private static final int MAX_BOOKS = 10; // Adjust as needed
    private static final int ITERATIONS = 100; // Number of times to repeat each test for averaging

    public static void main(String[] args) throws Exception {
        List<String> regexes = generateTestRegexes();
        testAutomatonConstruction(regexes);
        testTextSearch(regexes);
    }

    private static List<String> generateTestRegexes() {
        List<String> regexes = new ArrayList<>();

        // Simple word matches
        regexes.add("and");
        regexes.add("a");

        // Alternation
        regexes.add("S(a|g|r)*on");

        // complex patterns
        regexes.add("((t|s)h.*.*l)"); // Starts with t or s and ends with l , shall

        // Patterns likely to match in all books
        regexes.add("the|and|of|to|in|is|that|for|it|as");

        // Testing multiple wildcards
        regexes.add("a.*.*t");

        return regexes;
    }

    private static void testAutomatonConstruction(List<String> regexes) throws Exception {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        for (String regex : regexes) {
            double parseTime = 0, buildNFATime = 0, determinizeTime = 0, minimizeTime = 0;

            for (int i = 0; i < ITERATIONS; i++) {
                Uhlmann uhlmann = new Uhlmann(regex);

                long startTime = System.nanoTime();
                uhlmann.parseRegex();
                long endTime = System.nanoTime();
                parseTime += (endTime - startTime) / 1_000_000.0;

                startTime = System.nanoTime();
                uhlmann.buildNFA();
                endTime = System.nanoTime();
                buildNFATime += (endTime - startTime) / 1_000_000.0;

                startTime = System.nanoTime();
                uhlmann.determinize();
                endTime = System.nanoTime();
                determinizeTime += (endTime - startTime) / 1_000_000.0;

                startTime = System.nanoTime();
                uhlmann.minimize();
                endTime = System.nanoTime();
                minimizeTime += (endTime - startTime) / 1_000_000.0;
            }

            // Average the times
            parseTime /= ITERATIONS;
            buildNFATime /= ITERATIONS;
            determinizeTime /= ITERATIONS;
            minimizeTime /= ITERATIONS;

            // Add to dataset
            dataset.addValue(parseTime, "Parse", regex);
            dataset.addValue(buildNFATime, "Build NFA", regex);
            dataset.addValue(determinizeTime, "Determinize", regex);
            dataset.addValue(minimizeTime, "Minimize", regex);
        }

        createBarChart(dataset, "Automaton Construction Performance", "Regex", "Time (ms)", "automaton_construction.png");
    }

    private static void testTextSearch(List<String> regexes) throws Exception {
        XYSeriesCollection dataset = new XYSeriesCollection();

        File dir = new File(GUTENBERG_DIR);
        File[] books = dir.listFiles((d, name) -> name.endsWith(".txt"));

        if (books == null || books.length == 0) {
            throw new IOException("No text files found in the specified directory");
        }

        int bookCount = Math.min(books.length, MAX_BOOKS);

        for (String regex : regexes) {
            XYSeries series = new XYSeries("Search Time for regex: " + regex);
            Uhlmann matcher = new Uhlmann(regex);
            matcher.parseRegex();
            matcher.buildNFA();
            matcher.determinize();
            matcher.minimize();

            for (int i = 0; i < bookCount; i++) {
                String content = new String(Files.readAllBytes(Paths.get(books[i].getPath())));
                long totalTime = 0;

                for (int j = 0; j < ITERATIONS; j++) {
                    long startTime = System.nanoTime();
                    matcher.matchLine(content, false);
                    long endTime = System.nanoTime();
                    totalTime += (endTime - startTime);
                }

                double averageTime = totalTime / (double) ITERATIONS / 1_000_000.0; // Convert to milliseconds
                series.add(content.length(), averageTime);
            }

            dataset.addSeries(series);
        }

        createLineChart(dataset, "Text Search Performance", "Text Length (characters)", "Time (ms)", "text_search.png");
    }

    private static void createBarChart(DefaultCategoryDataset dataset, String title, String categoryAxisLabel, String valueAxisLabel, String filename) throws IOException {
        JFreeChart chart = ChartFactory.createStackedBarChart(
                title,
                categoryAxisLabel,
                valueAxisLabel,
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );
        ChartUtils.saveChartAsPNG(new File(filename), chart, 800, 600);
    }

    private static void createLineChart(XYSeriesCollection dataset, String title, String xAxisLabel, String yAxisLabel, String filename) throws IOException {
        JFreeChart chart = ChartFactory.createXYLineChart(title, xAxisLabel, yAxisLabel, dataset);
        ChartUtils.saveChartAsPNG(new File(filename), chart, 800, 600);
    }
}