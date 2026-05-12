import java.util.ArrayList;
import java.util.List;

public class Node {
    private List<Node> parents = new ArrayList<>();
    private List<Node> children = new ArrayList<>();
    List<String> outcome=new ArrayList<>();
    List<List<rowCal>> tables=new ArrayList<>();
    private String nodeName;

    public List<List<rowCal>> getTables() {
        return tables;
    }

    public Node(String name) {
        nodeName=name;
    }
    public void addTable(List<rowCal> l){
        tables.add(l);
    }

    public void setParents(List<Node> parents) {
        this.parents = parents;
    }

    public void setChildren(List<Node> children) {
        this.children = children;
    }

    public void clearTables() {
        tables.clear();

    }

    // Getters
    public List<Node> getParents() {
        return parents;
    }

    public List<Node> getChildren() {
        return children;
    }

    public String getName() {
        return nodeName;
    }

    public void addSon(Node n){children.add(n);}
    public void addParent(Node n){parents.add(n);}

    public void addOutcome(String s) {
        outcome.add(s);
    }
    public String getOutcome(int i){
        return outcome.get(i);
    }
    public int getOutcomeSize(){
        return outcome.size();
    }

    public void printNodeDetails() {
        System.out.println("Node: " + nodeName);

        // הדפסת הורים
        System.out.print("Parents: ");
        for (Node p : parents) {
            System.out.print(p.getName() + " ");
        }
        System.out.println();

        // הדפסת ילדים
        System.out.print("Children: ");
        for (Node c : children) {
            System.out.print(c.getName() + " ");
        }
        System.out.println();

        // הדפסת Outcomes
        System.out.print("Outcomes: ");
        for (String o : outcome) {
            System.out.print(o + " ");
        }
        System.out.println();

        // הדפסת הטבלאות עם כותרות עמודות
        System.out.println("Tables:");
        if (tables == null || tables.isEmpty()) {
            System.out.println("No tables.");
            return;
        }

        for (int i = 0; i < tables.size(); i++) {
            System.out.println("Table " + (i + 1) + ":");
            List<rowCal> table = tables.get(i);

            if (!table.isEmpty()) {
                // הדפסת כותרות העמודות (שמות ההורים ולאחר מכן שם הצומת הנוכחי)
                rowCal firstRow = table.get(0);
                for (Node p : firstRow.given) {
                    System.out.print(p.getName() + "\t");
                }
                System.out.println(this.getName() + "\t| Prob");
                System.out.println("------------------------------------");

                // הדפסת נתוני השורות
                for (rowCal row : table) {
                    for (String val : row.values) {
                        System.out.print(val + "\t");
                    }
                    System.out.println("| " + row.prob);
                }
            }
        }
    }



}
