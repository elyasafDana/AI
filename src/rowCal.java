import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class rowCal {
    String[] values; // מערך שיכיל את הערכים (למשל: ["T", "F", "T"])
    double prob;     // ההסתברות שמופיעה ב-XML עבור השילוב הזה
    List<Node> given=new ArrayList<>();


    public rowCal(String[] values, double prob,List<Node> depends) {
        this.values = values;
        this.prob = prob;
        given=depends;
    }

    // פונקציה להדפסה יפה של השורה (נוח מאוד לדיבאג)
    @Override
    public String toString() {
        return Arrays.toString(values) + " = " + prob;
    }

    public void setGiven(List<Node> given) {
        this.given = given;
    }

    public String[] getValues() {
        return values;
    }

    public List<Node> getGiven() {
        return given;
    }
}