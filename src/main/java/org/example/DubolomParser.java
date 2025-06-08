package org.example;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.File;
import java.util.*;

public class DubolomParser {

    public static Map<String, String> parseDubolom(String xmlFilePath) {
        Map<String, String> encumbrancesCodes = new HashMap<>();
        try {
            File xmlFile = new File(xmlFilePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dbFactory.setNamespaceAware(true);
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            NodeList restrictRecords = doc.getElementsByTagName("restrict_record");

            for (int i = 0; i < restrictRecords.getLength(); i++) {
                Element restrictRecord = (Element) restrictRecords.item(i);

                String code = getTagValue(restrictRecord);
                if (code == null || code.isEmpty()) continue;

                String name = tryGetNameFromVariousPaths(restrictRecord);
                if (name != null && !name.isEmpty()) {
                    encumbrancesCodes.put(name, code);
                }

                String[] periodPath = {"restrictions_encumbrances_data", "period", "period_info"};
                Element periodInfo = traverseToElement(restrictRecord, periodPath);
                if (periodInfo != null) {
                    NodeList children = periodInfo.getChildNodes();
                    for (int j = 0; j < children.getLength(); j++) {
                        Node node = children.item(j);
                        if (node.getNodeType() == Node.ELEMENT_NODE) {
                            String tag = node.getNodeName();
                            if ("start_date".equals(tag) || "end_date".equals(tag)) {
                                String dateValue = node.getTextContent().trim();
                                if (!dateValue.isEmpty()) {
                                    encumbrancesCodes.put(dateValue, code);
                                }
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("Error parsing Dubolom: " + e.getMessage());
        }
        return encumbrancesCodes;
    }

    public static List<List<String>> parseOwners(String xmlFilePath) {
        List<List<String>> owners = new ArrayList<>();
        try {
            File xmlFile = new File(xmlFilePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dbFactory.setNamespaceAware(true);
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            NodeList rightHolders = doc.getElementsByTagName("right_holder");

            for (int i = 0; i < rightHolders.getLength(); i++) {
                Element holder = (Element) rightHolders.item(i);
                Node parent = holder.getParentNode();
                while (parent != null && parent.getNodeType() == Node.ELEMENT_NODE) {
                    if (((Element) parent).getTagName().equals("right_record")) {
                        Element individual = getChildElement(holder);
                        if (individual != null) {
                            String surname = getTextContent(individual, "surname");
                            String name = getTextContent(individual, "name");
                            String patronymic = getTextContent(individual, "patronymic");
                            owners.add(Arrays.asList(surname, name, patronymic));
                        }
                        break;
                    }
                    parent = parent.getParentNode();
                }
            }

        } catch (Exception e) {
            System.out.println("Error parsing owners: " + e.getMessage());
        }
        return owners;
    }

    public static List<List<String>> parseTenants(String xmlFilePath) {
        List<List<String>> tenants = new ArrayList<>();
        try {
            File xmlFile = new File(xmlFilePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dbFactory.setNamespaceAware(true);
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            NodeList restrictRecords = doc.getElementsByTagName("restrict_record");

            for (int i = 0; i < restrictRecords.getLength(); i++) {
                Element restrictRecord = (Element) restrictRecords.item(i);
                NodeList rightHolders = restrictRecord.getElementsByTagName("right_holder");

                for (int j = 0; j < rightHolders.getLength(); j++) {
                    Element holder = (Element) rightHolders.item(j);
                    Element individual = getChildElement(holder);
                    if (individual != null) {
                        String surname = getTextContent(individual, "surname");
                        String name = getTextContent(individual, "name");
                        String patronymic = getTextContent(individual, "patronymic");
                        tenants.add(Arrays.asList(surname, name, patronymic));
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("Error parsing tenants: " + e.getMessage());
        }
        return tenants;
    }

    private static Element getChildElement(Element parent) {
        NodeList children = parent.getElementsByTagName("individual");
        if (children.getLength() == 0) return null;
        return (Element) children.item(0);
    }

    private static String getTextContent(Element parent, String tagName) {
        NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList.getLength() == 0) return "";
        return nodeList.item(0).getTextContent().trim();
    }

    private static String tryGetNameFromVariousPaths(Element restrictRecord) {
        String[] path1 = {"right_holders", "right_holder", "legal_entity", "entity", "resident", "name"};
        return traversePath(restrictRecord, path1);
    }

    private static String traversePath(Element root, String[] path) {
        Element current = root;
        for (String tag : path) {
            NodeList children = current.getElementsByTagName(tag);
            if (children.getLength() == 0) return null;
            current = (Element) children.item(0);
        }
        return current.getTextContent().trim();
    }

    private static Element traverseToElement(Element root, String[] path) {
        Element current = root;
        for (String tag : path) {
            NodeList children = current.getElementsByTagName(tag);
            if (children.getLength() == 0) return null;
            current = (Element) children.item(0);
        }
        return current;
    }

    private static String getTagValue(Element parent) {
        NodeList containers = parent.getElementsByTagName("restriction_encumbrance_type");
        if (containers.getLength() > 0) {
            Element container = (Element) containers.item(0);
            NodeList targets = container.getElementsByTagName("code");
            if (targets.getLength() > 0) {
                return targets.item(0).getTextContent().trim();
            }
        }
        return null;
    }

    public static String parsePeople(List<List<String>> people) {
        List<String> prepareParams = new ArrayList<>();
        List<String> result = new ArrayList<>();
        for (List<String> person : people) {
            if (person == null || person.isEmpty()) continue;
            if (person.get(1).toLowerCase().trim().equals("физическое лицо")) prepareParams.add(person.get(1));
            else prepareParams.add(String.join(" ", person));
        }
        HashSet<String> paramsND = new HashSet<>(prepareParams);
        for (String param : paramsND) {
            if (Collections.frequency(prepareParams, param) > 1) result.add(String.format("%s X %s", param, Collections.frequency(prepareParams, param)));
            else result.add(param);
        }
        return String.join(", ", result);
    }
}
