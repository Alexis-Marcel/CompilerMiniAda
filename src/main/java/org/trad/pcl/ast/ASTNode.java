package org.trad.pcl.ast;

import com.diogonunes.jcolor.Attribute;

import static com.diogonunes.jcolor.Ansi.colorize;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public abstract class ASTNode implements VisitorElement{

    private static boolean isJson = false;

    private int concernedLine = 0;

    public String format(String toFormat) {
        String tab = this.getTab();
        return toFormat.replaceAll("\n", "\n" + tab);
    }

    public void setIsJson(boolean status) {
        isJson = status;
    }

    public String getTab() {
        return "\t".repeat(depth);
    }

    private static int depth = 0;

    public int getConcernedLine() {
        return concernedLine;
    }

    public void setConcernedLine(int concernedLine) {
        this.concernedLine = concernedLine;
    }

    @Override
    public String toString() {
        depth++;
        List<Field> fields = getFields();

        String className = this.getClass().getSimpleName();
        StringBuilder res = new StringBuilder(colorize(className, Attribute.YELLOW_TEXT()) + " : { \n");
        if (isJson) {
            res = new StringBuilder("{ \n");
        }
        int lastIndex = fields.size() - 1;

        for (int i = 0; i < fields.size(); i++) {
            Field field = fields.get(i);
            field.setAccessible(true);
            try {
                Object attributeValue = field.get(this);
                if (attributeValue instanceof String) {
                    attributeValue = colorize("\"" + attributeValue + "\"", Attribute.GREEN_TEXT());
                }
                if (attributeValue == null) {
                    attributeValue = colorize("null", Attribute.BRIGHT_MAGENTA_TEXT());
                }
                if (attributeValue instanceof Character) {
                    res.append("\t").append("\"").append(colorize(field.getName(), Attribute.RED_TEXT())).append("\"").append(" : ");
                    if (isJson) {
                        String escaped = attributeValue.toString().replace("\\", "\\\\").replace("\"", "\\\"");
                        res.append("\"").append(escaped).append("\"");
                    } else {
                        res.append(colorize("'" + attributeValue + "'", Attribute.GREEN_TEXT()));
                    }
                } else res.append("\t").append("\"").append(colorize(field.getName(), Attribute.RED_TEXT())).append("\"").append(" : ").append(attributeValue);
                if (i < lastIndex || !isJson) {
                    res.append(",");
                }
                res.append(" \n");

            } catch (IllegalAccessException e) {
                System.err.println("Erreur lors de l'accès au champ " + field.getName());
            }
        }
        res.append("}");
        depth--;
        return format(res.toString());
    }

    private List<Field> getFields() {
        List<Field> fields = new ArrayList<>(List.of(this.getClass().getDeclaredFields()));

        List<Field> superClassFields = new ArrayList<>(List.of(this.getClass().getSuperclass().getDeclaredFields()));

        superClassFields.forEach(field -> {
            field.setAccessible(true);
            try {
                if (!field.getName().equals("depth") && !field.getName().equals("isJson")) {
                    fields.add(field);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return fields;
    }


}
