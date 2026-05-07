
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
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
//         catch (IOException e) {
//            throw new RuntimeException(e);
//        } catch (SAXException e) {
//            throw new RuntimeException(e);
//        }

     }

    public static void generateTable(List<Node> depends, Node mainNode, double[] probabilities) {
        Node m=mainNode;
        List<Node> allParticipants = new ArrayList<>(depends);
        allParticipants.add(m);
        List<rowCal> myTable=new ArrayList<>();
        // String[] outcomeName=new String[allParticipants.size()];
        for (int line=0;line<probabilities.length;line++){
            String[] outcomeName=new String[allParticipants.size()];
            for (int col=0;col<allParticipants.size();col++){
                outcomeName[col]=allParticipants.get(col).getOutcome(line% m.getOutcomeSize());
            }
            myTable.add(new rowCal(outcomeName,probabilities[line]));
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
