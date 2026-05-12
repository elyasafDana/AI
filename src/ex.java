
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.util.*;

public class ex {
    static int multCount = 0;
    static int addCount = 0;

     static Map<String, Node> allNodes = new HashMap<>();

    public static void main(String[] args) throws ParserConfigurationException {
        try {
            Scanner scanner = new Scanner(new File("input.txt")); // קריאה מהספרייה הנוכחית [cite: 103]

            if (scanner.hasNextLine()) {
                String xmlFileName = scanner.nextLine().trim();
                allNodes.clear(); // ליתר ביטחון
                extractFromXML(xmlFileName); // טעינה חד-פעמית של הרשת
            }

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) continue;

                // איפוס מונים לכל שאילתה [cite: 86]
                multCount = 0;
                addCount = 0;

                if (line.equals("reverse")) {
                    solveReverse(); // ללא שינוי הרשת המקורית [cite: 61]
                } else if (line.startsWith("P(")) {
                    System.out.println(solveQuery(line));
                } else {
                    System.out.println(solveBayesBall(line) ? "yes" : "no");
                }
            }
            scanner.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

//        extractFromXML("alarm_net.xml");
//        printNetwork();
//        System.out.println(solveBayesBall("B-E|"));
//        System.out.println(solveBayesBall("B-E|J"));
//        for (Node node : allNodes.values()) {
//            node.printNodeDetails();
//        }
//        solveQuery("P(J=T|B=T) A-E-M");


    }

    private static void solveReverse() {
        List<rowCal> allJoinedTables = new ArrayList<>();
        List<List<rowCal>> allTables = new ArrayList<>();
        for (Node n : allNodes.values()) {
            allTables.addAll(n.getTables());
        }

        // איחוד כל הטבלאות לקבלת ה-Joint Distribution המלאה
        allJoinedTables = allTables.get(0);
        for (int i = 1; i < allTables.size(); i++) {
            allJoinedTables = joinTwoTables(allJoinedTables, allTables.get(i), false);
        }
//        System.out.println("JOINED TABLE:");
//        printTable(allJoinedTables);


        for (Node node : allNodes.values()) {
            flipNode(node,allJoinedTables);
        }
        System.out.println("this is the revers");
         printNetwork();

    }

    private static void flipNode(Node node, List<rowCal> allJoinedTables) {
        List<List<rowCal>> allTables = new ArrayList<>();
        for (Node n : allNodes.values()) {
            allTables.addAll(n.getTables());
        }

        // איחוד כל הטבלאות לקבלת ה-Joint Distribution המלאה
        allJoinedTables = allTables.get(0);
        for (int i = 1; i < allTables.size(); i++) {
            allJoinedTables = joinTwoTables(allJoinedTables, allTables.get(i), false);
        }
        System.out.println("JOINED TABLE:");
        printTable(allJoinedTables);
        List<Node> newParents = new ArrayList<>(node.getChildren());
        List<Node> newChildren = new ArrayList<>(node.getParents());
        List<rowCal> newAllJoinedTables=new ArrayList<>(allJoinedTables);

        node.setParents(newParents);
        node.setChildren(newChildren);

         List<rowCal> cpt= caculateCPT( newParents, node,newAllJoinedTables);

         node.clearTables();
         node.addTable(cpt);


    }


    private static  List<rowCal> caculateCPT(List<Node> parents, Node currentNode,List<rowCal> allJoinedTables) {

        List<rowCal> allJoinedTablesTEMP=new ArrayList<>(allJoinedTables);
        List<Node> otherNodes=new ArrayList<>();
        for (Node node : allNodes.values()) {
            if (!parents.contains(node)&& !node.getName().equals(currentNode.getName())) otherNodes.add(node);
        }
        System.out.println("this is the other nodes:");
        for (Node n :otherNodes) System.out.println(n.getName()+ ",");

        for (Node nodeToEliminate:otherNodes){
            allJoinedTablesTEMP=eliminate(allJoinedTablesTEMP,nodeToEliminate.getName());
        }
        List<rowCal> allJoinedTablesWITHOUTCURRENTNODE=new ArrayList<>(allJoinedTablesTEMP);
        allJoinedTablesWITHOUTCURRENTNODE=eliminate(allJoinedTablesTEMP,currentNode.getName());

        List<rowCal> cptTable=new ArrayList<>();
        double cptProb=0;
        for (rowCal noMode : allJoinedTablesWITHOUTCURRENTNODE) {
            for (rowCal withNode : allJoinedTablesTEMP) {
                if (isCompatible(noMode, withNode)) {
                    if (noMode.getProb()==0){
                        cptProb=0;
                    }else {
                        cptProb=withNode.getProb() / noMode.getProb();
                    }
                    cptTable.add(new rowCal(withNode.getValues(), cptProb, withNode.getGiven()));
                }
            }
        }
        System.out.println("this is the new cpt for node:"+ currentNode.getName());
        printTable(cptTable);

        return cptTable;

    }


    public static void printNetwork() {
        System.out.println("--- Bayesian Network Structure ---");
        System.out.println(allNodes.size());


        // מעבר על כל הצמתים ב-Map
        for (Node node : allNodes.values()) {
            System.out.println("Node: " + node.getName());

            // הדפסת הורים
            System.out.print("  Parents: [");
            List<Node> parents = node.getParents();
            for (int i = 0; i < parents.size(); i++) {
                System.out.print(parents.get(i).getName() + (i < parents.size() - 1 ? ", " : ""));
            }
            System.out.println("]");

            // הדפסת ילדים
            System.out.print("  Children: [");
            List<Node> children = node.getChildren(); // בהנחה שקראת לזה getSons
            for (int i = 0; i < children.size(); i++) {
                System.out.print(children.get(i).getName() + (i < children.size() - 1 ? ", " : ""));
            }
            System.out.println("]");

            // הדפסת ערכים אפשריים (Outcomes)
            // System.out.println("  Outcomes: " + node.getOutcomes());
            System.out.println("CPT:");
            for (List<rowCal> t:node.tables){
                //printTable(t);
            }




            System.out.println("----------------------------------");
        }
    }


    public static String solveQuery(String query) {
        // 1. פירוק השאילתה למרכיבים
        // דוגמה: P(Q=q|E1=e1) H1-H2
        String queryPart = query.substring(query.indexOf("(") + 1, query.indexOf(")")); // Q=q|E1=e1
        String hiddenPart = query.contains(" ") ? query.substring(query.indexOf(" ") + 1) : ""; // H1-H2

        String[] splitQuery = queryPart.split("\\|");
        String queryVar = splitQuery[0].split("=")[0];
        String queryVal = splitQuery[0].split("=")[1];

        Map<String, String> evidence = new HashMap<>();
        if (splitQuery.length > 1) {
            String[] evs = splitQuery[1].split(",");
            for (int i=0;i<evs.length;i++){
                String[] pair = evs[i].split("=");
                evidence.put(pair[0].trim(), pair[1].trim());

            }
//            for (String e : evs) {
//                String[] pair = e.split("=");
//                evidence.put(pair[0].trim(), pair[1].trim());
//            }
        }

        String[] hiddenOrder = hiddenPart.isEmpty() ? new String[0] : hiddenPart.split("-");

        // 2. איסוף הטבלאות הרלוונטיות
        // אנחנו צריכים רק טבלאות של צמתים שהם אבות של ה-Query או ה-Evidence
        List<Node> releventNode = new ArrayList<>(allNodes.values());
        releventNode=getRelevantNodes(queryVar,evidence);
        releventNode=getActiveNodes(queryVar,evidence,releventNode);
//        System.out.println("THIS IS THE RELEVENT NODES AFTER ALL ");
//        for (Node n:releventNode){
//            n.printNodeDetails();
//        }
        for (int i=0;i< allNodes.size();i++) {
            allNodes.get(i);
        }


        //עד פה זה ההורדת NODE עכשיו מתחילים להתעסק עם הטבלאות שלהם
        List<List<rowCal>> allTablesOfReleventNodes=new ArrayList<>();
        for (Node node : releventNode) {
            allTablesOfReleventNodes.addAll(node.getTables());
        }
        List<rowCal> finalTable=new ArrayList<>();
        List<List<rowCal>> MinimizeTables = new ArrayList<>();
        for (List<rowCal> table: allTablesOfReleventNodes){
            List<rowCal> filteredTable = filterByEvidence(table, evidence);
            MinimizeTables.add(filteredTable);
        }
        List<List<rowCal>> hidenTables=new ArrayList<>();
        for (String hiddenVar : hiddenOrder) {

            // כל הטבלאות שמכילות את המשתנה
            List<List<rowCal>> relevantTables =
                    getReleventTables(MinimizeTables, hiddenVar);

            if (relevantTables.isEmpty()) {
                continue;
            }

            // JOIN לכל הטבלאות
            List<rowCal> joined = relevantTables.get(0);

            for (int i = 1; i < relevantTables.size(); i++) {
                joined = joinTwoTables(joined, relevantTables.get(i),true);
            }

            // ELIMINATE
            List<rowCal> eliminated = eliminate(joined, hiddenVar);

            // מוחקים את כל הטבלאות הישנות
            MinimizeTables.removeAll(relevantTables);

            // מוסיפים את הטבלה החדשה
            MinimizeTables.add(eliminated);

        }
        if (MinimizeTables.isEmpty()) return "0.00000";

        finalTable = MinimizeTables.get(0);
        for (int i = 1; i < MinimizeTables.size(); i++) {
            finalTable = joinTwoTables(finalTable, MinimizeTables.get(i),false);
        }

//        System.out.println("-------this is the tables after all the hidden and eliminate-----------");
//        printTable(finalTable);


        // 1. נרמול - הופך את 0.00085 ל-0.85
        double sum = 0;
        for (rowCal r : finalTable){
            sum += r.prob;
            addCount++;
        }
        for (rowCal r : finalTable) r.prob /= sum;

//        System.out.println("------- Final Normalized Table -------");
//        printTable(finalTable);
        System.out.println("Multiply operations: " + multCount);
        System.out.println("Add operations: " + addCount);

        // 2. שליפת התוצאה עבור J=T
        // כאן תכתוב לוגיקה שסורקת את finalTable ומחזירה את ה-prob של J=T

        return "";
    }


    public static List<List<rowCal>> getReleventTables(List<List<rowCal>> allTables,String hiddenName){
        List<List<rowCal>> curentTable =new ArrayList<>();
        for (int i=0;i<allTables.size();i++){

            if (allTables.get(i).get(0).getGiven().contains(allNodes.get(hiddenName))){
                curentTable.add(allTables.get(i));
            }
        }
        return curentTable;

    }

    public static void printTable(List<rowCal> table) {
        if (table == null || table.isEmpty()) {
            System.out.println("The table is empty.");
            return;
        }

        // 1. הדפסת שורת הכותרת (שמות הקודקודים)
        List<Node> nodes = table.get(0).getGiven();
        for (Node node : nodes) {
            System.out.print(node.getName() + "\t");
        }
        System.out.println("| Prob");

        System.out.println("------------------------------------");

        // 2. מעבר על כל שורה והדפסת הערכים וההסתברות
        for (rowCal row : table) {
            String[] values = row.getValues();
            for (String val : values) {
                System.out.print(val + "\t");
            }
            // הדפסת ההסתברות עם פורמט של 5 ספרות אחרי הנקודה (נוח לדיבאג)
            System.out.printf("| %.5f\n", row.prob);
        }
        System.out.println();
    }



private static boolean isCompatible(rowCal r1, rowCal r2) {
    // עוברים על רשימת הקודקודים של שורה 1
    for (int i = 0; i < r1.getGiven().size(); i++) {
        Node n1 = r1.getGiven().get(i);

        // מחפשים קודקוד עם אותו שם בשורה 2
        for (int j = 0; j < r2.getGiven().size(); j++) {
            Node n2 = r2.getGiven().get(j);

            // אם השמות זהים - זה אותו משתנה! בודקים אם הערכים שלו זהים
            if (n1.getName().equals(n2.getName())) {
                if (!r1.getValues()[i].equals(r2.getValues()[j])) {
                    return false; // סתירה בערכים (T מול F)
                }
            }
        }
    }
    return true;
}
    public static List<rowCal> eliminate(List<rowCal> table, String varName) {
        if (table == null || table.isEmpty()) return table;

        // 1. מציאת האינדקס של המשתנה שאנחנו רוצים להעלים בתוך ה-given
        int indexToRemove = -1;
        List<Node> Given = table.get(0).getGiven();
        //List<Node> newGiven = new ArrayList<>();

        for (int i = 0; i < Given.size(); i++) {
            if (Given.get(i).getName().equals(varName)) {
                indexToRemove = i;
            }
        }

        // אם המשתנה בכלל לא בטבלה, אין מה להעלים
        if (indexToRemove == -1) return table;

        //for (int i=0;i<table.size();i++){
            rowCal rc=table.get(0);
            rc.getGiven().remove(indexToRemove);
            int count=0;
        for (int i=0;i<table.size();i++){
            rc=table.get(i);
            String[] arr=new String[rc.getValues().length-1];
            for (int j=0;j<= arr.length;j++){
                if (j==indexToRemove) continue;
                else {
                    arr[count]=rc.getValues()[j];
                    count++;
                }
            }
            table.get(i).setValues(arr);
            count=0;
        }
        table.size();
        Map<String, Double> tempMap=new HashMap<>();
        for (int i=0;i<table.size();i++){
            rowCal r=table.get(i);
            String key=String.join(" ", r.getValues());
            if (tempMap.containsKey(key)){
                double value=tempMap.get(key);
                tempMap.put(key,value+table.get(i).getProb());
                addCount++;
            }else {
                tempMap.put(String.join(" ", r.getValues()),r.getProb());
            }

        }
        List<rowCal> finalTable = new ArrayList<>();
        for (Map.Entry<String, Double> entry : tempMap.entrySet()) {
            // הופכים את המפתח (הסטרינג) חזרה למערך של ערכים
            String[] values = entry.getKey().split(" ");
            double prob = entry.getValue();

            // מוסיפים לטבלה הסופית עם ה-given המעודכן
            finalTable.add(new rowCal(values, prob, table.get(0).getGiven()));
        }
        return finalTable;
    }

    public static List<rowCal> joinTwoTables(List<rowCal> table1, List<rowCal> table2,boolean b) {
//        System.out.println("we going to do join to");
//        System.out.println("table1:");
//        printTable(table1);
//        System.out.println("table2:");
//        printTable(table2);

        List<rowCal> result = new ArrayList<>();
        if (table1.isEmpty() || table2.isEmpty()) return result;

        // א. יצירת רשימת ה-Nodes של הטבלה החדשה (איחוד בלי כפילויות)
        List<Node> newGiven = new ArrayList<>(table1.get(0).getGiven());
        for (Node n : table2.get(0).getGiven()) {
            boolean exists = false;
            for (Node existing : newGiven) {
                if (existing.getName().equals(n.getName())) { exists = true; break; }
            }
            if (!exists) newGiven.add(n);
        }

        // ב. לולאה כפולה על השורות
        for (rowCal r1 : table1) {
            for (rowCal r2 : table2) {
                if (isCompatible(r1, r2)) {

                    String[] newVals = new String[newGiven.size()];
                    // מילוי ערכים לשורה החדשה לפי הסדר של newGiven
                    for (int i=0;i< newGiven.size();i++){
                      int index=  r2.getGiven().indexOf(newGiven.get(i));
                      if (index<0){
                          index=r1.getGiven().indexOf(newGiven.get(i));
                          newVals[i]=r1.getValues()[index];
                      }else{
                          newVals[i]=r2.getValues()[index];
                      }
                    }

                    result.add(new rowCal(newVals, r1.prob * r2.prob, newGiven));
                    if (b)multCount++;
                }
            }
        }
//        System.out.println("this is the result");
//        printTable(result);
        return result;
    }
    public static List<rowCal> filterByEvidence(List<rowCal> table, Map<String, String> evidence) {
        List<rowCal> tempTable = new ArrayList<>();

        for (int i = 0; i < table.size(); i++) {

            rowCal row = table.get(i);
            String[] values = row.getValues();
            List<Node> given = row.getGiven();

            boolean valid = true;

            for (int j = 0; j < given.size(); j++) {
                Node node = given.get(j);
                if (evidence.containsKey(node.getName())) {
                    if (!evidence.get(node.getName()).equals(values[j])) {
                        valid = false;
                        break;
                    }
                }
            }
            if (valid) {
                tempTable.add(row);
            }
        }
        return tempTable;
    }




    public static List<Node> getRelevantNodes(String queryName, Map<String, String> evidence) {
        Set<Node> relevant = new HashSet<>();
        Queue<Node> queue = new LinkedList<>();

        // 1. הוספת ה-Query לתור
        if (allNodes.containsKey(queryName)) {
            queue.add(allNodes.get(queryName));
        }

        // 2. הוספת כל ה-Evidence מתוך ה-Map לתור
        for (String evidenceName : evidence.keySet()) {
            if (allNodes.containsKey(evidenceName)) {
                queue.add(allNodes.get(evidenceName));
            }
        }

        // 3. סריקה למעלה (BFS)
        while (!queue.isEmpty()) {
            Node current = queue.poll();

            // אם הצומת קיים ולא ביקרנו בו עדיין
            if (current != null && !relevant.contains(current)) {
                relevant.add(current); // נשמר ב-Set (בלי כפילויות!)

                // מוסיפים את ההורים שלו לסריקה
                for (Node parent : current.getParents()) {
                    queue.add(parent);
                }
            }
        }
        List<Node> relevantList = new ArrayList<>(relevant);
        return relevantList;
    }

    public static List<Node> getActiveNodes(String queryVar, Map<String, String> evidence, List<Node> releventWithAnserstors) {
        List<Node> finalRelevent = new ArrayList<>();

        for (Node n: releventWithAnserstors){
            String queryStr = formatEvidenceForBayesBall(queryVar,evidence,n);
            if (solveBayesBall(queryStr)) {
                finalRelevent.add(n);
            }
        }
        return finalRelevent;

    }

    // פונקציית עזר להמרת ה-Map למחרוזת
    private static String formatEvidenceForBayesBall(String queryVar, Map<String, String> evidence, Node current) {
        if (evidence == null || evidence.isEmpty()) return null;

        String s=queryVar+"-"+current.getName()+"|";
        for (Map.Entry<String, String> entry : evidence.entrySet()) {
            s=s+entry.getKey() +",";
        }
        s = s.substring(0, s.length() - 1);
        return s;
    }

     public static void extractFromXML(String name) throws ParserConfigurationException {
         File file = new File(name);
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            Document doc = builder.parse(file);

            NodeList variables = doc.getElementsByTagName("VARIABLE");
            NodeList def = doc.getElementsByTagName("DEFINITION");



            Node sonNode=null;
            List<Node> givenList=new ArrayList<>();
            for (int i=0;i< variables.getLength();i++){
                Element e=(Element)variables.item(i);
                String varName=e.getElementsByTagName("NAME").item(0).getTextContent();
                Node myNode=new Node(varName);
                NodeList outcomes = e.getElementsByTagName("OUTCOME");
                for (int j = 0; j < outcomes.getLength(); j++) {
                    String outcomeValue = outcomes.item(j).getTextContent();
                    myNode.addOutcome(outcomeValue);
                }
                allNodes.put(varName,myNode);
            }

            for (int i=0;i< def.getLength();i++){
                Element e=(Element)def.item(i);
                String sonName=e.getElementsByTagName("FOR").item(0).getTextContent();
                sonNode=allNodes.get(sonName);
                NodeList given = e.getElementsByTagName("GIVEN");


                for (int j=0;j< given.getLength();j++){
                    String parentName=given.item(j).getTextContent();
                    Node ParentNode =allNodes.get(parentName);
                    ParentNode.addSon(allNodes.get(sonName));
                    givenList.add(allNodes.get(parentName));
                    sonNode.addParent(ParentNode);
                }
//
            }
            for (int i=0;i<def.getLength();i++){
                Element e=(Element)def.item(i);
                sonNode=allNodes.get(e.getElementsByTagName("FOR").item(0).getTextContent());
                String tableContent = e.getElementsByTagName("TABLE").item(0).getTextContent();
                String[] probArray = tableContent.trim().split("\\s+");
                double[] probArrayDouble=new double[probArray.length];
                for (int k=0;k< probArray.length;k++){
                    probArrayDouble[k]=Double.parseDouble(probArray[k]);
                }
                  generateTable(sonNode.getParents(),sonNode,probArrayDouble);
            }

        }
        catch (Exception e){
            System.out.println("error"+e.getMessage());

        }

     }

    public static void generateTable(List<Node> depends, Node mainNode, double[] probabilities) {
        Node m = mainNode;
        List<Node> allParticipants = new ArrayList<>(depends);
        allParticipants.add(m);
        List<rowCal> myTable = new ArrayList<>();

        for (int line = 0; line < probabilities.length; line++) {
            String[] outcomeName = new String[allParticipants.size()];
            int tempLine = line;
            for (int col = allParticipants.size() - 1; col >= 0; col--) {
                Node currentNode = allParticipants.get(col);
                int outcomeSize = currentNode.getOutcomeSize();
                outcomeName[col] = currentNode.getOutcome(tempLine % outcomeSize);
                tempLine = tempLine / outcomeSize;
            }
            myTable.add(new rowCal(outcomeName, probabilities[line],new ArrayList<>(allParticipants)));
        }
        mainNode.addTable(myTable);
    }

    private static boolean solveBayesBall(String query) {

        Set<String> marked = new HashSet<>();

        String[] parts = query.split("\\|");
        String[] targets = parts[0].split("-");

        if (parts.length > 1) {
            String[] temp= parts[1].split(",");
            for (int i=0;i<temp.length;i++) {
                marked.add(temp[i].split("=")[0].trim());
            }
        }

        //Node startNode = allNodes.get(targets[0].trim());
        Node endNode = allNodes.get(targets[1].trim());


        Queue<queuNode> queue = new LinkedList<>();
        Set<queuNode> visited = new HashSet<>();

        queuNode startqueuNode = new queuNode(allNodes.get(targets[0].trim()), false);
        queue.add(startqueuNode);

        while (!queue.isEmpty()) {
            queuNode current = queue.poll();
            if (visited.contains(current)) continue;
            visited.add(current);

            if (current.getNode().getName().equals(endNode.getName()) ){
                return true; // נמצא מסלול פעיל[cite: 6]
            }

            boolean isMarked = marked.contains(current.getNode().getName());

            List<Node> parentList=current.getNode().getParents();
            List<Node> childList=current.getNode().getChildren();

            if (isMarked){
                if (current.fromParent){// from up
                    for (int i=0;i<parentList.size();i++) {
                        queue.add(new queuNode(parentList.get(i), false));
                    }
                }else{ //from down

                     }
            }else{ //not marked
                if (current.fromParent){//from up
                    for (int i=0;i<childList.size();i++) {
                        queue.add(new queuNode(childList.get(i), true));
                    }
                }else{//from down
                    for (int i=0;i<parentList.size();i++) {
                        queue.add(new queuNode(parentList.get(i), false));
                    }
                    for (int i=0;i<childList.size();i++) {
                        queue.add(new queuNode(childList.get(i), true));
                    }
                }
            }
        }
        return false; // המשתנים בלתי תלויים[cite: 6]
    }


}
