package org.example;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

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

                // Получаем code
                String code = getTagValue(restrictRecord);
                if (code == null || code.isEmpty()) continue;

                // Получаем name из различных путей
                String name = tryGetNameFromVariousPaths(restrictRecord);
                if (name != null && !name.isEmpty()) {
                    encumbrancesCodes.put(name, code);
                }

                // Получаем start_date и end_date
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

    private static String tryGetNameFromVariousPaths(Element restrictRecord) {
        // Путь 1: right_holders > right_holder > legal_entity > entity > resident > name
        String[] path1 = {"right_holders", "right_holder", "legal_entity", "entity", "resident", "name"};
        return traversePath(restrictRecord, path1);

        // Можно добавить другие пути здесь при необходимости
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
}
