
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.sql.ClientInfoStatus;
import java.util.*;

public class ex {

     static Map<String, Node> allNodes = new HashMap<>();

    public static void main(String[] args) throws ParserConfigurationException {
        extractFromXML("src/alarm_net.xml");
        printNetwork();
        System.out.println(solveBayesBall("B-E|"));
        System.out.println(solveBayesBall("B-E|J"));
        for (Node node : allNodes.values()) {
            node.printNodeDetails();
        }
        solveQuery("P(J=T|B=T) A-E-M");


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

            System.out.println("----------------------------------");
        }
    }

    public List<rowCal> buildTableFromXML(String xmlContent, List<String> parentNames, String myName) {
        String[] probs = xmlContent.trim().split("\\s+");
        List<String> allVars = new ArrayList<>(parentNames);
        allVars.add(myName); // סדר המשתנים: הורים ואז הילד

        int numVars = allVars.size();
        List<rowCal> table = new ArrayList<>();

        for (int i = 0; i < probs.length; i++) {
            String[] config = new String[numVars];
            int temp = i;
            // המרה של אינדקס i לקומבינציית T/F (מונה בינארי)
            for (int j = numVars - 1; j >= 0; j--) {
                config[j] = (temp % 2 == 0) ? "T" : "F";
                temp /= 2;
            }
           // table.add(new Row(config, Double.parseDouble(probs[i])));
        }
        return table;
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
        System.out.println("THIS IS THE RELEVENT NODES AFTER ALL ");
        for (Node n:releventNode){
            n.printNodeDetails();
        }


        //עד פה זה ההורדת NODE עכשיו מתחילים להתעסק עם הטבלאות שלהם
        List<List<rowCal>> allTables=new ArrayList<>();
        for (Node node : allNodes.values()) {
            allTables.addAll(node.getTables());
        }
        return "";
    }
    public static List<rowCal> filterByEvidence(List<rowCal> table, Map<Node, String> evidence) {
        List<rowCal> tempTable = new ArrayList<>();

        for (int i = 0; i < table.size(); i++) {

            rowCal row = table.get(i);
            String[] values = row.getValues();
            List<Node> given = row.getGiven();

            boolean valid = true;

            for (int j = 0; j < given.size(); j++) {
                Node node = given.get(j);
                if (evidence.containsKey(node)) {
                    if (!evidence.get(node).equals(values[j])) {
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






    public static List<Node> findCommon(List<Node> l1, List<Node> l2){

        List<Node> common = new ArrayList<>();

        for(Node n1 : l1){

            if(l2.contains(n1)){
                common.add(n1);
            }
        }

        return common;
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



//        List<String> queryList=new ArrayList<>();
//        for (Node n: releventWithAnserstors){
//            String ev="";
//            for (Map.Entry<String, String> entry : evidence.entrySet()) {
//                ev=ev+entry.getKey() +",";
//            }
//            ev = ev.substring(0, ev.length() - 1);
//           queryList.add(queryVar+"-"+n.getName()+"|"+ev);
//        }
//        return queryList;
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
            myTable.add(new rowCal(outcomeName, probabilities[line],depends));
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
