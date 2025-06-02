package org.example;

import javafx.scene.control.*;
import java.util.*;

class TableSelector {
    public static CheckBoxTreeItem<String> buildTree(List<String> names) {
        CheckBoxTreeItem<String> root = new CheckBoxTreeItem<>("root");

        for (String path : names) {
            String[] parts = path.split("\\.");
            TreeItem<String> current = root;
            boolean flag = true;
            for (String part : parts) {
                Optional<TreeItem<String>> existing = current.getChildren().stream()
                        .filter(c -> c.getValue().equals(part)).findFirst();
                TreeItem<String> finalCurrent = current;
                boolean finalFlag = flag;
                current = existing.orElseGet(() -> {
                    TreeItem<String> child = new CheckBoxTreeItem<>(part);
                    if (finalFlag) child.setExpanded(true);
                    finalCurrent.getChildren().add(child);
                    return child;
                });
                flag = false;
            }
        }
        return root;
    }

    public static List<String> getSelectedTables(CheckBoxTreeItem<String> root) {
        List<String> selected = new ArrayList<>();
        collectSelected(root, "", selected);
        return selected;
    }

    private static void collectSelected(CheckBoxTreeItem<String> node, String path, List<String> selected) {
        String currentPath = path.isEmpty() ? node.getValue() : path + "." + node.getValue();
        if (node.isSelected() && node.getChildren().isEmpty()) {
            selected.add(currentPath.replace("root.", ""));
        }
        for (TreeItem<String> child : node.getChildren()) {
            collectSelected((CheckBoxTreeItem<String>) child, currentPath, selected);
        }
    }
}
