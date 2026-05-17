
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class Ex1 {
    static int multCount = 0;
    static int addCount = 0;

     static Map<String, Node> allNodes = new LinkedHashMap<>();

    public static void main(String[] args) throws ParserConfigurationException {
        //allNodesCopy = deepCopyNodes(allNodes);
        try {
            Scanner scanner = new Scanner(new File("input.txt"));

            if (scanner.hasNextLine()) {
                String xmlFileName = scanner.nextLine().trim();
                allNodes.clear();
                extractFromXML(xmlFileName);
            }

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) continue;

                multCount = 0;
                addCount = 0;

                if (line.equals("reverse")) {
                    writeToFile(solveReverse(),false);
                } else if (line.startsWith("P(")) {
                    writeToFile(solveQuery(line),true);
                } else {
                    writeToFile(solveBayesBall(line) ? "no" : "yes",true);
                }
            }
            scanner.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


     // Creates a deep copy of the Bayesian network nodes map,
    public static Map<String, Node> deepCopyNodes(Map<String, Node> original) {
        Map<String, Node> copyMap = new LinkedHashMap<>();
        for (Node n : original.values()) {
            Node newNode = new Node(n.getName());
            newNode.setOutcome(new ArrayList<>(n.outcome));
            copyMap.put(n.getName(), newNode);
        }
        for (Node originalNode : original.values()) {
            Node newNode = copyMap.get(originalNode.getName());
            for (Node p : originalNode.getParents()) {
                newNode.addParent(copyMap.get(p.getName()));
            }
            for (Node s : originalNode.getChildren()) {
                newNode.addSon(copyMap.get(s.getName()));
            }
            for (List<rowCal> table : originalNode.getTables()) {
                newNode.addTable(deepCopyTable(table));
            }
        }

        return copyMap;
    }


    //  Writes content into output.txt
    public static void writeToFile(String content, boolean ln) {
        try (FileWriter fw = new FileWriter("output.txt", true);
             PrintWriter out = new PrintWriter(fw)) {
            if (ln)out.println(content);
            else {
                out.print(content);
            }
        } catch (IOException e) {
            System.err.println("Error writing to output.txt: " + e.getMessage());
        }
    }


    //  Reverses the Bayesian network
    private static String solveReverse() {
        Map<String, Node> originalCopy = deepCopyNodes(allNodes);
        Map<String, Node> reversedCopy = deepCopyNodes(allNodes);
        List<List<rowCal>> allTables = new ArrayList<>();
        for (Node n : originalCopy.values()) {
            allTables.addAll(n.getTables());
        }
        List<rowCal> allJoinedTables = allTables.get(0);
        for (int i = 1; i < allTables.size(); i++) {
            allJoinedTables = joinTwoTables(allJoinedTables, allTables.get(i));
        }
        for (Node reversedNode : reversedCopy.values()) {
            Node originalNode = originalCopy.get(reversedNode.getName());
            List<Node> newParents = new ArrayList<>();
            for (Node child : originalNode.getChildren()) {
                newParents.add(reversedCopy.get(child.getName()));
            }
            List<Node> newChildren = new ArrayList<>();
            for (Node parent : originalNode.getParents()) {
                newChildren.add(reversedCopy.get(parent.getName()));
            }
            reversedNode.setParents(newParents);
            reversedNode.setChildren(newChildren);
            List<rowCal> cpt = caculateCPT(newParents, reversedNode, deepCopyTable(allJoinedTables), reversedCopy);
            reversedNode.clearTables();
            reversedNode.addTable(cpt);
        }

        return networkToXML(reversedCopy);
    }



    //  Converts a  network into XML format.
    public static String networkToXML(Map<String, Node> nodesMap) {
        StringBuilder xml = new StringBuilder();
        xml.append("<NETWORK>\n");

        for (Node node : nodesMap.values()) {
            xml.append("<VARIABLE>\n");
            xml.append("\t<NAME>").append(node.getName()).append("</NAME>\n");
            for (String outcome : node.outcome) {
                xml.append("\t<OUTCOME>").append(outcome).append("</OUTCOME>\n");
            }
            xml.append("</VARIABLE>\n\n");
        }
        List<Node> defs = new ArrayList<>(nodesMap.values());
        Collections.reverse(defs);
        defs.sort((n1, n2) -> {
            Node orig1 = allNodes.get(n1.getName());
            Node orig2 = allNodes.get(n2.getName());
            if (orig1 != null && orig2 != null && orig1.getChildren().isEmpty() && orig2.getChildren().isEmpty()) {
                return n1.getName().compareTo(n2.getName());
            }
            return 0;
        });
        for (Node node : defs) {
            xml.append("<DEFINITION>\n");
            xml.append("\t<FOR>").append(node.getName()).append("</FOR>\n");
            for (Node parent : node.getParents()) {
                xml.append("\t<GIVEN>").append(parent.getName()).append("</GIVEN>\n");
            }
            xml.append("\t<TABLE>");
            if (node.getTables() != null && !node.getTables().isEmpty()) {
                List<rowCal> cpt = node.getTables().get(0);
                for (int i = 0; i < cpt.size(); i++) {
                    xml.append(String.format("%.5f", cpt.get(i).getProb()));
                    if (i < cpt.size() - 1) {
                        xml.append(" ");
                    }
                }
            }
            xml.append("</TABLE>\n");
            if(defs.getLast().getName().equals(node.getName())){
                xml.append("</DEFINITION>\n");
                continue;
            }
            xml.append("</DEFINITION>\n\n");
        }

        xml.append("</NETWORK>");
        return xml.toString();
    }


    //  Creates a deep copy of a CPT table.
    private static List<rowCal> deepCopyTable(List<rowCal> table) {
        List<rowCal> copy = new ArrayList<>();
        for (rowCal r : table) {
            copy.add(new rowCal(r.getValues().clone(), r.getProb(), new ArrayList<>(r.getGiven())));
        }
        return copy;
    }

     // Calculates a new CPT for a node based on the full joint distribution.
private static List<rowCal> caculateCPT(List<Node> parents, Node currentNode, List<rowCal> allJoinedTables,Map<String, Node> allNodesCopy) {
    List<Node> familyNodes = new ArrayList<>(parents);
    familyNodes.add(currentNode);
    List<rowCal> familyJoint = eliminateToFamily(deepCopyTable(allJoinedTables), parents, currentNode,allNodesCopy);
    List<rowCal> parentsOnly = eliminate(deepCopyTable(familyJoint), currentNode.getName());
    List<rowCal> cptTable = new ArrayList<>();
    for (rowCal noMode : parentsOnly) {
        for (rowCal withNode : familyJoint) {
            if (isCompatible(noMode, withNode)) {
                double cptProb;
                if (noMode.getProb() == 0) {
                    cptProb = 0;
                } else {
                    cptProb = withNode.getProb() / noMode.getProb();
                }
                cptTable.add(new rowCal(withNode.getValues(), cptProb, new ArrayList<>(familyNodes)));
            }
        }
    }
    return cptTable;
}

     // Eliminates all irrelevant variables from a joint table,
    private static List<rowCal> eliminateToFamily(List<rowCal> table, List<Node> parents, Node current,Map<String, Node> allNodesCopy) {
        List<rowCal> result = table;
        for (Node node : allNodesCopy.values()) {
            String name = node.getName();
            if (!parents.contains(node) && !name.equals(current.getName())) {
                result = eliminate(result, name);
            }
        }
        return result;
    }



    // Solves a probabilistic inference query using Variable Elimination.
    public static String solveQuery(String query) {

        String queryPart = query.substring(query.indexOf("(") + 1, query.indexOf(")"));
        String hiddenPart = query.contains(" ") ? query.substring(query.indexOf(" ") + 1) : "";

        String[] splitQuery = queryPart.split("\\|");
        String queryVar = splitQuery[0].split("=")[0];
        String queryVal = splitQuery[0].split("=")[1];

        Map<String, String> evidence = new LinkedHashMap<>();

        if (splitQuery.length > 1) {
            String[] evs = splitQuery[1].split(",");
            for (int i = 0; i < evs.length; i++) {
                String[] pair = evs[i].split("=");
                evidence.put(pair[0].trim(), pair[1].trim());
            }
        }

        String[] hiddenOrder;
        if (hiddenPart.isEmpty()) {
            hiddenOrder = new String[0];
        }
        else {
            hiddenOrder = hiddenPart.split("-");
        }
        List<Node> releventNode = getRelevantNodes(queryVar, evidence);
        releventNode = getActiveNodes(queryVar, evidence, releventNode);
        List<List<rowCal>> allTablesOfReleventNodes = new ArrayList<>();
        for (Node node : releventNode) {
            allTablesOfReleventNodes.addAll(node.getTables());
        }

        List<List<rowCal>> minimizeTables = new ArrayList<>();
        for (List<rowCal> table : allTablesOfReleventNodes) {
            List<rowCal> filteredTable = filterByEvidence(table, evidence);
            if (filteredTable.isEmpty()) continue;

            boolean hasFreeVariables = false;
            for (Node n : filteredTable.get(0).getGiven()) {
                if (!evidence.containsKey(n.getName())) {
                    hasFreeVariables = true;
                    break;
                }
            }

            if (hasFreeVariables) {
                minimizeTables.add(filteredTable);
            }
        }
        for (String hiddenVar : hiddenOrder) {
            List<List<rowCal>> relevantTables = getReleventTables(minimizeTables, hiddenVar);
            if (relevantTables.isEmpty()) continue;
            relevantTables.sort((t1, t2) -> {
                int size1 = t1.size();
                int size2 = t2.size();
                if (size1 != size2) {
                    return Integer.compare(size1, size2);
                }
                int ascii1 = 0;
                int ascii2 = 0;
                for (Node n : t1.get(0).getGiven()) {
                    ascii1 += n.getName().charAt(0);
                }
                for (Node n : t2.get(0).getGiven()) {
                    ascii2 += n.getName().charAt(0);
                }
                return Integer.compare(ascii1, ascii2);
            });

            List<rowCal> joined = relevantTables.get(0);
            for (int i = 1; i < relevantTables.size(); i++) {
                joined = joinTwoTables(joined, relevantTables.get(i));
            }
            List<rowCal> eliminated =eliminate(joined, hiddenVar);
            minimizeTables.removeAll(relevantTables);
            minimizeTables.add(eliminated);
        }
        if (minimizeTables.isEmpty()) {
            return "0.00000,0,0";
        }
        List<rowCal> finalTable = minimizeTables.get(0);
        for (int i = 1; i < minimizeTables.size(); i++) {
            finalTable = joinTwoTables(finalTable, minimizeTables.get(i));
        }
        double sum = 0;
        boolean first = true;
        for (rowCal r : finalTable) {
            if (first) {
                sum = r.prob;
                first = false;
            } else {
                sum += r.prob;
                addCount++;
            }
        }
        for (rowCal r : finalTable) {
            r.prob /= sum;
        }
        double answer = 0;
        for (rowCal r : finalTable) {
            int idx = -1;
            for (int i = 0; i < r.getGiven().size(); i++) {
                if (r.getGiven().get(i).getName().equals(queryVar)) {
                    idx = i;
                    break;
                }
            }
            if (idx != -1 &&r.getValues()[idx].equals(queryVal)) {
                answer = r.getProb();
                break;
            }
        }

        return String.format("%.5f,%d,%d", answer, addCount, multCount);
    }


    //  Returns all tables that contain the given hidden variable.
    public static List<List<rowCal>> getReleventTables(List<List<rowCal>> allTables,String hiddenName){
        List<List<rowCal>> curentTable =new ArrayList<>();
        for (int i=0;i<allTables.size();i++){
            if (allTables.get(i).get(0).getGiven().contains(allNodes.get(hiddenName))){
                curentTable.add(allTables.get(i));
            }
        }
        return curentTable;

    }



     // Checks whether two CPT rows are compatible,
private static boolean isCompatible(rowCal r1, rowCal r2) {
    for (int i = 0; i < r1.getGiven().size(); i++) {
        Node n1 = r1.getGiven().get(i);

        for (int j = 0; j < r2.getGiven().size(); j++) {
            Node n2 = r2.getGiven().get(j);

            if (n1.getName().equals(n2.getName())) {
                if (!r1.getValues()[i].equals(r2.getValues()[j])) {
                    return false;
                }
            }
        }
    }
    return true;
}


    //  Eliminates a variable from a factor by summing matching rows.

    public static List<rowCal> eliminate(List<rowCal> table, String varName) {

        if (table == null || table.isEmpty()) {
            return table;
        }

        int indexToRemove = -1;

        List<Node> oldGiven = table.get(0).getGiven();

        for (int i = 0; i < oldGiven.size(); i++) {
            if (oldGiven.get(i).getName().equals(varName)) {
                indexToRemove = i;
                break;
            }
        }
        if (indexToRemove == -1) {
            return table;
        }

        List<Node> newGiven = new ArrayList<>(oldGiven);
        newGiven.remove(indexToRemove);
        Map<String, Double> tempMap = new LinkedHashMap<>();

        for (rowCal rc : table) {
            String[] oldVals = rc.getValues();
            String[] newVals =new String[oldVals.length - 1];
            int k = 0;
            for (int j = 0; j < oldVals.length; j++) {
                if (j == indexToRemove) continue;
                newVals[k++] = oldVals[j];
            }
            String key = String.join(" ", newVals);
            if (tempMap.containsKey(key)) {
                tempMap.put(key, tempMap.get(key) + rc.getProb());
                addCount++;
            } else {
                tempMap.put(key, rc.getProb());
            }
        }
        List<rowCal> finalTable = new ArrayList<>();
        for (Map.Entry<String, Double> entry : tempMap.entrySet()) {
            String[] values;
            if (entry.getKey().isEmpty()) {
                values = new String[0];
            } else {
                values = entry.getKey().split(" ");
            }
            finalTable.add(new rowCal(values, entry.getValue(), new ArrayList<>(newGiven)));
        }
        return finalTable;
    }


     // Joins two factor tables into a single factor table.
    public static List<rowCal> joinTwoTables(List<rowCal> table1, List<rowCal> table2) {


        List<rowCal> result = new ArrayList<>();
        if (table1.isEmpty() || table2.isEmpty()) return result;

        List<Node> newGiven = new ArrayList<>(table1.get(0).getGiven());
        for (Node n : table2.get(0).getGiven()) {
            boolean exists = false;
            for (Node existing : newGiven) {
                if (existing.getName().equals(n.getName())) { exists = true; break; }
            }
            if (!exists) newGiven.add(n);
        }

        for (rowCal r1 : table1) {
            for (rowCal r2 : table2) {
                if (isCompatible(r1, r2)) {

                    String[] newVals = new String[newGiven.size()];
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
                    multCount++;
                }
            }
        }

        return result;
    }


    //  Filters rows in a factor table according to evidence variables.

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





    //  Finds all relevant nodes for a query by collecting  the query node, evidence nodes, and their ancestors.
    public static List<Node> getRelevantNodes(String queryName, Map<String, String> evidence) {
        Set<Node> relevant = new HashSet<>();
        Queue<Node> queue = new LinkedList<>();

        if (allNodes.containsKey(queryName)) {
            queue.add(allNodes.get(queryName));
        }

        for (String evidenceName : evidence.keySet()) {
            if (allNodes.containsKey(evidenceName)) {
                queue.add(allNodes.get(evidenceName));
            }
        }
        while (!queue.isEmpty()) {
            Node current = queue.poll();

            if (current != null && !relevant.contains(current)) {
                relevant.add(current);

                for (Node parent : current.getParents()) {
                    queue.add(parent);
                }
            }
        }
        List<Node> relevantList = new ArrayList<>(relevant);
        return relevantList;
    }


      //Filters relevant nodes using the Bayes Ball algorithm
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



    //  Formats evidence into a Bayes Ball query string.

    private static String formatEvidenceForBayesBall(String queryVar, Map<String, String> evidence, Node current) {
        if (evidence == null || evidence.isEmpty()) return null;

        String s=queryVar+"-"+current.getName()+"|";
        for (Map.Entry<String, String> entry : evidence.entrySet()) {
            s=s+entry.getKey() +",";
        }
        s = s.substring(0, s.length() - 1);
        return s;
    }


     // Loads a Bayesian network from an XML file
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


    //  Generates a CPT table for a node
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



    //  Solves a conditional independence query using the Bayes Ball algorithm.
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
                return true;
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
        return false;
    }
}
