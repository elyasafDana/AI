import java.util.Arrays;

class rowCal {
    String[] values; // מערך שיכיל את הערכים (למשל: ["T", "F", "T"])
    double prob;     // ההסתברות שמופיעה ב-XML עבור השילוב הזה

    public rowCal(String[] values, double prob) {
        this.values = values;
        this.prob = prob;
    }

    // פונקציה להדפסה יפה של השורה (נוח מאוד לדיבאג)
    @Override
    public String toString() {
        return Arrays.toString(values) + " = " + prob;
    }
}